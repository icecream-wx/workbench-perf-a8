package com.example.workbenchperfa7.exception;

/**
 * 单字段校验错误，前端据此定位到具体表单项。
 */
public record FieldErrorItem(String field, String message) {
}
