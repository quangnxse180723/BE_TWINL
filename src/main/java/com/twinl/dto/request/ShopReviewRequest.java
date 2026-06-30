package com.twinl.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopReviewRequest {
    @Min(1)
    @Max(5)
    private Integer rating;
    private String comment;
}
