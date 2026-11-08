package com.example.workbenchperfa7.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一错误语义（EMP-04）：
 * 400 VALIDATION_ERROR / 409 DUPLICATE_EMPLOYEE_NO / 404 EMPLOYEE_NOT_FOUND / 500 INTERNAL_ERROR。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Pattern FIELD_REFERENCE = Pattern.compile("\\[\"([A-Za-z0-9_]+)\"\\]");

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiError> handleBiz(BizException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiError.of(exception.getCode(), exception.getMessage(), exception.getFieldErrors()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorItem> fieldErrors = new ArrayList<>();
        Map<String, Integer> indexByField = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            String message = fieldError.getDefaultMessage();
            Integer index = indexByField.get(field);
            if (index == null) {
                indexByField.put(field, fieldErrors.size());
                fieldErrors.add(new FieldErrorItem(field, message));
                continue;
            }
            // 同一字段被多条规则命中时，优先展示"不能为空"这类更直接的消息。
            FieldErrorItem existing = fieldErrors.get(index);
            if (isBlankMessage(message) && !isBlankMessage(existing.message())) {
                fieldErrors.set(index, new FieldErrorItem(field, message));
            }
        }
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", firstMessage(fieldErrors), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorItem> fieldErrors = exception.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", firstMessage(fieldErrors), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception) {
        String field = firstFieldName(exception);
        if (field == null) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of("VALIDATION_ERROR", "请求体格式不正确"));
        }
        String message = switch (field) {
            case "hireDate" -> "入职日期格式不正确（yyyy-MM-dd）";
            case "gender" -> "性别取值不合法";
            case "status" -> "员工状态取值不合法";
            default -> "字段格式不正确";
        };
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", message, List.of(new FieldErrorItem(field, message))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName();
        String message = "请求参数格式不正确";
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", message, List.of(new FieldErrorItem(field, message))));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("data integrity violation: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DUPLICATE_EMPLOYEE_NO", "工号已存在，请更换后重试",
                        List.of(new FieldErrorItem("employeeNo", "工号已存在，请更换后重试"))));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", "请求的资源不存在"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of("METHOD_NOT_ALLOWED", "不支持的请求方法"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception exception) {
        log.error("unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "服务器内部错误，请稍后重试"));
    }

    private FieldErrorItem toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int index = path.lastIndexOf('.');
        String field = index >= 0 ? path.substring(index + 1) : path;
        return new FieldErrorItem(field, violation.getMessage());
    }

    private static boolean isBlankMessage(String message) {
        return message != null && message.contains("不能为空");
    }

    private static String firstMessage(List<FieldErrorItem> fieldErrors) {
        return fieldErrors.isEmpty() ? "请求参数校验失败" : fieldErrors.get(0).message();
    }

    private static String firstFieldName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
                return mapping.getPath().get(0).getFieldName();
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        // Jackson 的 LocalDate 解析失败会把字段信息放在异常消息的 reference chain 中。
        String message = throwable == null ? null : throwable.getMessage();
        if (message != null) {
            Matcher matcher = FIELD_REFERENCE.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
