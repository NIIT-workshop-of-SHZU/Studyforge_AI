package com.studyforge.interaction.learning.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.content.entity.Post;
import com.studyforge.content.mapper.PostMapper;
import com.studyforge.content.service.PostQueryService;
import com.studyforge.content.vo.PostSummaryVO;
import com.studyforge.interaction.learning.entity.FavoriteItemRankRow;
import com.studyforge.interaction.learning.entity.UserLearningProfile;
import com.studyforge.interaction.learning.mapper.LearningSignalMapper;
import com.studyforge.interaction.learning.mapper.UserLearningProfileMapper;
import com.studyforge.interaction.learning.model.ImportanceResult;
import com.studyforge.interaction.learning.model.InterestTag;
import com.studyforge.interaction.learning.model.SemanticTag;
import com.studyforge.interaction.learning.service.FavoriteImportanceService;
import com.studyforge.interaction.learning.service.UserLearningProfileService;
import com.studyforge.interaction.learning.support.ImportanceScorer;
import com.studyforge.interaction.learning.support.InterestTagCodec;
import com.studyforge.interaction.mapper.FavoriteCollectionMapper;
import com.studyforge.interaction.entity.FavoriteCollection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteImportanceServiceImpl implements FavoriteImportanceService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final LearningSignalMapper learningSignalMapper;
    private final UserLearningProfileMapper userLearningProfileMapper;
    private final UserLearningProfileService userLearningProfileService;
    private final FavoriteCollectionMapper favoriteCollectionMapper;
    private final PostMapper postMapper;
    private final PostQueryService postQueryService;
    private final PostSemanticTagService postSemanticTagService;

    public FavoriteImportanceServiceImpl(LearningSignalMapper learningSignalMapper,
                                         UserLearningProfileMapper userLearningProfileMapper,
                                         UserLearningProfileService userLearningProfileService,
                                         FavoriteCollectionMapper favoriteCollectionMapper,
                                         PostMapper postMapper,
                                         PostQueryService postQueryService,
                                         PostSemanticTagService postSemanticTagService) {
        this.learningSignalMapper = learningSignalMapper;
        this.userLearningProfileMapper = userLearningProfileMapper;
        this.userLearningProfileService = userLearningProfileService;
        this.favoriteCollectionMapper = favoriteCollectionMapper;
        this.postMapper = postMapper;
        this.postQueryService = postQueryService;
        this.postSemanticTagService = postSemanticTagService;
    }

    @Override
    public List<PostSummaryVO> listCollectionPosts(Long userId,
                                                    Long collectionId,
                                                    String languageCode,
                                                    int limit,
                                                    String sort,
                                                    String tag) {
        requireOwnedCollection(userId, collectionId);
        String normalizedSort = "recent".equalsIgnoreCase(sort) ? "recent" : "importance";
        String normalizedLanguage = languageCode == null || languageCode.isBlank() ? "zh_CN" : languageCode;
        int normalizedLimit = limit <= 0 ? 30 : Math.min(limit, 50);

        if ("importance".equals(normalizedSort)) {
            recomputeCollection(userId, collectionId, normalizedLanguage);
        }

        String normalizedTag = tag == null || tag.isBlank() ? null : tag.trim();
        List<FavoriteItemRankRow> rows = learningSignalMapper.selectCollectionItems(
                userId,
                collectionId,
                normalizedLanguage,
                normalizedTag,
                normalizedSort,
                normalizedLimit
        );

        return rows.stream()
                .map(row -> toRankedSummary(row, normalizedLanguage))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public void recomputeCollection(Long userId, Long collectionId, String languageCode) {
        requireOwnedCollection(userId, collectionId);
        userLearningProfileService.ensureProfile(userId, languageCode);

        ScoreContext scoreContext = loadScoreContext(userId);
        Set<Long> topCategories = new HashSet<>(learningSignalMapper.selectTopCategoryIds(userId, 3));

        List<FavoriteItemRankRow> rows = learningSignalMapper.selectCollectionItems(
                userId,
                collectionId,
                languageCode,
                null,
                "importance",
                500
        );

        for (FavoriteItemRankRow row : rows) {
            ImportanceResult result = scoreRow(userId, row, scoreContext, topCategories, languageCode);
            learningSignalMapper.updateItemScore(
                    row.getCollectionId(),
                    row.getPostId(),
                    userId,
                    result.score(),
                    encodeReasons(result.rankReasons())
            );
        }
    }

    @Override
    @Transactional
    public void recomputeAllForUser(Long userId, String languageCode) {
        userLearningProfileService.ensureProfile(userId, languageCode);
        List<FavoriteItemRankRow> rows = learningSignalMapper.selectAllItemsByUser(userId, languageCode);
        ScoreContext scoreContext = loadScoreContext(userId);
        Set<Long> topCategories = new HashSet<>(learningSignalMapper.selectTopCategoryIds(userId, 3));

        for (FavoriteItemRankRow row : rows) {
            ImportanceResult result = scoreRow(userId, row, scoreContext, topCategories, languageCode);
            learningSignalMapper.updateItemScore(
                    row.getCollectionId(),
                    row.getPostId(),
                    userId,
                    result.score(),
                    encodeReasons(result.rankReasons())
            );
        }
    }

    @Override
    @Transactional
    public void setPinned(Long userId, Long collectionId, Long postId, boolean pinned, String languageCode) {
        requireOwnedCollection(userId, collectionId);
        Post post = postMapper.selectById(postId);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "post not found");
        }
        learningSignalMapper.updateItemPinned(collectionId, postId, userId, pinned ? 1 : 0);
        recomputeCollection(userId, collectionId, languageCode);
    }

    private ImportanceResult scoreRow(Long userId,
                                      FavoriteItemRankRow row,
                                      ScoreContext scoreContext,
                                      Set<Long> topCategories,
                                      String languageCode) {
        int views = learningSignalMapper.countViews(userId, row.getPostId());
        boolean aiEngaged = learningSignalMapper.countAiEngagement(userId, row.getPostId()) > 0;
        int collectionCount = learningSignalMapper.countCollectionsForPost(userId, row.getPostId());
        List<SemanticTag> postSemanticTags = postSemanticTagService.resolveTags(
                row.getPostId(),
                languageCode,
                row.getTitle(),
                row.getSummary(),
                row.getAiTags()
        );
        List<SemanticTag> userSemanticTags = selectUserSemanticTags(scoreContext);
        return ImportanceScorer.score(
                row.getTitle(),
                row.getSummary(),
                row.getAiTags(),
                row.getOriginalLanguage(),
                scoreContext.memoryMd(),
                scoreContext.memoryPrioritized(),
                row.getCategoryId(),
                topCategories,
                scoreContext.interestTags(),
                userSemanticTags,
                postSemanticTags,
                views,
                aiEngaged,
                collectionCount,
                row.getCreatedTime()
        );
    }

    private List<SemanticTag> selectUserSemanticTags(ScoreContext scoreContext) {
        return scoreContext.interestTags().stream()
                .filter(tag -> includeUserTag(tag, scoreContext.memoryPrioritized()))
                .map(InterestTag::toSemanticTag)
                .toList();
    }

    private boolean includeUserTag(InterestTag tag, boolean memoryPrioritized) {
        if (!memoryPrioritized) {
            return true;
        }
        String source = tag.source() == null ? "" : tag.source().toLowerCase();
        return source.equals("semantic") || source.equals("memory") || source.equals("manual");
    }

    private ScoreContext loadScoreContext(Long userId) {
        UserLearningProfile profile = userLearningProfileMapper.selectByUserId(userId);
        if (profile == null) {
            return new ScoreContext("", false, List.of());
        }
        String memoryMd = profile.getMemoryMd() == null ? "" : profile.getMemoryMd();
        List<InterestTag> interestTags = InterestTagCodec.decode(profile.getInterestTags());
        boolean memoryPrioritized = profile.getMemoryManuallyEdited() != null && profile.getMemoryManuallyEdited() == 1;
        return new ScoreContext(memoryMd, memoryPrioritized, interestTags);
    }

    private record ScoreContext(String memoryMd, boolean memoryPrioritized, List<InterestTag> interestTags) {
    }

    private PostSummaryVO toRankedSummary(FavoriteItemRankRow row, String languageCode) {
        PostSummaryVO summary = postQueryService.getSummary(row.getPostId(), languageCode);
        if (summary == null) {
            return null;
        }
        double score = row.getImportanceScore() == null ? 0.0 : row.getImportanceScore().doubleValue();
        boolean pinned = row.getPinned() != null && row.getPinned() == 1;
        List<String> reasons = decodeReasons(row.getScoreFactors());
        return summary.withRanking(score, pinned, reasons);
    }

    private FavoriteCollection requireOwnedCollection(Long userId, Long collectionId) {
        FavoriteCollection collection = favoriteCollectionMapper.selectById(collectionId);
        if (collection == null || !userId.equals(collection.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "collection not found");
        }
        return collection;
    }

    private String encodeReasons(List<String> reasons) {
        try {
            return OBJECT_MAPPER.writeValueAsString(reasons == null ? List.of() : reasons);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private List<String> decodeReasons(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> reasons = OBJECT_MAPPER.readValue(json, STRING_LIST);
            return reasons == null ? List.of() : reasons;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
