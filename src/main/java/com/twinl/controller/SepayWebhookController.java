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
    public ResponseEntity<Map<String, String>> handleSepayWebhook(
            @RequestHeader(value = "X-Sepay-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        log.info("[SEPAY Webhook] Nhận webhook từ SePay. Signature: {}", signature);

        // Xác thực chữ ký HMAC-SHA256
        String secretKey = sepayProperties.getWebhookToken();
        if (!isValidHmacSignature(rawBody, signature, secretKey)) {
            log.warn("[SEPAY Webhook] Từ chối truy cập: Sai chữ ký HMAC-SHA256. Received: {}", signature);
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

    /**
     * Tính HMAC-SHA256 của rawBody bằng secretKey và so sánh với signature nhận được.
     */
    private boolean isValidHmacSignature(String rawBody, String signature, String secretKey) {
        if (signature == null || signature.isBlank()) {
            log.warn("[SEPAY Webhook] Không có X-Sepay-Signature header!");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));

            // Chuyển sang chuỗi hex lowercase
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            String computed = hex.toString();
            boolean valid = computed.equalsIgnoreCase(signature);
            if (!valid) {
                log.warn("[SEPAY Webhook] Chữ ký không khớp. Expected: {}, Got: {}", computed, signature);
            }
            return valid;
        } catch (Exception e) {
            log.error("[SEPAY Webhook] Lỗi xác thực HMAC: {}", e.getMessage());
            return false;
        }
    }
}
