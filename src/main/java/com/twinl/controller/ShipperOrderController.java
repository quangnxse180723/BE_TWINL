package com.twinl.controller;

import com.twinl.dto.request.AssignShipperRequest;
import com.twinl.dto.request.UpdateOrderStatusRequest;
import com.twinl.dto.response.OrderResponse;
import com.twinl.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShipperOrderController {

	private final OrderService orderService;

	public ShipperOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * [Admin/Staff] Gán Shipper vào đơn hàng.
	 * POST /api/v1/orders/{orderId}/assign
	 */
	@PostMapping("/api/v1/orders/{orderId}/assign")
	public ResponseEntity<OrderResponse> assignOrderToShipper(
			@PathVariable Long orderId,
			@Valid @RequestBody AssignShipperRequest request
	) {
		return ResponseEntity.ok(orderService.assignOrderToShipper(orderId, request.getShipperId()));
	}

	/**
	 * [Shipper] Cập nhật trạng thái đơn hàng (PICKED_UP / DELIVERED).
	 * PUT /api/v1/shipper/orders/{orderId}/status
	 */
	@PutMapping("/api/v1/shipper/orders/{orderId}/status")
	public ResponseEntity<OrderResponse> updateOrderStatus(
			@PathVariable Long orderId,
			@Valid @RequestBody UpdateOrderStatusRequest request,
			Authentication authentication
	) {
		String shipperUsername = authentication.getName();
		return ResponseEntity.ok(
				orderService.updateOrderStatusByShipper(orderId, request.getStatus(), request.getNote(), shipperUsername)
		);
	}

	/**
	 * [Shipper] Xem danh sách đơn hàng được gán cho mình.
	 * GET /api/v1/shipper/orders
	 */
	@GetMapping("/api/v1/shipper/orders")
	public ResponseEntity<Page<OrderResponse>> getMyShipperOrders(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int sizePage,
			Authentication authentication
	) {
		String shipperUsername = authentication.getName();
		return ResponseEntity.ok(orderService.getMyShipperOrders(page, sizePage, shipperUsername));
	}
}
