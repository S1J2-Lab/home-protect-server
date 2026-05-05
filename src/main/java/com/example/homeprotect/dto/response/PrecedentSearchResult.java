package com.example.homeprotect.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrecedentSearchResult {

    private Integer clauseIndex;
    private String section;
    private String originalText;
    private String level;  // "danger" or "caution"
    private List<PrecedentMatch> matches;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrecedentMatch {
        private String precedentId;
        private double score;
        private String summary;
    }
}
