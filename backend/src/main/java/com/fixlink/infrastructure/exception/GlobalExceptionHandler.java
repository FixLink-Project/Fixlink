package com.fixlink.infrastructure.exception;

import com.fixlink.domain.exception.AccountBlockedException;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.InvalidCredentialsException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.exception.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.UNAUTHORIZED.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountBlocked(AccountBlockedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.FORBIDDEN.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(com.fixlink.domain.exception.AccountInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleAccountInactive(com.fixlink.domain.exception.AccountInactiveException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.FORBIDDEN.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(com.fixlink.domain.exception.AccountTemporarilyLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountTemporarilyLocked(com.fixlink.domain.exception.AccountTemporarilyLockedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.TOO_MANY_REQUESTS.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        error.put("remainingSeconds", ex.getRemainingSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.BAD_REQUEST.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(com.fixlink.domain.exception.UnverifiedTechnicianException.class)
    public ResponseEntity<Map<String, Object>> handleUnverifiedTechnician(com.fixlink.domain.exception.UnverifiedTechnicianException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.FORBIDDEN.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.NOT_FOUND.value());
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(DomainException ex) {
        Map<String, Object> error = new HashMap<>();
        int status = ex.getStatusCode() > 0 ? ex.getStatusCode() : HttpStatus.BAD_REQUEST.value();
        error.put("statusCode", status);
        error.put("errorCode", ex.getErrorCode());
        error.put("message", ex.getMessage());
        if (ex.getErrors() != null && !ex.getErrors().isEmpty()) {
            error.put("errors", ex.getErrors());
        }
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", HttpStatus.BAD_REQUEST.value());
        response.put("errorCode", "VALIDATION_FAILED");
        response.put("message", "Dữ liệu đầu vào không hợp lệ");
        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.FORBIDDEN.value());
        error.put("errorCode", "ACCESS_DENIED");
        error.put("message", ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Bạn không có quyền thực hiện hành động này");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Body request không đọc được (JSON sai cú pháp, sai bảng mã, thiếu body):
     * đây là lỗi của phía gọi nên trả 400, không phải 500 như handler tổng quát.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Không đọc được body request: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.BAD_REQUEST.value());
        error.put("errorCode", "MALFORMED_REQUEST_BODY");
        error.put("message", "Nội dung gửi lên không đọc được. Kiểm tra JSON và bảng mã UTF-8.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Đường dẫn không khớp tài nguyên tĩnh nào: trả 404 thay vì rơi vào
     * handler tổng quát bên dưới và bị báo nhầm thành lỗi hệ thống.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.NOT_FOUND.value());
        error.put("errorCode", "RESOURCE_NOT_FOUND");
        error.put("message", "Không tìm thấy đường dẫn: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        // Ghi log đầy đủ cho lập trình viên, nhưng không trả chi tiết nội bộ ra ngoài.
        log.error("Lỗi không lường trước khi xử lý request", ex);
        Map<String, Object> error = new HashMap<>();
        error.put("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("errorCode", "INTERNAL_SERVER_ERROR");
        error.put("message", "Hệ thống đang gặp sự cố. Vui lòng thử lại sau ít phút.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
