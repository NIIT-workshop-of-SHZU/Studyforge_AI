package com.studyforge.system.mapper;

import com.studyforge.system.entity.CommunityHomepageDesign;
import com.studyforge.system.entity.UserHomepage;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface HomepageMapper {
    UserHomepage selectUserHomepageByUserId(@Param("userId") Long userId);

    UserHomepage selectUserHomepageById(@Param("homepageId") Long homepageId);

    int insertUserHomepage(UserHomepage homepage);

    int updateUserHomepageDraft(UserHomepage homepage);

    int submitUserHomepage(@Param("homepageId") Long homepageId);

    int approveUserHomepage(@Param("homepageId") Long homepageId,
                            @Param("adminId") Long adminId,
                            @Param("remark") String remark);

    int rejectUserHomepage(@Param("homepageId") Long homepageId,
                           @Param("adminId") Long adminId,
                           @Param("remark") String remark);

    int updateUserHomepageCommunityTemplate(@Param("homepageId") Long homepageId,
                                            @Param("designId") Long designId);

    CommunityHomepageDesign selectCommunityDesignById(@Param("designId") Long designId);

    List<CommunityHomepageDesign> selectApprovedCommunityDesigns(@Param("keyword") String keyword,
                                                                 @Param("limit") int limit);

    int insertCommunityDesign(CommunityHomepageDesign design);

    int approveCommunityDesign(@Param("designId") Long designId,
                               @Param("adminId") Long adminId,
                               @Param("remark") String remark);

    int rejectCommunityDesign(@Param("designId") Long designId,
                              @Param("adminId") Long adminId,
                              @Param("remark") String remark);

    int incrementCommunityCloneCount(@Param("designId") Long designId);

    List<Map<String, Object>> selectHomepageReviews(@Param("status") String status,
                                                    @Param("limit") int limit);

    Map<String, Object> selectHomepageReview(@Param("targetType") String targetType,
                                             @Param("targetId") Long targetId);

    int insertAudit(@Param("adminId") Long adminId,
                    @Param("targetType") String targetType,
                    @Param("targetId") Long targetId,
                    @Param("actionType") String actionType,
                    @Param("remark") String remark);
}
