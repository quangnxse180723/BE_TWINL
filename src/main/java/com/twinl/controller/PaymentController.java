package com.twinl.controller;

import com.twinl.dto.response.PaymentCreateResponse;
import com.twinl.dto.response.VnpayIpnResponse;
import com.twinl.dto.response.VnpayReturnResponse;
import com.twinl.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý thanh toán.
 *
 * VNPay (Sandbox - test)  → /api/payments/vnpay/*
 * SePay (Tiền thật)       → /api/payments/sepay/create
 *                            Webhook riêng: /api/v1/payment/sepay-webhook  (xem SepayWebhookController)
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ──────────────── VNPAY (Sandbox) ────────────────

    @PostMapping("/vnpay/create")
    public ResponseEntity<PaymentCreateResponse> createVnpayPayment(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        return ResponseEntity.ok(paymentService.createVnpayPayment(clientIp));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<VnpayReturnResponse> handleVnpayReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnpayReturn(params));
    }

    /** VNPay IPN – nhận tín hiệu thanh toán từ server VNPay */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> handleVnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnpayIpn(params));
    }

    // ──────────────── SEPAY (Tiền thật - VietQR) ────────────────

    @PostMapping("/sepay/create")
    public ResponseEntity<PaymentCreateResponse> createSepayPayment() {
        return ResponseEntity.ok(paymentService.createSepayPayment());
    }

    /** Public endpoint – kiểm tra trạng thái thanh toán theo mã đơn hàng (không cần JWT) */
    @GetMapping("/status/{code}")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable String code) {
        String status = paymentService.getPaymentStatus(code);
        return ResponseEntity.ok(Map.of("paymentStatus", status));
    }

    // ────────────────────────────────────────────────────────────

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

