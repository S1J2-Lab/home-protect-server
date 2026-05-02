package com.example.homeprotect.exception;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Set<ErrorCode> INFRA_ERROR_CODES = Set.of(
            ErrorCode.CLAUDE_CALL_FAILED,
            ErrorCode.GEMINI_CALL_FAILED,
            ErrorCode.VECTOR_DB_FAILED
    );

    @ExceptionHandler(HomeProtectException.class)
    public ResponseEntity<Map<String, Object>> handleHomeProtectException(HomeProtectException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (INFRA_ERROR_CODES.contains(errorCode)) {
            log.error("[{}] {}", errorCode.name(), errorCode.getMessage(), e);
        } else {
            log.warn("[{}] {}", errorCode.name(), errorCode.getMessage());
        }
        return buildErrorResponse(errorCode.getStatus(), errorCode.name(), errorCode.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("[INTERNAL_ERROR] 예기치 못한 오류가 발생했습니다.", e);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "예기치 못한 오류가 발생했습니다."
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(int status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(Map.of("status", status, "code", code, "message", message));
    }
}
