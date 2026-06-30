package com.twinl.service;

import com.twinl.dto.request.ShopReviewRequest;
import com.twinl.dto.response.ShopReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShopReviewService {
    Page<ShopReviewResponse> getShopReviews(Long shopId, Pageable pageable);
    ShopReviewResponse addReview(Long shopId, ShopReviewRequest request);
}
