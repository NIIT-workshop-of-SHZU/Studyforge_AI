package com.studyforge.interaction.learning.model;

public record InterestTag(String tag, double weight, String source, String polarity) {
    public InterestTag {
        if (tag != null) {
            tag = tag.trim();
        }
        if (source == null || source.isBlank()) {
            source = "auto";
        }
        if (polarity == null || polarity.isBlank()) {
            polarity = "like";
        }
    }

    public InterestTag(String tag, double weight, String source) {
        this(tag, weight, source, "like");
    }

    public boolean isDislike() {
        return "dislike".equalsIgnoreCase(polarity);
    }

    public SemanticTag toSemanticTag() {
        return new SemanticTag(tag, weight, polarity);
    }
}
