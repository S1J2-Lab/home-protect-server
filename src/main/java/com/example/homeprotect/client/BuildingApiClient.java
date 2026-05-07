package com.example.homeprotect.client;

import java.net.URI;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class BuildingApiClient {

    private static final Logger log = LoggerFactory.getLogger(BuildingApiClient.class);

    private static final String RECAP_TITLE_ENDPOINT = "getBrRecapTitleInfo";
    private static final String TITLE_ENDPOINT = "getBrTitleInfo";
    private static final String JIGUJI_INFO_ENDPOINT = "getBrJijiguInfo";
    private static final int TITLE_PAGE_SIZE = 10;
    private static final int JIGUJI_PAGE_SIZE = 100;
    private static final Set<String> REDEVELOPMENT_KEYWORDS =
        Set.of("정비구역", "재개발", "재건축", "주거환경개선", "도시환경정비");

    @Value("${public-api.external.molit-building-url}")
    private String baseUrl;

    @Value("${public-api.external.molit-building-key}")
    private String serviceKey;

    private final WebClient webClient;

    public BuildingApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone().build();
    }

    public Mono<JsonNode> fetchTitleItem(String sigunguCd, String bjdongCd, String bun, String ji) {
        return callRecapTitleApi(sigunguCd, bjdongCd, bun, ji)
            .switchIfEmpty(callTitleApi(sigunguCd, bjdongCd, bun, ji))
            .switchIfEmpty(Mono.error(new HomeProtectException(ErrorCode.BUILDING_INFO_NOT_FOUND)))
            .onErrorMap(e -> !(e instanceof HomeProtectException), e -> {
                log.error("건축물대장 API 호출 실패: {}", e.getMessage());
                return new HomeProtectException(ErrorCode.API_UNAVAILABLE, e);
            });
    }

    private Mono<JsonNode> callRecapTitleApi(String sigunguCd, String bjdongCd, String bun, String ji) {
        return callApi(RECAP_TITLE_ENDPOINT, sigunguCd, bjdongCd, bun, ji, TITLE_PAGE_SIZE)
            .flatMap(this::extractFirstItem);
    }

    private Mono<JsonNode> callTitleApi(String sigunguCd, String bjdongCd, String bun, String ji) {
        return callApi(TITLE_ENDPOINT, sigunguCd, bjdongCd, bun, ji, TITLE_PAGE_SIZE)
            .flatMap(this::extractFirstItem);
    }

    // mgmBldrgstPk는 22자리 이상일 수 있어 Long 범위 초과 → 해당 필드는 반드시 asText()로 읽을 것
    private Mono<JsonNode> extractFirstItem(JsonNode root) {
        JsonNode totalCountNode = root.path("response").path("body").path("totalCount");
        if (totalCountNode.isMissingNode()) {
            return Mono.error(new HomeProtectException(ErrorCode.API_UNAVAILABLE));
        }
        if (totalCountNode.asInt() == 0) {
            return Mono.empty();
        }
        JsonNode items = root.path("response").path("body").path("items").path("item");
        JsonNode item = items.isArray() ? items.get(0) : items;
        if (item == null || item.isNull() || item.isMissingNode()) {
            return Mono.empty();
        }
        return Mono.just(item);
    }

    public Mono<Boolean> fetchIsRedevelopmentZone(String sigunguCd, String bjdongCd, String bun, String ji) {
        return callApi(JIGUJI_INFO_ENDPOINT, sigunguCd, bjdongCd, bun, ji, JIGUJI_PAGE_SIZE)
            .map(root -> {
                JsonNode items = root.path("response").path("body").path("items").path("item");
                if (items.isMissingNode() || items.isNull()) return false;
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        if (hasRedevelopmentKeyword(item.path("jijiguCdNm").asText())) return true;
                    }
                    return false;
                }
                return hasRedevelopmentKeyword(items.path("jijiguCdNm").asText());
            })
            .onErrorMap(e -> {
                log.error("건축물대장 지역지구구역 API 호출 실패: {}", e.getMessage());
                return new HomeProtectException(ErrorCode.API_UNAVAILABLE, e);
            });
    }

    private Mono<JsonNode> callApi(String endpoint, String sigunguCd, String bjdongCd,
        String bun, String ji, int numOfRows) {
        String uri = baseUrl + "/" + endpoint
            + "?serviceKey=" + serviceKey
            + "&sigunguCd=" + sigunguCd
            + "&bjdongCd=" + bjdongCd
            + "&bun=" + bun
            + "&ji=" + ji
            + "&_type=json"
            + "&numOfRows=" + numOfRows
            + "&pageNo=1";

      return webClient.get()
          .uri(URI.create(uri))
          .retrieve()
          .bodyToMono(JsonNode.class);
    }

    private boolean hasRedevelopmentKeyword(String value) {
        return REDEVELOPMENT_KEYWORDS.stream().anyMatch(value::contains);
    }
}
