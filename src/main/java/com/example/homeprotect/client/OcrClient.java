package com.example.homeprotect.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.homeprotect.exception.ErrorCode;
import com.example.homeprotect.exception.HomeProtectException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class OcrClient {

    private static final Logger log = LoggerFactory.getLogger(OcrClient.class);

    @Value("${clova.ocr.api-url}")
    private String apiUrl;

    @Value("${clova.ocr.secret-key}")
    private String secretKey;

    private final WebClient webClient;

    public OcrClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone().build();
    }

    public Mono<String> extractText(FilePart filePart, String format) {
        String message = buildOcrMessage(normalizeFormat(format));
        return DataBufferUtils.join(filePart.content())
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return callOcrApi(bytes, message, filePart.filename());
                });
    }

    private Mono<String> callOcrApi(byte[] bytes, String message, String filename) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("message", message);
        bodyBuilder.part("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        return webClient.post()
                .uri(apiUrl)
                .header("X-OCR-SECRET", secretKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseOcrText)
                .onErrorMap(e -> !(e instanceof HomeProtectException), e -> {
                    log.error("CLOVA OCR API 호출 실패: {}", e.getMessage());
                    return new HomeProtectException(ErrorCode.OCR_FAILED, e);
                });
    }

    private String parseOcrText(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode image : root.path("images")) {
            for (JsonNode field : image.path("fields")) {
                sb.append(field.path("inferText").asText()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String buildOcrMessage(String format) {
        return "{\"version\":\"V2\",\"requestId\":\"" + UUID.randomUUID()
                + "\",\"timestamp\":0,\"images\":[{\"format\":\"" + format + "\",\"name\":\"document\"}]}";
    }

    // CLOVA OCR API는 jpeg 대신 jpg 포맷 명칭을 사용
    private String normalizeFormat(String extension) {
        return "jpeg".equalsIgnoreCase(extension) ? "jpg" : extension.toLowerCase();
    }
}
