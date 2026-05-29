package com.studyforge.framework.web;

import com.studyforge.common.api.ApiResponse;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException exception) {
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(), exception.getMessage());
    }
}
