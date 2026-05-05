package com.example.homeprotect.dto.response;

import java.util.List;

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
public class RegistryAnalysisResult {

    private RegistryInfo registry;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class RegistryInfo {
        private int mortgageCount;
        private List<Mortgage> mortgages;
        private long totalMortgage;
        private boolean trustWarning;
        private boolean priorLease;
        private boolean ownershipChangeRecent;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class Mortgage {
        private String bank;
        private long amount;
    }
}
