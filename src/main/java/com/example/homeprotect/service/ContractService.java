package com.example.homeprotect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.homeprotect.client.ClaudeClient;
import com.example.homeprotect.dto.response.ContractClauseResult;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private static final String CONTRACT_PROMPT_TEMPLATE = """
            당신은 한국 임대차계약서를 분석하는 전문가입니다.
            아래 임대차계약서 OCR 원문을 읽고, 모든 조항을 분리하여 추출하세요.

            [추출 규칙]
            1. 제N조 형태의 조항은 각각 별도 항목으로 분리
            2. 특약사항 내 항목은 ①②③, 1.2.3., 가.나.다. 등
               어떤 형식이든 번호/기호별로 각각 별도 항목으로 분리
            3. 【주의사항】【확인사항】등 별도 섹션 제목이 있으면
               section에 해당 제목 그대로 표기하고 동일하게 분리
            4. 조항 번호나 섹션 구분 없이 나오는 일반 문단도
               section을 "기타"로 표기하고 문장 단위로 분리하여 포함
            5. originalText는 원문 문장 최대한 그대로 보존 (요약 금지)
            6. 한 조항이 여러 문장이어도 나누지 말고 조항 전체를 하나로
            7. 조항 제목은 title 필드로 분리 (없으면 null)
            8. 금액 정보가 주목적인 표(보증금, 계약금, 중도금 등)는 제외
            9. index는 문서 순서대로 1부터 부여
            10. 반드시 JSON만 출력, 다른 텍스트 절대 출력 금지

            [출력 형식]
            {
              "clauses": [
                {
                  "index": 1,
                  "section": "제2조",
                  "title": "존속기간",
                  "originalText": "..."
                }
              ]
            }

            [임대차계약서 OCR 원문]
            {{RAW_TEXT}}
            """;

    private final ClaudeClient claudeClient;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public ContractService(ClaudeClient claudeClient, RedisUtil redisUtil, ObjectMapper objectMapper) {
        this.claudeClient = claudeClient;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
    }

    public Mono<ContractClauseResult> analyze(String documentId) {
        return redisUtil.getOcrSession(documentId)
            .flatMap(session -> {
                String rawText = session.getRawText();
                if (rawText == null || rawText.isBlank()) {
                    return Mono.error(new HomeProtectException(ErrorCode.OCR_FAILED));
                }
                String prompt = CONTRACT_PROMPT_TEMPLATE.replace("{{RAW_TEXT}}", rawText);
                return claudeClient.generate(prompt);
            })
            .map(this::parseResult);
    }

    private ContractClauseResult parseResult(String responseText) {
        String json = stripMarkdown(responseText);
        try {
            return objectMapper.readValue(json, ContractClauseResult.class);
        } catch (Exception e) {
            log.error("Claude 응답 JSON 파싱 실패 (앞 100자): {}",
                json.length() > 100 ? json.substring(0, 100) + "..." : json);
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
