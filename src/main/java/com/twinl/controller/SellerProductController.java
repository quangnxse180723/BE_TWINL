package com.twinl.controller;

import com.twinl.dto.request.ProductRequest;
import com.twinl.dto.response.OrderResponse;
import com.twinl.dto.response.ProductResponse;
import com.twinl.service.OrderService;
import com.twinl.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import com.twinl.service.WalletService;
import com.twinl.dto.response.SellerStatisticsResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerProductController {

	private final ProductService productService;
	private final OrderService orderService;
	private final WalletService walletService;

	@GetMapping("/products")
	public ResponseEntity<Page<ProductResponse>> getMySellerProducts(
			Authentication authentication,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int sizePage
	) {
		String username = authentication.getName();
		Page<ProductResponse> responses = productService.getProductsBySeller(username, page, sizePage);
		return ResponseEntity.ok(responses);
	}

	@PostMapping("/products")
	public ResponseEntity<ProductResponse> createSellerProduct(
			Authentication authentication,
			@Valid @RequestBody ProductRequest request
	) {
		String username = authentication.getName();
		ProductResponse response = productService.createSellerProduct(request, username);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/orders")
	public ResponseEntity<Page<OrderResponse>> getMySellerOrders(
			Authentication authentication,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int sizePage
	) {
		String username = authentication.getName();
		Page<OrderResponse> responses = orderService.getOrdersBySeller(username, page, sizePage);
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/statistics")
	public ResponseEntity<SellerStatisticsResponse> getSellerStatistics(Authentication authentication) {
		String username = authentication.getName();
		SellerStatisticsResponse response = walletService.getSellerStatistics(username);
		return ResponseEntity.ok(response);
	}
}
