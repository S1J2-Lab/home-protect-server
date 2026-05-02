package com.example.homeprotect.controller.docs;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "Address", description = "행안부 도로명주소 검색 API (자동완성용)")
public interface AddressControllerDocs {

    @Operation(
            summary = "주소 검색",
            description = "행안부 도로명주소 API 프록시. 사용자가 선택한 주소의 admCd, rnMgtSn을 함께 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "검색 성공",
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "status": "success",
                      "data": {
                        "results": [
                          {
                            "roadAddress": "서울특별시 강남구 테헤란로 123 (역삼동)",
                            "jibunAddress": "서울특별시 강남구 역삼동 648-23 여삼빌딩",
                            "buildingName": "여삼빌딩",
                            "admCd": "1168010100",
                            "rnMgtSn": "116803122010"
                          }
                        ]
                      }
                    }
                    """)))
    @ApiResponse(responseCode = "400", description = "주소를 찾을 수 없거나 API 오류",
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "code": "INVALID_ADDRESS",
                      "message": "입력하신 주소로 건축물대장을 조회할 수 없어요. 주소를 다시 확인해주세요."
                    }
                    """)))
    Mono<ResponseEntity<Map<String, Object>>> searchAddress(
            @Parameter(description = "검색할 주소 키워드", example = "테헤란로 123")
            @RequestParam String query);
}
