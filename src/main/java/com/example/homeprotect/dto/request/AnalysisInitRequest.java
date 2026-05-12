package com.example.homeprotect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.homeprotect.dto.redis.InitSessionData.ContractPeriod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisInitRequest {

    @NotBlank private String address;
    @NotBlank private String admCd;
    @NotBlank private String bdMgtSn;
    private String rnMgtSn;
    private String mno;
    private String sno;
    @NotNull  private Long deposit;
    private Long monthlyRent;
    @NotBlank private String contractType;
    private ContractPeriod contractPeriod;
}
