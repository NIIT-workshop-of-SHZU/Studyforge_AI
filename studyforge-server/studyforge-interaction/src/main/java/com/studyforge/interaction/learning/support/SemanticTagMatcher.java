package com.studyforge.interaction.learning.support;

import com.studyforge.interaction.learning.model.SemanticTag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SemanticTagMatcher {
    private static final double MATCH_THRESHOLD = 0.55;

    private SemanticTagMatcher() {
    }

    public record MatchResult(double score, List<String> reasons) {
    }

    public static MatchResult match(List<SemanticTag> userTags, List<SemanticTag> postTags) {
        if (userTags == null || userTags.isEmpty() || postTags == null || postTags.isEmpty()) {
            return new MatchResult(0.0, List.of());
        }

        double positiveContribution = 0.0;
        double negativeContribution = 0.0;
        double positiveWeightSum = 0.0;
        double negativeWeightSum = 0.0;
        List<String> reasons = new ArrayList<>();
        Set<String> reasonKeys = new LinkedHashSet<>();

        for (SemanticTag userTag : userTags) {
            if (userTag.isDislike()) {
                negativeWeightSum += userTag.weight();
            } else {
                positiveWeightSum += userTag.weight();
            }

            for (SemanticTag postTag : postTags) {
                double similarity = tagSimilarity(userTag.tag(), postTag.tag());
                if (similarity < MATCH_THRESHOLD) {
                    continue;
                }
                double contribution = userTag.weight() * postTag.weight() * similarity;
                if (userTag.isDislike()) {
                    negativeContribution += contribution;
                    addReason(reasonKeys, reasons, "踩中避雷偏好: " + postTag.tag());
                } else {
                    positiveContribution += contribution;
                    addReason(reasonKeys, reasons, "语义匹配: " + userTag.tag() + " ↔ " + postTag.tag());
                }
            }
        }

        double positiveScore = positiveWeightSum <= 0 ? 0.0 : positiveContribution / positiveWeightSum;
        double negativeScore = negativeWeightSum <= 0 ? 0.0 : negativeContribution / negativeWeightSum;
        double score = Math.max(0.0, Math.min(1.0, positiveScore - negativeScore * 0.9));

        if (reasons.isEmpty() && score > 0) {
            reasons.add("语义标签弱匹配");
        }
        return new MatchResult(score, reasons);
    }

    private static void addReason(Set<String> reasonKeys, List<String> reasons, String reason) {
        if (reasonKeys.add(reason) && reasons.size() < 6) {
            reasons.add(reason);
        }
    }

    public static double tagSimilarity(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0.0;
        }
        String a = normalize(left);
        String b = normalize(right);
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.contains(b) || b.contains(a)) {
            return 0.88;
        }
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String token : tokensA) {
            if (tokensB.contains(token)) {
                intersection++;
            }
        }
        int union = tokensA.size() + tokensB.size() - intersection;
        if (union <= 0) {
            return 0.0;
        }
        return (intersection / (double) union) * 0.82;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static Set<String> tokenize(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String part : value.split("[\\s,，;；、/|]+")) {
            String token = part.trim().toLowerCase(Locale.ROOT);
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty() && value.length() >= 2) {
            tokens.add(value);
        }
        return tokens;
    }
}
