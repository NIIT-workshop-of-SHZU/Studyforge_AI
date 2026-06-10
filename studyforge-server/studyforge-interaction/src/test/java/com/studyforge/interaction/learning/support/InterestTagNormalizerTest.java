package com.studyforge.interaction.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterestTagNormalizerTest {
    @Test
    void mergesEnglishDislikeVariantsAndDropsNoiseTags() {
        List<InterestTag> normalized = InterestTagNormalizer.normalize(List.of(
                new InterestTag("英文", 0.7, "semantic", "dislike"),
                new InterestTag("English articles", 0.9, "semantic", "dislike"),
                new InterestTag("阅读", 0.6, "semantic", "like"),
                new InterestTag("Vue", 0.95, "semantic", "like"),
                new InterestTag("vue.js", 0.8, "memory", "like")
        ));

        assertThat(normalized).anyMatch(tag -> "Vue".equalsIgnoreCase(tag.tag()) || tag.tag().toLowerCase().contains("vue"));
        assertThat(normalized).noneMatch(tag -> "阅读".equals(tag.tag()));
        assertThat(normalized.stream().filter(InterestTag::isDislike).count()).isEqualTo(1);
        assertThat(normalized.stream().filter(InterestTag::isDislike).findFirst().orElseThrow().tag()).isEqualTo("英文文章");
    }

    @Test
    void filterForUiChipsReturnsLikeTagsOnly() {
        List<InterestTag> chips = InterestTagNormalizer.filterForUiChips(List.of(
                new InterestTag("Vue", 0.95, "semantic", "like"),
                new InterestTag("英文文章", 0.9, "semantic", "dislike")
        ));

        assertThat(chips).hasSize(1);
        assertThat(chips.get(0).tag()).isEqualTo("Vue");
    }
}
