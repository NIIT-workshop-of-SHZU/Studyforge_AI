package com.studyforge.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.studyforge.common.enums.UserStatus;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.system.dto.HomepageDraftRequest;
import com.studyforge.system.dto.HomepageReviewRequest;
import com.studyforge.system.entity.CommunityHomepageDesign;
import com.studyforge.system.entity.User;
import com.studyforge.system.entity.UserHomepage;
import com.studyforge.system.mapper.HomepageMapper;
import com.studyforge.system.mapper.UserMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomepageServiceImplTest {
    @Mock
    private HomepageMapper homepageMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private HomepageServiceImpl homepageService;

    @Test
    void saveDraftCreatesDefaultHomepageAndUpdatesDraft() {
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(homepageMapper.selectUserHomepageByUserId(7L)).thenReturn(null);
        doAnswer(invocation -> {
            UserHomepage homepage = invocation.getArgument(0);
            homepage.setHomepageId(42L);
            return 1;
        }).when(homepageMapper).insertUserHomepage(any(UserHomepage.class));

        var response = homepageService.saveDraft(7L, new HomepageDraftRequest(
                "CODE_AURORA",
                "code",
                "{\"accent\":\"#16a34a\"}",
                "<main>hello</main>",
                "[]"
        ));

        ArgumentCaptor<UserHomepage> captor = ArgumentCaptor.forClass(UserHomepage.class);
        verify(homepageMapper).insertUserHomepage(any(UserHomepage.class));
        verify(homepageMapper).updateUserHomepageDraft(captor.capture());
        UserHomepage saved = captor.getValue();
        assertThat(saved.getHomepageId()).isEqualTo(42L);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getTemplateType()).isEqualTo("CODE_AURORA");
        assertThat(saved.getLayoutMode()).isEqualTo("code");
        assertThat(saved.getCustomCodeDraft()).isEqualTo("<main>hello</main>");
        assertThat(response.homepageId()).isEqualTo(42L);
        assertThat(response.layoutMode()).isEqualTo("code");
        assertThat(response.self()).isTrue();
    }

    @Test
    void reviewApprovesPendingUserHomepageAndWritesAudit() {
        when(homepageMapper.selectHomepageReview("USER_HOMEPAGE", 42L))
                .thenReturn(reviewRow("USER_HOMEPAGE", 42L, "PENDING_REVIEW"))
                .thenReturn(reviewRow("USER_HOMEPAGE", 42L, "APPROVED"));

        var response = homepageService.review(99L, 42L, new HomepageReviewRequest(
                "USER_HOMEPAGE",
                "APPROVE",
                "looks good"
        ));

        verify(homepageMapper).approveUserHomepage(42L, 99L, "looks good");
        verify(homepageMapper, never()).rejectUserHomepage(any(), any(), any());
        verify(homepageMapper).insertAudit(99L, "USER_HOMEPAGE", 42L, "APPROVE_USER_HOMEPAGE", "looks good");
        assertThat(response.reviewStatus()).isEqualTo("APPROVED");
    }

    @Test
    void reviewRejectsUnsupportedDecisionBeforeTouchingMapper() {
        assertThatThrownBy(() -> homepageService.review(99L, 42L, new HomepageReviewRequest(
                "USER_HOMEPAGE",
                "MAYBE",
                ""
        )))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.VALIDATION_ERROR.getCode());

        verifyNoInteractions(homepageMapper);
    }

    @Test
    void cloneRejectsNonApprovedCommunityDesign() {
        CommunityHomepageDesign design = communityDesign(5L, 8L, "PENDING_REVIEW");
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(homepageMapper.selectCommunityDesignById(5L)).thenReturn(design);

        assertThatThrownBy(() -> homepageService.cloneCommunityDesign(7L, 5L))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND.getCode());

        verify(homepageMapper, never()).updateUserHomepageDraft(any());
        verify(homepageMapper, never()).incrementCommunityCloneCount(any());
    }

    @Test
    void cloneCopiesApprovedDesignIntoDraftAndIncrementsCloneCount() {
        CommunityHomepageDesign design = communityDesign(5L, 8L, "APPROVED");
        design.setTemplateType("MEDIA_GRID");
        design.setLayoutMode("media");
        design.setThemeConfig("{\"tone\":\"green\"}");
        design.setCustomCode("");
        design.setMediaLayout("[{\"url\":\"/api/v1/files/homepage-media/demo.gif\"}]");
        UserHomepage homepage = defaultHomepage(11L, 7L);

        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(homepageMapper.selectCommunityDesignById(5L)).thenReturn(design);
        when(homepageMapper.selectUserHomepageByUserId(7L)).thenReturn(homepage);

        var response = homepageService.cloneCommunityDesign(7L, 5L);

        ArgumentCaptor<UserHomepage> captor = ArgumentCaptor.forClass(UserHomepage.class);
        verify(homepageMapper).updateUserHomepageDraft(captor.capture());
        verify(homepageMapper).incrementCommunityCloneCount(5L);
        UserHomepage updated = captor.getValue();
        assertThat(updated.getHomepageId()).isEqualTo(11L);
        assertThat(updated.getTemplateType()).isEqualTo("MEDIA_GRID");
        assertThat(updated.getLayoutMode()).isEqualTo("media");
        assertThat(updated.getCommunityTemplateId()).isEqualTo(5L);
        assertThat(response.layoutMode()).isEqualTo("media");
        assertThat(response.communityTemplateId()).isEqualTo(5L);
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private UserHomepage defaultHomepage(Long homepageId, Long userId) {
        UserHomepage homepage = new UserHomepage();
        homepage.setHomepageId(homepageId);
        homepage.setUserId(userId);
        homepage.setTemplateType("GITHUB_DEFAULT");
        homepage.setLayoutMode("default");
        homepage.setThemeConfig("{}");
        homepage.setCustomCodeDraft("");
        homepage.setMediaLayoutDraft("[]");
        homepage.setPublishedTemplateType("GITHUB_DEFAULT");
        homepage.setPublishedLayoutMode("default");
        homepage.setPublishedThemeConfig("{}");
        homepage.setPublishedCustomCode("");
        homepage.setPublishedMediaLayout("[]");
        homepage.setPublishedVersion(0);
        homepage.setReviewStatus("DRAFT");
        return homepage;
    }

    private CommunityHomepageDesign communityDesign(Long designId, Long authorId, String status) {
        CommunityHomepageDesign design = new CommunityHomepageDesign();
        design.setDesignId(designId);
        design.setAuthorId(authorId);
        design.setAuthorName("Author");
        design.setTitle("Design");
        design.setStatus(status);
        return design;
    }

    private Map<String, Object> reviewRow(String targetType, Long targetId, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("targetType", targetType);
        row.put("targetId", targetId);
        row.put("ownerId", 7L);
        row.put("ownerName", "Owner");
        row.put("ownerAvatarUrl", "");
        row.put("title", "Homepage");
        row.put("summary", "");
        row.put("templateType", "GITHUB_DEFAULT");
        row.put("layoutMode", "default");
        row.put("themeConfig", "{}");
        row.put("customCode", "");
        row.put("mediaLayout", "[]");
        row.put("reviewStatus", status);
        row.put("reviewComment", "");
        row.put("overdue", 0);
        return row;
    }
}
