package com.twinl.controller;

import com.twinl.dto.request.AddCartItemRequest;
import com.twinl.dto.request.UpdateCartItemRequest;
import com.twinl.dto.response.CartResponse;
import com.twinl.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public ResponseEntity<CartResponse> getCurrentCart() {
		return ResponseEntity.ok(cartService.getCurrentCart());
	}

	@PostMapping("/items")
	public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
		return ResponseEntity.ok(cartService.addItem(request));
	}

	@PutMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> updateItem(
			@PathVariable Long itemId,
			@Valid @RequestBody UpdateCartItemRequest request
	) {
		return ResponseEntity.ok(cartService.updateItem(itemId, request));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
		return ResponseEntity.ok(cartService.removeItem(itemId));
	}

	@DeleteMapping
	public ResponseEntity<CartResponse> clearCart() {
		return ResponseEntity.ok(cartService.clearCart());
	}
}
