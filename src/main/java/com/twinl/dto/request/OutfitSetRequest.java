package com.twinl.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutfitSetRequest {
	@NotBlank
	@Size(max = 200)
	private String name;

	@Size(max = 2000)
	private String description;

	@Size(max = 500)
	private String coverImageUrl;

	@Size(max = 100)
	private String styleTag;

	@Min(0) @Max(100)
	private Integer discountTwoItems;

	@Min(0) @Max(100)
	private Integer discountThresholdLow;

	@Min(0) @Max(100)
	private Integer discountThresholdHigh;

	private Boolean active;

	@NotNull
	@Size(min = 1, max = 20)
	private List<OutfitSetItemRequest> items;

	@Getter
	@Setter
	public static class OutfitSetItemRequest {
		@NotNull
		private Long productId;

		@Size(max = 50)
		private String role;

		private Integer displayOrder;
	}
}
