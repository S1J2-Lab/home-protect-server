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
      summary = "문서 OCR 스캔",
      description = "등기부등본 또는 임대차계약서 이미지를 업로드하면 OCR 추출 후 개인정보 감지 결과를 반환합니다.",
      requestBody = @RequestBody(
          content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
      )
  )
  @ApiResponse(responseCode = "200", description = "OCR 성공",
      content = @Content(examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "data": {
                        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                        "safe": true
                      }
                    }
                    """)))
  @ApiResponse(responseCode = "400", description = "E-1: 개인정보(PII) 감지",
      content = @Content(examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "error": {
                        "code": "PII_DETECTED",
                        "message": "개인정보가 감지되었습니다. 가린 뒤 다시 업로드해주세요.",
                        "field": "file",
                        "nonMasked": ["주민등록번호", "전화번호"]
                      }
                    }
                    """)))
  @ApiResponse(responseCode = "422", description = "E-2: OCR 판독 실패",
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
  Mono<ResponseEntity<Map<String, Object>>> scanDocument(
      @Parameter(description = "업로드할 문서 파일 (jpg, jpeg, png, pdf)", required = true)
      @RequestPart("file") FilePart file,
      @Parameter(description = "문서 유형: registry(등기부등본) 또는 contract(임대차계약서)", example = "registry", required = true)
      @RequestPart("docType") String docType);
}
