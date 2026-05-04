package com.example.homeprotect.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {

  private final String status = "error";
  private final ErrorDetail error;

  private ErrorResponse(ErrorCode errorCode) {
    this.error = new ErrorDetail(errorCode);
  }

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode);
  }

  @Getter
  public static class ErrorDetail {
    private final String code;
    private final String message;

    private ErrorDetail(ErrorCode errorCode) {
      this.code = errorCode.name();
      this.message = errorCode.getMessage();
    }
  }
}
