package com.twinl.service;

import com.twinl.dto.response.OrderResponse;
import com.twinl.entity.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {
	Page<OrderResponse> getOrders(int page, int sizePage);
	Page<OrderResponse> getMyOrders(int page, int sizePage);
	OrderResponse getMyOrderByCode(String code);
	Page<OrderResponse> getOrdersBySeller(String username, int page, int sizePage);

	/**
	 * Admin/Staff: Gán Shipper vào đơn hàng.
	 * Kiểm tra User được gán phải có role SHIPPER.
	 * Đổi trạng thái đơn thành ASSIGNED.
	 */
	OrderResponse assignOrderToShipper(Long orderId, Long shipperId);

	/**
	 * Shipper: Cập nhật trạng thái đơn hàng (PICKED_UP, DELIVERED).
	 * Chỉ Shipper được gán vào đơn mới có quyền cập nhật.
	 * Nếu status = DELIVERED: set deliveredAt và log mock Escrow 48h.
	 */
	OrderResponse updateOrderStatusByShipper(Long orderId, OrderStatus status, String note, String shipperUsername);

	/**
	 * Shipper: Xem danh sách đơn hàng được gán cho mình.
	 */
	Page<OrderResponse> getMyShipperOrders(int page, int sizePage, String shipperUsername);
}
