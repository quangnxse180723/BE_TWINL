package com.twinl.dto.ghn;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GhnCreateOrderItem {
	private String name;
	private Integer quantity;
	private Integer price;
	private Integer weight;
}
