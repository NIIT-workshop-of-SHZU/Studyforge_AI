package com.studyforge.interaction.learning.support;

import com.studyforge.interaction.learning.model.ImportanceResult;
import com.studyforge.interaction.learning.model.InterestTag;
import com.studyforge.interaction.learning.model.SemanticTag;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ImportanceScorer {
    private ImportanceScorer() {
    }

    public static ImportanceResult score(String title,
                                         String summary,
                                         String aiTags,
                                         String originalLanguage,
                                         String memoryMd,
                                         boolean memoryPrioritized,
                                         Long categoryId,
                                         Set<Long> topCategoryIds,
                                         List<InterestTag> interestTags,
                                         int viewCount,
                                         boolean aiEngaged,
                                         int collectionCount,
                                         LocalDateTime savedAt) {
        return score(
                title,
                summary,
                aiTags,
                originalLanguage,
                memoryMd,
                memoryPrioritized,
                categoryId,
                topCategoryIds,
                interestTags,
                List.of(),
                List.of(),
                viewCount,
                aiEngaged,
                collectionCount,
                savedAt
        );
    }

    public static ImportanceResult score(String title,
                                         String summary,
                                         String aiTags,
                                         String originalLanguage,
                                         String memoryMd,
                                         boolean memoryPrioritized,
                                         Long categoryId,
                                         Set<Long> topCategoryIds,
                                         List<InterestTag> interestTags,
                                         List<SemanticTag> userSemanticTags,
                                         List<SemanticTag> postSemanticTags,
                                         int viewCount,
                                         boolean aiEngaged,
                                         int collectionCount,
                                         LocalDateTime savedAt) {
        SemanticTagMatcher.MatchResult semanticMatch = SemanticTagMatcher.match(userSemanticTags, postSemanticTags);
        boolean hasSemantic = userSemanticTags != null && !userSemanticTags.isEmpty()
                && postSemanticTags != null && !postSemanticTags.isEmpty();

        Set<String> postTags = LearningTextTagger.extractPostTags(title, summary, aiTags);
        List<InterestTag> profileTags = LearningTextTagger.buildUserInterestTags(memoryMd, interestTags);
        List<InterestTag> scoringTags = selectScoringTags(profileTags, memoryMd, memoryPrioritized);

        double tagMatch = tagMatchScore(postTags, scoringTags);
        double memoryMatch = memoryKeywordScore(title, summary, memoryMd);
        double directMemory = directMemoryInterestScore(title, summary, memoryMd);
        double ruleSemantic = Math.max(Math.max(tagMatch, memoryMatch * 0.95), directMemory);
        double semantic = hasSemantic ? semanticMatch.score() : ruleSemantic;

        LearningMemoryPreferences preferences = LearningMemoryPreferences.parse(memoryMd);
        double languageFit = hasSemantic
                ? languageFitFromSemanticTags(userSemanticTags, originalLanguage, preferences)
                : languageFitScore(originalLanguage, preferences);
        double behavior = behaviorScore(viewCount, aiEngaged, collectionCount);
        double category = categoryMatchScore(categoryId, topCategoryIds);
        double recency = recencyBoost(savedAt);

        double behaviorAdjusted = behavior * (0.2 + 0.8 * semantic);
        double total = hasSemantic
                ? 0.78 * semantic + 0.12 * languageFit + 0.07 * behaviorAdjusted + 0.03 * recency
                : 0.48 * semantic + 0.27 * languageFit + 0.15 * behaviorAdjusted + 0.07 * category + 0.03 * recency;
        total = Math.max(0.0, Math.min(1.0, total));

        List<String> reasons = new ArrayList<>();
        if (hasSemantic && semanticMatch.score() >= 0.25) {
            reasons.addAll(semanticMatch.reasons());
        } else if (tagMatch >= 0.35 || directMemory >= 0.45) {
            reasons.add("标签匹配: " + matchedTagsLabel(postTags, scoringTags));
        }
        if (!hasSemantic && memoryMatch >= 0.35) {
            reasons.add("符合 MEMORY.md 关注点");
        }
        if (languageFit >= 0.85 && (preferences.hasLanguagePreference() || hasLanguageDislike(userSemanticTags))) {
            reasons.add("符合你的中文阅读偏好");
        }
        if (languageFit <= 0.15 && (preferences.dislikesEnglish() || hasLanguageDislike(userSemanticTags))) {
            reasons.add("英文文章已按偏好降权");
        }
        if (aiEngaged && semantic >= 0.25) {
            reasons.add("你生成过 AI 学习内容");
        }
        if (viewCount >= 2 && semantic >= 0.20) {
            reasons.add("你多次打开过");
        }
        if (collectionCount > 1) {
            reasons.add("保存在多个收藏夹");
        }
        if (!hasSemantic && category >= 0.8) {
            reasons.add("符合你常看的主题分类");
        }
        if (recency >= 0.7) {
            reasons.add("最近收藏");
        }
        if (reasons.isEmpty() && total > 0) {
            reasons.add("综合学习行为评分");
        }

        return new ImportanceResult(total, reasons);
    }

    private static boolean hasLanguageDislike(List<SemanticTag> userSemanticTags) {
        if (userSemanticTags == null || userSemanticTags.isEmpty()) {
            return false;
        }
        return userSemanticTags.stream().anyMatch(tag -> tag.isDislike() && isLanguageTag(tag.tag()));
    }

    private static double languageFitFromSemanticTags(List<SemanticTag> userSemanticTags,
                                                      String originalLanguage,
                                                      LearningMemoryPreferences preferences) {
        if (hasLanguageDislike(userSemanticTags)) {
            return languageFitScore(originalLanguage, new LearningMemoryPreferences(true, true));
        }
        if (preferences.hasLanguagePreference()) {
            return languageFitScore(originalLanguage, preferences);
        }
        return 0.5;
    }

    private static boolean isLanguageTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String normalized = tag.toLowerCase(Locale.ROOT);
        return normalized.contains("英文")
                || normalized.contains("英语")
                || normalized.contains("english")
                || normalized.contains("中文")
                || normalized.contains("chinese");
    }

    public static Set<String> parseTags(String raw) {
        Set<String> tags = new HashSet<>();
        if (raw == null || raw.isBlank()) {
            return tags;
        }
        for (String part : raw.split("[,，;；]")) {
            String normalized = part.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                tags.add(normalized);
            }
        }
        return tags;
    }

    public static List<InterestTag> aggregateTagsFromRaw(List<String> rawTagLines, int maxTags) {
        Map<String, Integer> counts = new HashMap<>();
        for (String line : rawTagLines) {
            for (String tag : parseTags(line)) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            return List.of();
        }
        int maxCount = counts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        return counts.entrySet()
                .stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(maxTags)
                .map(entry -> new InterestTag(entry.getKey(), entry.getValue() / (double) maxCount, "auto"))
                .toList();
    }

    private static List<InterestTag> selectScoringTags(List<InterestTag> profileTags,
                                                       String memoryMd,
                                                       boolean memoryPrioritized) {
        if (!memoryPrioritized || memoryMd == null || memoryMd.isBlank()) {
            return profileTags;
        }
        List<InterestTag> prioritized = profileTags.stream()
                .filter(tag -> "memory".equalsIgnoreCase(tag.source()) || "manual".equalsIgnoreCase(tag.source()))
                .toList();
        if (!prioritized.isEmpty()) {
            return prioritized;
        }
        return LearningTextTagger.memoryDerivedTags(memoryMd);
    }

    private static double directMemoryInterestScore(String title, String summary, String memoryMd) {
        if (memoryMd == null || memoryMd.isBlank()) {
            return 0.0;
        }
        Set<String> memoryTags = LearningTextTagger.extractTags(memoryMd);
        if (memoryTags.isEmpty()) {
            return 0.0;
        }
        String haystack = ((title == null ? "" : title) + " " + (summary == null ? "" : summary))
                .toLowerCase(Locale.ROOT);
        if (haystack.isBlank()) {
            return 0.0;
        }
        long hits = memoryTags.stream()
                .filter(tag -> !"英文".equals(tag) && !"中文".equals(tag))
                .filter(tag -> haystack.contains(tag.toLowerCase(Locale.ROOT)))
                .count();
        if (hits == 0) {
            return 0.0;
        }
        return Math.min(1.0, hits / (double) Math.max(1, memoryTags.size() - countLanguageTags(memoryTags)));
    }

    private static int countLanguageTags(Set<String> memoryTags) {
        int count = 0;
        if (memoryTags.contains("英文")) {
            count++;
        }
        if (memoryTags.contains("中文")) {
            count++;
        }
        return count;
    }

    private static double languageFitScore(String originalLanguage, LearningMemoryPreferences preferences) {
        if (!preferences.hasLanguagePreference()) {
            return 0.5;
        }
        boolean isChinese = isChineseLanguage(originalLanguage);
        boolean isEnglish = isEnglishLanguage(originalLanguage);
        if (preferences.dislikesEnglish()) {
            if (isEnglish) {
                return 0.0;
            }
            if (isChinese) {
                return 1.0;
            }
            return 0.35;
        }
        if (preferences.prefersChinese() && isChinese) {
            return 1.0;
        }
        return 0.5;
    }

    private static boolean isChineseLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }
        String normalized = languageCode.toLowerCase(Locale.ROOT);
        return normalized.startsWith("zh");
    }

    private static boolean isEnglishLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }
        String normalized = languageCode.toLowerCase(Locale.ROOT);
        return normalized.startsWith("en");
    }

    private static double tagMatchScore(Set<String> postTags, List<InterestTag> interestTags) {
        if (interestTags == null || interestTags.isEmpty() || postTags.isEmpty()) {
            return 0.0;
        }
        double matchedWeight = 0.0;
        double totalWeight = 0.0;
        for (InterestTag interestTag : interestTags) {
            totalWeight += interestTag.weight();
            String normalized = LearningTextTagger.canonicalize(interestTag.tag());
            boolean matched = postTags.stream().anyMatch(postTag -> tagsOverlap(postTag, normalized));
            if (matched) {
                matchedWeight += interestTag.weight();
            }
        }
        if (totalWeight <= 0) {
            return 0.0;
        }
        return matchedWeight / totalWeight;
    }

    private static double memoryKeywordScore(String title, String summary, String memoryMd) {
        if (memoryMd == null || memoryMd.isBlank()) {
            return 0.0;
        }
        Set<String> memoryTags = LearningTextTagger.extractTags(memoryMd);
        if (memoryTags.isEmpty()) {
            return 0.0;
        }
        Set<String> postTags = LearningTextTagger.extractPostTags(title, summary, "");
        if (postTags.isEmpty()) {
            return 0.0;
        }
        long hits = memoryTags.stream().filter(postTags::contains).count();
        return hits / (double) memoryTags.size();
    }

    private static String matchedTagsLabel(Set<String> postTags, List<InterestTag> interestTags) {
        List<String> matched = new ArrayList<>();
        for (InterestTag interestTag : interestTags) {
            String normalized = LearningTextTagger.canonicalize(interestTag.tag());
            boolean hit = postTags.stream().anyMatch(postTag -> tagsOverlap(postTag, normalized));
            if (hit) {
                matched.add(interestTag.tag());
            }
            if (matched.size() >= 3) {
                break;
            }
        }
        return matched.isEmpty() ? "相关主题" : String.join(", ", matched);
    }

    private static boolean tagsOverlap(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        String a = left.toLowerCase(Locale.ROOT);
        String b = right.toLowerCase(Locale.ROOT);
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    private static double behaviorScore(int viewCount, boolean aiEngaged, int collectionCount) {
        double score = 0.0;
        score += Math.min(1.0, viewCount / 5.0) * 0.45;
        if (aiEngaged) {
            score += 0.40;
        }
        if (collectionCount > 1) {
            score += 0.15;
        }
        return Math.min(1.0, score);
    }

    private static double categoryMatchScore(Long categoryId, Set<Long> topCategoryIds) {
        if (categoryId == null || topCategoryIds == null || topCategoryIds.isEmpty()) {
            return 0.0;
        }
        return topCategoryIds.contains(categoryId) ? 1.0 : 0.0;
    }

    private static double recencyBoost(LocalDateTime savedAt) {
        if (savedAt == null) {
            return 0.0;
        }
        long days = ChronoUnit.DAYS.between(savedAt, LocalDateTime.now());
        if (days <= 0) {
            return 1.0;
        }
        if (days >= 90) {
            return 0.0;
        }
        return Math.max(0.0, 1.0 - (days / 90.0));
    }
}
