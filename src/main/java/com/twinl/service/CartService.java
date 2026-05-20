package com.twinl.service;

import com.twinl.dto.request.AddCartItemRequest;
import com.twinl.dto.request.UpdateCartItemRequest;
import com.twinl.dto.response.CartResponse;

public interface CartService {
	CartResponse getCurrentCart();
	CartResponse addItem(AddCartItemRequest request);
	CartResponse updateItem(Long itemId, UpdateCartItemRequest request);
	CartResponse removeItem(Long itemId);
	CartResponse clearCart();
}
