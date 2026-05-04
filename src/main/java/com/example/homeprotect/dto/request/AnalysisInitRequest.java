package com.example.homeprotect.dto.request;

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

    private String address;
    private String admCd;
    private String rnMgtSn;
    private Long deposit;
    private Long monthlyRent;
    private String contractType;
    private ContractPeriod contractPeriod;
}
