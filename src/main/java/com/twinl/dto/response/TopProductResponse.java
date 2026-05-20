package com.twinl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopProductResponse {
	private Long productId;
	private String productName;
	private Long totalSold;
}
