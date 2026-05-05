package com.example.homeprotect.client;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.api.model}")
    private String model;

    private final WebClient webClient;

    public GeminiClient(@Qualifier("geminiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> generate(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        return webClient.post()
            .uri("/v1beta/models/" + model + ":generateContent")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(root -> root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText())
            .onErrorMap(
                e -> !(e instanceof HomeProtectException),
                e -> {
                    log.error("Gemini API 호출 실패: {}", e.getMessage());
                    return new HomeProtectException(ErrorCode.GEMINI_CALL_FAILED, e);
                }
            );
    }
}
