package com.example.homeprotect.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.homeprotect.client.ClaudeClient;
import com.example.homeprotect.dto.response.ContractClauseResult.Clause;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class RiskClauseAnalyzer {

  private static final Logger log = LoggerFactory.getLogger(RiskClauseAnalyzer.class);

  private static final String PROMPT_TEMPLATE = """
            당신은 한국 임대차계약서를 분석하는 전문가입니다.
            아래 조항 목록을 검토하여 임차인에게 불리하거나 불공정한 조항의 index만 추출하세요.
            [주의]
            - 판단 기준에 명확히 해당하는 조항만 포함할 것
            - 애매한 경우 포함하지 말 것
            - 표준 임대차 계약서에 공통적으로 포함되는 조항은 제외할 것

            [판단 기준]
            - 법적으로 무효이거나 강행규정에 위반되는 조항
            - 임차인의 권리를 일방적으로 제한하는 조항
            - 임대인에게만 유리한 조항
            - 임차인이 인지하지 못할 수 있는 불리한 조건

            [출력 형식] - 반드시 JSON만 출력, 다른 텍스트 절대 출력 금지
            {"riskIndexes": [1, 3, 5]}

            [조항 목록]
            {{CLAUSES}}
            """;

  private final ClaudeClient claudeClient;
  private final ObjectMapper objectMapper;

  public RiskClauseAnalyzer(ClaudeClient claudeClient, ObjectMapper objectMapper) {
    this.claudeClient = claudeClient;
    this.objectMapper = objectMapper;
  }

  public Mono<List<Clause>> analyze(List<Clause> clauses) {
    if (clauses == null || clauses.isEmpty()) {
      return Mono.just(List.of());
    }

    String clauseText = clauses.stream()
        .map(c -> c.getIndex() + ". [" + c.getSection() + "] " + c.getOriginalText())
        .collect(java.util.stream.Collectors.joining("\n"));

    String prompt = PROMPT_TEMPLATE.replace("{{CLAUSES}}", clauseText);

    return claudeClient.generate(prompt)
        .map(response -> {
          try {
            String json = stripMarkdown(response);
            JsonNode root = objectMapper.readTree(json);
            Set<Integer> riskIndexes = new HashSet<>();
            root.path("riskIndexes").forEach(n -> riskIndexes.add(n.asInt()));
            return clauses.stream()
                .filter(c -> riskIndexes.contains(c.getIndex()))
                .toList();
          } catch (Exception e) {
            log.error("RiskClauseAnalyzer 파싱 실패", e);
            return List.of();
          }
        });
  }

  private String stripMarkdown(String text) {
    String stripped = text.strip();
    if (stripped.startsWith("```")) {
      int firstNewline = stripped.indexOf('\n');
      int lastFence = stripped.lastIndexOf("```");
      if (firstNewline != -1 && lastFence > firstNewline) {
        return stripped.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return stripped;
  }
}
