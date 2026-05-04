package com.example.homeprotect.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.request.AnalysisInitRequest;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.util.RedisUtil;

import reactor.core.publisher.Mono;

@Service
public class AnalysisService {

    private static final Set<String> VALID_CONTRACT_TYPES = Set.of("jeonse", "half_jeonse", "monthly");

    private final RedisUtil redisUtil;

    public AnalysisService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public Mono<String> initAnalysis(AnalysisInitRequest request, String ocrSessionId) {
        if (!VALID_CONTRACT_TYPES.contains(request.getContractType())) {
            return Mono.error(new HomeProtectException(ErrorCode.INVALID_CONTRACT_TYPE));
        }
        InitSessionData sessionData = InitSessionData.builder()
                .sessionId(UUID.randomUUID().toString())
                .ocrSessionId(ocrSessionId)
                .address(request.getAddress())
                .admCd(request.getAdmCd())
                .rnMgtSn(request.getRnMgtSn())
                .deposit(request.getDeposit())
                .monthlyRent(request.getMonthlyRent())
                .contractType(request.getContractType())
                .contractPeriod(request.getContractPeriod())
                .build();
        return redisUtil.saveInitSession(sessionData).thenReturn(sessionData.getSessionId());
    }
}
