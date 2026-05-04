package com.example.homeprotect.dto.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrSessionData {
    private String sessionId;
    private boolean safe;
    private String rawText;
    private String docType;
}
