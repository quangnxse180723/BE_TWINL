package com.twinl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
	@NotBlank
	@Size(max = 200)
	private String name;

	@Size(max = 2000)
	private String description;

	@NotNull
	@Positive
	private BigDecimal price;

	@NotNull
	private Long categoryId;

	@NotBlank
	@Size(max = 120)
	private String brand;

	@Size(max = 50)
	private String gender;

	@Size(min = 3, max = 6)
	private List<String> imageUrls;

	@Size(max = 30)
	private String status;

	@Size(max = 120)
	private String style;

	@NotNull
	@PositiveOrZero
	private Integer stock;

	private Set<String> sizes;
	private Set<Long> colorIds;
}
