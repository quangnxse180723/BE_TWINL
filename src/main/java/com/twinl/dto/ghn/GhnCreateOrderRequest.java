package com.twinl.dto.ghn;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GhnCreateOrderRequest {
	@JsonProperty("payment_type_id")
	private Integer paymentTypeId;

	private String note;

	@JsonProperty("required_note")
	private String requiredNote;

	@JsonProperty("return_phone")
	private String returnPhone;

	@JsonProperty("return_address")
	private String returnAddress;

	@JsonProperty("return_district_id")
	private Integer returnDistrictId;

	@JsonProperty("return_ward_code")
	private String returnWardCode;

	@JsonProperty("to_name")
	private String toName;

	@JsonProperty("to_phone")
	private String toPhone;

	@JsonProperty("to_address")
	private String toAddress;

	@JsonProperty("to_ward_code")
	private String toWardCode;

	@JsonProperty("to_district_id")
	private Integer toDistrictId;

	@JsonProperty("to_province_id")
	private Integer toProvinceId;

	@JsonProperty("cod_amount")
	private Integer codAmount;

	private String content;

	private Integer weight;

	private Integer length;

	private Integer width;

	private Integer height;

	@JsonProperty("service_type_id")
	private Integer serviceTypeId;

	private List<GhnCreateOrderItem> items;
}
