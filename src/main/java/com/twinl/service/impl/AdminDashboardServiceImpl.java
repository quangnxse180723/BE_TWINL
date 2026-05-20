package com.twinl.service.impl;

import com.twinl.dto.response.DashboardResponse;
import com.twinl.dto.response.OrderSummaryResponse;
import com.twinl.dto.response.TopProductResponse;
import com.twinl.entity.Order;
import com.twinl.repository.OrderItemRepository;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.AdminDashboardService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public AdminDashboardServiceImpl(
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			ProductRepository productRepository,
			UserRepository userRepository
	) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Override
	public DashboardResponse getDashboard() {
		BigDecimal totalRevenue = orderRepository.sumTotalAmount();
		long totalOrders = orderRepository.count();
		long totalProducts = productRepository.count();
		long totalUsers = userRepository.count();

		List<TopProductResponse> topProducts = orderItemRepository.findTopProducts();
		if (topProducts.size() > 5) {
			topProducts = topProducts.subList(0, 5);
		}

		List<OrderSummaryResponse> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc().stream()
				.map(this::toSummary)
				.collect(Collectors.toList());

		return DashboardResponse.builder()
				.totalRevenue(totalRevenue)
				.totalOrders(totalOrders)
				.totalProducts(totalProducts)
				.totalUsers(totalUsers)
				.topProducts(topProducts)
				.recentOrders(recentOrders)
				.build();
	}

	private OrderSummaryResponse toSummary(Order order) {
		return OrderSummaryResponse.builder()
				.id(order.getId())
				.code(order.getCode())
				.customerName(order.getCustomerName())
				.totalAmount(order.getTotalAmount())
				.status(order.getStatus())
				.createdAt(order.getCreatedAt())
				.build();
	}
}
