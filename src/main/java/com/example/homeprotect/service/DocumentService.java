package com.example.homeprotect.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import com.example.homeprotect.client.OcrClient;
import com.example.homeprotect.dto.redis.OcrSessionData;
import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.example.homeprotect.exception.PiiDetectedException;
import com.example.homeprotect.util.RedisUtil;

import reactor.core.publisher.Mono;

@Service
public class DocumentService {

    // Gemini(외부 AI) 전송 전 차단 — 패턴 감지 즉시 에러 반환
    private static final Map<Pattern, String> PII_PATTERNS = Map.of(
        Pattern.compile("(?<!\\d)\\d{6}-[1-4]\\d{6}(?!\\d)"),   "주민등록번호",
        Pattern.compile("(?<!\\d)0\\d{1,2}-\\d{3,4}-\\d{4}(?!\\d)"), "전화번호"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "application/pdf"
    );

    private final OcrClient ocrClient;
    private final RedisUtil redisUtil;

    public DocumentService(OcrClient ocrClient, RedisUtil redisUtil) {
        this.ocrClient = ocrClient;
        this.redisUtil = redisUtil;
    }

    public Mono<OcrSessionData> scanDocument(FilePart file, String docType) {
        String contentType = file.headers().getContentType() != null
            ? file.headers().getContentType().toString() : "";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return Mono.error(new HomeProtectException(ErrorCode.INVALID_FILE_TYPE));
        }

        String extension = extractExtension(file.filename()).toLowerCase();
        return ocrClient.extractText(file, extension)
            .flatMap(rawText -> {
                List<String> detected = detectPii(rawText);
                if (!detected.isEmpty()) {
                    return Mono.error(new PiiDetectedException(detected));
                }
                return saveSession(rawText, docType);
            });
    }

    private Mono<OcrSessionData> saveSession(String rawText, String docType) {
        OcrSessionData sessionData = OcrSessionData.builder()
                .sessionId(UUID.randomUUID().toString())
                .safe(true)
                .rawText(rawText)
                .docType(docType)
                .build();
        return redisUtil.saveOcrSession(sessionData).thenReturn(sessionData);
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
    }

    private List<String> detectPii(String text) {
        Set<String> detected = new LinkedHashSet<>();
        for (Map.Entry<Pattern, String> entry : PII_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(text);
            if (matcher.find()) {
                detected.add(entry.getValue()); // 유형명만 추가, 중복 제거
            }
        }
        return new ArrayList<>(detected);
    }

}
