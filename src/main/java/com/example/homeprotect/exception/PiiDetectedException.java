package com.example.homeprotect.exception;

import java.util.List;

public class PiiDetectedException extends HomeProtectException {

    private final List<String> nonMasked;

    public PiiDetectedException(List<String> nonMasked) {
        super(ErrorCode.PII_DETECTED);
        this.nonMasked = List.copyOf(nonMasked);
    }

    public List<String> getNonMasked() {
        return nonMasked;
    }
}
