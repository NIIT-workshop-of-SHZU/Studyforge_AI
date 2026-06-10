package com.studyforge.interaction.learning.service;

import com.studyforge.content.vo.PostSummaryVO;
import java.util.List;

public interface FavoriteImportanceService {
    List<PostSummaryVO> listCollectionPosts(Long userId,
                                            Long collectionId,
                                            String languageCode,
                                            int limit,
                                            String sort,
                                            String tag);

    void recomputeCollection(Long userId, Long collectionId, String languageCode);

    void recomputeCollection(Long userId, Long collectionId, String languageCode, boolean allowLlmForPosts);

    void recomputeAllForUser(Long userId, String languageCode);

    void recomputeAllForUser(Long userId, String languageCode, boolean allowLlmForPosts);

    void setPinned(Long userId, Long collectionId, Long postId, boolean pinned, String languageCode);
}
