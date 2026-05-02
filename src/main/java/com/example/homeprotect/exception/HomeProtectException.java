package com.example.homeprotect.exception;

public class HomeProtectException extends RuntimeException {

    private final ErrorCode errorCode;

    public HomeProtectException(ErrorCode errorCode) {
        super(requireNonNull(errorCode).getMessage());
        this.errorCode = errorCode;
    }

    public HomeProtectException(ErrorCode errorCode, Throwable cause) {
        super(requireNonNull(errorCode).getMessage(), cause);
        this.errorCode = errorCode;
    }

    private static ErrorCode requireNonNull(ErrorCode errorCode) {
        if (errorCode == null) {
            throw new IllegalArgumentException("errorCode must not be null");
        }
        return errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
