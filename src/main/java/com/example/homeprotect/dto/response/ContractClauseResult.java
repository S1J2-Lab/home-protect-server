package com.example.homeprotect.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ContractClauseResult {

    private List<Clause> clauses;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Clause {
        private Integer index;
        private String section;
        private String title;
        private String originalText;
    }
}
