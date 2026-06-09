package com.studyforge.interaction.learning.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.model.InterestTag;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LearningTextTaggerTest {
    @Test
    void extractsTagsFromTitleAndMemory() {
        Set<String> postTags = LearningTextTagger.extractPostTags(
                "Vue3 组件化与前端状态管理",
                "适合前端入门",
                "Vue,前端"
        );
        assertThat(postTags).contains("Vue", "前端");

        List<InterestTag> profileTags = LearningTextTagger.buildUserInterestTags(
                "## 近期学习重点\n- 主攻 Vue 和前端\n- 也关注 Spring 后端",
                List.of()
        );
        assertThat(profileTags.stream().map(InterestTag::tag).toList()).anyMatch(tag -> tag.contains("Vue") || tag.contains("前端"));
    }

    @Test
    void extractsVueAndFrontendFromPlainChineseSentence() {
        Set<String> tags = LearningTextTagger.extractTags("用户对Vue前端框架特别感兴趣，而且不喜欢读英文文章。");
        assertThat(tags).contains("Vue", "前端");
        assertThat(tags).doesNotContain("英文");
    }
}
