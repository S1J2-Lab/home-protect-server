package com.example.homeprotect.exception;

public class HomeProtectException extends RuntimeException {

    private final ErrorCode errorCode;

    public HomeProtectException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public HomeProtectException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
