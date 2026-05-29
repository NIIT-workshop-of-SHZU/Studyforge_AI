package com.studyforge.common.exception;

public enum ErrorCode {
    SUCCESS(0, "success"),
    VALIDATION_ERROR(4000, "validation error"),
    UNAUTHORIZED(4010, "unauthorized"),
    FORBIDDEN(4030, "forbidden"),
    NOT_FOUND(4040, "resource not found"),
    INTERNAL_ERROR(5000, "internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
