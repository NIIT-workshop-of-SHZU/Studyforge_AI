package com.studyforge.interaction.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.model.SemanticTag;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticTagParserTest {
    @Test
    void parsesJsonArrayAndObjectWrapper() {
        List<SemanticTag> arrayTags = SemanticTagParser.parseTags("""
                [{"tag":"Vue","weight":0.95,"polarity":"like"}]
                """);
        assertThat(arrayTags).hasSize(1);
        assertThat(arrayTags.get(0).tag()).isEqualTo("Vue");

        List<SemanticTag> wrappedTags = SemanticTagParser.parseTags("""
                {"tags":[{"tag":"前端","weight":0.8}]}
                """);
        assertThat(wrappedTags).extracting(SemanticTag::tag).contains("前端");
    }
}
