package com.studyforge.interaction.learning.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.interaction.learning.model.InterestTag;
import java.util.ArrayList;
import java.util.List;

public final class InterestTagCodec {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<InterestTag>> TAG_LIST = new TypeReference<>() {
    };

    private InterestTagCodec() {
    }

    public static List<InterestTag> decode(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<InterestTag> tags = OBJECT_MAPPER.readValue(json, TAG_LIST);
            return tags == null ? List.of() : tags;
        } catch (Exception exception) {
            return List.of();
        }
    }

    public static String encode(List<InterestTag> tags) {
        try {
            return OBJECT_MAPPER.writeValueAsString(tags == null ? List.of() : tags);
        } catch (Exception exception) {
            return "[]";
        }
    }

    public static List<InterestTag> merge(List<InterestTag> autoTags, List<InterestTag> manualTags) {
        List<InterestTag> merged = new ArrayList<>();
        if (manualTags != null) {
            merged.addAll(manualTags.stream().map(tag -> new InterestTag(tag.tag(), tag.weight(), "manual")).toList());
        }
        if (autoTags != null) {
            for (InterestTag autoTag : autoTags) {
                boolean duplicated = merged.stream().anyMatch(existing -> existing.tag().equalsIgnoreCase(autoTag.tag()));
                if (!duplicated) {
                    merged.add(autoTag);
                }
            }
        }
        return merged.stream().limit(30).toList();
    }
}
