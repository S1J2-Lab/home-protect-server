package com.example.homeprotect.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class ContractAnalysisResult {

    private List<AnalyzedClause> toxicClauses;
    private List<AnalyzedClause> cautionClauses;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class AnalyzedClause {
        private String level;
        private String title;
        private String originalText;
        private String legalIssue;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private String precedent;
        private String suggestion;
    }
}
