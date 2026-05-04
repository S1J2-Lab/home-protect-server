package com.example.homeprotect.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.homeprotect.client.BuildingApiClient;
import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.response.BuildingResponse;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.util.RedisUtil;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Service
public class BuildingService {

    private static final Set<String> SAFE_USES =
        Set.of("아파트", "공동주택", "다세대주택", "단독주택", "다가구주택", "연립주택");
    private static final Set<String> DANGER_USES =
        Set.of("근린생활시설", "제1종 근린생활시설", "제2종 근린생활시설",
               "업무시설", "공장", "창고시설", "위험물저장및처리시설");

    private final BuildingApiClient buildingApiClient;
    private final RedisUtil redisUtil;

    public BuildingService(BuildingApiClient buildingApiClient, RedisUtil redisUtil) {
        this.buildingApiClient = buildingApiClient;
        this.redisUtil = redisUtil;
    }

    public Mono<Void> calculateAndSave(InitSessionData sessionData) {
        String bdMgtSn = sessionData.getBdMgtSn();
        if (bdMgtSn == null || bdMgtSn.length() < 10) {
            return Mono.error(new HomeProtectException(ErrorCode.INVALID_ADDRESS));
        }
        String sigunguCd = bdMgtSn.substring(0, 5);
        String bjdongCd = bdMgtSn.substring(5, 10);
        String bun = bdMgtSn.substring(11, 15);  // 지번 본번
        String ji = bdMgtSn.substring(15, 19);   // 지번 부번

        return Mono.zip(
            buildingApiClient.fetchTitleItem(sigunguCd, bjdongCd, bun, ji),
            buildingApiClient.fetchIsRedevelopmentZone(sigunguCd, bjdongCd, bun, ji)
        ).flatMap(tuple -> {
            BuildingResponse response = buildResponse(tuple.getT1(), tuple.getT2());
            return redisUtil.saveBuildingInfo(sessionData.getSessionId(), response);
        });
    }

    private BuildingResponse buildResponse(JsonNode titleItem, boolean isRedevelopmentZone) {
        String primaryUse = titleItem.path("mainPurpsCdNm").asText("");
        String useAprDay = titleItem.path("useAprDay").asText("");

        return BuildingResponse.builder()
            .level(resolveLevel(primaryUse))
            .primaryUse(primaryUse.isEmpty() ? null : primaryUse)
            .isResidential(SAFE_USES.contains(primaryUse))
            .violation(false)
            .approvedDate(formatApprovedDate(useAprDay))
            .redevelopmentZone(isRedevelopmentZone)
            .build();
    }

    private String resolveLevel(String primaryUse) {
        if (SAFE_USES.contains(primaryUse)) return "safe";
        if (DANGER_USES.contains(primaryUse)) return "danger";
        return "caution";
    }

    private String formatApprovedDate(String useAprDay) {
        if (useAprDay == null || useAprDay.length() < 8) return null;
        return useAprDay.substring(0, 4) + "-" + useAprDay.substring(4, 6) + "-" + useAprDay.substring(6, 8);
    }

    private String padFour(String value) {
        if (value == null || value.isBlank()) return "0000";
        return String.format("%04d", Integer.parseInt(value));
    }
}
