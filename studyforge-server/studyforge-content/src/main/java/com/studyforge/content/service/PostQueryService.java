package com.studyforge.content.service;

import com.studyforge.content.vo.PostDetailVO;
import com.studyforge.content.vo.PostSummaryVO;
import java.util.List;

public interface PostQueryService {
    PostDetailVO getDetail(Long postId, String languageCode);

    List<PostSummaryVO> getTrending(String languageCode, int limit);

    List<PostSummaryVO> list(String languageCode, String categoryCode, String keyword, int limit);

    List<PostSummaryVO> listFavorites(Long userId, String languageCode, int limit);

    List<PostSummaryVO> listHistory(Long userId, String languageCode, int limit);
}
