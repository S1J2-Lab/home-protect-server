package com.example.homeprotect.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int RESPONSE_TIMEOUT_MS = 30_000;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = buildHttpClient(CONNECTION_TIMEOUT_MS, RESPONSE_TIMEOUT_MS);

        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    static HttpClient buildHttpClient(int connectTimeoutMs, int responseTimeoutMs) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));
    }
}
