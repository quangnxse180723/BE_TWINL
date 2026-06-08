package com.twinl.controller;

import com.twinl.config.SepayProperties;
import com.twinl.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Webhook riêng biệt cho SePay.
 * Nhận biến động số dư từ tài khoản ngân hàng cá nhân.
 * Xác thực bằng HMAC-SHA256 (X-Sepay-Signature header).
 */
@RestController
@RequestMapping("/api/v1/payment")
public class SepayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookController.class);
    private final PaymentService paymentService;
    private final SepayProperties sepayProperties;

    public SepayWebhookController(PaymentService paymentService, SepayProperties sepayProperties) {
        this.paymentService = paymentService;
        this.sepayProperties = sepayProperties;
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleSepayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        log.info("[SEPAY Webhook] Nhận webhook từ SePay. Auth: {}", authorization);
        log.info("[SEPAY Webhook] RawBody received: {}", rawBody);

        String secretKey = sepayProperties.getWebhookToken();
        
        // Kiểm tra API Key
        if (authorization == null || !authorization.equals("Apikey " + secretKey)) {
            log.warn("[SEPAY Webhook] Từ chối truy cập: Sai API Key.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", "false", "message", "Unauthorized"));
        }

        try {
            paymentService.handleSepayWebhook(rawBody);
            return ResponseEntity.ok(Map.of("success", "true", "message", "Đã xử lý thành công"));
        } catch (Exception e) {
            log.error("[SEPAY Webhook] Xử lý thất bại: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", "false", "message", e.getMessage()));
        }
    }
}
