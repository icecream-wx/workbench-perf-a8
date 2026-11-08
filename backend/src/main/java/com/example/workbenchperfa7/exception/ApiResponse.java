package com.example.workbenchperfa7.exception;

import java.util.List;

/**
 * 成功响应体：{ success: true, data: ... }。
 */
public record ApiResponse<T>(boolean success, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<List<T>> okList(List<T> data) {
        return new ApiResponse<>(true, data);
    }
}
