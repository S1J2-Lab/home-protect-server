package com.example.homeprotect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.homeprotect.client.GeminiClient;
import com.example.homeprotect.dto.response.RegistryAnalysisResult;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private static final String REGISTRY_PROMPT_TEMPLATE = """
            당신은 한국 등기부등본 문서를 분석하는 전문가입니다.
            아래에 등기부등본 OCR 원문 텍스트가 주어집니다.
            이 텍스트를 분석하여 다음 항목을 추출하고, 반드시 아래 JSON 형식으로만 응답하세요.
            설명, 서론, 주석 등 JSON 외의 어떤 텍스트도 출력하지 마세요.

            [분석 기준]
            1. mortgageCount: 근저당권 설정 건수 (없으면 0)
            2. mortgages: 근저당 목록. 각 항목은 bank(채권자/은행명)와 amount(채권최고액, 숫자만)
            3. totalMortgage: mortgages의 amount 합산
            4. trustWarning: "신탁" 키워드가 등기 목적 또는 원인에 포함되면 true
            5. priorLease: "선순위" 또는 "선순위임차인" 키워드가 포함되면 true
            6. ownershipChangeRecent: 소유권 이전 등기가 최근 2년 이내(현재 기준)에 발생했으면 true

            [출력 형식]
            {
              "registry": {
                "mortgageCount": 0,
                "mortgages": [],
                "totalMortgage": 0,
                "trustWarning": false,
                "priorLease": false,
                "ownershipChangeRecent": false
              }
            }

            [등기부등본 OCR 원문]
            {{RAW_TEXT}}
            """;

    private final GeminiClient geminiClient;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public RegistryService(GeminiClient geminiClient, RedisUtil redisUtil, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
    }

    public Mono<RegistryAnalysisResult> analyze(String documentId) {
        return redisUtil.getOcrSession(documentId)
            .flatMap(session -> {
                String prompt = REGISTRY_PROMPT_TEMPLATE.replace("{{RAW_TEXT}}", session.getRawText());
                return geminiClient.generate(prompt);
            })
            .map(this::parseResult);
    }

    private RegistryAnalysisResult parseResult(String responseText) {
        String json = stripMarkdown(responseText);
        try {
            return objectMapper.readValue(json, RegistryAnalysisResult.class);
        } catch (Exception e) {
            log.error("Gemini 응답 JSON 파싱 실패: {}", json);
            throw new HomeProtectException(ErrorCode.AI_PARSE_FAILED, e);
        }
    }

    // Gemini가 ```json ... ``` 블록으로 응답할 수 있어 마크다운 래퍼 제거
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
