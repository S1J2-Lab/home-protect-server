package com.example.homeprotect.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.homeprotect.dto.response.PrecedentSearchResult.PrecedentMatch;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class VectorDbClient {

  private static final Logger log = LoggerFactory.getLogger(VectorDbClient.class);

  @Value("${pinecone.api.namespace}")
  private String namespace;

  private final WebClient webClient;

  public VectorDbClient(@Qualifier("pineconeWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<List<PrecedentMatch>> search(String text, int topK) {
    Map<String, Object> inputs = new LinkedHashMap<>();
    inputs.put("text", text);

    Map<String, Object> query = new LinkedHashMap<>();
    query.put("inputs", inputs);
    query.put("top_k", topK);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("query", query);

    return webClient.post()
        .uri("/records/namespaces/{namespace}/search", namespace)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .onStatus(
            status -> status.is4xxClientError() || status.is5xxServerError(),
            response -> response.bodyToMono(String.class)
                .doOnNext(errorBody -> log.error("Pinecone 오류 응답 body: {}", errorBody))
                .flatMap(errorBody -> Mono.error(
                    new HomeProtectException(ErrorCode.VECTOR_DB_FAILED)))
        )
        .bodyToMono(JsonNode.class)
        .map(root -> {
          JsonNode hits = root.path("result").path("hits");
          List<PrecedentMatch> matches = new ArrayList<>();
          for (JsonNode hit : hits) {
            double score = hit.path("_score").asDouble();
            JsonNode fields = hit.path("fields");
            String caseNumber = fields.path("case_number").asText(hit.path("_id").asText());
            String summary = fields.path("case_summary").asText(null);
            matches.add(PrecedentMatch.builder()
                .precedentId(caseNumber)
                .score(score)
                .summary(summary)
                .build());
          }
          return matches;
        })
        .onErrorMap(
            e -> !(e instanceof HomeProtectException),
            e -> {
              log.error("Pinecone 검색 실패: {}", e.getMessage());
              return new HomeProtectException(ErrorCode.VECTOR_DB_FAILED, e);
            }
        );
  }
}
