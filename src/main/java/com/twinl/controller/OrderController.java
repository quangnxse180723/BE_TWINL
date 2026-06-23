package com.twinl.controller;

import com.twinl.dto.response.OrderResponse;
import com.twinl.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public ResponseEntity<Page<OrderResponse>> getMyOrders(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int sizePage,
			@RequestParam(required = false) String status
	) {
		return ResponseEntity.ok(orderService.getMyOrders(page, sizePage, status));
	}

	@GetMapping("/{code}")
	public ResponseEntity<OrderResponse> getOrderByCode(@PathVariable String code) {
		return ResponseEntity.ok(orderService.getMyOrderByCode(code));
	}

	@org.springframework.web.bind.annotation.PostMapping("/{id}/confirm-receipt")
	public ResponseEntity<OrderResponse> confirmReceipt(@PathVariable Long id) {
		return ResponseEntity.ok(orderService.confirmReceipt(id));
	}

	@org.springframework.web.bind.annotation.PostMapping("/{id}/report-missing")
	public ResponseEntity<OrderResponse> reportMissing(
			@PathVariable Long id,
			@org.springframework.web.bind.annotation.RequestBody(required = false) java.util.Map<String, String> body
	) {
		String reason = body != null ? body.get("reason") : null;
		return ResponseEntity.ok(orderService.reportMissing(id, reason));
	}
}
