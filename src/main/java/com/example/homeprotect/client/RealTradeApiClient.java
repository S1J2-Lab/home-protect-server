package com.example.homeprotect.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

@Component
public class RealTradeApiClient {

    private static final Logger log = LoggerFactory.getLogger(RealTradeApiClient.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${public-api.external.seoul-real-trade-url}")
    private String baseUrl;

    @Value("${public-api.external.seoul-real-trade-key}")
    private String apiKey;

    @Value("${public-api.external.seoul-real-trade-service}")
    private String serviceName;

    private final WebClient webClient;

    public RealTradeApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone().build();
    }

    public Mono<Long> fetchAverageTradeAmount(String cggCd, String bldgUsg) {
        String cutoffDate = LocalDate.now().minusYears(2).format(DATE_FMT);
        String thisYear = String.valueOf(LocalDate.now().getYear());
        String lastYear = String.valueOf(LocalDate.now().getYear() - 1);

        Mono<List<Long>> thisYearData = fetchTradeByYear(thisYear, cggCd, bldgUsg, cutoffDate);
        Mono<List<Long>> lastYearData = fetchTradeByYear(lastYear, cggCd, bldgUsg, cutoffDate);

        return Mono.zip(thisYearData, lastYearData)
            .map(tuple -> {
                List<Long> combined = new ArrayList<>(tuple.getT1());
                combined.addAll(tuple.getT2());
                return combined.stream().mapToLong(Long::longValue).sum()
                    / Math.max(combined.size(), 1);
            });
    }

    private Mono<List<Long>> fetchTradeByYear(String year, String cggCd, String bldgUsg, String cutoffDate) {
        String uri = buildUri(year, cggCd, bldgUsg);
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(root -> parseTradeAmounts(root, cutoffDate, cggCd))
            .onErrorResume(e -> {
                log.error("서울시 매매 실거래가 API 호출 실패 [{}년]: {}", year, e.getMessage());
                return Mono.just(new ArrayList<>());
            });
    }

    private String buildUri(String year, String cggCd, String bldgUsg) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(baseUrl)
            .pathSegment(apiKey, "json", serviceName, "1", "1000", year, cggCd);
        if (bldgUsg != null && !bldgUsg.isEmpty()) {
            builder.pathSegment(bldgUsg);
        }
        return builder.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    private List<Long> parseTradeAmounts(JsonNode root, String cutoffDate, String cggCd) {
        List<Long> amounts = new ArrayList<>();
        JsonNode rows = root.path(serviceName).path("row");
        for (JsonNode row : rows) {
            if (!cggCd.equals(row.path("CGG_CD").asText())) continue;
            if (row.path("CTRT_DAY").asText().compareTo(cutoffDate) < 0) continue;
            double amt = row.path("THING_AMT").asDouble();
            if (amt > 0) amounts.add((long) (amt * 10_000));
        }
        return amounts;
    }


}
