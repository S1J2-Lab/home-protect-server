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
public class AnalysisResultResponse {

  private String address;
  private String analyzedAt;
  private JeonseRatioResponse jeonseRatio;
  private RegistryAnalysisResult.RegistryInfo registry;
  private BuildingResponse building;
  private ContractAnalysisResult contract;
}
