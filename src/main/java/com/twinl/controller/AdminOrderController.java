package com.twinl.controller;

import com.twinl.dto.response.OrderResponse;
import com.twinl.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
	private final OrderService orderService;

	public AdminOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public ResponseEntity<Page<OrderResponse>> getOrders(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int sizePage
	) {
		return ResponseEntity.ok(orderService.getOrders(page, sizePage));
	}
}
