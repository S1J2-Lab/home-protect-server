package com.example.homeprotect.service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.request.AnalysisInitRequest;
import com.example.homeprotect.dto.request.AnalysisRunRequest;
import com.example.homeprotect.dto.response.AnalysisResult;
import com.example.homeprotect.dto.response.AnalysisResultResponse;
import com.example.homeprotect.dto.response.BuildingResponse;
import com.example.homeprotect.dto.response.ContractAnalysisResult;
import com.example.homeprotect.dto.response.JeonseRatioResponse;
import com.example.homeprotect.dto.response.RegistryAnalysisResult;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.util.RedisUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final Set<String> VALID_CONTRACT_TYPES = Set.of("jeonse", "half_jeonse", "monthly");
    private static final List<String> ANALYSIS_STEPS = List.of("jeonseRatio", "registryParse", "contractReview", "buildingCheck");
    private static final int ANALYSIS_TIMEOUT_SECONDS = 30;
    private static final long POLL_INTERVAL_MS = 500;
    private static final String COMPLETE_EVENT_DATA = "{\"step\":\"complete\"}";
    private static final DateTimeFormatter ANALYZED_AT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final RedisUtil redisUtil;
    private final JeonseRatioService jeonseRatioService;
    private final BuildingService buildingService;
    private final RegistryService registryService;
    private final ContractService contractService;
    private final ContractAnalysisService contractAnalysisService;

    public AnalysisService(RedisUtil redisUtil, JeonseRatioService jeonseRatioService,
        BuildingService buildingService, RegistryService registryService,
        ContractService contractService, ContractAnalysisService contractAnalysisService) {
        this.redisUtil = redisUtil;
        this.jeonseRatioService = jeonseRatioService;
        this.buildingService = buildingService;
        this.registryService = registryService;
        this.contractService = contractService;
        this.contractAnalysisService = contractAnalysisService;
    }

    public Mono<String> initAnalysis(AnalysisInitRequest request) {
        if (request.getContractType() == null || !VALID_CONTRACT_TYPES.contains(request.getContractType())) {
            return Mono.error(new HomeProtectException(ErrorCode.INVALID_CONTRACT_TYPE));
        }
        InitSessionData sessionData = InitSessionData.builder()
            .sessionId(java.util.UUID.randomUUID().toString())
            .address(request.getAddress())
            .admCd(request.getAdmCd())
            .bdMgtSn(request.getBdMgtSn())
            .rnMgtSn(request.getRnMgtSn())
            .mno(request.getMno())
            .sno(request.getSno())
            .deposit(request.getDeposit())
            .monthlyRent(request.getMonthlyRent())
            .contractType(request.getContractType())
            .contractPeriod(request.getContractPeriod())
            .build();
        return redisUtil.saveInitSession(sessionData)
            .doOnSuccess(ignored -> {
                jeonseRatioService.calculateAndSave(sessionData)
                    .onErrorResume(e -> {
                        log.error("전세가율 백그라운드 계산 실패 [{}]: {}", sessionData.getSessionId(), e.getMessage());
                        return Mono.empty();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();

                buildingService.calculateAndSave(sessionData)
                    .onErrorResume(e -> {
                        log.error("건축물 정보 백그라운드 조회 실패 [{}]: {}", sessionData.getSessionId(), e.getMessage());
                        return Mono.empty();
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
            })
            .thenReturn(sessionData.getSessionId());
    }

    public Mono<String> runAnalysis(AnalysisRunRequest request) {
        if (!request.isOwnerVerified()) {
            return Mono.error(new HomeProtectException(ErrorCode.OWNER_NOT_VERIFIED));
        }
        redisUtil.getInitSession(request.getSessionId())
            .flatMap(sessionData -> executeAnalyses(request, sessionData))
            .onErrorResume(e -> {
                log.error("분석 실패 [{}]: {}", request.getSessionId(), e.getMessage());
                return Mono.empty();
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
        return Mono.just(request.getSessionId());
    }

    public Mono<AnalysisResultResponse> getAnalysisResult(String sessionId) {
        return redisUtil.getAnalysisResult(sessionId)
            .map(result -> AnalysisResultResponse.builder()
                .address(result.getAddress())
                .analyzedAt(result.getAnalyzedAt())
                .jeonseRatio(result.getJeonseRatio())
                .registry(result.getRegistryParse() != null ? result.getRegistryParse().getRegistry() : null)
                .building(result.getBuildingCheck())
                .contract(result.getContractReview())
                .build());
    }

    private Mono<Void> executeAnalyses(AnalysisRunRequest request, InitSessionData sessionData) {
        String sessionId = request.getSessionId();
        CompletableFuture<JeonseRatioResponse> jeonseFuture = runStep(
            redisUtil.getJeonseRatio(sessionId)
                .onErrorResume(HomeProtectException.class,
                    e -> jeonseRatioService.calculateAndSave(sessionData).then(redisUtil.getJeonseRatio(sessionId))),
            sessionId, "jeonseRatio");
        CompletableFuture<RegistryAnalysisResult> registryFuture = runStep(
            registryService.analyze(request.getRegistrySessionId()), sessionId, "registryParse");
        CompletableFuture<ContractAnalysisResult> contractFuture = runStep(
            contractService.analyze(request.getContractSessionId())
                .then(contractAnalysisService.analyze(request.getContractSessionId())),
            sessionId, "contractReview");
        CompletableFuture<BuildingResponse> buildingFuture = runStep(
            redisUtil.getBuildingInfo(sessionId)
                .onErrorResume(HomeProtectException.class,
                    e -> buildingService.calculateAndSave(sessionData).then(redisUtil.getBuildingInfo(sessionId))),
            sessionId, "buildingCheck");
        return awaitAndSaveResult(sessionId, sessionData.getAddress(), jeonseFuture, registryFuture, contractFuture, buildingFuture);
    }

    private Mono<Void> awaitAndSaveResult(String sessionId, String address,
        CompletableFuture<JeonseRatioResponse> jeonseFuture,
        CompletableFuture<RegistryAnalysisResult> registryFuture,
        CompletableFuture<ContractAnalysisResult> contractFuture,
        CompletableFuture<BuildingResponse> buildingFuture) {
        return Mono.fromFuture(CompletableFuture
            .allOf(jeonseFuture, registryFuture, contractFuture, buildingFuture)
            .thenCompose(ignored -> {
                String analyzedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(ANALYZED_AT_FORMATTER);
                AnalysisResult result = AnalysisResult.builder()
                    .address(address)
                    .analyzedAt(analyzedAt)
                    .jeonseRatio(jeonseFuture.getNow(null))
                    .registryParse(registryFuture.getNow(null))
                    .contractReview(contractFuture.getNow(null))
                    .buildingCheck(buildingFuture.getNow(null))
                    .build();
                return redisUtil.saveAnalysisResult(sessionId, result).toFuture();
            })
        ).then();
    }

    private <T> CompletableFuture<T> runStep(Mono<T> analysis, String sessionId, String step) {
        return analysis
            .timeout(Duration.ofSeconds(ANALYSIS_TIMEOUT_SECONDS))
            .flatMap(result -> redisUtil.saveAnalysisStatus(sessionId, step, "done").thenReturn(result))
            .onErrorResume(e -> {
                log.error("분석 단계 실패 [{}][{}]: {}", sessionId, step, e.getMessage());
                return redisUtil.saveAnalysisStatus(sessionId, step, "error").then(Mono.empty());
            })
            .toFuture();
    }

    public Flux<ServerSentEvent<String>> streamAnalysisStatus(String sessionId) {
        Map<String, String> emitted = new HashMap<>();
        return Flux.interval(Duration.ZERO, Duration.ofMillis(POLL_INTERVAL_MS))
            .concatMap(tick -> collectNewEvents(sessionId, emitted))
            .takeUntil(sse -> ANALYSIS_STEPS.stream().allMatch(emitted::containsKey));
    }

    private Flux<ServerSentEvent<String>> collectNewEvents(String sessionId, Map<String, String> emitted) {
        return Flux.fromIterable(ANALYSIS_STEPS)
            .filter(step -> !emitted.containsKey(step))
            .concatMap(step ->
                redisUtil.getAnalysisStatus(sessionId, step)
                    .filter(status -> "done".equals(status) || "error".equals(status))
                    .doOnNext(status -> emitted.put(step, status))
                    .map(status -> "error".equals(status)
                        ? toSse("{\"step\":\"error\",\"errorCode\":\"API_UNAVAILABLE\"}")
                        : toSse("{\"step\":\"" + step + "\",\"status\":\"done\"}"))
            )
            .concatWith(Mono.defer(() -> {
                boolean allDone = ANALYSIS_STEPS.stream().allMatch(emitted::containsKey);
                return allDone ? Mono.just(toSse(COMPLETE_EVENT_DATA)) : Mono.empty();
            }));
    }

    private ServerSentEvent<String> toSse(String data) {
        return ServerSentEvent.<String>builder().data(data).build();
    }
}
