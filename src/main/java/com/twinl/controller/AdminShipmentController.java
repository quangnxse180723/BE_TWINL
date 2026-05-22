package com.twinl.controller;

import com.twinl.dto.request.GhnCreateShipmentRequest;
import com.twinl.dto.response.ShipmentResponse;
import com.twinl.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shipments")
public class AdminShipmentController {
	private final ShippingService shippingService;

	public AdminShipmentController(ShippingService shippingService) {
		this.shippingService = shippingService;
	}

	@PostMapping("/ghn/create/{orderId}")
	public ResponseEntity<ShipmentResponse> createGhnShipment(
			@PathVariable Long orderId,
			@Valid @RequestBody GhnCreateShipmentRequest request
	) {
		return ResponseEntity.ok(shippingService.createGhnShipment(orderId, request));
	}
}
