package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShopReviewResponse {
    private Long id;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerAvatarUrl;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
