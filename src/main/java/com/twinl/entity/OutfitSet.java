package com.twinl.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "outfit_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitSet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "cover_image_url", length = 500)
	private String coverImageUrl;

	@Column(name = "style_tag", length = 100)
	private String styleTag;

	// % giảm khi chọn 2 món trong cùng 1 set (mặc định 5)
	@Column(name = "discount_two_items", nullable = false)
	@Builder.Default
	private Integer discountTwoItems = 5;

	// % giảm khi mua nguyên bộ, tính theo tổng giá trị
	// < 500,000 VND → 8%; >= 500,000 VND → 10%
	@Column(name = "discount_threshold_low", nullable = false)
	@Builder.Default
	private Integer discountThresholdLow = 8;

	@Column(name = "discount_threshold_high", nullable = false)
	@Builder.Default
	private Integer discountThresholdHigh = 10;

	@Column(name = "discount_price_threshold", nullable = false, precision = 12, scale = 2)
	@Builder.Default
	private BigDecimal discountPriceThreshold = new BigDecimal("500000");

	@Column(nullable = false)
	@Builder.Default
	private Boolean active = true;

	@OneToMany(mappedBy = "outfitSet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("displayOrder ASC")
	@Builder.Default
	private List<OutfitSetItem> items = new ArrayList<>();

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
