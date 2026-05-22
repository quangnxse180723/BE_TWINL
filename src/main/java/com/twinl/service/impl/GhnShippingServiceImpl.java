package com.twinl.service.impl;

import com.twinl.config.GhnProperties;
import com.twinl.dto.ghn.GhnCreateOrderItem;
import com.twinl.dto.ghn.GhnCreateOrderRequest;
import com.twinl.dto.ghn.GhnCreateOrderResponse;
import com.twinl.dto.request.GhnCreateShipmentRequest;
import com.twinl.dto.request.GhnWebhookRequest;
import com.twinl.dto.response.GhnWebhookResponse;
import com.twinl.dto.response.ShipmentResponse;
import com.twinl.entity.Order;
import com.twinl.entity.OrderStatus;
import com.twinl.entity.PaymentStatus;
import com.twinl.entity.Shipment;
import com.twinl.entity.ShipmentStatus;
import com.twinl.entity.ShippingProvider;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.ShipmentRepository;
import com.twinl.service.ShippingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GhnShippingServiceImpl implements ShippingService {
	private static final String GHN_CREATE_ORDER_PATH = "/v2/shipping-order/create";
	private static final int GHN_SUCCESS_CODE = 200;

	private final RestTemplate restTemplate;
	private final GhnProperties ghnProperties;
	private final OrderRepository orderRepository;
	private final ShipmentRepository shipmentRepository;

	public GhnShippingServiceImpl(
			RestTemplate restTemplate,
			GhnProperties ghnProperties,
			OrderRepository orderRepository,
			ShipmentRepository shipmentRepository
	) {
		this.restTemplate = restTemplate;
		this.ghnProperties = ghnProperties;
		this.orderRepository = orderRepository;
		this.shipmentRepository = shipmentRepository;
	}

	@Override
	public ShipmentResponse createGhnShipment(Long orderId, GhnCreateShipmentRequest request) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

		ensureOrderIsShippable(order);
		shipmentRepository.findByOrderId(orderId).ifPresent(existing -> {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipment already created");
		});

		GhnCreateOrderRequest ghnRequest = buildGhnRequest(order, request);
		GhnCreateOrderResponse ghnResponse = callGhnCreateOrder(ghnRequest);
		Shipment shipment = saveShipment(order, ghnResponse);

		return toResponse(shipment);
	}

	@Override
	public GhnWebhookResponse handleGhnWebhook(GhnWebhookRequest request, String tokenHeader) {
		validateWebhookToken(tokenHeader);
		if (request == null || request.getOrderCode() == null || request.getOrderCode().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing order_code");
		}

		Shipment shipment = shipmentRepository.findByTrackingCode(request.getOrderCode())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));
		ShipmentStatus mappedStatus = mapGhnStatus(request.getStatus());
		if (mappedStatus != null) {
			shipment.setStatus(mappedStatus);
			shipmentRepository.save(shipment);
			updateOrderStatusFromShipment(shipment.getOrder(), mappedStatus);
		}

		return GhnWebhookResponse.builder()
				.code(200)
				.message("OK")
				.build();
	}

	private void ensureOrderIsShippable(Order order) {
		if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not paid");
		}
		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is cancelled");
		}
	}

	private GhnCreateOrderRequest buildGhnRequest(Order order, GhnCreateShipmentRequest request) {
		List<GhnCreateOrderItem> items = request.getItems().stream()
				.map(item -> GhnCreateOrderItem.builder()
						.name(item.getName())
						.quantity(item.getQuantity())
						.price(item.getPrice())
						.weight(item.getWeight())
						.build())
				.collect(Collectors.toList());

		String requiredNote = request.getRequiredNote() == null || request.getRequiredNote().isBlank()
				? "KHONGCHOXEMHANG"
				: request.getRequiredNote();

		return GhnCreateOrderRequest.builder()
				.paymentTypeId(resolvePaymentTypeId())
				.note(request.getNote())
				.requiredNote(requiredNote)
				.returnPhone(ghnProperties.getReturnPhone())
				.returnAddress(ghnProperties.getReturnAddress())
				.returnDistrictId(ghnProperties.getReturnDistrictId())
				.returnWardCode(ghnProperties.getReturnWardCode())
				.toName(request.getToName())
				.toPhone(request.getToPhone())
				.toAddress(request.getToAddress())
				.toWardCode(request.getToWardCode())
				.toDistrictId(request.getToDistrictId())
				.toProvinceId(request.getToProvinceId())
				.codAmount(request.getCodAmount())
				.content(buildContent(order))
				.weight(request.getWeight())
				.length(request.getLength())
				.width(request.getWidth())
				.height(request.getHeight())
				.serviceTypeId(resolveServiceTypeId())
				.items(items)
				.build();
	}

	private Integer resolvePaymentTypeId() {
		return ghnProperties.getPaymentTypeId() == null ? 1 : ghnProperties.getPaymentTypeId();
	}

	private Integer resolveServiceTypeId() {
		return ghnProperties.getServiceTypeId() == null ? 2 : ghnProperties.getServiceTypeId();
	}

	private String buildContent(Order order) {
		return "Order " + order.getCode();
	}

	private GhnCreateOrderResponse callGhnCreateOrder(GhnCreateOrderRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Token", ghnProperties.getToken());
		if (ghnProperties.getShopId() != null) {
			headers.add("ShopId", ghnProperties.getShopId().toString());
		}

		HttpEntity<GhnCreateOrderRequest> entity = new HttpEntity<>(request, headers);
		String url = ghnProperties.getBaseUrl() + GHN_CREATE_ORDER_PATH;
		ResponseEntity<GhnCreateOrderResponse> response = restTemplate.postForEntity(
				url,
				entity,
				GhnCreateOrderResponse.class
		);

		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GHN create order failed");
		}

		GhnCreateOrderResponse body = response.getBody();
		if (body.getCode() == null || body.getCode() != GHN_SUCCESS_CODE) {
			String message = body.getMessage() == null ? "GHN create order failed" : body.getMessage();
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
		}

		if (body.getData() == null || body.getData().getOrderCode() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GHN response missing order code");
		}

		return body;
	}

	private Shipment saveShipment(Order order, GhnCreateOrderResponse response) {
		Integer totalFee = response.getData().getTotalFee();
		Shipment shipment = Shipment.builder()
				.order(order)
				.provider(ShippingProvider.GHN)
				.trackingCode(response.getData().getOrderCode())
				.shippingFee(totalFee == null ? null : BigDecimal.valueOf(totalFee))
				.status(ShipmentStatus.CREATED)
				.build();

		Shipment saved = shipmentRepository.save(shipment);
		order.setShipment(saved);
		order.setStatus(OrderStatus.PROCESSING);
		orderRepository.save(order);
		return saved;
	}

	private void updateOrderStatusFromShipment(Order order, ShipmentStatus status) {
		if (order == null || status == null) {
			return;
		}
		switch (status) {
			case DELIVERED -> order.setStatus(OrderStatus.COMPLETED);
			case CANCELLED, FAILED -> order.setStatus(OrderStatus.CANCELLED);
			default -> order.setStatus(OrderStatus.PROCESSING);
		}
		orderRepository.save(order);
	}

	private ShipmentResponse toResponse(Shipment shipment) {
		return ShipmentResponse.builder()
				.id(shipment.getId())
				.orderId(shipment.getOrder().getId())
				.provider(shipment.getProvider())
				.trackingCode(shipment.getTrackingCode())
				.shippingFee(shipment.getShippingFee())
				.status(shipment.getStatus())
				.build();
	}

	private ShipmentStatus mapGhnStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		return switch (status.toLowerCase()) {
			case "ready_to_pick", "picking" -> ShipmentStatus.PICKING;
			case "picked", "storing", "transporting", "sorting", "delivering" -> ShipmentStatus.SHIPPING;
			case "delivered" -> ShipmentStatus.DELIVERED;
			case "cancel" -> ShipmentStatus.CANCELLED;
			case "delivery_fail", "return", "returned" -> ShipmentStatus.FAILED;
			default -> ShipmentStatus.SHIPPING;
		};
	}

	private void validateWebhookToken(String tokenHeader) {
		String expected = ghnProperties.getWebhookToken();
		if (expected == null || expected.isBlank()) {
			return;
		}
		if (tokenHeader == null || !expected.equals(tokenHeader)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook token");
		}
	}
}
