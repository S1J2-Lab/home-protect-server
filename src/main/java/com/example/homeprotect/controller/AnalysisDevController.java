package com.example.homeprotect.controller;

import com.example.homeprotect.util.RedisUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Profile("dev")
@RestController
@RequestMapping("/analysis/dev")
public class AnalysisDevController {

  private final RedisUtil redisUtil;

  public AnalysisDevController(RedisUtil redisUtil) {
    this.redisUtil = redisUtil;
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
}
