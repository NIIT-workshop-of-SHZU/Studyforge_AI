package com.studyforge.system.service;

import com.studyforge.system.dto.HomepageDraftRequest;
import com.studyforge.system.dto.HomepageReviewRequest;
import com.studyforge.system.dto.PublishHomepageRequest;
import com.studyforge.system.vo.AdminHomepageReviewVO;
import com.studyforge.system.vo.CommunityHomepageDesignVO;
import com.studyforge.system.vo.UserHomepageVO;
import java.util.List;

public interface HomepageService {
    UserHomepageVO getMyHomepage(Long userId);

    UserHomepageVO getUserHomepage(Long viewerId, Long userId);

    UserHomepageVO saveDraft(Long userId, HomepageDraftRequest request);

    UserHomepageVO submitReview(Long userId);

    CommunityHomepageDesignVO publishCommunity(Long userId, PublishHomepageRequest request);

    List<CommunityHomepageDesignVO> listCommunityDesigns(Long viewerId, String keyword, int limit);

    CommunityHomepageDesignVO getCommunityDesign(Long viewerId, Long designId);

    UserHomepageVO cloneCommunityDesign(Long userId, Long designId);

    List<AdminHomepageReviewVO> listReviews(String status, int limit);

    AdminHomepageReviewVO review(Long adminId, Long targetId, HomepageReviewRequest request);
}
