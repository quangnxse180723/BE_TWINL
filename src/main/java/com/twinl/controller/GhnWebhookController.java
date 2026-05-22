package com.twinl.controller;

import com.twinl.dto.request.GhnWebhookRequest;
import com.twinl.dto.response.GhnWebhookResponse;
import com.twinl.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments/ghn")
public class GhnWebhookController {
	private final ShippingService shippingService;

	public GhnWebhookController(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@PostMapping("/webhook")
	public ResponseEntity<GhnWebhookResponse> handleWebhook(
			@RequestBody GhnWebhookRequest request,
			@RequestHeader(value = "Token", required = false) String tokenHeader
	) {
		return ResponseEntity.ok(shippingService.handleGhnWebhook(request, tokenHeader));
	}
}
