package com.studyforge.interaction.learning.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.interaction.learning.model.InterestTag;
import com.studyforge.interaction.learning.model.SemanticTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SemanticTagParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<SemanticTag>> TAG_LIST = new TypeReference<>() {
    };
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private SemanticTagParser() {
    }

    public static List<SemanticTag> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String json = extractJson(raw.trim());
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root.isArray()) {
                List<SemanticTag> tags = OBJECT_MAPPER.convertValue(root, TAG_LIST);
                return sanitize(tags);
            }
            JsonNode tagsNode = root.path("tags");
            if (tagsNode.isArray()) {
                List<SemanticTag> tags = OBJECT_MAPPER.convertValue(tagsNode, TAG_LIST);
                return sanitize(tags);
            }
            JsonNode interestTagsNode = root.path("interest_tags");
            if (interestTagsNode.isArray()) {
                return sanitizeInterestTags(interestTagsNode);
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    public static String encodeTags(List<SemanticTag> tags) {
        try {
            return OBJECT_MAPPER.writeValueAsString(tags == null ? List.of() : tags);
        } catch (Exception exception) {
            return "[]";
        }
    }

    public static List<SemanticTag> decodeCached(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<SemanticTag> tags = OBJECT_MAPPER.readValue(json, TAG_LIST);
            return sanitize(tags);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static List<SemanticTag> sanitizeInterestTags(JsonNode interestTagsNode) {
        List<SemanticTag> tags = new ArrayList<>();
        for (JsonNode node : interestTagsNode) {
            String tag = node.path("tag").asText("").trim();
            if (tag.isBlank()) {
                continue;
            }
            double weight = node.path("weight").asDouble(0.5);
            String polarity = node.path("polarity").asText("like");
            tags.add(new SemanticTag(tag, weight, polarity));
        }
        return sanitize(tags);
    }

    private static List<SemanticTag> sanitize(List<SemanticTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<SemanticTag> cleaned = new ArrayList<>();
        for (SemanticTag tag : tags) {
            if (tag == null || tag.tag() == null || tag.tag().isBlank()) {
                continue;
            }
            cleaned.add(tag);
            if (cleaned.size() >= 12) {
                break;
            }
        }
        return cleaned;
    }

    private static String extractJson(String raw) {
        Matcher matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        start = raw.indexOf('{');
        end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    public static List<InterestTag> toInterestTags(List<SemanticTag> tags, String source) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        String normalizedSource = source == null || source.isBlank() ? "semantic" : source;
        return tags.stream()
                .map(tag -> new InterestTag(tag.tag(), tag.weight(), normalizedSource, tag.polarity()))
                .toList();
    }

    public static String fingerprint(String title, String summary) {
        String payload = ((title == null ? "" : title.trim()) + "\n" + (summary == null ? "" : summary.trim()))
                .toLowerCase(Locale.ROOT);
        return Integer.toHexString(payload.hashCode());
    }
}
