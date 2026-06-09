package com.studyforge.interaction.learning.mapper;

import com.studyforge.interaction.learning.entity.FavoriteItemRankRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LearningSignalMapper {
    List<FavoriteItemRankRow> selectCollectionItems(@Param("userId") Long userId,
                                                    @Param("collectionId") Long collectionId,
                                                    @Param("languageCode") String languageCode,
                                                    @Param("tag") String tag,
                                                    @Param("sort") String sort,
                                                    @Param("limit") int limit);

    List<FavoriteItemRankRow> selectAllItemsByUser(@Param("userId") Long userId,
                                                   @Param("languageCode") String languageCode);

    int countViews(@Param("userId") Long userId, @Param("postId") Long postId);

    int countAiEngagement(@Param("userId") Long userId, @Param("postId") Long postId);

    int countCollectionsForPost(@Param("userId") Long userId, @Param("postId") Long postId);

    List<Long> selectTopCategoryIds(@Param("userId") Long userId, @Param("limit") int limit);

    List<String> selectFavoriteAiTagLines(@Param("userId") Long userId, @Param("languageCode") String languageCode);

    List<String> selectTopFavoriteTitles(@Param("userId") Long userId,
                                         @Param("languageCode") String languageCode,
                                         @Param("limit") int limit);

    int updateItemScore(@Param("collectionId") Long collectionId,
                        @Param("postId") Long postId,
                        @Param("userId") Long userId,
                        @Param("importanceScore") double importanceScore,
                        @Param("scoreFactors") String scoreFactors);

    int updateItemPinned(@Param("collectionId") Long collectionId,
                         @Param("postId") Long postId,
                         @Param("userId") Long userId,
                         @Param("pinned") int pinned);
}
