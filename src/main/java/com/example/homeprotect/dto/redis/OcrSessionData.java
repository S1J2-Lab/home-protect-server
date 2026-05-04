package com.example.homeprotect.dto.redis;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class OcrSessionData {
    private String sessionId;
    private boolean safe;
    private String rawText;
    private String docType;
}
