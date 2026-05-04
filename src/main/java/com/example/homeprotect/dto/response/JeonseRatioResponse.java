package com.example.homeprotect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class JeonseRatioResponse {

    private String ratioType;
    private Long recentHigh;
    private Long recentLow;
    private Long average;
    private Long convertedDeposit;
    private Double ratioPercent;
    private Integer sampleCount;
    private Boolean lowReliability;
    private String riskLevel;
}
