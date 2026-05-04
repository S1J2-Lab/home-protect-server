package com.example.homeprotect.dto.redis;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class InitSessionData {

    private String sessionId;
    private String ocrSessionId;
    private String address;
    private String admCd;
    private String rnMgtSn;
    private String mno;
    private String sno;
    private Long deposit;
    private Long monthlyRent;
    private String contractType;
    private ContractPeriod contractPeriod;

    @Getter
    @Builder
    @Jacksonized
    public static class ContractPeriod {
        private String startDate;
        private String endDate;
    }
}
