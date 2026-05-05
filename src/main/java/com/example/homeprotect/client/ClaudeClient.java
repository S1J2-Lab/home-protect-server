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
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    private final WebClient webClient;

    public ClaudeClient(@Qualifier("claudeWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> generate(String prompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
            .uri("/v1/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(root -> {
                JsonNode content = root.path("content").get(0);
                if (content == null) {
                    throw new HomeProtectException(ErrorCode.CLAUDE_CALL_FAILED);
                }
                String text = content.path("text").asText();
                if (text.isBlank()) {
                    throw new HomeProtectException(ErrorCode.CLAUDE_CALL_FAILED);
                }
                return text;
            })
            .onErrorMap(
                e -> !(e instanceof HomeProtectException),
                e -> {
                    log.error("Claude API 호출 실패: {}", e.getMessage());
                    return new HomeProtectException(ErrorCode.CLAUDE_CALL_FAILED, e);
                }
            );
    }
}
