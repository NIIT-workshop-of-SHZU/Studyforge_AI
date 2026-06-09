package com.studyforge.interaction.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.model.SemanticTag;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticTagMatcherTest {
    @Test
    void matchesVueInterestAgainstVuePostTags() {
        List<SemanticTag> userTags = List.of(
                new SemanticTag("Vue", 0.95, "like"),
                new SemanticTag("英文文章", 0.85, "dislike")
        );
        List<SemanticTag> vuePost = List.of(
                new SemanticTag("Vue", 0.92),
                new SemanticTag("前端状态管理", 0.74)
        );
        List<SemanticTag> englishPost = List.of(
                new SemanticTag("English productivity", 0.88),
                new SemanticTag("Markdown editor", 0.7)
        );

        SemanticTagMatcher.MatchResult vueMatch = SemanticTagMatcher.match(userTags, vuePost);
        SemanticTagMatcher.MatchResult englishMatch = SemanticTagMatcher.match(userTags, englishPost);

        assertThat(vueMatch.score()).isGreaterThan(englishMatch.score());
        assertThat(vueMatch.reasons()).anyMatch(reason -> reason.contains("Vue"));
    }
}
