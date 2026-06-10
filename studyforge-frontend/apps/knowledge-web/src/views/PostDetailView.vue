<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import {
  ArrowLeft,
  BookMarked,
  Bookmark,
  Bot,
  CalendarClock,
  Flag,
  Flame,
  Languages,
  MessageSquareText,
  PencilLine,
  Search,
  Send,
  ThumbsUp,
  Volume2
} from '@lucide/vue';
import { generateReviewCards, generateSummary } from '@/api/ai';
import { ApiError } from '@/api/http';
import {
  createComment,
  deleteComment,
  getComments,
  getInteractionState,
  recordPostView,
  reportPost,
  toggleCommentLike,
  toggleFavorite,
  toggleLike
} from '@/api/interactions';
import { getPostDetail } from '@/api/posts';
import { textToSpeech } from '@/api/voice';
import EmptyState from '@/components/EmptyState.vue';
import ForumThreadItem from '@/components/ForumThreadItem.vue';
import LoadingState from '@/components/LoadingState.vue';
import MentionTextarea from '@/components/MentionTextarea.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { usePreferencesStore } from '@/stores/preferences';
import { useSessionStore } from '@/stores/session';
import type { AiResult, CommentItem, PostDetail, PostInteractionState, VoiceResult } from '@/types/api';
import type { ForumThreadNode } from '@/types/forum';
import { formatDateTime, formatShortDateTime } from '@/utils/date';

const route = useRoute();
const preferencesStore = usePreferencesStore();
const sessionStore = useSessionStore();
const post = ref<PostDetail | null>(null);
const comments = ref<CommentItem[]>([]);
const interaction = ref<PostInteractionState | null>(null);
const aiSummary = ref<AiResult | null>(null);
const reviewCards = ref<AiResult | null>(null);
const voice = ref<VoiceResult | null>(null);
const newComment = ref('');
const replyComment = ref('');
const replyingToComment = ref<CommentItem | null>(null);
const reportReason = ref('');
const reportSuccess = ref('');
const aiLoading = ref('');
const reportLoading = ref(false);
const actionError = ref('');
const loading = ref(false);
const errorMessage = ref('');
const postId = computed(() => String(route.params.postId));
const requestedLanguage = computed(() => (typeof route.query.language === 'string' ? route.query.language : preferencesStore.languageCode));
const postCreatedTime = computed(() => formatDateTime(post.value?.createdTime, preferencesStore.languageCode));
const postUpdatedTime = computed(() => formatDateTime(post.value?.updatedTime, preferencesStore.languageCode));
const commentTree = computed(() => buildCommentTree(comments.value));
const relatedSearchTitle = computed(() => (preferencesStore.languageCode === 'en_US' ? 'Related searches' : '相关搜索'));
const relatedSearchTerms = computed(() => {
  if (!post.value) {
    return [];
  }

  const sourceText = [post.value.title, post.value.summary, post.value.categoryCode.replace(/_/g, ' '), post.value.authorName]
    .filter(Boolean)
    .join(' ');

  const phraseCandidates = sourceText
    .split(/[，。！？；、,.!?;:()\[\]{}<>《》“”"']/)
    .map((item) => item.trim())
    .filter((item) => item.length >= 2 && item.length <= 20);

  const tokenCandidates = [
    ...(sourceText.match(/[A-Za-z][A-Za-z0-9+#.-]{2,}/g) ?? []),
    ...(sourceText.match(/[\u4e00-\u9fff]{2,10}/g) ?? [])
  ];

  const stopWords = new Set([
    '文章',
    '内容',
    '相关',
    '学习',
    '知识',
    'today',
    'about',
    'article',
    'post'
  ]);

  const unique = new Set<string>();
  const terms: string[] = [];

  for (const raw of [...phraseCandidates, ...tokenCandidates]) {
    const term = raw.replace(/\s+/g, ' ').trim();
    const key = term.toLowerCase();
    if (!term || term.length < 2 || stopWords.has(key) || unique.has(key)) {
      continue;
    }
    unique.add(key);
    terms.push(term);
    if (terms.length >= 8) {
      break;
    }
  }

  return terms;
});

async function loadDetail() {
  loading.value = true;
  errorMessage.value = '';
  actionError.value = '';

  try {
    post.value = await getPostDetail(postId.value, requestedLanguage.value);
    comments.value = await getComments(postId.value);
    await loadInteractionState();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '这篇内容暂时没取到';
  } finally {
    loading.value = false;
  }
}

async function loadInteractionState() {
  if (!sessionStore.isAuthenticated) {
    interaction.value = null;
    return;
  }

  try {
    interaction.value = await getInteractionState(postId.value);
    await recordPostView(postId.value);
  } catch (error) {
    interaction.value = null;

    if (error instanceof ApiError && error.code === 4010) {
      actionError.value = '登录状态已过期，文章可以继续阅读，互动功能请重新登录后使用。';
      void sessionStore.logout().catch(() => undefined);
    }
  }
}

onMounted(loadDetail);

watch(
  () => [postId.value, requestedLanguage.value],
  () => loadDetail()
);

async function runAuthenticated<T>(task: () => Promise<T>) {
  actionError.value = '';
  if (!sessionStore.isAuthenticated) {
    actionError.value = '请先登录再使用这个功能';
    return null;
  }

  try {
    return await task();
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : '操作暂时没有成功';
    return null;
  }
}

async function likePost() {
  const next = await runAuthenticated(() => toggleLike(postId.value));
  if (next) {
    interaction.value = next;
  }
}

async function favoritePost() {
  const next = await runAuthenticated(() => toggleFavorite(postId.value));
  if (next) {
    interaction.value = next;
  }
}

async function submitComment() {
  const content = newComment.value.trim();
  if (!content) {
    return;
  }

  const comment = await runAuthenticated(() => createComment(postId.value, content, post.value?.languageCode ?? preferencesStore.languageCode));
  if (comment) {
    upsertComment(comment);
    newComment.value = '';
    interaction.value = interaction.value
      ? { ...interaction.value, commentCount: interaction.value.commentCount + 1 }
      : interaction.value;
  }
}

async function submitReply() {
  const parent = replyingToComment.value;
  const content = replyComment.value.trim();
  if (!parent || !content) {
    return;
  }

  const comment = await runAuthenticated(() =>
    createComment(postId.value, content, post.value?.languageCode ?? preferencesStore.languageCode, parent.commentId)
  );
  if (comment) {
    upsertComment(comment);
    replyComment.value = '';
    replyingToComment.value = null;
    interaction.value = interaction.value
      ? { ...interaction.value, commentCount: interaction.value.commentCount + 1 }
      : interaction.value;
  }
}

function startReply(node: ForumThreadNode) {
  const source = comments.value.find((item) => item.commentId === node.id);
  if (!source || source.deleted) {
    return;
  }
  replyingToComment.value = source;
  replyComment.value = source.authorUsername ? `@${source.authorUsername} ` : '';
}

function cancelReply() {
  replyingToComment.value = null;
  replyComment.value = '';
}

async function likeComment(node: ForumThreadNode) {
  const updated = await runAuthenticated(() => toggleCommentLike(postId.value, node.id));
  if (updated) {
    upsertComment(updated);
  }
}

async function removeComment(node: ForumThreadNode): Promise<void> {
  if (!window.confirm('确定删除这条评论吗？删除后会保留楼层和回复关系。')) {
    return;
  }
  const result = await runAuthenticated(() => deleteComment(postId.value, node.id));
  if (result !== null) {
    comments.value = comments.value.map((comment) =>
      comment.commentId === node.id
        ? { ...comment, status: 'DELETED', deleted: true, canDelete: false, likedByViewer: false, content: '这条评论已删除' }
        : comment
    );
    interaction.value = interaction.value
      ? { ...interaction.value, commentCount: Math.max(0, interaction.value.commentCount - 1) }
      : interaction.value;
  }
}

function upsertComment(comment: CommentItem) {
  if (comments.value.some((item) => item.commentId === comment.commentId)) {
    comments.value = comments.value.map((item) => (item.commentId === comment.commentId ? comment : item));
  } else {
    comments.value = [...comments.value, comment].sort((a, b) => (a.floorNo || 0) - (b.floorNo || 0));
  }
}

async function submitReport() {
  const reason = reportReason.value.trim();
  if (!reason) {
    actionError.value = '请写下举报原因，方便管理员判断';
    return;
  }

  reportLoading.value = true;
  reportSuccess.value = '';
  const result = await runAuthenticated(() => reportPost(postId.value, reason));
  if (result) {
    reportReason.value = '';
    reportSuccess.value = `举报已提交，编号 #${result.reportId}`;
  }
  reportLoading.value = false;
}

async function createSummary() {
  aiLoading.value = 'summary';
  const result = await runAuthenticated(() =>
    generateSummary(postId.value, post.value?.languageCode ?? requestedLanguage.value, preferencesStore.languageCode)
  );
  if (result) {
    aiSummary.value = result;
  }
  aiLoading.value = '';
}

async function createReviewCards() {
  aiLoading.value = 'cards';
  const result = await runAuthenticated(() =>
    generateReviewCards(postId.value, post.value?.languageCode ?? requestedLanguage.value, preferencesStore.languageCode)
  );
  if (result) {
    reviewCards.value = result;
  }
  aiLoading.value = '';
}

async function playVoice() {
  if (!post.value) {
    return;
  }
  aiLoading.value = 'voice';
  const result = await runAuthenticated(() => textToSpeech(postId.value, `${post.value?.title}\n${post.value?.summary}`, post.value?.languageCode ?? 'zh_CN'));
  if (result) {
    voice.value = result;
    setTimeout(() => {
      const audio = document.querySelector<HTMLAudioElement>('#post-audio-player');
      audio?.play();
    }, 50);
  }
  aiLoading.value = '';
}

function commentTime(comment: CommentItem) {
  return formatShortDateTime(comment.createdTime, preferencesStore.languageCode);
}

function buildCommentTree(items: CommentItem[]): ForumThreadNode[] {
  const nodes = new Map<number, ForumThreadNode>();
  const roots: ForumThreadNode[] = [];

  for (const comment of [...items].sort((a, b) => (a.floorNo || 0) - (b.floorNo || 0))) {
    const node: ForumThreadNode = {
      id: comment.commentId,
      userId: comment.userId,
      authorUsername: comment.authorUsername || `user_${comment.userId}`,
      authorName: comment.authorName || comment.authorUsername || `#${comment.userId}`,
      authorAvatarUrl: comment.authorAvatarUrl,
      parentAuthorName: comment.parentAuthorName,
      content: comment.content,
      floorNo: comment.floorNo,
      likeCount: comment.likeCount,
      likedByViewer: comment.likedByViewer,
      canDelete: comment.canDelete,
      deleted: comment.deleted,
      createdLabel: commentTime(comment),
      replies: []
    };
    nodes.set(comment.commentId, node);
  }

  // 构建树形结构（内部使用 parentId，但不暴露给外部）
  for (const comment of items) {
    const node = nodes.get(comment.commentId);
    if (!node) continue;
    
    if (comment.parentCommentId && nodes.has(comment.parentCommentId)) {
      nodes.get(comment.parentCommentId)?.replies.push(node);
    } else {
      roots.push(node);
    }
  }

  return roots;
}
</script>

<template>
  <section class="detail-page">
    <RouterLink class="secondary-button return-link" to="/knowledge">
      <ArrowLeft :size="17" />
      <span>返回知识流</span>
    </RouterLink>

    <LoadingState v-if="loading" label="正在打开文章" />
    <EmptyState v-else-if="errorMessage" title="文章暂时打不开" :description="errorMessage" />

    <article v-else-if="post" class="article-layout">
      <div class="article-main">
        <div class="article-meta">
          <span class="score-pill">
            <Flame :size="15" />
            {{ post.hotScore.toFixed(1) }}
          </span>
          <span>
            <Languages :size="15" />
            {{ post.languageCode }}
          </span>
          <span v-if="postCreatedTime">
            <CalendarClock :size="15" />
            {{ postCreatedTime }}
          </span>
          <span>{{ post.categoryCode }}</span>
          <span>{{ interaction?.viewCount ?? post.viewCount }} 次阅读</span>
        </div>

        <h1>{{ post.title }}</h1>
        <p class="article-summary">{{ post.summary }}</p>

        <img v-if="post.coverImageUrl" class="article-cover" :src="post.coverImageUrl" alt="" />

        <div class="article-content">
          <MarkdownRenderer :content="post.content" />
        </div>

        <section v-if="relatedSearchTerms.length" class="related-search-section">
          <div class="panel-title">
            <Search :size="18" />
            <span>{{ relatedSearchTitle }}</span>
          </div>
          <div class="related-search-list">
            <RouterLink
              v-for="term in relatedSearchTerms"
              :key="term"
              class="related-search-link"
              :to="{ path: '/knowledge', query: { q: term } }"
            >
              {{ term }}
            </RouterLink>
          </div>
        </section>

        <div class="article-actions">
          <RouterLink v-if="sessionStore.isAuthenticated && post.authorId === sessionStore.userId" class="secondary-button" :to="`/posts/${post.postId}/edit`">
            <PencilLine :size="17" />
            <span>编辑文章</span>
          </RouterLink>
          <button class="secondary-button" type="button" @click="likePost">
            <ThumbsUp :size="17" />
            <span>{{ interaction?.liked ? '已赞' : '点赞' }} {{ interaction?.likeCount ?? post.likeCount }}</span>
          </button>
          <button class="secondary-button" type="button" @click="favoritePost">
            <Bookmark :size="17" />
            <span>{{ interaction?.favorited ? '已收藏' : '收藏' }} {{ interaction?.favoriteCount ?? post.favoriteCount }}</span>
          </button>
          <button class="secondary-button" type="button" :disabled="aiLoading === 'voice'" @click="playVoice">
            <Volume2 :size="17" />
            <span>{{ aiLoading === 'voice' ? '生成中' : '朗读摘要' }}</span>
          </button>
        </div>

        <audio v-if="voice" id="post-audio-player" class="audio-player" controls :src="voice.audioDataUrl" />
        <p v-if="actionError" class="form-error">{{ actionError }}</p>

        <section class="comment-section">
          <div class="panel-title">
            <MessageSquareText :size="18" />
            <span>讨论</span>
          </div>
          <div v-if="commentTree.length" class="comment-list forum-thread-list">
            <ForumThreadItem
              v-for="comment in commentTree"
              :key="comment.id"
              :node="comment"
              :replying-to-id="replyingToComment?.commentId ?? null"
              :reply-text="replyComment"
              reply-placeholder="写下回复，输入 @用户名 可以提醒对方"
              submit-label="发送回复"
              @reply="startReply"
              @cancel-reply="cancelReply"
              @update-reply-text="replyComment = $event"
              @submit-reply="submitReply"
              @like="likeComment"
              @delete="removeComment"
            />
          </div>
          <EmptyState v-else title="还没有讨论" description="读完后可以留下一个问题或补充。" />
          <form class="comment-form" @submit.prevent="submitComment">
            <MentionTextarea v-model="newComment" rows="3" placeholder="写下你的问题、补充或读后想法，输入 @ 可以选择好友" />
            <button class="primary-button" type="submit">
              <Send :size="17" />
              <span>发送</span>
            </button>
          </form>
        </section>
      </div>

      <aside class="article-side">
        <section class="side-panel assistant-tools">
          <div class="panel-title">
            <Bot :size="18" />
            <span>AI 学习助手</span>
          </div>
          <button class="primary-button full-width" type="button" :disabled="aiLoading === 'summary'" @click="createSummary">
            {{ aiLoading === 'summary' ? '正在整理' : '生成摘要' }}
          </button>
          <button class="secondary-button full-width" type="button" :disabled="aiLoading === 'cards'" @click="createReviewCards">
            {{ aiLoading === 'cards' ? '正在生成' : '生成复习卡片' }}
          </button>
          <MarkdownRenderer v-if="aiSummary" class="ai-output" :content="aiSummary.text" />
          <MarkdownRenderer v-if="reviewCards" class="ai-output" :content="reviewCards.text" />
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <BookMarked :size="18" />
            <span>文章信息</span>
          </div>
          <dl class="info-list">
            <div>
              <dt>作者</dt>
              <dd>
                <RouterLink class="text-link" :to="`/users/${post.authorId}`">
                  {{ post.authorName || `#${post.authorId}` }}
                </RouterLink>
              </dd>
            </div>
            <div>
              <dt>编号</dt>
              <dd>#{{ post.postId }}</dd>
            </div>
            <div>
              <dt>发布</dt>
              <dd>{{ postCreatedTime || '-' }}</dd>
            </div>
            <div>
              <dt>更新</dt>
              <dd>{{ postUpdatedTime || '-' }}</dd>
            </div>
            <div>
              <dt>评论</dt>
              <dd>{{ interaction?.commentCount ?? post.commentCount }}</dd>
            </div>
          </dl>
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <Flag :size="18" />
            <span>举报文章</span>
          </div>
          <form class="compact-form" @submit.prevent="submitReport">
            <textarea v-model.trim="reportReason" rows="4" placeholder="说明哪里可能有问题，例如广告、误导、攻击或其他不适合社区的内容" />
            <button class="secondary-button full-width" type="submit" :disabled="reportLoading">
              {{ reportLoading ? '提交中' : '提交举报' }}
            </button>
            <p v-if="reportSuccess" class="form-success">{{ reportSuccess }}</p>
          </form>
        </section>

        <section class="side-panel">
          <div class="panel-title">
            <MessageSquareText :size="18" />
            <span>读完可以做</span>
          </div>
          <p>用三句话写下核心结论，再列出一个还想追问的问题。</p>
        </section>
      </aside>
    </article>
  </section>
</template>

<style scoped>
.related-search-section {
  margin: 1.5rem 0;
  padding: 1rem;
  border: 1px solid var(--line-color, #e2e8f0);
  border-radius: 0.9rem;
  background: var(--surface-muted, #f8fafc);
}

.related-search-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin-top: 0.8rem;
}

.related-search-link {
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  border: 1px solid var(--line-color, #cbd5e1);
  background: var(--surface-color, #ffffff);
  color: var(--text-strong, #0f172a);
  text-decoration: none;
  font-size: 0.88rem;
}

.related-search-link:hover {
  border-color: var(--brand-color, #0f766e);
  color: var(--brand-color, #0f766e);
}
</style>
