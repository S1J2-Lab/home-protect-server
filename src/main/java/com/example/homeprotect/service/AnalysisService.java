package com.example.homeprotect.service;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.request.AnalysisInitRequest;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.util.RedisUtil;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final Set<String> VALID_CONTRACT_TYPES = Set.of("jeonse", "half_jeonse", "monthly");

    private final RedisUtil redisUtil;
    private final JeonseRatioService jeonseRatioService;

    public AnalysisService(RedisUtil redisUtil, JeonseRatioService jeonseRatioService) {
        this.redisUtil = redisUtil;
        this.jeonseRatioService = jeonseRatioService;
    }

    public Mono<String> initAnalysis(AnalysisInitRequest request, String ocrSessionId) {
        if (request.getContractType() == null || !VALID_CONTRACT_TYPES.contains(request.getContractType())) {
            return Mono.error(new HomeProtectException(ErrorCode.INVALID_CONTRACT_TYPE));
        }
        InitSessionData sessionData = InitSessionData.builder()
                .sessionId(UUID.randomUUID().toString())
                .ocrSessionId(ocrSessionId)
                .address(request.getAddress())
                .admCd(request.getAdmCd())
                .rnMgtSn(request.getRnMgtSn())
                .mno(request.getMno())
                .sno(request.getSno())
                .deposit(request.getDeposit())
                .monthlyRent(request.getMonthlyRent())
                .contractType(request.getContractType())
                .contractPeriod(request.getContractPeriod())
                .build();
        return redisUtil.saveInitSession(sessionData)
                .doOnSuccess(v ->
                        jeonseRatioService.calculateAndSave(sessionData)
                                .onErrorResume(e -> {
                                    log.error("전세가율 백그라운드 계산 실패 [{}]: {}",
                                            sessionData.getSessionId(), e.getMessage());
                                    return Mono.empty();
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe()
                )
                .thenReturn(sessionData.getSessionId());
    }
}
