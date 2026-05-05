package com.example.homeprotect.util;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.redis.OcrSessionData;
import com.example.homeprotect.dto.response.BuildingResponse;
import com.example.homeprotect.dto.response.ContractClauseResult;
import com.example.homeprotect.dto.response.ContractClauseResult.Clause;
import com.example.homeprotect.dto.response.JeonseRatioResponse;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class RedisUtil {

    private static final String OCR_KEY_PREFIX = "ocr:";
    private static final String INIT_KEY_PREFIX = "init:";
    private static final String JEONSE_RATIO_KEY_PREFIX = "jeonseRatio:";
    private static final String BUILDING_KEY_PREFIX = "building:";
    private static final String CLAUSE_KEY_PREFIX = "clause:";
    private static final String RISK_CLAUSES_KEY_PREFIX = "riskClauses:";
    private static final Duration OCR_TTL = Duration.ofMinutes(30);

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisUtil(ReactiveRedisTemplate<String, String> reactiveRedisTemplate, ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> saveOcrSession(OcrSessionData data) {
        String key = OCR_KEY_PREFIX + data.getSessionId();
        return serialize(data)
            .flatMap(json -> reactiveRedisTemplate.opsForValue().set(key, json, OCR_TTL))
            .then();
    }

    public Mono<Void> saveInitSession(InitSessionData data) {
        String key = INIT_KEY_PREFIX + data.getSessionId();
        return serialize(data)
            .flatMap(json -> reactiveRedisTemplate.opsForValue().set(key, json, OCR_TTL))
            .then();
    }

    public Mono<InitSessionData> getInitSession(String sessionId) {
        String key = INIT_KEY_PREFIX + sessionId;
        return reactiveRedisTemplate.opsForValue().get(key)
            .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.SESSION_EXPIRED)))
            .flatMap(json -> deserialize(json, InitSessionData.class));
    }

    public Mono<Void> saveJeonseRatio(String sessionId, JeonseRatioResponse response) {
        String key = JEONSE_RATIO_KEY_PREFIX + sessionId;
        return serialize(response)
            .flatMap(json -> reactiveRedisTemplate.opsForValue().set(key, json, OCR_TTL))
            .then();
    }

    public Mono<Void> saveBuildingInfo(String sessionId, BuildingResponse response) {
        String key = BUILDING_KEY_PREFIX + sessionId;
        return serialize(response)
            .flatMap(json -> reactiveRedisTemplate.opsForValue().set(key, json, OCR_TTL))
            .then();
    }

    public Mono<BuildingResponse> getBuildingInfo(String sessionId) {
        String key = BUILDING_KEY_PREFIX + sessionId;
        return reactiveRedisTemplate.opsForValue().get(key)
            .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.SESSION_EXPIRED)))
            .flatMap(json -> deserialize(json, BuildingResponse.class));
    }

    public Mono<JeonseRatioResponse> getJeonseRatio(String sessionId) {
        String key = JEONSE_RATIO_KEY_PREFIX + sessionId;
        return reactiveRedisTemplate.opsForValue().get(key)
            .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.SESSION_EXPIRED)))
            .flatMap(json -> deserialize(json, JeonseRatioResponse.class));
    }

    public Mono<Void> saveClauseResult(String documentId, ContractClauseResult result) {
        String key = CLAUSE_KEY_PREFIX + documentId;
        return serialize(result)
            .flatMap(json -> reactiveRedisTemplate.opsForValue().set(key, json, OCR_TTL))
            .then();
    }

    public Mono<ContractClauseResult> getClauseResult(String documentId) {
        String key = CLAUSE_KEY_PREFIX + documentId;
        return reactiveRedisTemplate.opsForValue().get(key)
            .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.CLAUSE_NOT_ANALYZED)))
            .flatMap(json -> deserialize(json, ContractClauseResult.class));
    }

    public Mono<Void> saveRiskClauses(String documentId, List<Clause> clauses) {
        String key = RISK_CLAUSES_KEY_PREFIX + documentId;
        return serialize(clauses)
            .flatMap(json -> reactiveRedisTemplate.opsForValue().set(key, json, OCR_TTL))
            .then();
    }

    public Mono<List<Clause>> getRiskClauses(String documentId) {
        String key = RISK_CLAUSES_KEY_PREFIX + documentId;
        return reactiveRedisTemplate.opsForValue().get(key)
            .flatMap(json -> {
                try {
                    List<Clause> clauses = objectMapper.readValue(
                        json,
                        objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, Clause.class)
                    );
                    return Mono.just(clauses);
                } catch (JsonProcessingException e) {
                    return Mono.error(new HomeProtectException(ErrorCode.INTERNAL_ERROR, e));
                }
            });
        // 캐시 없으면 empty 반환
    }

    public Mono<OcrSessionData> getOcrSession(String sessionId) {
        String key = OCR_KEY_PREFIX + sessionId;
        return reactiveRedisTemplate.opsForValue().get(key)
            .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.SESSION_EXPIRED)))
            .flatMap(json -> deserialize(json, OcrSessionData.class));
    }

    private <T> Mono<String> serialize(T data) {
        try {
            return Mono.just(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            return Mono.error(new HomeProtectException(ErrorCode.INTERNAL_ERROR, e));
        }
    }

    private <T> Mono<T> deserialize(String json, Class<T> type) {
        try {
            return Mono.just(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            return Mono.error(new HomeProtectException(ErrorCode.INTERNAL_ERROR, e));
        }
    }
}
