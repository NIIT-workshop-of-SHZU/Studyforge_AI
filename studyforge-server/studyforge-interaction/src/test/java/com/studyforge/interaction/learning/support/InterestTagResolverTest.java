package com.studyforge.interaction.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterestTagResolverTest {
    @Test
    void resolvesTagsFromMemoryWhenStoredTagsAreEmpty() {
        String memory = "用户对Vue前端框架特别感兴趣，而且不喜欢读英文文章。";
        List<InterestTag> resolved = InterestTagResolver.resolve(memory, List.of());

        assertThat(resolved).isNotEmpty();
        assertThat(resolved).anyMatch(tag -> tag.tag().toLowerCase().contains("vue"));
    }

    @Test
    void mergePreservesExistingSemanticTagsWhenReExtracting() {
        List<InterestTag> existing = List.of(new InterestTag("Spring Boot", 0.9, "semantic", "like"));
        List<InterestTag> extracted = List.of(new InterestTag("Vue", 0.95, "semantic", "like"));
        List<InterestTag> merged = InterestTagResolver.mergePreservingExisting(existing, extracted, List.of());

        assertThat(merged).anyMatch(tag -> tag.tag().contains("Spring"));
        assertThat(merged).anyMatch(tag -> tag.tag().contains("Vue"));
    }
}
