package com.example.homeprotect.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "pinecone.api.api-key")
@EnableConfigurationProperties(PineconeConfig.PineconeProperties.class)
public class PineconeConfig {

    private static final String API_KEY_HEADER = "Api-Key";

    @ConfigurationProperties(prefix = "pinecone.api")
    public record PineconeProperties(String apiKey, String host, String index, String namespace, int timeout) {}

    private final PineconeProperties properties;

    public PineconeConfig(PineconeProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "pineconeWebClient")
    public WebClient pineconeWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
            .clone()
            .baseUrl(properties.host())
            .defaultHeader(API_KEY_HEADER, properties.apiKey())
            .defaultHeader("X-Pinecone-API-Version", "2025-04")
            .clientConnector(new ReactorClientHttpConnector(
                WebClientConfig.buildHttpClient(5_000, properties.timeout())))
            .build();
    }
}
