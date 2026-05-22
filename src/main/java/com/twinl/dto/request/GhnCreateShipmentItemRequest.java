package com.twinl.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnCreateShipmentItemRequest {
	@NotBlank
	private String name;

	@NotNull
	@Min(1)
	private Integer quantity;

	@NotNull
	@Min(0)
	private Integer price;

	@NotNull
	@Min(1)
	private Integer weight;
}
