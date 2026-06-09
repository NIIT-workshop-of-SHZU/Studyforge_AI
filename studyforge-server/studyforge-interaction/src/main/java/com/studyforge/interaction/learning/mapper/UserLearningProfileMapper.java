package com.studyforge.interaction.learning.mapper;

import com.studyforge.interaction.learning.entity.UserLearningProfile;
import org.apache.ibatis.annotations.Param;

public interface UserLearningProfileMapper {
    UserLearningProfile selectByUserId(@Param("userId") Long userId);

    int insert(UserLearningProfile profile);

    int update(UserLearningProfile profile);
}
