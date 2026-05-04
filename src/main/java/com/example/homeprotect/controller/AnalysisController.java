package com.example.homeprotect.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.homeprotect.dto.request.AnalysisInitRequest;
import com.example.homeprotect.service.AnalysisService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/init")
    public Mono<ResponseEntity<Map<String, Object>>> initAnalysis(
            @RequestBody AnalysisInitRequest request,
            @RequestParam("ocrSessionId") String ocrSessionId) {
        return analysisService.initAnalysis(request, ocrSessionId)
                .map(sessionId -> {
                    Map<String, Object> inner = new LinkedHashMap<>();
                    inner.put("sessionId", sessionId);

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("status", "success");
                    response.put("data", inner);

                    return ResponseEntity.ok(response);
                });
    }
}
