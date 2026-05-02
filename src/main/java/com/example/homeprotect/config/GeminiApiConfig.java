package com.example.homeprotect.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

// + BaseApiConfig 상속으로 retryFilter() 공통화, Retry/Duration import 제거
@Configuration
@ConditionalOnProperty(name = "gemini.api.api-key")
// 타입 안정성을 위해 @Value 대신 Type-safe Properties(record)를 활용
@EnableConfigurationProperties(GeminiApiConfig.GeminiProperties.class)
public class GeminiApiConfig extends BaseApiConfig {

    public static final String API_KEY_PARAM = "key";

    // @Value 대신 record를 사용하여 계층 구조를 명확히 하고, 애플리케이션 시작 시점에 설정값의 누락 여부를 검증
    @ConfigurationProperties(prefix = "gemini.api")
    public record GeminiProperties(String apiKey, String baseUrl, String model, int timeout) {}

    private final GeminiProperties properties;

    // 불변성 확보: 생성자 주입을 통해 필드를 final로 관리
    public GeminiApiConfig(GeminiProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "geminiWebClient")
    public WebClient geminiWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
            .clone()
            .baseUrl(properties.baseUrl())
            .filter(appendApiKeyFilter())
            .filter(retryFilter()) // + BaseApiConfig의 공통 retryFilter() 사용
            .clientConnector(new ReactorClientHttpConnector(
                WebClientConfig.buildHttpClient(5_000, properties.timeout())))
            .build();
    }

    private ExchangeFilterFunction appendApiKeyFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            var uri = UriComponentsBuilder.fromUri(request.url())
                .queryParam(API_KEY_PARAM, properties.apiKey())
                .build().toUri();
            return Mono.just(ClientRequest.from(request).url(uri).build());
        });
    }
}
