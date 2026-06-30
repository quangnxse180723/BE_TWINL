package com.twinl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import com.twinl.entity.DefectType;

@Getter
@Builder
public class ProductResponse {
	private Long id;
	private Long sellerId;
	private String sellerName;
	private String sellerAvatarUrl;
	private String name;
	private String description;
	private BigDecimal price;
	private Long categoryId;
	private String category;
	private String brand;
	private String gender;
	private List<String> imageUrls;
	private String status;
	private String style;
	private Integer stock;
	private Set<String> sizes;
	private Set<Long> colorIds;
	private Set<String> colors;
	private Integer conditionPercentage;
	private Float length;
	private Float shoulder;
	private Float chest;
	private Float waist;
	private Set<DefectType> defects;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
