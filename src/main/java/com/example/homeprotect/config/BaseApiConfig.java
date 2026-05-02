package com.example.homeprotect.config;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

// ClaudeApiConfig, GeminiApiConfig가 해당 클래스를 상속하여 재사용
public abstract class BaseApiConfig {

  static class RetryableException extends RuntimeException {
    RetryableException(String msg) { super(msg); }
  }

  protected ExchangeFilterFunction retryFilter() {
    return (request, next) ->
        Mono.defer(() -> next.exchange(request)
                .flatMap(response -> {
                  // 503은 예외로 변환해서 retryWhen이 잡을 수 있게
                  if (response.statusCode().value() == 503) {
                    return response.releaseBody()
                        .then(Mono.error(new RetryableException("503 Service Unavailable")));
                  }
                  return Mono.just(response);
                }))
            // 일시적인 503 오류나 타임아웃 발생 시 3번까지 재시도
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1))
                // 일반 IO 오류뿐만 아니라 Reactor Netty의 갑작스러운 연결 종료도 재시도 대상으로 포함
                .filter(throwable -> throwable instanceof java.io.IOException
                    || throwable instanceof reactor.netty.channel.AbortedException  // 수정
                    || throwable instanceof RetryableException));
  }
}
