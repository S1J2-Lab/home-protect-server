package com.example.homeprotect.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.homeprotect.dto.request.AnalysisInitRequest;
import com.example.homeprotect.dto.request.AnalysisRunRequest;
import com.example.homeprotect.service.AnalysisService;

import reactor.core.publisher.Flux;
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
            @RequestBody AnalysisInitRequest request) {
        return analysisService.initAnalysis(request)
                .map(sessionId -> {
                    Map<String, Object> inner = new LinkedHashMap<>();
                    inner.put("sessionId", sessionId);

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("status", "success");
                    response.put("data", inner);

                    return ResponseEntity.ok(response);
                });
    }

    @PostMapping("/run")
    public Mono<ResponseEntity<Map<String, Object>>> runAnalysis(
        @Valid @RequestBody AnalysisRunRequest request) {
      return analysisService.runAnalysis(request)
          .map(sessionId -> {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("sessionId", sessionId);
            inner.put("analysisStatus", "processing");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", inner);

            return ResponseEntity.ok(response);
          });
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamAnalysisStatus(@RequestParam String sessionId) {
      return analysisService.streamAnalysisStatus(sessionId);
    }

    @GetMapping("/result")
    public Mono<ResponseEntity<Map<String, Object>>> getAnalysisResult(
        @RequestParam String sessionId) {
      return analysisService.getAnalysisResult(sessionId)
          .map(result -> {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", result);

            return ResponseEntity.ok(response);
          });
    }

}
