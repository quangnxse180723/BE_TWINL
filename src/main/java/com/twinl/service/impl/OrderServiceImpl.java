package com.twinl.service.impl;

import com.twinl.dto.response.OrderItemResponse;
import com.twinl.dto.response.OrderResponse;
import com.twinl.entity.Order;
import com.twinl.repository.OrderRepository;
import com.twinl.service.OrderService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
	private final OrderRepository orderRepository;

	public OrderServiceImpl(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Override
	public Page<OrderResponse> getOrders(int page, int sizePage) {
		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return orderRepository.findAll(pageable).map(this::toResponse);
	}

	private OrderResponse toResponse(Order order) {
		Set<OrderItemResponse> items = order.getItems().stream()
				.map(item -> OrderItemResponse.builder()
						.productId(item.getProduct() != null ? item.getProduct().getId() : null)
						.productName(item.getProduct() != null ? item.getProduct().getName() : null)
						.quantity(item.getQuantity())
						.unitPrice(item.getUnitPrice())
						.lineTotal(item.getLineTotal())
						.build())
				.collect(Collectors.toSet());

		return OrderResponse.builder()
				.id(order.getId())
				.code(order.getCode())
				.customerName(order.getCustomerName())
				.customerEmail(order.getCustomerEmail())
				.customerPhone(order.getCustomerPhone())
				.shippingAddress(order.getShippingAddress())
				.status(order.getStatus())
				.totalAmount(order.getTotalAmount())
				.items(items)
				.createdAt(order.getCreatedAt())
				.updatedAt(order.getUpdatedAt())
				.build();
	}
}
