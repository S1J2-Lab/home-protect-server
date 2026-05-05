package com.example.homeprotect.controller;

import jakarta.validation.Valid;

import com.example.homeprotect.dto.request.ContractAnalysisRequestDto;
import com.example.homeprotect.dto.request.RegistryAnalysisRequestDto;
import com.example.homeprotect.service.ContractService;
import com.example.homeprotect.service.RegistryService;
import com.example.homeprotect.util.RedisUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Profile("dev")
@RestController
@RequestMapping("/analysis/dev")
public class AnalysisDevController {

  private final RedisUtil redisUtil;
  private final RegistryService registryService;
  private final ContractService contractService;

  public AnalysisDevController(RedisUtil redisUtil, RegistryService registryService, ContractService contractService) {
    this.redisUtil = redisUtil;
    this.registryService = registryService;
    this.contractService = contractService;
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
}
