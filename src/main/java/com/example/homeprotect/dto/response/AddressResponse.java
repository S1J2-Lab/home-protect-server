package com.example.homeprotect.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressResponse {

    private String roadAddress;
    private String jibunAddress;
    private String buildingName;
    private String admCd;
    private String rnMgtSn;
    private String mno;
    private String sno;
}
