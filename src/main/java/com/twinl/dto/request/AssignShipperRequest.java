package com.twinl.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignShipperRequest {

	@NotNull(message = "orderId is required")
	private Long orderId;

	@NotNull(message = "shipperId is required")
	private Long shipperId;
}
