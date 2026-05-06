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
public class AnalysisResult {

    private JeonseRatioResponse jeonseRatio;
    private RegistryAnalysisResult registryParse;
    private ContractAnalysisResult contractReview;
    private BuildingResponse buildingCheck;
}
