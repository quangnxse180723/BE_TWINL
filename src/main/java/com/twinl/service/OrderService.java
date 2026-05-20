package com.twinl.service;

import com.twinl.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {
	Page<OrderResponse> getOrders(int page, int sizePage);
}
