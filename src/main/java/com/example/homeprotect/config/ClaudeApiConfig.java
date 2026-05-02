package com.example.homeprotect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

// + BaseApiConfig 상속으로 retryFilter() 공통화, ExchangeFilterFunction import 제거
@Configuration
// 타입 안정성을 위해 @Value 대신 Type-safe Properties(record)를 활용
@EnableConfigurationProperties(ClaudeApiConfig.ClaudeProperties.class)
public class ClaudeApiConfig extends BaseApiConfig {

    public static final String ANTHROPIC_VERSION_HEADER = "anthropic-version";
    public static final String ANTHROPIC_VERSION_VALUE = "2023-06-01";
    public static final String API_KEY_HEADER = "x-api-key";

    // @Value 대신 record를 사용하여 계층 구조를 명확히 하고, 애플리케이션 시작 시점에 설정값의 누락 여부를 검증
    @ConfigurationProperties(prefix = "claude.api")
    public record ClaudeProperties(String apiKey, String baseUrl, String model, int maxTokens, int timeout) {}

    private final ClaudeProperties properties;

    // 불변성 확보: 생성자 주입을 통해 필드를 final로 관리
    public ClaudeApiConfig(ClaudeProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "claudeWebClient")
    public WebClient claudeWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
            .clone()
            .baseUrl(properties.baseUrl())
            .defaultHeader(API_KEY_HEADER, properties.apiKey())
            .defaultHeader(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION_VALUE)
            .filter(retryFilter()) // + BaseApiConfig의 공통 retryFilter() 사용
            .clientConnector(new ReactorClientHttpConnector(
                WebClientConfig.buildHttpClient(5_000, properties.timeout())))
            .build();
    }
}
