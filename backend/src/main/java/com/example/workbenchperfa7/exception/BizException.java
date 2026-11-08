package com.example.workbenchperfa7.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 业务异常：携带 HTTP 状态、稳定的错误类别与可选字段错误。
 */
public class BizException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorItem> fieldErrors;

    public BizException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public BizException(HttpStatus status, String code, String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
