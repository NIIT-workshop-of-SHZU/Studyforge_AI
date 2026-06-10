package com.studyforge.interaction.learning.support;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InterestTagNormalizer {
    private InterestTagNormalizer() {
    }

    public static List<InterestTag> normalize(List<InterestTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Map<String, InterestTag> likeTags = new LinkedHashMap<>();
        InterestTag dislikeEnglish = null;

        for (InterestTag tag : tags) {
            if (tag == null || tag.tag() == null || tag.tag().isBlank()) {
                continue;
            }
            if (isGenericNoise(tag.tag())) {
                continue;
            }
            if (tag.isDislike() && isEnglishPreferenceTag(tag.tag())) {
                dislikeEnglish = mergeDislike(dislikeEnglish, tag);
                continue;
            }
            if (tag.isDislike()) {
                continue;
            }
            String key = canonicalKey(tag.tag());
            likeTags.merge(
                    key,
                    tag,
                    (left, right) -> new InterestTag(
                            preferLabel(left.tag(), right.tag()),
                            Math.max(left.weight(), right.weight()),
                            preferSource(left.source(), right.source()),
                            "like"
                    )
            );
        }

        List<InterestTag> normalized = new ArrayList<>(likeTags.values());
        if (dislikeEnglish != null) {
            normalized.add(dislikeEnglish);
        }
        return normalized.stream().limit(12).toList();
    }

    public static List<InterestTag> filterForUiChips(List<InterestTag> tags) {
        return normalize(tags).stream()
                .filter(tag -> !tag.isDislike())
                .limit(5)
                .toList();
    }

    private static InterestTag mergeDislike(InterestTag existing, InterestTag incoming) {
        if (existing == null) {
            return new InterestTag("英文文章", incoming.weight(), incoming.source(), "dislike");
        }
        return new InterestTag(
                existing.tag(),
                Math.max(existing.weight(), incoming.weight()),
                preferSource(existing.source(), incoming.source()),
                "dislike"
        );
    }

    private static boolean isEnglishPreferenceTag(String tag) {
        String normalized = tag.toLowerCase(Locale.ROOT);
        return normalized.contains("英文")
                || normalized.contains("英语")
                || normalized.contains("english");
    }

    private static boolean isGenericNoise(String tag) {
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("阅读")
                || normalized.equals("reading")
                || normalized.equals("文章")
                || normalized.equals("学习");
    }

    private static String canonicalKey(String tag) {
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("vue")) {
            return "vue";
        }
        if (normalized.contains("前端") || normalized.contains("frontend")) {
            return "frontend";
        }
        if (normalized.contains("spring")) {
            return "spring";
        }
        if (normalized.contains("mybatis")) {
            return "mybatis";
        }
        if (normalized.contains("中文") || normalized.contains("chinese")) {
            return "chinese";
        }
        return normalized;
    }

    private static String preferLabel(String left, String right) {
        if (left.length() <= right.length()) {
            return left;
        }
        return right;
    }

    private static String preferSource(String left, String right) {
        if ("semantic".equalsIgnoreCase(left) || "semantic".equalsIgnoreCase(right)) {
            return "semantic";
        }
        if ("memory".equalsIgnoreCase(left) || "memory".equalsIgnoreCase(right)) {
            return "memory";
        }
        return left == null || left.isBlank() ? right : left;
    }
}
