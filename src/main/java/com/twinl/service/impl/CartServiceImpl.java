package com.twinl.service.impl;

import com.twinl.dto.request.AddCartItemRequest;
import com.twinl.dto.request.UpdateCartItemRequest;
import com.twinl.dto.response.CartItemResponse;
import com.twinl.dto.response.CartResponse;
import com.twinl.entity.Cart;
import com.twinl.entity.CartItem;
import com.twinl.entity.Product;
import com.twinl.entity.User;
import com.twinl.repository.CartRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.CartService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartServiceImpl implements CartService {
	private final CartRepository cartRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public CartServiceImpl(
			CartRepository cartRepository,
			ProductRepository productRepository,
			UserRepository userRepository
	) {
		this.cartRepository = cartRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Override
	public CartResponse getCurrentCart() {
		User user = getCurrentAuthenticatedUser();
		Cart cart = getOrCreateCart(user);
		return toResponse(cart);
	}

	@Override
	public CartResponse addItem(AddCartItemRequest request) {
		User user = getCurrentAuthenticatedUser();
		Cart cart = getOrCreateCart(user);
		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
		int stock = normalizeStock(product.getStock());
		if (stock < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is out of stock");
		}
		int quantity = normalizeQuantity(request.getQuantity());
		if (quantity > stock) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity exceeds stock");
		}

		CartItem item = findItemByProduct(cart, product.getId());
		if (item == null) {
			item = CartItem.builder()
					.cart(cart)
					.product(product)
					.quantity(quantity)
					.unitPrice(product.getPrice())
					.lineTotal(calculateLineTotal(product.getPrice(), quantity))
					.build();
			cart.getItems().add(item);
		} else {
			int newQuantity = item.getQuantity() + quantity;
			if (newQuantity > stock) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity exceeds stock");
			}
			item.setQuantity(newQuantity);
			item.setUnitPrice(product.getPrice());
			item.setLineTotal(calculateLineTotal(product.getPrice(), newQuantity));
		}

		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Override
	public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
		User user = getCurrentAuthenticatedUser();
		Cart cart = getOrCreateCart(user);
		CartItem item = findItemById(cart, itemId);
		if (item == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
		}

		int quantity = normalizeQuantity(request.getQuantity());
		Product product = item.getProduct();
		int stock = normalizeStock(product != null ? product.getStock() : null);
		if (stock < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is out of stock");
		}
		if (quantity > stock) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity exceeds stock");
		}
		item.setQuantity(quantity);
		BigDecimal unitPrice = product != null ? product.getPrice() : item.getUnitPrice();
		item.setUnitPrice(unitPrice);
		item.setLineTotal(calculateLineTotal(unitPrice, quantity));

		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Override
	public CartResponse removeItem(Long itemId) {
		User user = getCurrentAuthenticatedUser();
		Cart cart = getOrCreateCart(user);
		CartItem item = findItemById(cart, itemId);
		if (item == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
		}

		cart.getItems().remove(item);
		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Override
	public CartResponse clearCart() {
		User user = getCurrentAuthenticatedUser();
		Cart cart = getOrCreateCart(user);
		cart.getItems().clear();
		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	private Cart getOrCreateCart(User user) {
		return cartRepository.findByUserId(user.getId())
				.orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
	}

	private CartItem findItemByProduct(Cart cart, Long productId) {
		if (cart.getItems() == null) {
			return null;
		}
		Optional<CartItem> match = cart.getItems().stream()
				.filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
				.findFirst();
		return match.orElse(null);
	}

	private CartItem findItemById(Cart cart, Long itemId) {
		if (cart.getItems() == null) {
			return null;
		}
		Optional<CartItem> match = cart.getItems().stream()
				.filter(item -> item.getId() != null && item.getId().equals(itemId))
				.findFirst();
		return match.orElse(null);
	}

	private BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
		if (unitPrice == null) {
			return BigDecimal.ZERO;
		}
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	private CartResponse toResponse(Cart cart) {
		Set<CartItemResponse> items = cart.getItems().stream()
				.map(item -> CartItemResponse.builder()
						.id(item.getId())
						.productId(item.getProduct() != null ? item.getProduct().getId() : null)
						.productName(item.getProduct() != null ? item.getProduct().getName() : null)
						.imageUrl(extractImageUrl(item.getProduct()))
						.availableStock(item.getProduct() != null ? item.getProduct().getStock() : null)
						.quantity(item.getQuantity())
						.unitPrice(item.getUnitPrice())
						.lineTotal(item.getLineTotal())
						.build())
				.collect(Collectors.toSet());

		int totalQuantity = cart.getItems().stream()
				.mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
				.sum();
		BigDecimal subtotal = cart.getItems().stream()
				.map(item -> item.getLineTotal() == null ? BigDecimal.ZERO : item.getLineTotal())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return CartResponse.builder()
				.id(cart.getId())
				.userId(cart.getUser() != null ? cart.getUser().getId() : null)
				.items(items)
				.totalQuantity(totalQuantity)
				.subtotal(subtotal)
				.createdAt(cart.getCreatedAt())
				.updatedAt(cart.getUpdatedAt())
				.build();
	}

	private String extractImageUrl(Product product) {
		if (product == null || product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
			return null;
		}
		return product.getImageUrls().get(0);
	}

	private int normalizeQuantity(Integer quantity) {
		if (quantity == null || quantity < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
		}
		return quantity;
	}

	private int normalizeStock(Integer stock) {
		return stock == null ? 0 : stock;
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
