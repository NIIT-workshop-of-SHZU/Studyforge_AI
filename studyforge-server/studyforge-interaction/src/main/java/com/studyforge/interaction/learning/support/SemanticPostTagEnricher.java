package com.studyforge.interaction.learning.support;

import com.studyforge.interaction.learning.model.SemanticTag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SemanticPostTagEnricher {
    private SemanticPostTagEnricher() {
    }

    public static List<SemanticTag> enrich(List<SemanticTag> primary, String title, String summary, String aiTags) {
        Map<String, SemanticTag> merged = new LinkedHashMap<>();
        if (primary != null) {
            for (SemanticTag tag : primary) {
                if (tag != null && tag.tag() != null && !tag.tag().isBlank()) {
                    merged.put(normalize(tag.tag()), tag);
                }
            }
        }
        for (String ruleTag : LearningTextTagger.extractPostTags(title, summary, aiTags)) {
            merged.putIfAbsent(normalize(ruleTag), new SemanticTag(ruleTag, 0.72));
        }
        return new ArrayList<>(merged.values());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
