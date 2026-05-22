package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VnpayIpnResponse {
	private String rspCode;
	private String message;
}
