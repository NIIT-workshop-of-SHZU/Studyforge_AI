package com.studyforge.system.vo;

import com.studyforge.common.enums.RoleType;

public record LoginVO(String accessToken, Long userId, String username, RoleType role) {
}
