package com.twinl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OutfitSetResponse {
	private Long id;
	private String name;
	private String description;
	private String coverImageUrl;
	private String styleTag;
	private Integer discountTwoItems;
	private Integer discountThresholdLow;
	private Integer discountThresholdHigh;
	private BigDecimal discountPriceThreshold;
	private Boolean active;
	private int itemCount;
	private BigDecimal totalPrice;
	private LocalDateTime createdAt;
	private List<OutfitSetItemResponse> items;

	@Getter
	@Builder
	public static class OutfitSetItemResponse {
		private Long id;
		private Long productId;
		private String productName;
		private String productBrand;
		private BigDecimal productPrice;
		private String productImageUrl;
		private String productStatus;
		private Integer productStock;
		private String role;
		private Integer displayOrder;
	}
}
