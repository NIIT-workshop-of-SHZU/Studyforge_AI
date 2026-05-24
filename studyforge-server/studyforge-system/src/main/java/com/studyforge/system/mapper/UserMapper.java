package com.studyforge.system.mapper;

import com.studyforge.system.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    User selectById(@Param("userId") Long userId);

    User selectByAccount(@Param("account") String account);

    int insert(User user);

    int updateById(User user);
}
