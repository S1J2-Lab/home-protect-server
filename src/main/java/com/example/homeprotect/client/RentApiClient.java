package com.example.homeprotect.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RentApiClient {

    private static final Logger log = LoggerFactory.getLogger(RentApiClient.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int PAGE_SIZE = 1000;

    @Value("${public-api.external.seoul-rent-url}")
    private String baseUrl;

    @Value("${public-api.external.seoul-rent-key}")
    private String apiKey;

    @Value("${public-api.external.seoul-rent-service}")
    private String serviceName;

    @Value("${public-api.external.seoul-rent-type-field}")
    private String rentTypeField;

    @Value("${public-api.external.seoul-rent-type-jeonse}")
    private String jeonseTypeValue;

    private static final Charset CP949 = Charset.forName("MS949");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RentApiClient(@Qualifier("seoulWebClientBuilder") WebClient.Builder webClientBuilder,
                         ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.clone().build();
        this.objectMapper = objectMapper;
    }

    public Mono<List<Long>> fetchJeonseAmounts(String cggCd, String stdgCd, String mno, String sno, String bldgUsg) {
        LocalDate today = LocalDate.now();
        String cutoffDate = today.minusYears(2).format(DATE_FMT);
        String thisYear = String.valueOf(today.getYear());
        String lastYear = String.valueOf(today.getYear() - 1);
        String twoYearsAgo = String.valueOf(today.getYear() - 2);

        return Flux.concat(
                fetchByYear(thisYear, cggCd, stdgCd, mno, sno, bldgUsg, cutoffDate),
                fetchByYear(lastYear, cggCd, stdgCd, mno, sno, bldgUsg, cutoffDate),
                fetchByYear(twoYearsAgo, cggCd, stdgCd, mno, sno, bldgUsg, cutoffDate)
            )
            .collectList()
            .map(lists -> lists.stream()
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.toList()));
    }

    private Mono<List<Long>> fetchByYear(String year, String cggCd, String stdgCd,
        String mno, String sno, String bldgUsg, String cutoffDate) {
        return fetchPage(1, PAGE_SIZE, year, cggCd, bldgUsg)
            .flatMap(root -> {
                List<Long> firstPage = parseJeonseAmounts(root, cutoffDate, cggCd, stdgCd, mno, sno);
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
                        .map(r -> parseJeonseAmounts(r, cutoffDate, cggCd, stdgCd, mno, sno))
                        .onErrorResume(e -> {
                            log.error("서울시 전월세가 API 호출 실패 [{}년 {}페이지]: {}", year, p, e.getMessage());
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
                log.error("서울시 전월세가 API 호출 실패 [{}년]:", year, e);
                return Mono.just(new ArrayList<>());
            });
    }

    private Mono<JsonNode> fetchPage(int start, int end, String year, String cggCd, String bldgUsg) {
        String uri = buildUri(start, end, year, cggCd, bldgUsg);
        log.info("[전월세API] 요청 URL={}", uri);
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(byte[].class)
            .flatMap(bytes -> {
                try {
                    return Mono.just(objectMapper.readTree(decodeSeoulResponse(bytes)));
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("Seoul rent API parse failed: " + e.getMessage()));
                }
            });
    }

    private String decodeSeoulResponse(byte[] bytes) {
        try {
            CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT);
            return utf8.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (Exception e) {
            return new String(bytes, CP949);
        }
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

    private List<Long> parseJeonseAmounts(JsonNode root, String cutoffDate,
        String cggCd, String stdgCd,
        String mno, String sno) {
        List<Long> amounts = new ArrayList<>();
        JsonNode rows = root.path(serviceName).path("row");
        int totalRows = rows.size();
        log.info("[전월세API] 응답 총 rows={}, 필터 cggCd={}, stdgCd={}, mno={}", totalRows, cggCd, stdgCd, mno);
        for (JsonNode row : rows) {
            if (!cggCd.equals(row.path("CGG_CD").asText())) continue;
            if (stdgCd != null && !stdgCd.isEmpty()
                && !stdgCd.equals(row.path("STDG_CD").asText())) continue;
            if (mno != null && !mno.isEmpty()
                && !mno.equals(row.path("MNO").asText())) continue;
            log.info("[전월세API] 매칭 row - CGG_CD={}, STDG_CD={}, MNO={}, SNO={}, GRFE={}",
                row.path("CGG_CD").asText(), row.path("STDG_CD").asText(),
                row.path("MNO").asText(), row.path("SNO").asText(), row.path("GRFE").asText());
            if (sno != null && !sno.isEmpty()
                && !sno.equals(row.path("SNO").asText())) continue;
            if (!jeonseTypeValue.equals(row.path(rentTypeField).asText())) continue;
            if (row.path("CTRT_DAY").asText().compareTo(cutoffDate) < 0) continue;
            double grfe = row.path("GRFE").asDouble();
            if (grfe > 0) amounts.add((long) (grfe * 10_000));
        }
        return amounts;
    }
}
