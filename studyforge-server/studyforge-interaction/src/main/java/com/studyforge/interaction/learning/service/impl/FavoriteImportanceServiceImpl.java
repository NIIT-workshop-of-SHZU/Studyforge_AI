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
import com.studyforge.interaction.learning.support.InterestTagNormalizer;
import com.studyforge.interaction.learning.support.InterestTagResolver;
import com.studyforge.interaction.learning.support.LearningTextTagger;
import com.studyforge.interaction.learning.support.SemanticTagMatcher;
import com.studyforge.interaction.learning.support.InterestTagCodec;
import com.studyforge.interaction.mapper.FavoriteCollectionMapper;
import com.studyforge.interaction.entity.FavoriteCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private final PostSemanticTagWarmupService postSemanticTagWarmupService;

    public FavoriteImportanceServiceImpl(LearningSignalMapper learningSignalMapper,
                                         UserLearningProfileMapper userLearningProfileMapper,
                                         UserLearningProfileService userLearningProfileService,
                                         FavoriteCollectionMapper favoriteCollectionMapper,
                                         PostMapper postMapper,
                                         PostQueryService postQueryService,
                                         PostSemanticTagService postSemanticTagService,
                                         PostSemanticTagWarmupService postSemanticTagWarmupService) {
        this.learningSignalMapper = learningSignalMapper;
        this.userLearningProfileMapper = userLearningProfileMapper;
        this.userLearningProfileService = userLearningProfileService;
        this.favoriteCollectionMapper = favoriteCollectionMapper;
        this.postMapper = postMapper;
        this.postQueryService = postQueryService;
        this.postSemanticTagService = postSemanticTagService;
        this.postSemanticTagWarmupService = postSemanticTagWarmupService;
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
                null,
                normalizedSort,
                normalizedTag == null ? normalizedLimit : Math.min(normalizedLimit * 4, 200)
        );
        if (normalizedTag != null) {
            rows = rows.stream()
                    .filter(row -> matchesInterestTag(row, normalizedTag, normalizedLanguage))
                    .limit(normalizedLimit)
                    .toList();
        }

        List<PostSummaryVO> summaries = rows.stream()
                .map(row -> toRankedSummary(row, normalizedLanguage))
                .filter(Objects::nonNull)
                .toList();
        if ("importance".equals(normalizedSort)) {
            return summaries.stream().sorted(importanceComparator()).toList();
        }
        return summaries;
    }

    private static Comparator<PostSummaryVO> importanceComparator() {
        return Comparator.comparing(post -> Boolean.TRUE.equals(post.pinned()), Comparator.reverseOrder())
                .thenComparing(
                        PostSummaryVO::importanceScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(PostSummaryVO::createdTime, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    @Override
    @Transactional
    public void recomputeCollection(Long userId, Long collectionId, String languageCode) {
        recomputeCollection(userId, collectionId, languageCode, false);
        postSemanticTagWarmupService.warmCollectionAsync(userId, collectionId, languageCode);
    }

    @Override
    @Transactional
    public void recomputeCollection(Long userId, Long collectionId, String languageCode, boolean allowLlmForPosts) {
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
        if (allowLlmForPosts) {
            postSemanticTagWarmupService.warmCollectionAsync(userId, collectionId, languageCode);
        }
    }

    @Override
    @Transactional
    public void recomputeAllForUser(Long userId, String languageCode) {
        recomputeAllForUser(userId, languageCode, false);
    }

    @Override
    @Transactional
    public void recomputeAllForUser(Long userId, String languageCode, boolean allowLlmForPosts) {
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
        List<SemanticTag> postSemanticTags = postSemanticTagService.tagsForScoring(
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
        List<InterestTag> merged = new ArrayList<>(
                InterestTagNormalizer.normalize(
                        scoreContext.interestTags().stream()
                                .filter(tag -> includeUserTag(tag, scoreContext.memoryPrioritized()))
                                .toList()
                )
        );
        if (scoreContext.memoryPrioritized()
                && scoreContext.memoryMd() != null
                && !scoreContext.memoryMd().isBlank()) {
            for (InterestTag memoryTag : LearningTextTagger.memoryDerivedTags(scoreContext.memoryMd())) {
                boolean exists = merged.stream().anyMatch(tag -> tagsOverlap(tag.tag(), memoryTag.tag()));
                if (!exists) {
                    merged.add(memoryTag);
                }
            }
        }
        return merged.stream().map(InterestTag::toSemanticTag).toList();
    }

    private boolean matchesInterestTag(FavoriteItemRankRow row, String filterTag, String languageCode) {
        String normalizedFilter = filterTag.trim();
        if (normalizedFilter.isBlank()) {
            return true;
        }
        String haystack = ((row.getTitle() == null ? "" : row.getTitle()) + " "
                + (row.getSummary() == null ? "" : row.getSummary()) + " "
                + (row.getAiTags() == null ? "" : row.getAiTags())).toLowerCase(Locale.ROOT);
        String filter = normalizedFilter.toLowerCase(Locale.ROOT);
        if (haystack.contains(filter)) {
            return true;
        }
        List<SemanticTag> postTags = postSemanticTagService.tagsForScoring(
                row.getPostId(),
                languageCode,
                row.getTitle(),
                row.getSummary(),
                row.getAiTags()
        );
        return postTags.stream().anyMatch(postTag ->
                SemanticTagMatcher.tagSimilarity(normalizedFilter, postTag.tag()) >= 0.35
        );
    }

    private static boolean tagsOverlap(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        String a = left.toLowerCase(Locale.ROOT);
        String b = right.toLowerCase(Locale.ROOT);
        return a.equals(b) || a.contains(b) || b.contains(a);
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
        List<InterestTag> interestTags = InterestTagResolver.resolve(
                memoryMd,
                InterestTagCodec.decode(profile.getInterestTags())
        );
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
