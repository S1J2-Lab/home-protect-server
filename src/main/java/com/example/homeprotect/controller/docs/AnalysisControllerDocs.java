package com.example.homeprotect.controller.docs;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.homeprotect.dto.request.AnalysisRunRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Analysis", description = "전세 안전 분석 실행 및 결과 조회 API")
public interface AnalysisControllerDocs {

    @Operation(
        summary = "분석 실행",
        description = """
            등기부등본·임대차계약서 OCR 세션과 소유자 확인 여부를 받아 분석을 백그라운드로 시작합니다.
            응답은 즉시 processing으로 반환되며, 진행 상황은 /analysis/stream SSE로 수신합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "분석 시작 성공",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "data": {
                    "sessionId": "uuid-xxxx",
                    "analysisStatus": "processing"
                  }
                }
                """)))
    @ApiResponse(responseCode = "400", description = "소유자 미확인",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 400,
                  "code": "OWNER_NOT_VERIFIED",
                  "message": "등기부상 소유자와 계약서상 임대인이 동일한지 확인 후 체크해주세요."
                }
                """)))
    @ApiResponse(responseCode = "404", description = "세션 만료",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 404,
                  "code": "SESSION_EXPIRED",
                  "message": "분석 결과가 만료되었어요. (30분 초과) 처음부터 다시 분석해주세요."
                }
                """)))
    Mono<ResponseEntity<Map<String, Object>>> runAnalysis(
        @RequestBody AnalysisRunRequest request);

    @Operation(
        summary = "분석 진행 상황 스트리밍 (SSE)",
        description = """
            분석 각 단계(jeonseRatio, registryParse, contractReview, buildingCheck)의 완료 여부를
            Server-Sent Events로 실시간 수신합니다.
            재연결 시 이미 완료된 단계는 즉시 재전송됩니다.

            이벤트 형식:
            - 단계 완료: {"step": "jeonseRatio", "status": "done"}
            - 전체 완료: {"step": "complete"}
            - 에러 발생: {"step": "error", "errorCode": "API_UNAVAILABLE"}
            """
    )
    @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공",
        content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
            examples = @ExampleObject(value = """
                    data: {"step": "jeonseRatio", "status": "done"}

                    data: {"step": "registryParse", "status": "done"}

                    data: {"step": "contractReview", "status": "done"}

                    data: {"step": "buildingCheck", "status": "done"}

                    data: {"step": "complete"}
                    """)))
    Flux<ServerSentEvent<String>> streamAnalysisStatus(
        @Parameter(description = "분석 세션 ID (/analysis/init 응답값)", required = true)
        @RequestParam String sessionId);

    @Operation(
        summary = "분석 결과 조회",
        description = "SSE에서 complete 이벤트를 수신한 후 호출하여 전체 분석 결과를 가져옵니다."
    )
    @ApiResponse(responseCode = "200", description = "결과 조회 성공",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "data": {
                    "address": "서울특별시 강남구 테헤란로 123",
                    "analyzedAt": "2026-05-07T14:30:00+09:00",
                    "jeonseRatio": {
                      "ratioType": "jeonse",
                      "ratioPercent": 72.5,
                      "riskLevel": "caution",
                      "recentHigh": 450000000,
                      "recentLow": 320000000,
                      "average": 385000000,
                      "convertedDeposit": 380000000,
                      "sampleCount": 8,
                      "lowReliability": false
                    },
                    "registry": {
                      "mortgageCount": 1,
                      "mortgages": [
                        { "bank": "국민은행", "amount": 200000000 }
                      ],
                      "totalMortgage": 200000000,
                      "trustWarning": false,
                      "priorLease": false,
                      "ownershipChangeRecent": false
                    },
                    "building": {
                      "level": "safe",
                      "primaryUse": "아파트",
                      "isResidential": true,
                      "violation": false,
                      "approvedDate": "2010-03-15",
                      "redevelopmentZone": false
                    },
                    "contract": {
                      "toxicClauses": [
                        {
                          "level": "danger",
                          "title": "임의 해지 조항",
                          "originalText": "임대인은 사전 통보 없이 계약을 해지할 수 있다.",
                          "legalIssue": "임차인의 주거 안정권을 침해하는 일방적 조항입니다.",
                          "precedent": "대법원 2019다12345",
                          "suggestion": "해당 조항 삭제 또는 임차인 동의 요건 추가를 요구하세요."
                        }
                      ],
                      "cautionClauses": []
                    }
                  }
                }
                """)))
    @ApiResponse(responseCode = "404", description = "세션 만료 또는 분석 미완료",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": 404,
                  "code": "SESSION_EXPIRED",
                  "message": "분석 결과가 만료되었어요. (30분 초과) 처음부터 다시 분석해주세요."
                }
                """)))
    Mono<ResponseEntity<Map<String, Object>>> getAnalysisResult(
        @Parameter(description = "분석 세션 ID (/analysis/init 응답값)", required = true)
        @RequestParam String sessionId);
}
