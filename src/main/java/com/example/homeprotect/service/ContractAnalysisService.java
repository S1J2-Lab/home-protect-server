package com.example.homeprotect.service;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.homeprotect.client.ClaudeClient;
import com.example.homeprotect.dto.response.ContractAnalysisResult;
import com.example.homeprotect.dto.response.PrecedentSearchResult;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class ContractAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ContractAnalysisService.class);

    private static final String ANALYSIS_PROMPT_TEMPLATE = """
            당신은 한국 임대차계약서의 불리한 조항을 분석하는 법률 전문가입니다.
            아래는 계약서에서 추출된 위험 조항 목록과 관련 판례 정보입니다.
            각 조항을 분석하여 임차인이 이해하기 쉬운 설명과 구체적인 대응 방법을 제시하세요.

            [분석 대상 조항]
            {{CLAUSES}}

            [분석 규칙]
            1. 각 조항에 대해 title(한 줄 핵심 요약), legalIssue(일반인이 이해할 수 있는 법적 문제 설명), suggestion(임차인이 취해야 할 구체적 행동) 생성
            2. level은 입력값 그대로 사용 (danger 또는 caution)
            3. originalText는 입력 원문 그대로 사용 (요약 금지)
            4. precedent: matches에 판례가 있으면 첫 번째 판례 번호(precedentId)를 문자열로 사용, 없으면 null
            5. toxicClauses: level이 "danger"인 조항 목록, cautionClauses: level이 "caution"인 조항 목록
            6. 위험 조항이 없으면 해당 배열을 빈 배열로 반환
            7. matches가 없는 조항도 반드시 포함하여 Claude가 법리적으로 자체 판단
            8. 반드시 JSON만 출력, 다른 텍스트 절대 출력 금지

            [출력 형식]
            {
              "toxicClauses": [
                {
                  "level": "danger",
                  "title": "...",
                  "originalText": "...",
                  "legalIssue": "...",
                  "precedent": "대법원 2019다12345",
                  "suggestion": "..."
                }
              ],
              "cautionClauses": [
                {
                  "level": "caution",
                  "title": "...",
                  "originalText": "...",
                  "legalIssue": "...",
                  "precedent": null,
                  "suggestion": "..."
                }
              ]
            }
            """;

    private final ClaudeClient claudeClient;
    private final PrecedentService precedentService;
    private final ObjectMapper objectMapper;

    public ContractAnalysisService(ClaudeClient claudeClient, PrecedentService precedentService,
            ObjectMapper objectMapper) {
        this.claudeClient = claudeClient;
        this.precedentService = precedentService;
        this.objectMapper = objectMapper;
    }

    public Mono<ContractAnalysisResult> analyze(String documentId) {
        return analyzeRaw(documentId).map(this::prefixPrecedents);
    }

    public Mono<ContractAnalysisResult> analyzeRaw(String documentId) {
        return precedentService.search(documentId)
            .flatMap(results -> {
                if (results.isEmpty()) {
                    return Mono.just(ContractAnalysisResult.builder()
                        .toxicClauses(List.of())
                        .cautionClauses(List.of())
                        .build());
                }
                String prompt = buildPrompt(results);
                return claudeClient.generate(prompt).map(this::parseResult);
            });
    }

    private String buildPrompt(List<PrecedentSearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            PrecedentSearchResult result = results.get(i);
            sb.append("조항 ").append(i + 1).append(":\n");
            sb.append("- level: ").append(result.getLevel()).append("\n");
            sb.append("- originalText: ").append(result.getOriginalText()).append("\n");
            sb.append("- matches: ");
            if (result.getMatches() == null || result.getMatches().isEmpty()) {
                sb.append("없음\n");
            } else {
                sb.append("\n");
                for (PrecedentSearchResult.PrecedentMatch match : result.getMatches()) {
                    sb.append("  * ").append(match.getPrecedentId())
                        .append(" (유사도: ").append(match.getScore()).append(")")
                        .append(" - ").append(match.getSummary()).append("\n");
                }
            }
            sb.append("\n");
        }
        return ANALYSIS_PROMPT_TEMPLATE.replace("{{CLAUSES}}", sb.toString());
    }

    private ContractAnalysisResult prefixPrecedents(ContractAnalysisResult result) {
        Stream.concat(result.getToxicClauses().stream(), result.getCautionClauses().stream())
            .filter(clause -> clause.getPrecedent() != null)
            .forEach(clause -> clause.setPrecedent("대법원 " + clause.getPrecedent()));
        return result;
    }

    private ContractAnalysisResult parseResult(String responseText) {
        String json = stripMarkdown(responseText);
        try {
            return objectMapper.readValue(json, ContractAnalysisResult.class);
        } catch (Exception e) {
            log.error("Claude 응답 JSON 파싱 실패");
            throw new HomeProtectException(ErrorCode.AI_PARSE_FAILED, e);
        }
    }

    // Claude가 ```json ... ``` 블록으로 응답할 수 있어 마크다운 래퍼 제거
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
