# User Learning Memory — Design Spec

**Date:** 2026-06-03  
**Status:** Approved (conversation) — pending written spec review  
**Scope:** Shared user profile for favorite importance ranking and AI context injection

---

## 1. Problem

Users accumulate many saved posts in favorite collections. The current list is ordered by `favorite_collection_items.created_time DESC` only, so high-value items get buried. AI features (summary, review cards, Q&A) do not use any persisted user preference context.

We need a product-level **Learning Memory** layer — analogous to a `MEMORY.md` for the model — that:

1. **Ranks favorites by personal importance** (tag/keyword-aware, behavior-informed).
2. **Feeds the same profile into AI prompts** so outputs align with what the user cares about.

---

## 2. Goals

| ID | Goal |
|----|------|
| G1 | Default favorite collection view sorted by **importance**, with fallback to **recent** |
| G2 | Single source of truth: `user_learning_profiles` used by both ranking and AI |
| G3 | Hybrid scoring: explicit rules (tags, behavior, pin) + optional vector similarity (phase 2) |
| G4 | User can **view and edit** their memory; transparent and correctable |
| G5 | Privacy: memory visible only to the owner; deletable on account removal |

## 3. Non-Goals (this spec)

- Full-site recommendation feed reordering
- Cross-language semantic search (separate future work per existing docs)
- Replacing `post_i18n.ai_tags` with a normalized tag table (MVP keeps comma-separated tags)
- Real-time embedding on every page view (lazy load + cache only)

---

## 4. Current System Context

| Area | Today |
|------|--------|
| Favorites | `favorite_collections`, `favorite_collection_items`; sort by item `created_time DESC` |
| Tags | `post_i18n.ai_tags` (VARCHAR 500); `AiService.generateTags()` exists but not wired to publish flow everywhere |
| User prefs | Frontend `languageCode` in localStorage only |
| AI | `AiController` calls `generateSummary` / `generateQuiz` / `answerQuestion` with post content only — no user context |
| Behavior | `post_view_history`, `ai_logs` (SUMMARY, REVIEW_CARD, QUESTION), `post_favorites` |

Relevant query today (`PostMapper.xml`):

```sql
ORDER BY i.created_time DESC
```

---

## 5. Recommended Approach: Hybrid (Rules + LLM Memory + Phase-2 Vectors)

### 5.1 Why hybrid

| Approach | Verdict |
|----------|---------|
| Rules + tags only | Fast and explainable but misses semantic similarity |
| Vectors only | Good recall, weak cold-start and behavior signals |
| **Hybrid (chosen)** | Rules for MVP + explainability; LLM for compact memory text; vectors added in V1.2 without blocking launch |

---

## 6. Data Model

### 6.1 New table: `user_learning_profiles`

One row per user.

```sql
CREATE TABLE IF NOT EXISTS user_learning_profiles (
    user_id BIGINT UNSIGNED PRIMARY KEY,
    memory_md TEXT NULL COMMENT 'Human/LLM-readable learning memory (~800 chars max target)',
    interest_tags JSON NULL COMMENT '[{"tag":"Spring","weight":0.82,"source":"auto|manual"}, ...]',
    embedding_json MEDIUMTEXT NULL COMMENT 'Phase 2: JSON array of floats',
    profile_version INT UNSIGNED NOT NULL DEFAULT 1,
    ai_memory_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0 = AI prompts skip memory',
    last_refreshed_at DATETIME NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_learning_profiles_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Per-user learning memory and interest profile';
```

### 6.2 Extend: `favorite_collection_items`

```sql
ALTER TABLE favorite_collection_items
    ADD COLUMN importance_score DECIMAL(8,4) NOT NULL DEFAULT 0.0000 AFTER created_time,
    ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 AFTER importance_score,
    ADD COLUMN score_factors JSON NULL COMMENT 'Optional rank explanation for UI' AFTER pinned;

CREATE INDEX idx_favorite_items_collection_importance
    ON favorite_collection_items (collection_id, pinned DESC, importance_score DESC, created_time DESC);
```

**Pin semantics:** `pinned = 1` items sort above all non-pinned items regardless of score. Among pinned items, sort by `importance_score DESC`, then `created_time DESC`.

### 6.3 Phase 2 (optional): `post_embeddings`

Deferred until V1.2. Store lazy-computed embeddings keyed by `(post_id, language_code)` with text fingerprint to invalidate on content change.

---

## 7. Interest Tags & Memory Content

### 7.1 `interest_tags` JSON schema

```json
[
  { "tag": "Spring", "weight": 0.82, "source": "auto" },
  { "tag": "MyBatis", "weight": 0.65, "source": "manual" }
]
```

- Weights normalized to `[0, 1]` internally; max ~30 tags retained (top by weight).
- Manual edits merge with auto tags; manual tags get `source: "manual"` and are not removed by auto refresh unless user clears them.

### 7.2 `memory_md` format (target)

Plain markdown, ≤ ~800 characters, example:

```markdown
## 近期学习重点
- 主攻 Java 后端：Spring Boot、MyBatis
- 偏好实战教程与可落地的代码示例

## 收藏习惯
- 数据库与性能优化类文章权重高
- 常对收藏文生成复习卡片

## 备注
- 用户手动补充：正在准备面试
```

Used for: UI display, LLM profile refresh input/output, AI prompt injection.

---

## 8. Scoring Model

### 8.1 Signals

| Signal | Source | Notes |
|--------|--------|-------|
| Tag overlap | `post_i18n.ai_tags` ∩ `interest_tags` | Tokenize tags by comma; case-insensitive |
| Category match | `posts.category_id` vs user's top categories from history/favorites | Frequency-based |
| View depth | `post_view_history` count for `(user_id, post_id)` | Cap contribution (e.g. max 5 views) |
| AI engagement | `ai_logs` where `ai_type IN ('SUMMARY','REVIEW_CARD','QUESTION')` | Strong positive |
| Multi-collection | Item appears in >1 collection for same user | Moderate positive |
| Recency | `favorite_collection_items.created_time` | Mild boost, decays over ~90 days |
| Pin | `pinned = 1` | Always first tier |

### 8.2 Phase 1 formula (no vectors)

All sub-scores normalized to `[0, 1]`:

```
importance_score =
    0.45 * tagMatchScore
  + 0.25 * behaviorScore
  + 0.15 * categoryMatchScore
  + 0.10 * recencyBoost
  + pinTierBonus   -- pinned items: sort key override, score stored for display
```

**`tagMatchScore`:** Jaccard-like overlap weighted by user tag weights, or sum of matched tag weights / max possible.

**`behaviorScore`:** Weighted sum of normalized view count, AI log presence, multi-collection flag.

**`recencyBoost`:** `exp(-daysSinceSaved / 90)` or linear decay.

**Recompute triggers:**

- On favorite add/remove, pin toggle, profile refresh
- Batch recompute for all items in affected collection(s)
- Async job acceptable; UI may show previous scores until refresh completes (< few seconds target)

### 8.3 Phase 2 addition (vectors)

```
importance_score += 0.20 * cosineSimilarity(user_interest_embedding, post_embedding)
```

Re-normalize weights so total coefficients sum to 1.0 after adding vector term (adjust rule weights proportionally).

User embedding: computed from `memory_md` + top tags via SiliconFlow embeddings API, cached in `embedding_json`.

Post embedding: lazy on first importance sort or on favorite add; cache in `post_embeddings`; input text = `title + summary + ai_tags` for requested `languageCode`.

---

## 9. Memory Refresh (LLM)

### 9.1 Service method

`UserLearningProfileService.refreshMemoryMd(userId)`

**Input bundle (structured, not raw posts):**

- Top 15 tags from behavior aggregation (favorites, views, AI logs)
- Top 10 favorite post titles (by current importance score)
- Top 3 categories by frequency
- Previous `memory_md` (if any)
- Manual `interest_tags` with `source=manual`

**LLM prompt output:** JSON with fields `{ "memory_md": "...", "interest_tags": [...] }` — parsed and validated; on parse failure, keep previous profile.

**Constraints in prompt:**

- Max 800 chars for `memory_md`
- 4–12 auto tags with weights
- Do not invent topics with no signal evidence
- Preserve manual tags and user-edited paragraphs when still relevant

### 9.2 Triggers & debouncing

| Trigger | Action |
|---------|--------|
| Favorite add/remove | Recompute item scores immediately; queue memory refresh (debounced 5 min) |
| Pin toggle | Recompute sort immediately; optional memory refresh if debounce window open |
| AI log write | Increment behavior signal; debounced memory refresh |
| Daily first login | Refresh if `last_refreshed_at` older than 24h |
| `POST /users/me/learning-memory/refresh` | Immediate refresh; rate limit 3/hour/user |

**MVP fallback:** If LLM unavailable, build initial `memory_md` from rule-only template (tag list + category names) so ranking and AI still work.

---

## 10. API Design

Base path: `/api/v1` (consistent with existing controllers).

### 10.1 Learning memory

| Method | Path | Description |
|--------|------|-------------|
| GET | `/users/me/learning-memory` | Returns `memoryMd`, `interestTags`, `aiMemoryEnabled`, `lastRefreshedAt`, `profileVersion` |
| PUT | `/users/me/learning-memory` | Body: `{ memoryMd?, interestTags?, aiMemoryEnabled? }` — manual edits |
| POST | `/users/me/learning-memory/refresh` | Force LLM refresh; returns updated profile |

**Response VO example:**

```json
{
  "memoryMd": "## 近期学习重点\n...",
  "interestTags": [{ "tag": "Spring", "weight": 0.82, "source": "auto" }],
  "aiMemoryEnabled": true,
  "lastRefreshedAt": "2026-06-03T10:00:00",
  "profileVersion": 3
}
```

### 10.2 Favorite collection posts (extended)

**Existing:** `GET /collections/{collectionId}/posts?languageCode=zh_CN&limit=30`

**New query params:**

| Param | Default | Values |
|-------|---------|--------|
| `sort` | `importance` | `importance`, `recent` |
| `tag` | — | Optional filter: posts whose `ai_tags` contain tag (case-insensitive) |

**Extended `PostSummaryVO` fields (when sort=importance):**

```json
{
  "importanceScore": 0.78,
  "pinned": false,
  "rankReasons": ["标签匹配: Spring", "你生成过复习卡片"]
}
```

### 10.3 Favorite item actions

| Method | Path | Description |
|--------|------|-------------|
| PATCH | `/collections/{collectionId}/posts/{postId}` | Body: `{ pinned?: boolean }` |

Pin/unpin triggers score resort for that collection.

---

## 11. AI Integration

### 11.1 Context injection

New helper: `UserLearningProfileService.getMemoryContext(userId)` returns empty string if profile missing or `aiMemoryEnabled = false`.

Inject into prompts for:

- `POST /ai/posts/{postId}/summary`
- `POST /ai/posts/{postId}/review-cards`
- `POST /ai/posts/{postId}/questions`

**Prompt appendix (zh_CN example):**

```
【用户学习记忆】
{memory_md}

【近期关注标签】
{comma-separated top 8 tags by weight}

请结合上述用户背景，让输出更贴合其学习重点。若与正文无关，忽略记忆部分。
```

English prompts use equivalent English section headers when `promptLanguage` is `en_US`.

### 11.2 Token budget

- Cap injected memory at ~600 tokens (~800 chars `memory_md` + tag line).
- Never inject full favorite post lists or raw view history.

### 11.3 AiService changes

Option A (preferred): extend method signatures with optional `userContext` string.  
Option B: wrap at controller level by prepending context to content argument.

Use Option A for clarity and testability.

---

## 12. Frontend (knowledge-web)

### 12.1 Favorites page (`/favorites`)

- Default sort: **对我更重要** (`sort=importance`)
- Toggle: **最近收藏** (`sort=recent`)
- Optional tag filter chips derived from user's `interestTags` (top 8)
- Per-card: pin button, optional collapsible **为何靠前** using `rankReasons`
- Empty state unchanged for unauthenticated users

### 12.2 Learning preferences UI

New section under user settings or `/me`:

- View/edit `memory_md` (textarea, markdown preview optional in V1.1)
- Edit tag list (add/remove manual tags)
- Toggle: **AI 使用我的学习记忆**
- Button: **刷新学习画像** → calls refresh API
- Link from favorites page: "管理学习偏好"

### 12.3 API client

New module: `src/api/learningMemory.ts`  
Extend: `src/api/collections.ts` with `sort`, `tag`, pin PATCH.

---

## 13. Backend Module Structure

```
studyforge-learning-memory/   (new Maven module, or package under studyforge-ai)
  entity/UserLearningProfile
  mapper/UserLearningProfileMapper
  service/UserLearningProfileService
  service/FavoriteImportanceService
  vo/LearningMemoryVO
  dto/UpdateLearningMemoryRequest

Dependencies:
  studyforge-content (posts, tags, categories)
  studyforge-interaction (favorites)
  studyforge-ai (LLM refresh, phase-2 embeddings)
  studyforge-system (auth)
```

**Hook points:**

| Event | Handler |
|-------|---------|
| `FavoriteCollectionService.addPost/removePost` | Recompute scores for user+collection |
| View history insert | Update behavior aggregates (async) |
| `AiController.log` after success | Behavior signal + debounced refresh |
| Login reward flow (optional) | Daily refresh check |

Use Spring `@Async` or lightweight in-process queue for debounced LLM refresh; no new infra for MVP.

---

## 14. Privacy & Safety

- `GET /users/me/learning-memory` requires auth; no public endpoint for other users' memory.
- `memory_md` must not store passwords, tokens, or PII beyond learning topics.
- User can disable AI memory without disabling importance sorting.
- **Clear profile:** `PUT` with empty `memoryMd` and `interestTags: []` or dedicated delete action in V1.1.
- Cascade delete on `users` FK.

---

## 15. Error Handling

| Scenario | Behavior |
|----------|----------|
| LLM refresh fails | Keep previous profile; log error; return 503 on manual refresh with message |
| No profile row yet | Lazy-create on first favorite or first AI call; rule-only initial profile |
| Missing `ai_tags` on post | Tag score = 0; other signals still apply |
| Score recompute partial failure | Fall back to `recent` sort for affected request; log |

---

## 16. Testing Plan

### 16.1 Unit

- Tag overlap scoring with comma-separated `ai_tags`
- Pin overrides sort order
- Recency decay monotonicity
- Manual tag merge on profile update

### 16.2 Integration

- Add favorite → scores computed → `GET .../posts?sort=importance` order changes
- AI summary request includes memory when enabled, excludes when disabled
- Refresh endpoint rate limit

### 16.3 Manual QA

- User with 20+ favorites: pin 2 items, verify they stay on top
- Edit memory manually, regenerate summary, verify tone/focus shift
- Toggle sort recent vs importance

---

## 17. Rollout Phases

| Phase | Deliverables | Dependency |
|-------|--------------|------------|
| **MVP** | Tables, rule scoring, importance sort, pin, AI context injection, rule-only initial memory | None |
| **V1.1** | LLM refresh, settings UI edit, rank reasons in API, manual refresh rate limit | SiliconFlow text API |
| **V1.2** | Embeddings table, vector term in score, lazy post embedding cache | SiliconFlow embeddings API |

MVP must ship with **working importance sort** even if LLM refresh is stubbed to rule-template.

---

## 18. Open Questions (resolved)

| Question | Decision |
|----------|----------|
| User-visible MEMORY? | Yes — view/edit in settings |
| Default favorite sort? | `importance` with recent toggle |
| Shared profile for A+B? | Yes — single `user_learning_profiles` |

---

## 19. Success Metrics

- Users with ≥10 favorites use importance sort (telemetry: `sort` param ratio)
- Reduced time-to-open pinned/high-score items (optional analytics later)
- AI feature repeat usage after memory enabled vs disabled (A/B optional future)

---

## 20. References

- `sql/001_schema.sql` — favorites, `post_i18n.ai_tags`, `ai_logs`, `post_view_history`
- `PostMapper.xml` — `selectFavoriteCollectionByUser`
- `AiController.java` — current AI endpoints without user context
- `SiliconFlowAiServiceImpl.java` — existing LLM integration pattern

---

*End of spec.*
