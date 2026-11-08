package com.example.workbenchperfa7.exception;

import java.util.List;

/**
 * 失败响应体：{ success: false, error: { code, message, fieldErrors } }。
 */
public record ApiError(boolean success, ErrorDetail error) {

    public static ApiError of(String code, String message) {
        return new ApiError(false, new ErrorDetail(code, message, List.of()));
    }

    public static ApiError of(String code, String message, List<FieldErrorItem> fieldErrors) {
        return new ApiError(false, new ErrorDetail(code, message, fieldErrors == null ? List.of() : fieldErrors));
    }

    public record ErrorDetail(String code, String message, List<FieldErrorItem> fieldErrors) {
    }
}
