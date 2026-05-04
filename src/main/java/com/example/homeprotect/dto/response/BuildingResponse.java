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
public class BuildingResponse {

    private String level;
    private String primaryUse;
    private Boolean isResidential;
    private Boolean violation;
    private String approvedDate;
    private Boolean redevelopmentZone;
}
