package com.example.homeprotect.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // OCR 관련
    PII_DETECTED(400, "개인정보가 감지되었습니다. 가린 뒤 다시 업로드해주세요."),
    OCR_FAILED(422, "이미지를 판독할 수 없어요. 더 선명한 사진으로 다시 업로드해주세요."),
    INVALID_FILE_TYPE(400, "지원하지 않는 파일 형식이에요. (jpg, jpeg, png, pdf)"),

    // 분석 관련
    OWNER_NOT_VERIFIED(400, "등기부상 소유자와 계약서상 임대인이 동일한지 확인 후 체크해주세요."),
    API_UNAVAILABLE(503, "공공데이터 서버가 일시적으로 응답하지 않아요. 잠시 후 다시 시도해주세요."),
    ANALYSIS_TIMEOUT(504, "분석 시간이 너무 오래 걸리고 있어요. 다시 시도해주세요."),
    INVALID_ADDRESS(400, "입력하신 주소로 건축물대장을 조회할 수 없어요. 주소를 다시 확인해주세요."),
    AI_PARSE_FAILED(502, "AI 분석 중 오류가 발생했어요. 다시 시도해주세요."),
    INVALID_CONTRACT_TYPE(400, "계약 유형이 올바르지 않아요. (jeonse / half_jeonse / monthly)"),

    // 세션 관련
    SESSION_EXPIRED(404, "분석 결과가 만료되었어요. (30분 초과) 처음부터 다시 분석해주세요."),
    SESSION_NOT_FOUND(404, "세션 정보를 찾을 수 없어요. 처음부터 다시 시도해주세요."),

    // AI 클라이언트
    CLAUDE_CALL_FAILED(502, "Claude API 호출에 실패했습니다."),
    GEMINI_CALL_FAILED(502, "Gemini API 호출에 실패했습니다."),

    // 인프라
    VECTOR_DB_FAILED(500, "판례 검색에 실패했습니다."),

    // 공통
    INTERNAL_ERROR(500, "예기치 못한 오류가 발생했습니다.");

    private final int status;
    private final String message;

}
