package com.studyforge.interaction.learning.service.impl;

import com.studyforge.ai.service.AiService;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.content.mapper.CategoryMapper;
import com.studyforge.interaction.learning.dto.UpdateLearningMemoryRequest;
import com.studyforge.interaction.learning.entity.UserLearningProfile;
import com.studyforge.interaction.learning.mapper.LearningSignalMapper;
import com.studyforge.interaction.learning.mapper.UserLearningProfileMapper;
import com.studyforge.interaction.learning.model.InterestTag;
import com.studyforge.interaction.learning.model.SemanticTag;
import com.studyforge.interaction.learning.service.UserLearningProfileService;
import com.studyforge.interaction.learning.support.ImportanceScorer;
import com.studyforge.interaction.learning.support.InterestTagCodec;
import com.studyforge.interaction.learning.support.LearningTextTagger;
import com.studyforge.interaction.learning.support.SemanticTagParser;
import com.studyforge.interaction.learning.vo.LearningMemoryVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLearningProfileServiceImpl implements UserLearningProfileService {
    private static final int MAX_MEMORY_CHARS = 800;
    private static final int REFRESH_LIMIT_PER_HOUR = 10;
    private static final Map<Long, List<LocalDateTime>> REFRESH_GUARD = new ConcurrentHashMap<>();

    private final UserLearningProfileMapper userLearningProfileMapper;
    private final LearningSignalMapper learningSignalMapper;
    private final CategoryMapper categoryMapper;
    private final AiService aiService;

    public UserLearningProfileServiceImpl(UserLearningProfileMapper userLearningProfileMapper,
                                          LearningSignalMapper learningSignalMapper,
                                          CategoryMapper categoryMapper,
                                          AiService aiService) {
        this.userLearningProfileMapper = userLearningProfileMapper;
        this.learningSignalMapper = learningSignalMapper;
        this.categoryMapper = categoryMapper;
        this.aiService = aiService;
    }

    @Override
    public LearningMemoryVO getMine(Long userId) {
        UserLearningProfile profile = requireProfile(userId, "zh_CN");
        return toVO(profile);
    }

    @Override
    @Transactional
    public LearningMemoryVO updateMine(Long userId, UpdateLearningMemoryRequest request) {
        UserLearningProfile profile = requireProfile(userId, "zh_CN");
        if (request == null) {
            return toVO(profile);
        }

        if (request.memoryMd() != null) {
            profile.setMemoryMd(limit(request.memoryMd().trim(), MAX_MEMORY_CHARS));
            profile.setMemoryManuallyEdited(1);
        }
        if (request.memoryMd() != null || request.interestTags() != null) {
            profile.setInterestTags(InterestTagCodec.encode(rebuildInterestTags(profile, request)));
        }
        if (request.aiMemoryEnabled() != null) {
            profile.setAiMemoryEnabled(request.aiMemoryEnabled() ? 1 : 0);
        }
        profile.setProfileVersion(safeVersion(profile.getProfileVersion()) + 1);
        userLearningProfileMapper.update(profile);
        return toVO(reloadProfile(userId));
    }

    @Override
    public void assertRefreshQuota(Long userId) {
        enforceRefreshRateLimit(userId);
    }

    @Override
    @Transactional
    public LearningMemoryVO refreshMine(Long userId, String languageCode) {
        UserLearningProfile profile = requireProfile(userId, languageCode);
        List<InterestTag> autoTags = buildAutoTags(userId, languageCode);
        List<InterestTag> manualTags = InterestTagCodec.decode(profile.getInterestTags())
                .stream()
                .filter(tag -> "manual".equalsIgnoreCase(tag.source()))
                .toList();
        List<InterestTag> merged = InterestTagCodec.merge(autoTags, manualTags);
        profile.setInterestTags(InterestTagCodec.encode(merged));
        profile.setMemoryMd(buildRuleMemoryMd(userId, languageCode, merged));
        profile.setMemoryManuallyEdited(0);
        profile.setLastRefreshedAt(LocalDateTime.now());
        profile.setProfileVersion(safeVersion(profile.getProfileVersion()) + 1);
        userLearningProfileMapper.update(profile);
        return toVO(reloadProfile(userId));
    }

    @Override
    @Transactional
    public void syncAutoSignals(Long userId, String languageCode) {
        UserLearningProfile profile = requireProfile(userId, languageCode);
        List<InterestTag> autoTags = buildAutoTags(userId, languageCode);
        List<InterestTag> manualTags = InterestTagCodec.decode(profile.getInterestTags())
                .stream()
                .filter(tag -> "manual".equalsIgnoreCase(tag.source()))
                .toList();
        profile.setInterestTags(InterestTagCodec.encode(InterestTagCodec.merge(autoTags, manualTags)));
        if (!isMemoryManuallyEdited(profile)) {
            profile.setMemoryMd(buildRuleMemoryMd(userId, languageCode, InterestTagCodec.merge(autoTags, manualTags)));
        }
        profile.setProfileVersion(safeVersion(profile.getProfileVersion()) + 1);
        userLearningProfileMapper.update(profile);
    }

    @Override
    @Transactional
    public void ensureProfile(Long userId, String languageCode) {
        if (userId == null) {
            return;
        }
        UserLearningProfile existing = userLearningProfileMapper.selectByUserId(userId);
        if (existing != null) {
            return;
        }
        UserLearningProfile profile = new UserLearningProfile();
        profile.setUserId(userId);
        List<InterestTag> autoTags = buildAutoTags(userId, languageCode);
        profile.setInterestTags(InterestTagCodec.encode(autoTags));
        profile.setMemoryMd(buildRuleMemoryMd(userId, languageCode, autoTags));
        profile.setProfileVersion(1);
        profile.setAiMemoryEnabled(1);
        profile.setMemoryManuallyEdited(0);
        profile.setLastRefreshedAt(LocalDateTime.now());
        userLearningProfileMapper.insert(profile);
    }

    @Override
    public String getMemoryContext(Long userId) {
        if (userId == null) {
            return "";
        }
        UserLearningProfile profile = userLearningProfileMapper.selectByUserId(userId);
        if (profile == null || profile.getAiMemoryEnabled() != null && profile.getAiMemoryEnabled() == 0) {
            return "";
        }
        String memoryMd = profile.getMemoryMd() == null ? "" : profile.getMemoryMd().trim();
        List<InterestTag> tags = InterestTagCodec.decode(profile.getInterestTags());
        if (memoryMd.isBlank() && tags.isEmpty()) {
            return "";
        }
        String tagLine = tags.stream()
                .sorted((left, right) -> Double.compare(right.weight(), left.weight()))
                .limit(8)
                .map(InterestTag::tag)
                .collect(Collectors.joining(", "));
        StringBuilder builder = new StringBuilder();
        if (!memoryMd.isBlank()) {
            builder.append("【用户学习记忆】\n").append(memoryMd);
        }
        if (!tagLine.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("【近期关注标签】\n").append(tagLine);
        }
        return builder.toString();
    }

    @Override
    public String buildRefreshSignals(Long userId, String languageCode) {
        List<InterestTag> tags = buildAutoTags(userId, languageCode);
        List<String> titles = learningSignalMapper.selectTopFavoriteTitles(userId, languageCode, 10);
        List<Long> categoryIds = learningSignalMapper.selectTopCategoryIds(userId, 3);
        List<String> categories = categoryIds.stream()
                .map(categoryMapper::selectById)
                .filter(category -> category != null)
                .map(category -> category.getCategoryCode())
                .toList();

        String tagSummary = tags.stream()
                .map(tag -> tag.tag() + ":" + String.format(Locale.ROOT, "%.2f", tag.weight()))
                .collect(Collectors.joining(", "));
        String titleSummary = String.join(" | ", titles);
        String categorySummary = String.join(", ", categories);
        return """
                tags=%s
                categories=%s
                favoriteTitles=%s
                """.formatted(tagSummary, categorySummary, titleSummary).trim();
    }

    @Override
    @Transactional
    public LearningMemoryVO applyGeneratedProfile(Long userId, String memoryMd, List<InterestTag> autoTags) {
        UserLearningProfile profile = requireProfile(userId, "zh_CN");
        List<InterestTag> manualTags = InterestTagCodec.decode(profile.getInterestTags())
                .stream()
                .filter(tag -> "manual".equalsIgnoreCase(tag.source()))
                .toList();
        if (memoryMd != null && !memoryMd.isBlank()) {
            profile.setMemoryMd(limit(memoryMd.trim(), MAX_MEMORY_CHARS));
            profile.setMemoryManuallyEdited(0);
        }
        if (autoTags != null && !autoTags.isEmpty()) {
            profile.setInterestTags(InterestTagCodec.encode(InterestTagCodec.merge(autoTags, manualTags)));
        }
        profile.setLastRefreshedAt(LocalDateTime.now());
        profile.setProfileVersion(safeVersion(profile.getProfileVersion()) + 1);
        userLearningProfileMapper.update(profile);
        return toVO(reloadProfile(userId));
    }

    private UserLearningProfile requireProfile(Long userId, String languageCode) {
        ensureProfile(userId, languageCode);
        UserLearningProfile profile = userLearningProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "learning profile unavailable");
        }
        return profile;
    }

    private List<InterestTag> buildAutoTags(Long userId, String languageCode) {
        List<String> lines = learningSignalMapper.selectFavoriteAiTagLines(userId, languageCode);
        return ImportanceScorer.aggregateTagsFromRaw(lines, 12);
    }

    private String buildRuleMemoryMd(Long userId, String languageCode, List<InterestTag> tags) {
        List<String> titles = learningSignalMapper.selectTopFavoriteTitles(userId, languageCode, 5);
        List<Long> categoryIds = learningSignalMapper.selectTopCategoryIds(userId, 3);
        List<String> categories = categoryIds.stream()
                .map(categoryMapper::selectById)
                .filter(category -> category != null)
                .map(category -> category.getCategoryCode())
                .toList();

        String tagLine = tags.stream().map(InterestTag::tag).limit(8).collect(Collectors.joining("、"));
        String titleLine = titles.isEmpty() ? "暂无" : String.join("；", titles);
        String categoryLine = categories.isEmpty() ? "暂无" : String.join("、", categories);

        return limit("""
                ## 近期学习重点
                - 关注标签：%s

                ## 常看主题
                - %s

                ## 高权重收藏
                - %s
                """.formatted(
                tagLine.isBlank() ? "待积累" : tagLine,
                categoryLine,
                titleLine
        ).trim(), MAX_MEMORY_CHARS);
    }

    private LearningMemoryVO toVO(UserLearningProfile profile) {
        return new LearningMemoryVO(
                profile.getMemoryMd() == null ? "" : profile.getMemoryMd(),
                InterestTagCodec.decode(profile.getInterestTags()),
                profile.getAiMemoryEnabled() == null || profile.getAiMemoryEnabled() != 0,
                isMemoryManuallyEdited(profile),
                profile.getLastRefreshedAt(),
                safeVersion(profile.getProfileVersion())
        );
    }

    private List<InterestTag> rebuildInterestTags(UserLearningProfile profile, UpdateLearningMemoryRequest request) {
        List<InterestTag> existing = InterestTagCodec.decode(profile.getInterestTags());
        List<InterestTag> manualTags = request != null && request.interestTags() != null
                ? request.interestTags().stream().map(tag -> new InterestTag(tag.tag(), clamp(tag.weight()), "manual")).toList()
                : existing.stream().filter(tag -> "manual".equalsIgnoreCase(tag.source())).toList();
        if (isMemoryManuallyEdited(profile) && profile.getMemoryMd() != null && !profile.getMemoryMd().isBlank()) {
            return InterestTagCodec.merge(extractSemanticInterestTags(profile.getMemoryMd()), manualTags);
        }
        List<InterestTag> memoryTags = LearningTextTagger.memoryDerivedTags(profile.getMemoryMd())
                .stream()
                .map(tag -> new InterestTag(tag.tag(), tag.weight(), "memory", "like"))
                .toList();
        List<InterestTag> autoTags = existing.stream()
                .filter(tag -> "auto".equalsIgnoreCase(tag.source()))
                .toList();
        return InterestTagCodec.merge(InterestTagCodec.merge(autoTags, memoryTags), manualTags);
    }

    private List<InterestTag> extractSemanticInterestTags(String memoryMd) {
        String raw = aiService.extractMemorySemanticTags(memoryMd, "zh_CN");
        List<SemanticTag> parsed = SemanticTagParser.parseTags(raw);
        if (!parsed.isEmpty()) {
            return SemanticTagParser.toInterestTags(parsed, "semantic");
        }
        return LearningTextTagger.memoryDerivedTags(memoryMd)
                .stream()
                .map(tag -> new InterestTag(tag.tag(), tag.weight(), "memory", "like"))
                .toList();
    }

    private UserLearningProfile reloadProfile(Long userId) {
        UserLearningProfile profile = userLearningProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "learning profile unavailable");
        }
        return profile;
    }

    private boolean isMemoryManuallyEdited(UserLearningProfile profile) {
        return profile.getMemoryManuallyEdited() != null && profile.getMemoryManuallyEdited() == 1;
    }

    private void enforceRefreshRateLimit(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> attempts = REFRESH_GUARD.computeIfAbsent(userId, key -> new ArrayList<>());
        attempts.removeIf(time -> time.isBefore(now.minusHours(1)));
        if (attempts.size() >= REFRESH_LIMIT_PER_HOUR) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "刷新太频繁，请稍后再试");
        }
        attempts.add(now);
    }

    private int safeVersion(Integer version) {
        return version == null ? 1 : version;
    }

    private double clamp(double weight) {
        return Math.max(0.0, Math.min(1.0, weight));
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
