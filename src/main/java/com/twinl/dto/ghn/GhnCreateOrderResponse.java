package com.twinl.dto.ghn;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class GhnCreateOrderResponse {
	private Integer code;
	private String message;
	private GhnCreateOrderResponseData data;

	@Getter
	@Setter
	public static class GhnCreateOrderResponseData {
		@JsonProperty("order_code")
		private String orderCode;

		@JsonProperty("total_fee")
		private Integer totalFee;
	}
}
