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
			@RequestParam(defaultValue = "10") int sizePage
	) {
		return ResponseEntity.ok(orderService.getMyOrders(page, sizePage));
	}

	@GetMapping("/{code}")
	public ResponseEntity<OrderResponse> getOrderByCode(@PathVariable String code) {
		return ResponseEntity.ok(orderService.getMyOrderByCode(code));
	}
}
