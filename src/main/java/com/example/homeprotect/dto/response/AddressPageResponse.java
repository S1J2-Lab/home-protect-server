package com.example.homeprotect.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressPageResponse {

    private List<AddressResponse> results;
    private int totalCount;
    private int currentPage;
    private int countPerPage;
    private boolean hasMore;
}
