package com.twinl.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnCreateShipmentRequest {
	@NotBlank
	private String toName;

	@NotBlank
	private String toPhone;

	@NotBlank
	private String toAddress;

	@NotBlank
	private String toWardCode;

	@NotNull
	private Integer toDistrictId;

	@NotNull
	private Integer toProvinceId;

	@NotNull
	@Min(0)
	private Integer codAmount;

	@NotNull
	@Min(1)
	private Integer weight;

	@NotNull
	@Min(1)
	private Integer length;

	@NotNull
	@Min(1)
	private Integer width;

	@NotNull
	@Min(1)
	private Integer height;

	private String note;

	private String requiredNote;

	@NotEmpty
	@Valid
	private List<GhnCreateShipmentItemRequest> items;
}
