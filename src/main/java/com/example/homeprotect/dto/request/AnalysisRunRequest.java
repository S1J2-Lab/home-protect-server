package com.example.homeprotect.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class AnalysisRunRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String registrySessionId;

    @NotBlank
    private String contractSessionId;

    private boolean ownerVerified;
}
