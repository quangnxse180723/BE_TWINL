package com.twinl.service.impl;

import com.twinl.dto.request.ShopReviewRequest;
import com.twinl.dto.response.ShopReviewResponse;
import com.twinl.entity.ShopReview;
import com.twinl.entity.User;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.ShopReviewRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.ShopReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ShopReviewServiceImpl implements ShopReviewService {

    private final ShopReviewRepository shopReviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ShopReviewServiceImpl(ShopReviewRepository shopReviewRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.shopReviewRepository = shopReviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<ShopReviewResponse> getShopReviews(Long shopId, Pageable pageable) {
        return shopReviewRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable)
                .map(this::toResponse);
    }

    @Override
    public ShopReviewResponse addReview(Long shopId, ShopReviewRequest request) {
        User currentUser = getCurrentUser();
        
        if (currentUser.getId().equals(shopId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot review your own shop");
        }

        User shop = userRepository.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));

        long deliveredOrders = orderRepository.countDeliveredOrdersByUserIdAndSellerId(currentUser.getId(), shopId);
        if (deliveredOrders == 0) {
            throw new RuntimeException("You must have successfully purchased from this shop to review it");
        }

        ShopReview review = shopReviewRepository.findByShopIdAndReviewerId(shopId, currentUser.getId())
                .orElse(ShopReview.builder()
                        .shop(shop)
                        .reviewer(currentUser)
                        .build());

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        ShopReview saved = shopReviewRepository.save(review);
        return toResponse(saved);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private ShopReviewResponse toResponse(ShopReview review) {
        return ShopReviewResponse.builder()
                .id(review.getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getDisplayName())
                .reviewerAvatarUrl(review.getReviewer().getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt() : LocalDateTime.now())
                .build();
    }
}
