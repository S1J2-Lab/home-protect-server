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
        private Integer mortgageCount;
        private List<Mortgage> mortgages;
        private Long totalMortgage;
        private Boolean trustWarning;
        private Boolean priorLease;
        private Boolean ownershipChangeRecent;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class Mortgage {
        private String bank;
        private Long amount;
    }
}
