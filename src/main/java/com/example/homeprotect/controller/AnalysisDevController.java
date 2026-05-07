package com.example.homeprotect.controller;

import com.example.homeprotect.filter.RiskClauseAnalyzer;
import jakarta.validation.Valid;

import com.example.homeprotect.dto.request.ContractAnalysisRequestDto;
import com.example.homeprotect.dto.request.RegistryAnalysisRequestDto;
import com.example.homeprotect.service.ContractAnalysisService;
import com.example.homeprotect.service.ContractService;
import com.example.homeprotect.service.PrecedentService;
import com.example.homeprotect.service.RegistryService;
import com.example.homeprotect.util.RedisUtil;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Hidden
@Profile("dev")
@RestController
@RequestMapping("/analysis/dev")
public class AnalysisDevController {

  private final RedisUtil redisUtil;
  private final RegistryService registryService;
  private final ContractService contractService;
  private final PrecedentService precedentService;
  private final RiskClauseAnalyzer riskClauseAnalyzer;
  private final ContractAnalysisService contractAnalysisService;

  public AnalysisDevController(RedisUtil redisUtil, RegistryService registryService,
      ContractService contractService, PrecedentService precedentService,
      RiskClauseAnalyzer riskClauseAnalyzer, ContractAnalysisService contractAnalysisService) {
    this.redisUtil = redisUtil;
    this.registryService = registryService;
    this.contractService = contractService;
    this.precedentService = precedentService;
    this.riskClauseAnalyzer = riskClauseAnalyzer;
    this.contractAnalysisService = contractAnalysisService;
  }

  @GetMapping("/{sessionId}/jeonse")
  public Mono<ResponseEntity<Object>> getJeonseDebug(@PathVariable String sessionId) {
    return redisUtil.getJeonseRatio(sessionId)
        .map(data -> ResponseEntity.ok((Object) data))
        .onErrorReturn(ResponseEntity.notFound().build());
  }

  @GetMapping("/{sessionId}/building")
  public Mono<ResponseEntity<Object>> getBuildingDebug(@PathVariable String sessionId) {
    return redisUtil.getBuildingInfo(sessionId)
        .map(data -> ResponseEntity.ok((Object) data))
        .onErrorReturn(ResponseEntity.notFound().build());
  }

  @GetMapping("/{sessionId}/init")
  public Mono<ResponseEntity<Object>> getInitDebug(@PathVariable String sessionId) {
    return redisUtil.getInitSession(sessionId)
        .map(data -> ResponseEntity.ok((Object) data))
        .onErrorReturn(ResponseEntity.notFound().build());
  }

  @PostMapping("/registry/analyze")
  public Mono<ResponseEntity<Object>> analyzeRegistry(
      @Valid @RequestBody RegistryAnalysisRequestDto request) {
    return registryService.analyze(request.getDocumentId())
        .map(result -> ResponseEntity.ok((Object) result));
  }

  @PostMapping("/contract/clauses")
  public Mono<ResponseEntity<Object>> extractContractClauses(
      @Valid @RequestBody ContractAnalysisRequestDto request) {
    return contractService.analyze(request.getDocumentId())
        .map(result -> ResponseEntity.ok((Object) result));
  }

  @PostMapping("/contract/precedents")
  public Mono<ResponseEntity<Object>> searchPrecedents(
      @Valid @RequestBody ContractAnalysisRequestDto request) {
    return precedentService.search(request.getDocumentId())
        .map(result -> ResponseEntity.ok((Object) result));
  }

  @PostMapping("/contract/risk-clauses")
  public Mono<ResponseEntity<Object>> extractRiskClauses(
      @Valid @RequestBody ContractAnalysisRequestDto request) {
    return redisUtil.getClauseResult(request.getDocumentId())
        .flatMap(clauseResult -> riskClauseAnalyzer.analyze(clauseResult.getClauses()))
        .map(result -> ResponseEntity.ok((Object) result));
  }

  @PostMapping("/contract/analyze/raw")
  public Mono<ResponseEntity<Object>> analyzeContractRaw(
      @Valid @RequestBody ContractAnalysisRequestDto request) {
    return contractAnalysisService.analyzeRaw(request.getDocumentId())
        .map(result -> ResponseEntity.ok((Object) result));
  }

  @PostMapping("/contract/analyze")
  public Mono<ResponseEntity<Object>> analyzeContract(
      @Valid @RequestBody ContractAnalysisRequestDto request) {
    return contractAnalysisService.analyze(request.getDocumentId())
        .map(result -> ResponseEntity.ok((Object) result));
  }
}
