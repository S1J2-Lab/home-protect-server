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

import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RealTradeApiClient {

    private static final Logger log = LoggerFactory.getLogger(RealTradeApiClient.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int PAGE_SIZE = 1000;

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
        LocalDate today = LocalDate.now();
        String cutoffDate = today.minusYears(2).format(DATE_FMT);
        String thisYear = String.valueOf(today.getYear());
        String lastYear = String.valueOf(today.getYear() - 1);
        String twoYearsAgo = String.valueOf(today.getYear() - 2);

        Mono<List<Long>> thisYearData = fetchTradeByYear(thisYear, cggCd, bldgUsg, cutoffDate);
        Mono<List<Long>> lastYearData = fetchTradeByYear(lastYear, cggCd, bldgUsg, cutoffDate);
        Mono<List<Long>> twoYearsAgoData = fetchTradeByYear(twoYearsAgo, cggCd, bldgUsg, cutoffDate);

        return Mono.zip(thisYearData, lastYearData, twoYearsAgoData)
            .map(tuple -> {
                List<Long> combined = new ArrayList<>(tuple.getT1());
                combined.addAll(tuple.getT2());
                combined.addAll(tuple.getT3());
                return combined.stream().mapToLong(Long::longValue).sum()
                    / Math.max(combined.size(), 1);
            });
    }

    private Mono<List<Long>> fetchTradeByYear(String year, String cggCd, String bldgUsg, String cutoffDate) {
        return fetchPage(1, PAGE_SIZE, year, cggCd, bldgUsg)
            .flatMap(root -> {
                List<Long> firstPage = parseTradeAmounts(root, cutoffDate, cggCd);
                int totalCount = root.path(serviceName).path("list_total_count").asInt();
                if (totalCount <= PAGE_SIZE) {
                    return Mono.just(firstPage);
                }
                int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                List<Mono<List<Long>>> remainingMonos = new ArrayList<>();
                for (int page = 2; page <= totalPages; page++) {
                    int start = (page - 1) * PAGE_SIZE + 1;
                    int end = page * PAGE_SIZE;
                    final int p = page;
                    remainingMonos.add(fetchPage(start, end, year, cggCd, bldgUsg)
                        .map(r -> parseTradeAmounts(r, cutoffDate, cggCd))
                        .onErrorResume(e -> {
                            log.error("서울시 매매 실거래가 API 호출 실패 [{}년 {}페이지]: {}", year, p, e.getMessage());
                            return Mono.just(new ArrayList<>());
                        }));
                }
                return Flux.merge(remainingMonos)
                    .collectList()
                    .map(lists -> {
                        List<Long> all = new ArrayList<>(firstPage);
                        lists.forEach(all::addAll);
                        return all;
                    });
            })
            .onErrorResume(e -> {
                log.error("서울시 매매 실거래가 API 호출 실패 [{}년]: {}", year, e.getMessage());
                return Mono.just(new ArrayList<>());
            });
    }

    private Mono<JsonNode> fetchPage(int start, int end, String year, String cggCd, String bldgUsg) {
        String uri = buildUri(start, end, year, cggCd, bldgUsg);
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }

    private String buildUri(int start, int end, String year, String cggCd, String bldgUsg) {
      StringBuilder path = new StringBuilder("/")
          .append(apiKey).append("/json/").append(serviceName).append("/")
          .append(start).append("/").append(end).append("/")
          .append(year).append("/").append(cggCd);
      if (bldgUsg != null && !bldgUsg.isEmpty()) {
        path.append("/").append(UriUtils.encodePath(bldgUsg, StandardCharsets.UTF_8));
      }
      String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
      return base + path;
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
