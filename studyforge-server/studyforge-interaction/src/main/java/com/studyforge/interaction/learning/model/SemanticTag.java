package com.studyforge.interaction.learning.model;

public record SemanticTag(String tag, double weight, String polarity) {
    public SemanticTag {
        if (tag != null) {
            tag = tag.trim();
        }
        if (polarity == null || polarity.isBlank()) {
            polarity = "like";
        }
        weight = Math.max(0.0, Math.min(1.0, weight));
    }

    public SemanticTag(String tag, double weight) {
        this(tag, weight, "like");
    }

    public boolean isDislike() {
        return "dislike".equalsIgnoreCase(polarity);
    }
}
