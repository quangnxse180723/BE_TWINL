package com.twinl.service;

import com.twinl.dto.response.PaymentCreateResponse;
import com.twinl.dto.response.VnpayIpnResponse;
import com.twinl.dto.response.VnpayReturnResponse;
import java.util.Map;

public interface PaymentService {
    // ──────────────── VNPAY (Sandbox) ────────────────
    PaymentCreateResponse createVnpayPayment(String clientIp);
    VnpayReturnResponse handleVnpayReturn(Map<String, String> params);
    VnpayIpnResponse handleVnpayIpn(Map<String, String> params);

    // ──────────────── SEPAY (Tiền thật - VietQR) ────────────────
    PaymentCreateResponse createSepayPayment();
    void handleSepayWebhook(String rawBody);
}
