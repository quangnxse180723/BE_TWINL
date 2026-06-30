package com.twinl.repository;

import com.twinl.entity.ShopReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    Page<ShopReview> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    Optional<ShopReview> findByShopIdAndReviewerId(Long shopId, Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM ShopReview r WHERE r.shop.id = :shopId")
    Double getAverageRatingByShopId(@Param("shopId") Long shopId);

    long countByShopId(Long shopId);
}
