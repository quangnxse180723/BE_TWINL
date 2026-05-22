package com.twinl.service;

import com.twinl.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {
	Page<OrderResponse> getOrders(int page, int sizePage);
	Page<OrderResponse> getMyOrders(int page, int sizePage);
	OrderResponse getMyOrderByCode(String code);
}
