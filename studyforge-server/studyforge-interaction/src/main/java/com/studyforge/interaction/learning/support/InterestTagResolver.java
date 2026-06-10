package com.studyforge.interaction.learning.support;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InterestTagResolver {
    private InterestTagResolver() {
    }

    public static List<InterestTag> resolve(String memoryMd, List<InterestTag> storedTags) {
        List<InterestTag> merged = new ArrayList<>();
        if (storedTags != null) {
            merged.addAll(storedTags);
        }
        if (memoryMd != null && !memoryMd.isBlank()) {
            for (InterestTag derived : LearningTextTagger.memoryDerivedTags(memoryMd)) {
                mergeIfAbsent(merged, derived);
            }
            for (InterestTag built : LearningTextTagger.buildUserInterestTags(memoryMd, List.of())) {
                mergeIfAbsent(merged, built);
            }
        }
        if (merged.isEmpty()) {
            return List.of();
        }
        return InterestTagNormalizer.normalize(merged);
    }

    public static List<InterestTag> mergePreservingExisting(List<InterestTag> existing,
                                                            List<InterestTag> extracted,
                                                            List<InterestTag> manualTags) {
        List<InterestTag> merged = new ArrayList<>();
        if (extracted != null) {
            merged.addAll(extracted);
        }
        if (existing != null) {
            for (InterestTag tag : existing) {
                if ("manual".equalsIgnoreCase(tag.source())) {
                    continue;
                }
                mergeIfAbsent(merged, tag);
            }
        }
        if (manualTags != null) {
            for (InterestTag manualTag : manualTags) {
                mergeIfAbsent(merged, new InterestTag(manualTag.tag(), manualTag.weight(), "manual", "like"));
            }
        }
        return InterestTagNormalizer.normalize(merged);
    }

    private static void mergeIfAbsent(List<InterestTag> merged, InterestTag candidate) {
        if (candidate == null || candidate.tag() == null || candidate.tag().isBlank()) {
            return;
        }
        boolean exists = merged.stream().anyMatch(tag -> tagsOverlap(tag.tag(), candidate.tag()));
        if (!exists) {
            merged.add(candidate);
        }
    }

    private static boolean tagsOverlap(String left, String right) {
        String a = left == null ? "" : left.toLowerCase(Locale.ROOT);
        String b = right == null ? "" : right.toLowerCase(Locale.ROOT);
        return a.equals(b) || a.contains(b) || b.contains(a);
    }
}
