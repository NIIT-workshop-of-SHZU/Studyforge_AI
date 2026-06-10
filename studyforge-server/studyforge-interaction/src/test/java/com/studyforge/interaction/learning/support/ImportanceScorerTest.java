package com.studyforge.interaction.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.model.ImportanceResult;
import com.studyforge.interaction.learning.model.SemanticTag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImportanceScorerTest {
    private static final String MEMORY = "用户对Vue前端框架特别感兴趣，而且不喜欢读英文文章。";

    @Test
    void ranksChineseVuePostAboveEnglishPostDespiteAiEngagement() {
        List<SemanticTag> userTags = List.of(
                new SemanticTag("Vue", 0.95, "like"),
                new SemanticTag("前端", 0.9, "like"),
                new SemanticTag("英文文章", 0.85, "dislike")
        );
        List<SemanticTag> vuePostTags = List.of(
                new SemanticTag("Vue", 0.92),
                new SemanticTag("前端状态管理", 0.78)
        );
        List<SemanticTag> englishPostTags = List.of(
                new SemanticTag("English productivity", 0.88),
                new SemanticTag("Markdown editor", 0.74)
        );

        ImportanceResult vueChinese = ImportanceScorer.score(
                "Vue 知识流页面的状态设计：从请求到缓存",
                "前端状态管理实践",
                "Vue,状态管理,前端",
                "zh_CN",
                MEMORY,
                true,
                1L,
                Set.of(),
                List.of(),
                userTags,
                vuePostTags,
                0,
                false,
                1,
                LocalDateTime.now().minusDays(3)
        );

        ImportanceResult englishPost = ImportanceScorer.score(
                "How to structure a learning system",
                "English productivity article",
                "productivity,english",
                "en_US",
                MEMORY,
                true,
                1L,
                Set.of(),
                List.of(),
                userTags,
                englishPostTags,
                5,
                true,
                1,
                LocalDateTime.now()
        );

        assertThat(vueChinese.score()).isGreaterThan(englishPost.score());
        assertThat(vueChinese.rankReasons()).anyMatch(reason -> reason.contains("语义匹配") || reason.contains("Vue"));
        assertThat(englishPost.score()).isLessThan(0.25);
        assertThat(englishPost.rankReasons()).anyMatch(reason -> reason.contains("降权"));
    }

    @Test
    void parsesDislikeEnglishPreferenceFromPlainChineseMemory() {
        LearningMemoryPreferences preferences = LearningMemoryPreferences.parse(MEMORY);
        assertThat(preferences.dislikesEnglish()).isTrue();
        assertThat(preferences.prefersChinese()).isTrue();
    }

    @Test
    void profileDrivenUsesRuleSemanticWhenLlmSemanticIsZero() {
        List<SemanticTag> dislikeOnlyUserTags = List.of(
                new SemanticTag("英文文章", 0.9, "dislike"),
                new SemanticTag("English articles", 0.85, "dislike")
        );

        ImportanceResult vueChinese = ImportanceScorer.score(
                "Vue 知识流页面的状态设计：从请求到缓存",
                "前端状态管理实践",
                "Vue,状态管理,前端",
                "zh_CN",
                MEMORY,
                true,
                1L,
                Set.of(),
                List.of(),
                dislikeOnlyUserTags,
                List.of(),
                0,
                false,
                1,
                LocalDateTime.now().minusDays(3)
        );

        assertThat(vueChinese.score()).isGreaterThan(0.25);
        assertThat(vueChinese.rankReasons()).anyMatch(reason ->
                reason.contains("MEMORY") || reason.contains("标签匹配") || reason.contains("标题命中")
        );
        assertThat(vueChinese.rankReasons()).noneMatch(reason -> reason.equals("综合学习行为评分"));
    }
}
