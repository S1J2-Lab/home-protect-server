package com.example.homeprotect.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.homeprotect.dto.response.AddressResponse;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Service
public class AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressService.class);

    @Value("${public-api.external.mois-address-url}")
    private String moisAddressUrl;

    @Value("${public-api.external.mois-address-key}")
    private String moisAddressKey;

    private final WebClient webClient;

    public AddressService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .defaultHeaders(headers -> headers.remove(HttpHeaders.CONTENT_TYPE))
                .build();
    }

    public Mono<List<AddressResponse>> searchAddress(String keyword) {
        return webClient.get()
            .uri(buildUri(keyword))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .flatMap(root -> {
                log.debug("행안부 API 응답 totalCount: {}", root.path("results").path("common").path("totalCount").asText());
                try {
                    validateResponse(root);
                    return Mono.just(parseJusoList(root.path("results").path("juso")));
                } catch (HomeProtectException e) {
                    return Mono.error(e);
                }
            })
            .onErrorMap(e -> !(e instanceof HomeProtectException), e -> {
                if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException wex) {
                    log.error("주소 API 호출 실패: {} | 응답 바디: {}", e.getMessage(), wex.getResponseBodyAsString());
                } else {
                    log.error("주소 API 호출 실패: {}", e.getMessage());
                }
                return new HomeProtectException(ErrorCode.INVALID_ADDRESS);
            });
    }

    private URI buildUri(String keyword) {
        return UriComponentsBuilder.fromUriString(moisAddressUrl)
            .queryParam("confmKey", moisAddressKey)
            .queryParam("currentPage", 1)
            .queryParam("countPerPage", 10)
            .queryParam("keyword", keyword)
            .queryParam("resultType", "json")
            .encode()
            .build()
            .toUri();
    }

    private void validateResponse(JsonNode root) {
        String errorCode = root.path("results").path("common").path("errorCode").asText();
        if (!"0".equals(errorCode)) {
            throw new HomeProtectException(ErrorCode.INVALID_ADDRESS);
        }
        JsonNode juso = root.path("results").path("juso");
        if (!juso.isArray() || juso.isEmpty()) {
            throw new HomeProtectException(ErrorCode.INVALID_ADDRESS);
        }
    }

    private List<AddressResponse> parseJusoList(JsonNode jusoArray) {
        List<AddressResponse> results = new ArrayList<>();
        for (JsonNode juso : jusoArray) {
            results.add(AddressResponse.builder()
                .roadAddress(juso.path("roadAddr").asText())
                .jibunAddress(juso.path("jibunAddr").asText())
                .buildingName(juso.path("bdNm").asText())
                .admCd(juso.path("admCd").asText())
                .rnMgtSn(juso.path("rnMgtSn").asText())
                .mno(String.format("%04d", juso.path("buldMnnm").asInt()))  // 추가
                .sno(String.format("%04d", juso.path("buldSlno").asInt()))  // 추가
                .build());
        }
        return results;
    }
}
