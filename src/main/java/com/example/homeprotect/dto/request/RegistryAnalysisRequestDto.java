package com.example.homeprotect.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistryAnalysisRequestDto {
    @NotBlank
    private String documentId;
}
