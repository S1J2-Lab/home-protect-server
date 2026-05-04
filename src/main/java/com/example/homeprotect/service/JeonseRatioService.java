package com.example.homeprotect.service;

import org.springframework.stereotype.Service;

import com.example.homeprotect.client.RealTradeApiClient;
import com.example.homeprotect.client.RentApiClient;
import com.example.homeprotect.dto.redis.InitSessionData;
import com.example.homeprotect.dto.response.JeonseRatioResponse;
import com.example.homeprotect.util.RedisUtil;

import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class JeonseRatioService {

    private final RentApiClient rentApiClient;
    private final RealTradeApiClient realTradeApiClient;
    private final RedisUtil redisUtil;

    public JeonseRatioService(RentApiClient rentApiClient,
                               RealTradeApiClient realTradeApiClient,
                               RedisUtil redisUtil) {
        this.rentApiClient = rentApiClient;
        this.realTradeApiClient = realTradeApiClient;
        this.redisUtil = redisUtil;
    }

    public Mono<Void> calculateAndSave(InitSessionData sessionData) {
        if ("monthly".equals(sessionData.getContractType())) {
            JeonseRatioResponse monthly = JeonseRatioResponse.builder()
                    .ratioType("monthly")
                    .build();
            return redisUtil.saveJeonseRatio(sessionData.getSessionId(), monthly);
        }

        String admCd = sessionData.getAdmCd();
        if (admCd == null || admCd.length() < 10) {
            return Mono.error(new IllegalArgumentException("admCd가 null이거나 10자 미만입니다: " + admCd));
        }
        String cggCd = admCd.substring(0, 5);
        String stdgCd = admCd.substring(5, 10);
        String mno = sessionData.getMno();
        String sno = sessionData.getSno();
        String bldgUsg = parseBldgUsg(sessionData.getAddress());

        Mono<List<Long>> jeonseAmountsMono =
            // 1차: 건물 단위
            rentApiClient.fetchJeonseAmounts(cggCd, stdgCd, mno, sno, bldgUsg)
                .flatMap(amounts -> {
                    if (amounts.size() >= 3) return Mono.just(amounts);
                    // 2차: 법정동 단위로 확장
                    return rentApiClient.fetchJeonseAmounts(cggCd, stdgCd, null, null, bldgUsg);
                })
                .flatMap(amounts -> {
                    if (amounts.size() >= 3) return Mono.just(amounts);
                    // 3차: 구 단위로 확장
                    return rentApiClient.fetchJeonseAmounts(cggCd, null, null, null, bldgUsg);
                });

        return Mono.zip(jeonseAmountsMono, realTradeApiClient.fetchAverageTradeAmount(cggCd, bldgUsg))
                .flatMap(tuple -> {
                    List<Long> jeonseAmounts = tuple.getT1();
                    long avgTradeAmount = tuple.getT2();
                    JeonseRatioResponse response = buildResponse(sessionData, jeonseAmounts, avgTradeAmount);
                    return redisUtil.saveJeonseRatio(sessionData.getSessionId(), response);
                });
    }

    private JeonseRatioResponse buildResponse(InitSessionData data, List<Long> jeonseAmounts, long avgTradeAmount) {
        int sampleCount = jeonseAmounts.size();
        long recentHigh = jeonseAmounts.stream().mapToLong(Long::longValue).max().orElse(0L);
        long recentLow = jeonseAmounts.stream().mapToLong(Long::longValue).min().orElse(0L);
        long average = sampleCount > 0
                ? jeonseAmounts.stream().mapToLong(Long::longValue).sum() / sampleCount
                : 0L;

        long convertedDeposit = calcConvertedDeposit(data);

        Double ratioPercent = (convertedDeposit > 0 && avgTradeAmount > 0)
            ? (double) convertedDeposit / avgTradeAmount * 100
            : null;

        boolean lowReliability = sampleCount < 3;
        String riskLevel = resolveRiskLevel(ratioPercent);

        return JeonseRatioResponse.builder()
                .ratioType(data.getContractType())
                .recentHigh(recentHigh)
                .recentLow(recentLow)
                .average(average)
                .convertedDeposit(convertedDeposit)
                .ratioPercent(ratioPercent)
                .sampleCount(sampleCount)
                .lowReliability(lowReliability)
                .riskLevel(riskLevel)
                .build();
    }

    private long calcConvertedDeposit(InitSessionData data) {
        long deposit = data.getDeposit() != null ? data.getDeposit() : 0L;
        if ("half_jeonse".equals(data.getContractType())) {
            long monthlyRent = data.getMonthlyRent() != null ? data.getMonthlyRent() : 0L;
            return deposit + (long) (monthlyRent * 12 / 0.0475);
        }
        return deposit;
    }

    private String resolveRiskLevel(Double ratioPercent) {
        if (ratioPercent == null) return null;
        if (ratioPercent >= 80) return "danger";
        if (ratioPercent >= 60) return "caution";
        return "safe";
    }

    // 도로명 주소에는 건물 유형이 없는 경우가 많으므로 미인식 시 빈 문자열 반환 (BLDG_USG 필터 없이 전체 조회)
    private String parseBldgUsg(String address) {
        if (address == null) return "";
        if (address.contains("오피스텔")) return "오피스텔";
        if (address.contains("연립") || address.contains("다세대") || address.contains("빌라")) return "연립다세대";
        if (address.contains("단독") || address.contains("다가구")) return "단독다가구";
        if (address.contains("아파트") || address.contains("APT") || address.contains("apt")) return "아파트";
        return "";
    }
}
