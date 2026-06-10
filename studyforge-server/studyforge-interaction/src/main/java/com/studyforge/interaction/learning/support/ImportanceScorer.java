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
        List<SemanticTag> effectivePostTags = SemanticPostTagEnricher.enrich(
                postSemanticTags,
                title,
                summary,
                aiTags
        );
        SemanticTagMatcher.MatchResult semanticMatch = SemanticTagMatcher.match(userSemanticTags, effectivePostTags);
        boolean hasUserSemantic = userSemanticTags != null && !userSemanticTags.isEmpty();
        boolean hasSemantic = hasUserSemantic && !effectivePostTags.isEmpty();
        boolean profileDriven = memoryPrioritized && hasUserSemantic;

        Set<String> postTags = LearningTextTagger.extractPostTags(title, summary, aiTags);
        List<InterestTag> profileTags = LearningTextTagger.buildUserInterestTags(memoryMd, interestTags);
        List<InterestTag> scoringTags = selectScoringTags(profileTags, memoryMd, memoryPrioritized);

        double tagMatch = tagMatchScore(postTags, scoringTags);
        double memoryMatch = memoryKeywordScore(title, summary, memoryMd);
        double directMemory = directMemoryInterestScore(title, summary, memoryMd);
        double titleUserMatch = titleMatchesUserLikeTags(title, summary, userSemanticTags);
        double ruleSemantic = Math.max(
                Math.max(Math.max(tagMatch, memoryMatch * 0.95), directMemory),
                titleUserMatch
        );
        double llmSemantic = semanticMatch.score();
        double semantic = profileDriven
                ? Math.max(llmSemantic, ruleSemantic)
                : (hasSemantic ? Math.max(llmSemantic, ruleSemantic * 0.9) : ruleSemantic);

        LearningMemoryPreferences preferences = LearningMemoryPreferences.parse(memoryMd);
        double languageFit = hasSemantic
                ? languageFitFromSemanticTags(userSemanticTags, originalLanguage, preferences)
                : languageFitScore(originalLanguage, preferences);
        double behavior = behaviorScore(viewCount, aiEngaged, collectionCount);
        double category = categoryMatchScore(categoryId, topCategoryIds);
        double recency = profileDriven ? 0.0 : recencyBoost(savedAt);

        double behaviorAdjusted = behavior * (profileDriven ? 0.15 : (0.2 + 0.8 * semantic));
        double total;
        if (profileDriven) {
            total = 0.92 * semantic + 0.06 * languageFit + 0.02 * behaviorAdjusted;
        } else if (hasSemantic) {
            total = 0.86 * semantic + 0.10 * languageFit + 0.04 * behaviorAdjusted;
        } else {
            total = 0.48 * semantic + 0.27 * languageFit + 0.15 * behaviorAdjusted + 0.07 * category + 0.03 * recency;
        }
        total = Math.max(0.0, Math.min(1.0, total));

        List<String> reasons = new ArrayList<>();
        reasons.addAll(semanticMatch.reasons());
        appendPartialSemanticReasons(reasons, semanticMatch);
        if (titleUserMatch >= 0.35) {
            reasons.add("标题命中 MEMORY 关注点: " + matchedLikeTagsInTitle(title, summary, userSemanticTags)
                    + "（" + percent(titleUserMatch) + "）");
        } else if (tagMatch >= 0.25 || directMemory >= 0.35) {
            reasons.add("规则标签匹配: " + matchedTagsLabel(postTags, scoringTags)
                    + "（" + percent(Math.max(tagMatch, directMemory)) + "）");
        }
        if (memoryMatch >= 0.25 && reasons.stream().noneMatch(reason -> reason.contains("MEMORY"))) {
            reasons.add("MEMORY.md 关键词命中（" + percent(memoryMatch) + "）");
        }
        if (profileDriven && !reasons.isEmpty()) {
            reasons.add(0, "综合语义相关度 " + percent(semantic));
        }
        if (languageFit >= 0.85 && (preferences.hasLanguagePreference() || hasLanguageDislike(userSemanticTags))) {
            reasons.add("符合你的中文阅读偏好");
        }
        if (languageFit <= 0.15 && (preferences.dislikesEnglish() || hasLanguageDislike(userSemanticTags))) {
            reasons.add("英文文章已按偏好降权");
        }
        if (!profileDriven && aiEngaged && semantic >= 0.25) {
            reasons.add("你生成过 AI 学习内容");
        }
        if (!profileDriven && viewCount >= 2 && semantic >= 0.20) {
            reasons.add("你多次打开过");
        }
        if (!profileDriven && collectionCount > 1) {
            reasons.add("保存在多个收藏夹");
        }
        if (!profileDriven && !hasSemantic && category >= 0.8) {
            reasons.add("符合你常看的主题分类");
        }
        if (!profileDriven && recency >= 0.7) {
            reasons.add("最近收藏");
        }
        if (reasons.isEmpty() && total > 0) {
            if (profileDriven) {
                reasons.addAll(lowMatchReasons(semanticMatch, scoringTags, postTags, effectivePostTags, percent(semantic)));
            } else {
                reasons.add("综合学习行为评分（语义 " + percent(semantic) + "）");
            }
        }

        return new ImportanceResult(total, dedupeReasons(reasons));
    }

    private static int percent(double value) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * 100);
    }

    private static void appendPartialSemanticReasons(List<String> reasons, SemanticTagMatcher.MatchResult semanticMatch) {
        if (!reasons.isEmpty() || semanticMatch.matches().isEmpty()) {
            return;
        }
        SemanticTagMatcher.TagMatch best = semanticMatch.matches().stream()
                .filter(match -> !match.dislike())
                .findFirst()
                .orElse(semanticMatch.matches().get(0));
        reasons.add(SemanticTagMatcher.formatMatchReason(
                best.userTag(),
                best.postTag(),
                best.similarity(),
                best.dislike()
        ) + "（未达筛选阈值）");
    }

    private static List<String> lowMatchReasons(SemanticTagMatcher.MatchResult semanticMatch,
                                                List<InterestTag> scoringTags,
                                                Set<String> postTags,
                                                List<SemanticTag> effectivePostTags,
                                                int semanticPercent) {
        List<String> reasons = new ArrayList<>();
        reasons.add("与 MEMORY.md 相关度 " + semanticPercent + "%，暂无强匹配");
        if (!semanticMatch.matches().isEmpty()) {
            SemanticTagMatcher.TagMatch best = semanticMatch.matches().get(0);
            reasons.add("最接近: " + SemanticTagMatcher.formatMatchReason(
                    best.userTag(),
                    best.postTag(),
                    best.similarity(),
                    best.dislike()
            ));
        }
        if (!scoringTags.isEmpty()) {
            reasons.add("你的关注: " + scoringTags.stream().map(InterestTag::tag).limit(4).reduce((a, b) -> a + "、" + b).orElse("无"));
        }
        if (!effectivePostTags.isEmpty()) {
            reasons.add("文章标签: " + effectivePostTags.stream().map(SemanticTag::tag).limit(4).reduce((a, b) -> a + "、" + b).orElse("无"));
        } else if (!postTags.isEmpty()) {
            reasons.add("文章标签: " + postTags.stream().limit(4).reduce((a, b) -> a + "、" + b).orElse("无"));
        }
        return reasons;
    }

    private static List<String> dedupeReasons(List<String> reasons) {
        List<String> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String reason : reasons) {
            if (reason != null && !reason.isBlank() && seen.add(reason) && deduped.size() < 8) {
                deduped.add(reason);
            }
        }
        return deduped;
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
        List<InterestTag> likeTags = profileTags == null
                ? List.of()
                : profileTags.stream().filter(tag -> !tag.isDislike()).toList();
        if (!memoryPrioritized || memoryMd == null || memoryMd.isBlank()) {
            return likeTags;
        }
        List<InterestTag> prioritized = likeTags.stream()
                .filter(tag -> {
                    String source = tag.source() == null ? "" : tag.source().toLowerCase(Locale.ROOT);
                    return source.equals("memory")
                            || source.equals("manual")
                            || source.equals("semantic")
                            || source.equals("auto");
                })
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

    private static double titleMatchesUserLikeTags(String title,
                                                   String summary,
                                                   List<SemanticTag> userSemanticTags) {
        if (userSemanticTags == null || userSemanticTags.isEmpty()) {
            return 0.0;
        }
        String haystack = ((title == null ? "" : title) + " " + (summary == null ? "" : summary))
                .toLowerCase(Locale.ROOT);
        if (haystack.isBlank()) {
            return 0.0;
        }
        double matchedWeight = 0.0;
        double totalWeight = 0.0;
        for (SemanticTag userTag : userSemanticTags) {
            if (userTag.isDislike()) {
                continue;
            }
            totalWeight += userTag.weight();
            String normalized = LearningTextTagger.canonicalize(userTag.tag()).toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            if (haystack.contains(normalized) || haystack.contains(userTag.tag().toLowerCase(Locale.ROOT))) {
                matchedWeight += userTag.weight();
            }
        }
        if (totalWeight <= 0) {
            return 0.0;
        }
        return matchedWeight / totalWeight;
    }

    private static String matchedLikeTagsInTitle(String title,
                                                 String summary,
                                                 List<SemanticTag> userSemanticTags) {
        if (userSemanticTags == null || userSemanticTags.isEmpty()) {
            return "相关主题";
        }
        String haystack = ((title == null ? "" : title) + " " + (summary == null ? "" : summary))
                .toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (SemanticTag userTag : userSemanticTags) {
            if (userTag.isDislike()) {
                continue;
            }
            String normalized = LearningTextTagger.canonicalize(userTag.tag()).toLowerCase(Locale.ROOT);
            if (haystack.contains(normalized) || haystack.contains(userTag.tag().toLowerCase(Locale.ROOT))) {
                matched.add(userTag.tag());
            }
            if (matched.size() >= 3) {
                break;
            }
        }
        return matched.isEmpty() ? "相关主题" : String.join(", ", matched);
    }
}
