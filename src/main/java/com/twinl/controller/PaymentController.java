package com.twinl.controller;

import com.twinl.dto.response.PaymentCreateResponse;
import com.twinl.dto.response.VnpayIpnResponse;
import com.twinl.dto.response.VnpayReturnResponse;
import com.twinl.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/vnpay/create")
	public ResponseEntity<PaymentCreateResponse> createVnpayPayment(HttpServletRequest request) {
		String clientIp = resolveClientIp(request);
		return ResponseEntity.ok(paymentService.createVnpayPayment(clientIp));
	}

	@GetMapping("/vnpay/return")
	public ResponseEntity<VnpayReturnResponse> handleVnpayReturn(@RequestParam Map<String, String> params) {
		return ResponseEntity.ok(paymentService.handleVnpayReturn(params));
	}

	@GetMapping("/vnpay/ipn")
	public ResponseEntity<VnpayIpnResponse> handleVnpayIpn(@RequestParam Map<String, String> params) {
		return ResponseEntity.ok(paymentService.handleVnpayIpn(params));
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
