package com.studyforge.interaction.learning.support;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule-based keyword tagging for titles, summaries and MEMORY.md.
 * Maps aliases (Vue, 前端, MySQL...) to canonical tags for overlap scoring.
 */
public final class LearningTextTagger {
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s,，;；、/|]+");
    private static final Pattern MARKDOWN_NOISE = Pattern.compile("[#>*`\\[\\]()\\-]+");

    private static final Map<String, String> KEYWORD_TO_CANONICAL = buildKeywordMap();

    private LearningTextTagger() {
    }

    public static Set<String> extractPostTags(String title, String summary, String aiTags) {
        Set<String> tags = new LinkedHashSet<>();
        tags.addAll(extractTags(title));
        tags.addAll(extractTags(summary));
        tags.addAll(ImportanceScorer.parseTags(aiTags));
        tags.addAll(extractTags(aiTags));
        return tags;
    }

    public static List<InterestTag> buildUserInterestTags(String memoryMd, List<InterestTag> storedTags) {
        Map<String, InterestTag> merged = new LinkedHashMap<>();

        if (storedTags != null) {
            for (InterestTag tag : storedTags) {
                if (tag == null || tag.tag() == null || tag.tag().isBlank()) {
                    continue;
                }
                String canonical = canonicalize(tag.tag());
                merged.putIfAbsent(canonical, new InterestTag(displayLabel(tag.tag(), canonical), tag.weight(), tag.source()));
            }
        }

        for (String extracted : extractTags(memoryMd)) {
            String canonical = canonicalize(extracted);
            merged.merge(
                    canonical,
                    new InterestTag(displayLabel(extracted, canonical), 0.95, "memory"),
                    (left, right) -> new InterestTag(left.tag(), Math.max(left.weight(), right.weight()), preferSource(left.source(), right.source()))
            );
        }

        return merged.values()
                .stream()
                .sorted(Comparator.comparingDouble(InterestTag::weight).reversed())
                .limit(30)
                .toList();
    }

    public static List<InterestTag> memoryDerivedTags(String memoryMd) {
        List<InterestTag> tags = new ArrayList<>();
        for (String extracted : extractTags(memoryMd)) {
            String canonical = canonicalize(extracted);
            tags.add(new InterestTag(displayLabel(extracted, canonical), 0.95, "memory"));
        }
        return tags.stream().limit(20).toList();
    }

    public static Set<String> extractTags(String raw) {
        Set<String> tags = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return tags;
        }

        String normalized = MARKDOWN_NOISE.matcher(raw).replaceAll(" ").toLowerCase(Locale.ROOT);
        String compact = raw.replace('\n', ' ');

        for (Map.Entry<String, String> entry : KEYWORD_TO_CANONICAL.entrySet()) {
            String keyword = entry.getKey();
            if (containsKeyword(normalized, keyword) || containsKeyword(compact, keyword)) {
                tags.add(entry.getValue());
            }
        }

        for (String token : TOKEN_SPLIT.split(normalized)) {
            String trimmed = token.trim();
            if (trimmed.length() < 2) {
                continue;
            }
            String canonical = KEYWORD_TO_CANONICAL.get(trimmed);
            if (canonical != null) {
                tags.add(canonical);
            }
        }

        return filterLanguageTagsByPreference(raw, tags);
    }

    private static Set<String> filterLanguageTagsByPreference(String raw, Set<String> tags) {
        LearningMemoryPreferences preferences = LearningMemoryPreferences.parse(raw);
        if (!preferences.dislikesEnglish()) {
            return tags;
        }
        Set<String> filtered = new LinkedHashSet<>(tags);
        filtered.remove("英文");
        filtered.remove("english");
        return filtered;
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lowered = value.trim().toLowerCase(Locale.ROOT);
        return KEYWORD_TO_CANONICAL.getOrDefault(lowered, lowered);
    }

    private static boolean containsKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        String lowered = text.toLowerCase(Locale.ROOT);
        String key = keyword.toLowerCase(Locale.ROOT);
        if (key.chars().allMatch(Character::isLetter)) {
            if (lowered.contains(key)) {
                return true;
            }
            return Pattern.compile("\\b" + Pattern.quote(key) + "\\b", Pattern.CASE_INSENSITIVE).matcher(lowered).find();
        }
        return lowered.contains(key) || text.contains(keyword);
    }

    private static String displayLabel(String original, String canonical) {
        if (original != null && !original.isBlank() && original.length() <= 24) {
            return original.trim();
        }
        return canonical;
    }

    private static String preferSource(String left, String right) {
        if ("manual".equalsIgnoreCase(left) || "manual".equalsIgnoreCase(right)) {
            return "manual";
        }
        if ("memory".equalsIgnoreCase(left) || "memory".equalsIgnoreCase(right)) {
            return "memory";
        }
        return left == null || left.isBlank() ? right : left;
    }

    private static Map<String, String> buildKeywordMap() {
        Map<String, String> map = new LinkedHashMap<>();
        register(map, "vue", "Vue");
        register(map, "vue3", "Vue");
        register(map, "vue前端", "Vue");
        register(map, "vue 前端", "Vue");
        register(map, "react", "React");
        register(map, "angular", "Angular");
        register(map, "vite", "Vite");
        register(map, "typescript", "TypeScript");
        register(map, "javascript", "JavaScript");
        register(map, "node", "Node.js");
        register(map, "nodejs", "Node.js");
        register(map, "前端", "前端");
        register(map, "frontend", "前端");
        register(map, "web前端", "前端");
        register(map, "spring", "Spring");
        register(map, "springboot", "Spring Boot");
        register(map, "spring boot", "Spring Boot");
        register(map, "mybatis", "MyBatis");
        register(map, "java", "Java");
        register(map, "python", "Python");
        register(map, "go", "Go");
        register(map, "golang", "Go");
        register(map, "后端", "后端");
        register(map, "backend", "后端");
        register(map, "mysql", "数据库");
        register(map, "mariadb", "数据库");
        register(map, "postgresql", "数据库");
        register(map, "postgres", "数据库");
        register(map, "redis", "数据库");
        register(map, "database", "数据库");
        register(map, "sql", "数据库");
        register(map, "数据库", "数据库");
        register(map, "索引", "数据库");
        register(map, "markdown", "Markdown");
        register(map, "git", "Git");
        register(map, "docker", "Docker");
        register(map, "kubernetes", "Kubernetes");
        register(map, "k8s", "Kubernetes");
        register(map, "ai", "AI");
        register(map, "llm", "AI");
        register(map, "面试", "面试");
        register(map, "interview", "面试");
        register(map, "复习", "复习");
        register(map, "review", "复习");
        register(map, "效率", "效率");
        register(map, "productivity", "效率");
        register(map, "中文", "中文");
        register(map, "chinese", "中文");
        register(map, "英文", "英文");
        register(map, "英语", "英文");
        register(map, "english", "英文");
        register(map, "en_us", "英文");
        register(map, "zh_cn", "中文");
        register(map, "技术", "技术实践");
        register(map, "technology", "技术实践");
        register(map, "business", "商业");
        register(map, "商业", "商业");
        register(map, "career", "职业");
        register(map, "职业", "职业");
        register(map, "finance", "财务");
        register(map, "财务", "财务");
        return Map.copyOf(map);
    }

    private static void register(Map<String, String> map, String keyword, String canonical) {
        map.put(keyword.toLowerCase(Locale.ROOT), canonical);
        if (!keyword.equals(keyword.toLowerCase(Locale.ROOT))) {
            map.put(keyword, canonical);
        }
    }
}
