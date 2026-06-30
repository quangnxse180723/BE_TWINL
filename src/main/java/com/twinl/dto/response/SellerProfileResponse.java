package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerProfileResponse {
    private Long id;
    private String displayName;
    private String avatarUrl;
    private long productCount;
    private long soldCount;
    private Double averageRating;
    private long reviewCount;
}
