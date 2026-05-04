package com.example.homeprotect.util;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.redis.OcrSessionData;
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

    public Mono<JeonseRatioResponse> getJeonseRatio(String sessionId) {
        String key = JEONSE_RATIO_KEY_PREFIX + sessionId;
        return reactiveRedisTemplate.opsForValue().get(key)
                .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.SESSION_EXPIRED)))
                .flatMap(json -> deserialize(json, JeonseRatioResponse.class));
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
