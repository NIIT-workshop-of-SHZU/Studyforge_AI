package com.studyforge.system.service.impl;

import com.studyforge.common.enums.UserStatus;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.system.dto.HomepageDraftRequest;
import com.studyforge.system.dto.HomepageReviewRequest;
import com.studyforge.system.dto.PublishHomepageRequest;
import com.studyforge.system.entity.CommunityHomepageDesign;
import com.studyforge.system.entity.User;
import com.studyforge.system.entity.UserHomepage;
import com.studyforge.system.mapper.HomepageMapper;
import com.studyforge.system.mapper.UserMapper;
import com.studyforge.system.service.HomepageService;
import com.studyforge.system.vo.AdminHomepageReviewVO;
import com.studyforge.system.vo.CommunityHomepageDesignVO;
import com.studyforge.system.vo.UserHomepageVO;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomepageServiceImpl implements HomepageService {
    private static final String DEFAULT_TEMPLATE = "GITHUB_DEFAULT";
    private static final String DEFAULT_LAYOUT = "default";
    private static final String DEFAULT_THEME = "{}";
    private static final String EMPTY_MEDIA_LAYOUT = "[]";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String TARGET_USER_HOMEPAGE = "USER_HOMEPAGE";
    private static final String TARGET_COMMUNITY_DESIGN = "COMMUNITY_DESIGN";
    private static final String DECISION_APPROVE = "APPROVE";
    private static final String DECISION_REJECT = "REJECT";
    private static final Set<String> LAYOUT_MODES = Set.of(DEFAULT_LAYOUT, "code", "media");
    private static final int MAX_TEMPLATE_LENGTH = 50;
    private static final int MAX_LAYOUT_LENGTH = 20;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_SUMMARY_LENGTH = 300;
    private static final int MAX_REVIEW_REMARK_LENGTH = 500;
    private static final int MAX_THEME_LENGTH = 65_000;
    private static final int MAX_DRAFT_BODY_LENGTH = 1_000_000;

    private final HomepageMapper homepageMapper;
    private final UserMapper userMapper;

    public HomepageServiceImpl(HomepageMapper homepageMapper, UserMapper userMapper) {
        this.homepageMapper = homepageMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserHomepageVO getMyHomepage(Long userId) {
        requireActiveUser(userId);
        return toUserHomepageVO(ensureHomepage(userId), true);
    }

    @Override
    public UserHomepageVO getUserHomepage(Long viewerId, Long userId) {
        requireActiveUser(userId);
        boolean self = viewerId != null && viewerId.equals(userId);
        UserHomepage homepage = homepageMapper.selectUserHomepageByUserId(userId);
        if (homepage == null) {
            return toUserHomepageVO(defaultHomepage(userId, 0L), self);
        }
        return toUserHomepageVO(homepage, self);
    }

    @Override
    @Transactional
    public UserHomepageVO saveDraft(Long userId, HomepageDraftRequest request) {
        requireActiveUser(userId);
        UserHomepage homepage = ensureHomepage(userId);
        applyDraft(homepage, request);
        homepageMapper.updateUserHomepageDraft(homepage);
        return toUserHomepageVO(selectHomepageOrFallback(homepage), true);
    }

    @Override
    @Transactional
    public UserHomepageVO submitReview(Long userId) {
        requireActiveUser(userId);
        UserHomepage homepage = ensureHomepage(userId);
        homepageMapper.submitUserHomepage(homepage.getHomepageId());
        return toUserHomepageVO(selectHomepageOrFallback(homepage), true);
    }

    @Override
    @Transactional
    public CommunityHomepageDesignVO publishCommunity(Long userId, PublishHomepageRequest request) {
        requireActiveUser(userId);
        String title = trimToNull(request == null ? null : request.title());
        if (title == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "homepage design title is required");
        }

        UserHomepage homepage = ensureHomepage(userId);
        CommunityHomepageDesign design = new CommunityHomepageDesign();
        design.setAuthorId(userId);
        design.setSourceHomepageId(homepage.getHomepageId());
        design.setTitle(limit(title, MAX_TITLE_LENGTH));
        design.setSummary(limit(text(request == null ? null : request.summary()), MAX_SUMMARY_LENGTH));
        design.setTemplateType(textOrDefault(homepage.getTemplateType(), DEFAULT_TEMPLATE));
        design.setLayoutMode(textOrDefault(homepage.getLayoutMode(), DEFAULT_LAYOUT));
        design.setThemeConfig(textOrDefault(homepage.getThemeConfig(), DEFAULT_THEME));
        design.setCustomCode(text(homepage.getCustomCodeDraft()));
        design.setMediaLayout(textOrDefault(homepage.getMediaLayoutDraft(), EMPTY_MEDIA_LAYOUT));
        design.setStatus(STATUS_PENDING_REVIEW);
        design.setReviewComment("");
        homepageMapper.insertCommunityDesign(design);
        homepageMapper.updateUserHomepageCommunityTemplate(homepage.getHomepageId(), design.getDesignId());

        CommunityHomepageDesign saved = homepageMapper.selectCommunityDesignById(design.getDesignId());
        return toCommunityDesignVO(saved == null ? design : saved, userId);
    }

    @Override
    public List<CommunityHomepageDesignVO> listCommunityDesigns(Long viewerId, String keyword, int limit) {
        return homepageMapper.selectApprovedCommunityDesigns(emptyToNull(keyword), normalizeLimit(limit, 100))
                .stream()
                .map(design -> toCommunityDesignVO(design, viewerId))
                .toList();
    }

    @Override
    public CommunityHomepageDesignVO getCommunityDesign(Long viewerId, Long designId) {
        CommunityHomepageDesign design = requireApprovedDesign(designId);
        return toCommunityDesignVO(design, viewerId);
    }

    @Override
    @Transactional
    public UserHomepageVO cloneCommunityDesign(Long userId, Long designId) {
        requireActiveUser(userId);
        CommunityHomepageDesign design = requireApprovedDesign(designId);
        if (userId.equals(design.getAuthorId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "cannot clone your own homepage design");
        }

        UserHomepage homepage = ensureHomepage(userId);
        homepage.setTemplateType(textOrDefault(design.getTemplateType(), DEFAULT_TEMPLATE));
        homepage.setLayoutMode(textOrDefault(design.getLayoutMode(), DEFAULT_LAYOUT));
        homepage.setThemeConfig(textOrDefault(design.getThemeConfig(), DEFAULT_THEME));
        homepage.setCustomCodeDraft(text(design.getCustomCode()));
        homepage.setMediaLayoutDraft(textOrDefault(design.getMediaLayout(), EMPTY_MEDIA_LAYOUT));
        homepage.setCommunityTemplateId(design.getDesignId());
        homepageMapper.updateUserHomepageDraft(homepage);
        homepageMapper.incrementCommunityCloneCount(design.getDesignId());
        return toUserHomepageVO(selectHomepageOrFallback(homepage), true);
    }

    @Override
    public List<AdminHomepageReviewVO> listReviews(String status, int limit) {
        return homepageMapper.selectHomepageReviews(normalizeReviewStatus(status), normalizeLimit(limit, 100))
                .stream()
                .map(this::toAdminReviewVO)
                .toList();
    }

    @Override
    @Transactional
    public AdminHomepageReviewVO review(Long adminId, Long targetId, HomepageReviewRequest request) {
        if (adminId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (targetId == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "review target id is required");
        }

        String targetType = normalizeTargetType(request == null ? null : request.targetType());
        String decision = normalizeDecision(request == null ? null : request.decision());
        String remark = limit(text(request == null ? null : request.remark()), MAX_REVIEW_REMARK_LENGTH);
        Map<String, Object> row = homepageMapper.selectHomepageReview(targetType, targetId);
        if (row == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "homepage review target not found");
        }
        if (!STATUS_PENDING_REVIEW.equals(stringValue(row.get("reviewStatus")))) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "review target has already been handled");
        }

        if (TARGET_USER_HOMEPAGE.equals(targetType)) {
            if (DECISION_APPROVE.equals(decision)) {
                homepageMapper.approveUserHomepage(targetId, adminId, remark);
            } else {
                homepageMapper.rejectUserHomepage(targetId, adminId, remark);
            }
        } else if (DECISION_APPROVE.equals(decision)) {
            homepageMapper.approveCommunityDesign(targetId, adminId, remark);
        } else {
            homepageMapper.rejectCommunityDesign(targetId, adminId, remark);
        }

        homepageMapper.insertAudit(adminId, targetType, targetId, decision + "_" + targetType, remark);
        Map<String, Object> refreshed = homepageMapper.selectHomepageReview(targetType, targetId);
        return toAdminReviewVO(refreshed == null ? row : refreshed);
    }

    private User requireActiveUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private UserHomepage ensureHomepage(Long userId) {
        UserHomepage existing = homepageMapper.selectUserHomepageByUserId(userId);
        if (existing != null) {
            return existing;
        }

        UserHomepage homepage = defaultHomepage(userId, null);
        try {
            homepageMapper.insertUserHomepage(homepage);
            return homepage;
        } catch (DuplicateKeyException exception) {
            UserHomepage concurrent = homepageMapper.selectUserHomepageByUserId(userId);
            if (concurrent != null) {
                return concurrent;
            }
            throw exception;
        }
    }

    private UserHomepage defaultHomepage(Long userId, Long homepageId) {
        UserHomepage homepage = new UserHomepage();
        homepage.setHomepageId(homepageId);
        homepage.setUserId(userId);
        homepage.setTemplateType(DEFAULT_TEMPLATE);
        homepage.setLayoutMode(DEFAULT_LAYOUT);
        homepage.setThemeConfig(DEFAULT_THEME);
        homepage.setCustomCodeDraft("");
        homepage.setMediaLayoutDraft(EMPTY_MEDIA_LAYOUT);
        homepage.setPublishedTemplateType(DEFAULT_TEMPLATE);
        homepage.setPublishedLayoutMode(DEFAULT_LAYOUT);
        homepage.setPublishedThemeConfig(DEFAULT_THEME);
        homepage.setPublishedCustomCode("");
        homepage.setPublishedMediaLayout(EMPTY_MEDIA_LAYOUT);
        homepage.setPublishedVersion(0);
        homepage.setReviewStatus(STATUS_DRAFT);
        homepage.setReviewComment("");
        return homepage;
    }

    private void applyDraft(UserHomepage homepage, HomepageDraftRequest request) {
        String layoutMode = textOrDefault(request == null ? null : request.layoutMode(), DEFAULT_LAYOUT).toLowerCase(Locale.ROOT);
        if (!LAYOUT_MODES.contains(layoutMode)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "unsupported homepage layout mode");
        }

        homepage.setTemplateType(limit(textOrDefault(request == null ? null : request.templateType(), DEFAULT_TEMPLATE), MAX_TEMPLATE_LENGTH));
        homepage.setLayoutMode(limit(layoutMode, MAX_LAYOUT_LENGTH));
        homepage.setThemeConfig(limit(textOrDefault(request == null ? null : request.themeConfig(), DEFAULT_THEME), MAX_THEME_LENGTH));
        homepage.setCustomCodeDraft(limit(text(request == null ? null : request.customCodeDraft()), MAX_DRAFT_BODY_LENGTH));
        homepage.setMediaLayoutDraft(limit(textOrDefault(request == null ? null : request.mediaLayoutDraft(), EMPTY_MEDIA_LAYOUT), MAX_DRAFT_BODY_LENGTH));
    }

    private UserHomepage selectHomepageOrFallback(UserHomepage fallback) {
        UserHomepage refreshed = homepageMapper.selectUserHomepageById(fallback.getHomepageId());
        return refreshed == null ? fallback : refreshed;
    }

    private CommunityHomepageDesign requireApprovedDesign(Long designId) {
        if (designId == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "homepage design id is required");
        }
        CommunityHomepageDesign design = homepageMapper.selectCommunityDesignById(designId);
        if (design == null || !STATUS_APPROVED.equals(design.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "homepage design not found");
        }
        return design;
    }

    private String normalizeReviewStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return "ALL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!STATUS_PENDING_REVIEW.equals(normalized)
                && !STATUS_APPROVED.equals(normalized)
                && !STATUS_REJECTED.equals(normalized)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "unsupported review status");
        }
        return normalized;
    }

    private String normalizeTargetType(String targetType) {
        String normalized = trimToNull(targetType);
        if (normalized == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "review target type is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!TARGET_USER_HOMEPAGE.equals(normalized) && !TARGET_COMMUNITY_DESIGN.equals(normalized)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "unsupported review target type");
        }
        return normalized;
    }

    private String normalizeDecision(String decision) {
        String normalized = trimToNull(decision);
        if (normalized == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "review decision is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!DECISION_APPROVE.equals(normalized) && !DECISION_REJECT.equals(normalized)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "unsupported review decision");
        }
        return normalized;
    }

    private int normalizeLimit(int limit, int max) {
        if (limit <= 0) {
            return Math.min(30, max);
        }
        return Math.min(limit, max);
    }

    private UserHomepageVO toUserHomepageVO(UserHomepage homepage, boolean self) {
        String publishedTemplate = textOrDefault(homepage.getPublishedTemplateType(), DEFAULT_TEMPLATE);
        String publishedLayout = textOrDefault(homepage.getPublishedLayoutMode(), DEFAULT_LAYOUT);
        String publishedTheme = textOrDefault(homepage.getPublishedThemeConfig(), DEFAULT_THEME);
        String publishedCustom = text(homepage.getPublishedCustomCode());
        String publishedMedia = textOrDefault(homepage.getPublishedMediaLayout(), EMPTY_MEDIA_LAYOUT);

        return new UserHomepageVO(
                homepage.getHomepageId(),
                homepage.getUserId(),
                self ? textOrDefault(homepage.getTemplateType(), DEFAULT_TEMPLATE) : publishedTemplate,
                self ? textOrDefault(homepage.getLayoutMode(), DEFAULT_LAYOUT) : publishedLayout,
                self ? textOrDefault(homepage.getThemeConfig(), DEFAULT_THEME) : publishedTheme,
                self ? text(homepage.getCustomCodeDraft()) : "",
                self ? textOrDefault(homepage.getMediaLayoutDraft(), EMPTY_MEDIA_LAYOUT) : EMPTY_MEDIA_LAYOUT,
                publishedTemplate,
                publishedLayout,
                publishedTheme,
                publishedCustom,
                publishedMedia,
                homepage.getPublishedVersion() == null ? 0 : homepage.getPublishedVersion(),
                textOrDefault(homepage.getReviewStatus(), STATUS_DRAFT),
                homepage.getSubmittedAt(),
                homepage.getReviewDeadlineAt(),
                text(homepage.getReviewComment()),
                homepage.getCommunityTemplateId(),
                self
        );
    }

    private CommunityHomepageDesignVO toCommunityDesignVO(CommunityHomepageDesign design, Long viewerId) {
        Long authorId = design.getAuthorId();
        boolean canClone = viewerId == null || !viewerId.equals(authorId);
        return new CommunityHomepageDesignVO(
                design.getDesignId(),
                authorId,
                text(design.getAuthorName()),
                text(design.getAuthorAvatarUrl()),
                text(design.getTitle()),
                text(design.getSummary()),
                textOrDefault(design.getTemplateType(), DEFAULT_TEMPLATE),
                textOrDefault(design.getLayoutMode(), DEFAULT_LAYOUT),
                textOrDefault(design.getThemeConfig(), DEFAULT_THEME),
                text(design.getCustomCode()),
                textOrDefault(design.getMediaLayout(), EMPTY_MEDIA_LAYOUT),
                textOrDefault(design.getStatus(), STATUS_PENDING_REVIEW),
                design.getSubmittedAt(),
                design.getReviewDeadlineAt(),
                text(design.getReviewComment()),
                design.getCloneCount() == null ? 0 : design.getCloneCount(),
                design.getLikeCount() == null ? 0 : design.getLikeCount(),
                design.getFavoriteCount() == null ? 0 : design.getFavoriteCount(),
                design.getCommentCount() == null ? 0 : design.getCommentCount(),
                design.getCreatedTime(),
                canClone
        );
    }

    private AdminHomepageReviewVO toAdminReviewVO(Map<String, Object> row) {
        return new AdminHomepageReviewVO(
                stringValue(row.get("targetType")),
                longValue(row.get("targetId")),
                longValue(row.get("ownerId")),
                stringValue(row.get("ownerName")),
                stringValue(row.get("ownerAvatarUrl")),
                stringValue(row.get("title")),
                stringValue(row.get("summary")),
                stringValue(row.get("templateType")),
                stringValue(row.get("layoutMode")),
                stringValue(row.get("themeConfig")),
                stringValue(row.get("customCode")),
                stringValue(row.get("mediaLayout")),
                stringValue(row.get("reviewStatus")),
                timeValue(row.get("submittedAt")),
                timeValue(row.get("reviewDeadlineAt")),
                stringValue(row.get("reviewComment")),
                booleanValue(row.get("overdue"))
        );
    }

    private String emptyToNull(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : limit(trimmed, 120);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String textOrDefault(String value, String fallback) {
        String normalized = text(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private String limit(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(stringValue(value));
    }

    private LocalDateTime timeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().atStartOfDay();
        }
        return null;
    }
}
