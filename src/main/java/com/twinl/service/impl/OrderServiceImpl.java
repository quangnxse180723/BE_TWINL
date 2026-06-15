package com.twinl.service.impl;

import com.twinl.dto.response.OrderItemResponse;
import com.twinl.dto.response.OrderResponse;
import com.twinl.entity.Order;
import com.twinl.entity.OrderStatus;
import com.twinl.entity.RoleName;
import com.twinl.entity.User;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.NotificationService;
import com.twinl.service.OrderService;
import com.twinl.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final WalletService walletService;

	public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository, NotificationService notificationService, WalletService walletService) {
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.walletService = walletService;
	}

	// ─────────────────────────── READ ───────────────────────────

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

	@Override
	@Transactional
	public OrderResponse confirmReceipt(Long orderId) {
		Order order = getOrderById(orderId);
		User currentUser = getCurrentAuthenticatedUser();
		if (!order.getUser().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này.");
		}
		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xác nhận khi đơn hàng đã được giao.");
		}
		
		order.setStatus(OrderStatus.COMPLETED);
		orderRepository.save(order);
		
		// Giải ngân cho seller ngay lập tức
		try {
			walletService.releaseEscrow(order);
			order.setEscrowReleased(true);
			orderRepository.save(order);
		} catch (Exception e) {
			log.error("Lỗi giải ngân khi khách hàng xác nhận: ", e);
		}
		
		notificationService.sendNotification(
			order.getUser(), 
			"Xác nhận thành công", 
			"Cảm ơn bạn đã xác nhận nhận hàng. Đơn hàng " + order.getCode() + " đã hoàn thành.", 
			"ORDER_STATUS"
		);
		
		return toResponse(order);
	}

	@Override
	@Transactional
	public OrderResponse reportMissing(Long orderId, String reason) {
		Order order = getOrderById(orderId);
		User currentUser = getCurrentAuthenticatedUser();
		if (!order.getUser().getId().equals(currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này.");
		}
		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể khiếu nại khi đơn hàng ở trạng thái đã giao.");
		}
		
		order.setStatus(OrderStatus.DISPUTED);
		String reportNote = "Khách hàng báo chưa nhận được hàng. Lý do: " + (reason != null ? reason : "Không có");
		order.setNote(reportNote);
		orderRepository.save(order);
		
		// Gửi thông báo cho Admin (giả sử có hệ thống nhận message admin, tạm log)
		log.warn("[DISPUTE] Đơn hàng {} bị khiếu nại: {}", order.getCode(), reportNote);
		
		// Gửi thông báo cho Shipper
		if (order.getShipper() != null) {
			notificationService.sendNotification(
				order.getShipper(),
				"Đơn hàng bị khiếu nại",
				"Đơn hàng " + order.getCode() + " bị khách hàng báo cáo chưa nhận được. Vui lòng kiểm tra lại.",
				"ORDER_STATUS"
			);
		}
		
		return toResponse(order);
	}

	@Override
	public Page<OrderResponse> getMyShipperOrders(int page, int sizePage, String shipperUsername) {
		User shipper = getUserByEmail(shipperUsername);
		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return orderRepository.findByShipperId(shipper.getId(), pageable).map(this::toResponse);
	}

	@Override
	public Page<OrderResponse> getOrdersBySeller(String username, int page, int sizePage) {
		User seller = getUserByEmail(username);
		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return orderRepository.findOrdersBySellerId(seller.getId(), pageable).map(this::toResponse);
	}

	// ─────────────────────────── IN-HOUSE LOGISTICS ───────────────────────────

	@Override
	@Transactional
	public OrderResponse assignOrderToShipper(Long orderId, Long shipperId) {
		Order order = getOrderById(orderId);

		// Chỉ có thể assign đơn đang ở trạng thái PENDING
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Chỉ có thể gán Shipper cho đơn hàng ở trạng thái PENDING. Trạng thái hiện tại: " + order.getStatus());
		}

		User shipper = userRepository.findById(shipperId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Không tìm thấy User với id: " + shipperId));

		// Kiểm tra User được gán phải có role SHIPPER
		boolean isShipper = shipper.getRoles().stream()
				.anyMatch(role -> role.getName() == RoleName.SHIPPER);
		if (!isShipper) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"User id=" + shipperId + " không có role SHIPPER. Không thể gán vào đơn hàng.");
		}

		order.setShipper(shipper);
		order.setStatus(OrderStatus.ASSIGNED);
		Order saved = orderRepository.save(order);

		log.info("[ASSIGN] Đơn hàng {} đã được gán cho Shipper {} ({})",
				order.getCode(), shipper.getDisplayName(), shipper.getEmail());

		if (saved.getUser() != null) {
			String title = "Đơn hàng " + saved.getCode() + " đang được giao";
			String message = "Đơn hàng của bạn đã được giao cho Shipper: " + shipper.getDisplayName() + ".";
			notificationService.sendNotification(saved.getUser(), title, message, "ORDER_STATUS");
		}

		return toResponse(saved);
	}

	@Override
	@Transactional
	public OrderResponse updateOrderStatusByShipper(Long orderId, OrderStatus status, String note, String shipperUsername) {
		Order order = getOrderById(orderId);

		// Kiểm tra đúng Shipper được gán mới có quyền cập nhật
		if (order.getShipper() == null || !order.getShipper().getEmail().equals(shipperUsername)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Bạn không được phân công xử lý đơn hàng này.");
		}

		// Validate flow trạng thái hợp lệ cho Shipper
		validateShipperStatusTransition(order.getStatus(), status);

		order.setStatus(status);
		if (note != null && !note.isBlank()) {
			order.setNote(note);
		}

		if (status == OrderStatus.DELIVERED) {
			order.setDeliveredAt(LocalDateTime.now());
			log.info("[WEBHOOK MOCK] Giao thành công - Kích hoạt bộ đếm 48h Escrow cho Order: {}", orderId);
			walletService.holdEscrow(order);
		}

		Order saved = orderRepository.save(order);
		log.info("[SHIPPER UPDATE] Đơn hàng {} → trạng thái mới: {} (Shipper: {})",
				order.getCode(), status, shipperUsername);

		if (saved.getUser() != null) {
			String title = "Cập nhật đơn hàng " + saved.getCode();
			String statusName = switch (status) {
				case PENDING -> "Chờ xác nhận";
				case ASSIGNED -> "Đã điều phối Shipper";
				case PICKED_UP -> "Đang giao hàng";
				case DELIVERED -> "Đã giao hàng";
				case COMPLETED -> "Hoàn thành";
				case CANCELED -> "Đã hủy";
				default -> status.name();
			};
			String message = "Đơn hàng của bạn đã được cập nhật sang trạng thái: " + statusName;
			if (note != null && !note.isBlank()) {
				message += ". Ghi chú: " + note;
			}
			notificationService.sendNotification(saved.getUser(), title, message, "ORDER_STATUS");
		}

		return toResponse(saved);
	}

	// ─────────────────────────── PRIVATE HELPERS ───────────────────────────

	/**
	 * Validate chỉ cho phép Shipper chuyển trạng thái theo flow chuẩn:
	 * ASSIGNED → PICKED_UP → DELIVERED
	 */
	private void validateShipperStatusTransition(OrderStatus current, OrderStatus next) {
		boolean valid = switch (current) {
			case ASSIGNED -> next == OrderStatus.PICKED_UP;
			case PICKED_UP -> next == OrderStatus.DELIVERED;
			default -> false;
		};
		if (!valid) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Không thể chuyển trạng thái từ " + current + " sang " + next
							+ ". Flow hợp lệ: ASSIGNED → PICKED_UP → DELIVERED");
		}
	}

	private Order getOrderById(Long orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Không tìm thấy đơn hàng với id: " + orderId));
	}

	private User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Không tìm thấy user với email: " + email));
	}

	private User getCurrentAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
		}
		return getUserByEmail(authentication.getName());
	}

	private OrderResponse toResponse(Order order) {
		Set<OrderItemResponse> items = order.getItems().stream()
				.map(item -> OrderItemResponse.builder()
						.productId(item.getProduct() != null ? item.getProduct().getId() : null)
						.productName(item.getProduct() != null ? item.getProduct().getName() : null)
						.imageUrl(item.getProduct() != null && item.getProduct().getImageUrls() != null && !item.getProduct().getImageUrls().isEmpty() ? item.getProduct().getImageUrls().get(0) : null)
						.quantity(item.getQuantity())
						.unitPrice(item.getUnitPrice())
						.lineTotal(item.getLineTotal())
						.build())
				.collect(Collectors.toSet());

		BigDecimal platformFee = null;
		BigDecimal sellerAmount = null;
		String escrowStatus = null;

		if (order.getStatus() == OrderStatus.DELIVERED && order.getDeliveredAt() != null) {
			// Platform fee giả định: 10%
			platformFee = order.getTotalAmount().multiply(new BigDecimal("0.10"));
			sellerAmount = order.getTotalAmount().subtract(platformFee);

			if (Boolean.TRUE.equals(order.getEscrowReleased())) {
				escrowStatus = "RELEASED";
			} else {
				escrowStatus = "HELD";
			}
		}

		return OrderResponse.builder()
				.id(order.getId())
				.code(order.getCode())
				.customerName(order.getCustomerName())
				.customerEmail(order.getCustomerEmail())
				.customerPhone(order.getCustomerPhone())
				.shippingAddress(order.getShippingAddress())
				.shippingWardCode(order.getShippingWardCode())
				.shippingDistrictId(order.getShippingDistrictId())
				.shippingProvinceId(order.getShippingProvinceId())
				.status(order.getStatus())
				.totalAmount(order.getTotalAmount())
				.paymentMethod(order.getPaymentMethod())
				.paymentStatus(order.getPaymentStatus())
				.shipperId(order.getShipper() != null ? order.getShipper().getId() : null)
				.shipperName(order.getShipper() != null ? order.getShipper().getDisplayName() : null)
				.deliveredAt(order.getDeliveredAt())
				.note(order.getNote())
				.platformFee(platformFee)
				.sellerAmount(sellerAmount)
				.escrowStatus(escrowStatus)
				.items(items)
				.createdAt(order.getCreatedAt())
				.updatedAt(order.getUpdatedAt())
				.build();
	}
}
