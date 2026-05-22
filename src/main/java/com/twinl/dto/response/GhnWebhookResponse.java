package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GhnWebhookResponse {
	private int code;
	private String message;
}
