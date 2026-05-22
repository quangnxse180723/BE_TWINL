package com.twinl.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnWebhookRequest {
	@JsonProperty("order_code")
	private String orderCode;

	private String status;

	@JsonProperty("status_id")
	private Integer statusId;

	@JsonProperty("shop_id")
	private Integer shopId;

	@JsonProperty("updated_date")
	private String updatedDate;
}
