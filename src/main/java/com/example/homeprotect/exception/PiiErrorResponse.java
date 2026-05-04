package com.example.homeprotect.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class PiiErrorResponse {

    private final String status = "error";
    private final ErrorDetail error;

    public PiiErrorResponse(ErrorCode errorCode, List<String> nonMasked) {
        this.error = new ErrorDetail(errorCode, nonMasked);
    }

    @Getter
    public static class ErrorDetail {
        private final String code;
        private final String message;
        private final String field = "file";
        private final List<String> nonMasked;

        public ErrorDetail(ErrorCode errorCode, List<String> nonMasked) {
            this.code = errorCode.name();
            this.message = errorCode.getMessage();
            this.nonMasked = nonMasked;
        }
    }
}
