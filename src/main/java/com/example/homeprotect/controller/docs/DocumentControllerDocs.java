package com.example.homeprotect.controller.docs;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestPart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "Document", description = "문서 OCR 및 개인정보 감지 API")
public interface DocumentControllerDocs {

    @Operation(
        summary = "등기부등본 OCR 스캔",
        description = "등기부등본 이미지를 업로드하면 OCR 추출 후 개인정보 감지 결과를 반환합니다.",
        requestBody = @RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
        )
    )
    @ApiResponse(responseCode = "200", description = "OCR 성공",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "data": {
                    "registrySessionId": "550e8400-e29b-41d4-a716-446655440000",
                    "safe": true
                  }
                }
                """)))
    @ApiResponse(responseCode = "400", description = "요청 오류 (PII 감지 또는 지원하지 않는 파일 형식)",
        content = @Content(examples = {
            @ExampleObject(name = "PII_DETECTED", value = """
                {
                  "status": "error",
                  "error": {
                    "code": "PII_DETECTED",
                    "message": "개인정보가 감지되었습니다. 가린 뒤 다시 업로드해주세요.",
                    "field": "file",
                    "nonMasked": ["주민등록번호", "전화번호"]
                  }
                }
                """),
            @ExampleObject(name = "INVALID_FILE_TYPE", value = """
                {
                  "status": "error",
                  "error": {
                    "code": "INVALID_FILE_TYPE",
                    "message": "지원하지 않는 파일 형식이에요. (jpg, jpeg, png, pdf)",
                    "field": "file"
                  }
                }
                """)
        }))
    @ApiResponse(responseCode = "422", description = "E-3: OCR 판독 실패",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": "error",
                  "error": {
                    "code": "OCR_FAILED",
                    "message": "이미지를 판독할 수 없어요. 더 선명한 사진으로 다시 업로드해주세요.",
                    "field": "file"
                  }
                }
                """)))
    Mono<ResponseEntity<Map<String, Object>>> scanRegistry(
        @Parameter(description = "업로드할 등기부등본 파일 (jpg, jpeg, png, pdf)", required = true)
        @RequestPart("file") FilePart file);

    @Operation(
        summary = "임대차계약서 OCR 스캔",
        description = "임대차계약서 이미지를 업로드하면 OCR 추출 후 개인정보 감지 결과를 반환합니다.",
        requestBody = @RequestBody(
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
        )
    )
    @ApiResponse(responseCode = "200", description = "OCR 성공",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": "success",
                  "data": {
                    "contractSessionId": "550e8400-e29b-41d4-a716-446655440000",
                    "safe": true
                  }
                }
                """)))
    @ApiResponse(responseCode = "400", description = "요청 오류 (PII 감지 또는 지원하지 않는 파일 형식)",
        content = @Content(examples = {
            @ExampleObject(name = "PII_DETECTED", value = """
                {
                  "status": "error",
                  "error": {
                    "code": "PII_DETECTED",
                    "message": "개인정보가 감지되었습니다. 가린 뒤 다시 업로드해주세요.",
                    "field": "file",
                    "nonMasked": ["주민등록번호", "전화번호"]
                  }
                }
                """),
            @ExampleObject(name = "INVALID_FILE_TYPE", value = """
                {
                  "status": "error",
                  "error": {
                    "code": "INVALID_FILE_TYPE",
                    "message": "지원하지 않는 파일 형식이에요. (jpg, jpeg, png, pdf)",
                    "field": "file"
                  }
                }
                """)
        }))
    @ApiResponse(responseCode = "422", description = "E-3: OCR 판독 실패",
        content = @Content(examples = @ExampleObject(value = """
                {
                  "status": "error",
                  "error": {
                    "code": "OCR_FAILED",
                    "message": "이미지를 판독할 수 없어요. 더 선명한 사진으로 다시 업로드해주세요.",
                    "field": "file"
                  }
                }
                """)))
    Mono<ResponseEntity<Map<String, Object>>> scanContract(
        @Parameter(description = "업로드할 임대차계약서 파일 (jpg, jpeg, png, pdf)", required = true)
        @RequestPart("file") FilePart file);
}
