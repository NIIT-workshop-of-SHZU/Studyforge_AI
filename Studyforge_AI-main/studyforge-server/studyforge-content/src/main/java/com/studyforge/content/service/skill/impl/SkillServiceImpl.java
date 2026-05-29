package com.studyforge.content.service.skill.impl;

import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.content.entity.skill.LearningRoadmap;
import com.studyforge.content.entity.skill.RoadmapNode;
import com.studyforge.content.entity.skill.SkillTopic;
import com.studyforge.content.entity.skill.SkillTopicI18n;
import com.studyforge.content.entity.skill.UserRoadmapProgress;
import com.studyforge.content.mapper.skill.LearningRoadmapMapper;
import com.studyforge.content.mapper.skill.RoadmapNodeMapper;
import com.studyforge.content.mapper.skill.SkillTopicI18nMapper;
import com.studyforge.content.mapper.skill.SkillTopicMapper;
import com.studyforge.content.mapper.skill.UserRoadmapProgressMapper;
import com.studyforge.content.service.skill.SkillService;
import com.studyforge.content.vo.skill.RoadmapDetailVO;
import com.studyforge.content.vo.skill.RoadmapNodeVO;
import com.studyforge.content.vo.skill.RoadmapSummaryVO;
import com.studyforge.content.vo.skill.SkillDetailVO;
import com.studyforge.content.vo.skill.SkillSummaryVO;
import com.studyforge.content.vo.skill.UserRoadmapProgressVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillServiceImpl implements SkillService {
    private static final String DEFAULT_LANGUAGE = "zh_CN";

    private final SkillTopicMapper skillTopicMapper;
    private final SkillTopicI18nMapper skillTopicI18nMapper;
    private final LearningRoadmapMapper learningRoadmapMapper;
    private final RoadmapNodeMapper roadmapNodeMapper;
    private final UserRoadmapProgressMapper userRoadmapProgressMapper;

    public SkillServiceImpl(SkillTopicMapper skillTopicMapper,
                           SkillTopicI18nMapper skillTopicI18nMapper,
                           LearningRoadmapMapper learningRoadmapMapper,
                           RoadmapNodeMapper roadmapNodeMapper,
                           UserRoadmapProgressMapper userRoadmapProgressMapper) {
        this.skillTopicMapper = skillTopicMapper;
        this.skillTopicI18nMapper = skillTopicI18nMapper;
        this.learningRoadmapMapper = learningRoadmapMapper;
        this.roadmapNodeMapper = roadmapNodeMapper;
        this.userRoadmapProgressMapper = userRoadmapProgressMapper;
    }

    @Override
    public List<SkillSummaryVO> listSkills(String languageCode, String difficulty, int limit) {
        String normalizedLanguage = normalizeLanguage(languageCode);
        List<SkillTopic> skills;

        if (difficulty != null && !difficulty.isBlank()) {
            skills = skillTopicMapper.selectByDifficulty(difficulty.toUpperCase(), "ACTIVE");
        } else {
            skills = skillTopicMapper.selectAll("ACTIVE");
        }

        return skills.stream()
                .limit(limit > 0 ? Math.min(limit, 50) : 20)
                .map(skill -> toSkillSummary(skill, normalizedLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public SkillDetailVO getSkillDetail(String skillCode, String languageCode) {
        String normalizedLanguage = normalizeLanguage(languageCode);
        SkillTopic skill = skillTopicMapper.selectByCode(skillCode);
        if (skill == null || !"ACTIVE".equals(skill.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "skill not found");
        }

        SkillTopicI18n i18n = resolveSkillI18n(skill, normalizedLanguage);
        if (i18n == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "skill content not found");
        }

        List<LearningRoadmap> roadmaps = learningRoadmapMapper.selectBySkillId(skill.getSkillId(), "PUBLISHED");

        return new SkillDetailVO(
                skill.getSkillId(),
                skill.getSkillCode(),
                i18n.getName(),
                i18n.getDescription(),
                skill.getCoverImageUrl(),
                skill.getDifficulty(),
                safeInt(skill.getLearnerCount()),
                toDouble(skill.getRatingScore()),
                safeInt(skill.getPostCount()),
                roadmaps.stream().map(r -> toRoadmapSummary(r, normalizedLanguage)).toList(),
                skill.getCreatedTime(),
                skill.getUpdatedTime()
        );
    }

    @Override
    public List<RoadmapSummaryVO> listRoadmapsBySkill(Long skillId, String languageCode, int limit) {
        String normalizedLanguage = normalizeLanguage(languageCode);
        int normalizedLimit = limit > 0 ? Math.min(limit, 50) : 20;

        return learningRoadmapMapper.selectBySkillId(skillId, "PUBLISHED")
                .stream()
                .limit(normalizedLimit)
                .map(roadmap -> toRoadmapSummary(roadmap, normalizedLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<RoadmapSummaryVO> listPopularRoadmaps(String languageCode, int limit) {
        String normalizedLanguage = normalizeLanguage(languageCode);
        int normalizedLimit = limit > 0 ? Math.min(limit, 50) : 20;

        return learningRoadmapMapper.selectPopular(normalizedLimit, "PUBLISHED")
                .stream()
                .map(roadmap -> toRoadmapSummary(roadmap, normalizedLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public RoadmapDetailVO getRoadmapDetail(Long roadmapId, String languageCode) {
        String normalizedLanguage = normalizeLanguage(languageCode);
        LearningRoadmap roadmap = learningRoadmapMapper.selectById(roadmapId);
        if (roadmap == null || !"PUBLISHED".equals(roadmap.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "roadmap not found");
        }

        List<RoadmapNode> nodes = roadmapNodeMapper.selectByRoadmapId(roadmapId);
        SkillTopic skill = skillTopicMapper.selectById(roadmap.getSkillId());
        SkillTopicI18n skillI18n = skill != null ? resolveSkillI18n(skill, normalizedLanguage) : null;

        return new RoadmapDetailVO(
                roadmap.getRoadmapId(),
                roadmap.getSkillId(),
                skillI18n != null ? skillI18n.getName() : "",
                roadmap.getAuthorId(),
                roadmap.getTitle(),
                roadmap.getSummary(),
                roadmap.getDifficulty(),
                safeInt(roadmap.getEstimatedDays()),
                roadmap.getCoverImageUrl(),
                toDouble(roadmap.getRatingScore()),
                safeInt(roadmap.getLearnerCount()),
                safeInt(roadmap.getNodeCount()),
                nodes.stream().map(this::toRoadmapNodeVO).toList(),
                roadmap.getCreatedTime(),
                roadmap.getUpdatedTime()
        );
    }

    @Override
    @Transactional
    public Long startRoadmap(Long userId, Long roadmapId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        LearningRoadmap roadmap = learningRoadmapMapper.selectById(roadmapId);
        if (roadmap == null || !"PUBLISHED".equals(roadmap.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "roadmap not found");
        }

        UserRoadmapProgress existing = userRoadmapProgressMapper.selectByUserAndRoadmap(userId, roadmapId);
        if (existing != null) {
            return existing.getProgressId();
        }

        List<RoadmapNode> nodes = roadmapNodeMapper.selectByRoadmapId(roadmapId);
        Long firstNodeId = nodes.isEmpty() ? null : nodes.get(0).getNodeId();

        UserRoadmapProgress progress = new UserRoadmapProgress();
        progress.setUserId(userId);
        progress.setRoadmapId(roadmapId);
        progress.setCurrentNodeId(firstNodeId);
        progress.setCompletedNodes(0);
        progress.setProgressPercent(BigDecimal.ZERO);
        progress.setStatus("IN_PROGRESS");
        userRoadmapProgressMapper.insert(progress);

        learningRoadmapMapper.incrementLearnerCount(roadmapId, 1);

        return progress.getProgressId();
    }

    @Override
    @Transactional
    public UserRoadmapProgressVO completeNode(Long userId, Long nodeId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        RoadmapNode node = roadmapNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "node not found");
        }

        UserRoadmapProgress progress = userRoadmapProgressMapper.selectByUserAndRoadmap(userId, node.getRoadmapId());
        if (progress == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "please start the roadmap first");
        }

        List<RoadmapNode> allNodes = roadmapNodeMapper.selectByRoadmapId(node.getRoadmapId());
        int totalNodes = allNodes.size();
        int newCompleted = progress.getCompletedNodes() + 1;
        BigDecimal percent = totalNodes > 0
                ? BigDecimal.valueOf(newCompleted).divide(BigDecimal.valueOf(totalNodes), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        progress.setCompletedNodes(newCompleted);
        progress.setProgressPercent(percent);

        if (newCompleted >= totalNodes) {
            progress.setStatus("COMPLETED");
            progress.setCurrentNodeId(null);
        } else {
            RoadmapNode nextNode = allNodes.stream()
                    .filter(n -> n.getSortNo() > node.getSortNo())
                    .findFirst()
                    .orElse(null);
            progress.setCurrentNodeId(nextNode != null ? nextNode.getNodeId() : null);
        }

        userRoadmapProgressMapper.updateById(progress);

        return toProgressVO(progress, allNodes);
    }

    @Override
    public UserRoadmapProgressVO getProgress(Long userId, Long roadmapId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        UserRoadmapProgress progress = userRoadmapProgressMapper.selectByUserAndRoadmap(userId, roadmapId);
        if (progress == null) {
            return null;
        }

        List<RoadmapNode> allNodes = roadmapNodeMapper.selectByRoadmapId(roadmapId);
        return toProgressVO(progress, allNodes);
    }

    @Override
    public List<UserRoadmapProgressVO> listUserProgress(Long userId, String status) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        return userRoadmapProgressMapper.selectByUserId(userId, status)
                .stream()
                .map(progress -> {
                    List<RoadmapNode> allNodes = roadmapNodeMapper.selectByRoadmapId(progress.getRoadmapId());
                    return toProgressVO(progress, allNodes);
                })
                .toList();
    }

    private SkillSummaryVO toSkillSummary(SkillTopic skill, String languageCode) {
        SkillTopicI18n i18n = resolveSkillI18n(skill, languageCode);
        if (i18n == null) {
            return null;
        }

        return new SkillSummaryVO(
                skill.getSkillId(),
                skill.getSkillCode(),
                i18n.getName(),
                i18n.getDescription(),
                skill.getCoverImageUrl(),
                skill.getDifficulty(),
                safeInt(skill.getLearnerCount()),
                toDouble(skill.getRatingScore()),
                safeInt(skill.getPostCount())
        );
    }

    private RoadmapSummaryVO toRoadmapSummary(LearningRoadmap roadmap, String languageCode) {
        SkillTopic skill = skillTopicMapper.selectById(roadmap.getSkillId());
        SkillTopicI18n skillI18n = skill != null ? resolveSkillI18n(skill, languageCode) : null;

        return new RoadmapSummaryVO(
                roadmap.getRoadmapId(),
                roadmap.getSkillId(),
                skillI18n != null ? skillI18n.getName() : "",
                roadmap.getTitle(),
                roadmap.getSummary(),
                roadmap.getDifficulty(),
                safeInt(roadmap.getEstimatedDays()),
                roadmap.getCoverImageUrl(),
                toDouble(roadmap.getRatingScore()),
                safeInt(roadmap.getLearnerCount()),
                safeInt(roadmap.getNodeCount())
        );
    }

    private RoadmapNodeVO toRoadmapNodeVO(RoadmapNode node) {
        return new RoadmapNodeVO(
                node.getNodeId(),
                node.getSortNo(),
                node.getTitle(),
                node.getDescription(),
                safeInt(node.getEstimatedMinutes()),
                node.getRelatedPostId()
        );
    }

    private UserRoadmapProgressVO toProgressVO(UserRoadmapProgress progress, List<RoadmapNode> allNodes) {
        LearningRoadmap roadmap = learningRoadmapMapper.selectById(progress.getRoadmapId());
        List<RoadmapNodeVO> nodeVOs = allNodes.stream().map(this::toRoadmapNodeVO).toList();

        return new UserRoadmapProgressVO(
                progress.getProgressId(),
                progress.getUserId(),
                progress.getRoadmapId(),
                roadmap != null ? roadmap.getTitle() : "",
                progress.getCurrentNodeId(),
                safeInt(progress.getCompletedNodes()),
                allNodes.size(),
                toDouble(progress.getProgressPercent()),
                progress.getStatus(),
                nodeVOs,
                progress.getStartedTime(),
                progress.getUpdatedTime()
        );
    }

    private SkillTopicI18n resolveSkillI18n(SkillTopic skill, String languageCode) {
        SkillTopicI18n i18n = skillTopicI18nMapper.selectBySkillIdAndLanguage(skill.getSkillId(), languageCode);
        if (i18n != null) {
            return i18n;
        }

        i18n = skillTopicI18nMapper.selectBySkillIdAndLanguage(skill.getSkillId(), DEFAULT_LANGUAGE);
        if (i18n != null) {
            return i18n;
        }

        List<SkillTopicI18n> all = skillTopicI18nMapper.selectBySkillId(skill.getSkillId());
        return all.isEmpty() ? null : all.get(0);
    }

    private String normalizeLanguage(String languageCode) {
        return languageCode == null || languageCode.isBlank() ? DEFAULT_LANGUAGE : languageCode;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
