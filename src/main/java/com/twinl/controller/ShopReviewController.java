package com.twinl.controller;

import com.twinl.dto.request.ShopReviewRequest;
import com.twinl.dto.response.ShopReviewResponse;
import com.twinl.service.ShopReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers/{sellerId}/reviews")
public class ShopReviewController {

    private final ShopReviewService shopReviewService;

    public ShopReviewController(ShopReviewService shopReviewService) {
        this.shopReviewService = shopReviewService;
    }

    @GetMapping
    public ResponseEntity<Page<ShopReviewResponse>> getShopReviews(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int sizePage
    ) {
        return ResponseEntity.ok(shopReviewService.getShopReviews(sellerId, PageRequest.of(page, sizePage)));
    }

    @PostMapping
    public ResponseEntity<ShopReviewResponse> addReview(
            @PathVariable Long sellerId,
            @Valid @RequestBody ShopReviewRequest request
    ) {
        return ResponseEntity.ok(shopReviewService.addReview(sellerId, request));
    }
}
