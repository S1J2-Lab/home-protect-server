package com.example.homeprotect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Configuration
// 타입 안정성을 위해 @Value 대신 Type-safe Properties(record)를 활용
@EnableConfigurationProperties(ClaudeApiConfig.ClaudeProperties.class)
public class ClaudeApiConfig {

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
            .filter(retryFilter())
            .clientConnector(new ReactorClientHttpConnector(
                WebClientConfig.buildHttpClient(5_000, properties.timeout())))
            .build();
    }

    // 일시적인 503 오류나 타임아웃 발생 시 3번까지 재시도
    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                // 일반 IO 오류뿐만 아니라 Reactor Netty의 갑작스러운 연결 종료도 재시도 대상으로 포함
                .filter(throwable -> throwable instanceof java.io.IOException
                    || throwable instanceof reactor.netty.http.client.PrematureCloseException));
    }
}
