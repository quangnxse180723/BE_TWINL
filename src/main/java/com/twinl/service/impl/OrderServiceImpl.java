package com.twinl.service.impl;

import com.twinl.dto.response.OrderItemResponse;
import com.twinl.dto.response.OrderResponse;
import com.twinl.entity.Order;
import com.twinl.entity.User;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.OrderService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderServiceImpl implements OrderService {
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;

	public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository) {
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
	}

	@Override
	public Page<OrderResponse> getOrders(int page, int sizePage) {
		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return orderRepository.findAll(pageable).map(this::toResponse);
	}

	@Override
	public Page<OrderResponse> getMyOrders(int page, int sizePage) {
		User user = getCurrentAuthenticatedUser();
		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
	}

	@Override
	public OrderResponse getMyOrderByCode(String code) {
		User user = getCurrentAuthenticatedUser();
		Order order = orderRepository.findByCodeAndUserId(code, user.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		return toResponse(order);
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
				.paymentMethod(order.getPaymentMethod())
				.paymentStatus(order.getPaymentStatus())
				.shippingProvider(order.getShipment() != null ? order.getShipment().getProvider() : null)
				.shippingStatus(order.getShipment() != null ? order.getShipment().getStatus() : null)
				.trackingCode(order.getShipment() != null ? order.getShipment().getTrackingCode() : null)
				.shippingFee(order.getShipment() != null ? order.getShipment().getShippingFee() : null)
				.items(items)
				.createdAt(order.getCreatedAt())
				.updatedAt(order.getUpdatedAt())
				.build();
	}

	private User getCurrentAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
		}
		String email = authentication.getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}
}
