package com.studyforge.framework.web;

import com.studyforge.common.constants.HttpHeaders;
import com.studyforge.common.enums.RoleType;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = request.getHeader(HttpHeaders.USER_ROLE);
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!RoleType.ADMIN.name().equalsIgnoreCase(role) && (authorization == null || authorization.isBlank())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
