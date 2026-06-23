package com.twinl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinl.config.SepayProperties;
import com.twinl.config.VnpayProperties;
import com.twinl.dto.response.PaymentCreateResponse;
import com.twinl.dto.response.VnpayIpnResponse;
import com.twinl.dto.response.VnpayReturnResponse;
import com.twinl.entity.Cart;
import com.twinl.entity.CartItem;
import com.twinl.entity.Order;
import com.twinl.entity.OrderItem;
import com.twinl.entity.OrderStatus;
import com.twinl.entity.PaymentMethod;
import com.twinl.entity.PaymentStatus;
import com.twinl.entity.Product;
import com.twinl.entity.User;
import com.twinl.repository.CartRepository;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final String VNPAY_SUCCESS_CODE = "00";
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties vnpayProperties;
    private final SepayProperties sepayProperties;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final com.twinl.service.NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    public PaymentServiceImpl(
            VnpayProperties vnpayProperties,
            SepayProperties sepayProperties,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            com.twinl.service.NotificationService notificationService) {
        this.vnpayProperties = vnpayProperties;
        this.sepayProperties = sepayProperties;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public PaymentCreateResponse createVnpayPayment(String clientIp) {
        User user = getCurrentAuthenticatedUser();
        validateUserProfile(user);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = buildOrderFromCart(user, cart);
        Order saved = orderRepository.save(order);
        String paymentUrl = buildVnpayPaymentUrl(saved, clientIp);

        return PaymentCreateResponse.builder()
                .orderId(saved.getId())
                .orderCode(saved.getCode())
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public VnpayReturnResponse handleVnpayReturn(Map<String, String> params) {
        ValidationResult validationResult = validateVnpayResponse(params);
        Order order = orderRepository.findByCode(validationResult.txnRef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        updateOrderFromVnpay(order, validationResult);

        String message = validationResult.isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại";
        return VnpayReturnResponse.builder()
                .orderCode(order.getCode())
                .paymentStatus(order.getPaymentStatus().name())
                .message(message)
                .build();
    }

    @Override
    public VnpayIpnResponse handleVnpayIpn(Map<String, String> params) {
        try {
            ValidationResult validationResult = validateVnpayResponse(params);
            Order order = orderRepository.findByCode(validationResult.txnRef)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            updateOrderFromVnpay(order, validationResult);
            return VnpayIpnResponse.builder()
                    .rspCode(VNPAY_SUCCESS_CODE)
                    .message("Confirm Success")
                    .build();
        } catch (ResponseStatusException ex) {
            return VnpayIpnResponse.builder()
                    .rspCode("99")
                    .message(ex.getReason() == null ? "Invalid request" : ex.getReason())
                    .build();
        }
    }

    private void updateOrderFromVnpay(Order order, ValidationResult validationResult) {
        if (validationResult.amount != null && order.getTotalAmount() != null) {
            String expected = toVnpayAmount(order.getTotalAmount());
            if (!expected.equals(validationResult.amount)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
            }
        }

        if (validationResult.isSuccess) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            // Đơn ở trạng thái PENDING, chờ Admin gán Shipper nội bộ
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentTransactionNo(validationResult.transactionNo);
            order.setPaymentPaidAt(LocalDateTime.now());
            clearCart(order);

            // Trừ số lượng trong kho
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null && product.getStock() != null) {
                    int newStock = product.getStock() - item.getQuantity();
                    product.setStock(Math.max(newStock, 0));
                    productRepository.save(product);
                }
            }

            orderRepository.save(order);
            log.info("[PAYMENT] Đơn hàng {} thanh toán thành công. Chờ Admin gán Shipper.", order.getCode());
            
            notificationService.sendNotification(order.getUser(), "Thanh toán thành công", "Đơn hàng " + order.getCode() + " đã được thanh toán thành công qua VNPay và đang chờ xác nhận.", "ORDER_STATUS");

            java.util.List<User> admins = userRepository.findByRoles_Name(com.twinl.entity.RoleName.ADMIN);
            for (User admin : admins) {
                notificationService.sendNotification(admin, "Đơn hàng mới", "Đơn hàng " + order.getCode() + " vừa được thanh toán thành công. Vui lòng kiểm tra và gán Shipper.", "NEW_ORDER_PAID");
            }
            return;
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            notificationService.sendNotification(order.getUser(), "Thanh toán thất bại", "Thanh toán cho đơn hàng " + order.getCode() + " đã bị hủy hoặc thất bại.", "ORDER_STATUS");
        }
        orderRepository.save(order);
    }

    private void clearCart(Order order) {
        User user = order.getUser();
        if (user == null || user.getId() == null) {
            return;
        }
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private ValidationResult validateVnpayResponse(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing vnp_SecureHash");
        }
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing vnp_TxnRef");
        }

        Map<String, String> filtered = new HashMap<>(params);
        filtered.remove("vnp_SecureHash");
        filtered.remove("vnp_SecureHashType");
        String calculatedHash = hmacSha512(vnpayProperties.getHashSecret(), buildHashData(filtered));
        if (!secureHash.equalsIgnoreCase(calculatedHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }

        boolean isSuccess = VNPAY_SUCCESS_CODE.equals(params.get("vnp_ResponseCode"))
                && VNPAY_SUCCESS_CODE.equals(params.get("vnp_TransactionStatus"));
        return new ValidationResult(
                txnRef,
                params.get("vnp_TransactionNo"),
                params.get("vnp_Amount"),
                isSuccess);
    }

    /** Build order from cart – shared between VNPay and PayOS flows */
    private Order buildOrderFromCart(User user, Cart cart) {
        return buildOrderFromCart(user, cart, PaymentMethod.VNPAY);
    }

    private Order buildOrderFromCart(User user, Cart cart, PaymentMethod paymentMethod) {
        BigDecimal totalAmount = calculateCartTotal(cart);
        String orderCode = generateOrderCode();

        Order order = Order.builder()
                .code(orderCode)
                .customerName(user.getDisplayName())
                .customerEmail(user.getEmail())
                .customerPhone(user.getPhone())
                .shippingAddress(user.getAddress())
                .shippingWardCode(user.getWardCode())
                .shippingDistrictId(user.getDistrictId())
                .shippingProvinceId(user.getProvinceId())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .user(user)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentTxnRef(orderCode)
                .build();

        order.setItems(cart.getItems().stream()
                .map(item -> toOrderItem(order, item))
                .collect(Collectors.toSet()));

        return order;
    }

    private OrderItem toOrderItem(Order order, CartItem cartItem) {
        if (cartItem.getProduct() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart item is missing product");
        }
        Integer stock = cartItem.getProduct().getStock();
        int available = stock == null ? 0 : stock;
        if (cartItem.getQuantity() == null || cartItem.getQuantity() < 1 || cartItem.getQuantity() > available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cart quantity");
        }

        return OrderItem.builder()
                .order(order)
                .product(cartItem.getProduct())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .lineTotal(cartItem.getLineTotal())
                .build();
    }

    private BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getLineTotal() == null ? BigDecimal.ZERO : item.getLineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildVnpayPaymentUrl(Order order, String clientIp) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnpayProperties.getVersion());
        params.put("vnp_Command", vnpayProperties.getCommand());
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        params.put("vnp_TxnRef", order.getPaymentTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Amount", toVnpayAmount(order.getTotalAmount()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", LocalDateTime.now().format(VNPAY_DATE_FORMAT));

        String hashData = buildHashData(params);
        String secureHash = hmacSha512(vnpayProperties.getHashSecret(), hashData);
        String query = buildQuery(params) + "&vnp_SecureHash=" + secureHash;
        return vnpayProperties.getPayUrl() + "?" + query;
    }

    private String buildHashData(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String buildQuery(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String secretKey, String data) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing VNPay secret");
        }
        try {
            String normalizedKey = secretKey.trim();
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    normalizedKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign VNPay request");
        }
    }

    private String toVnpayAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String generateOrderCode() {
        return "TWINL" + System.currentTimeMillis();
    }

    // ══════════════════════════════════════════════════════════════
    //  SEPAY – Tiền thật (VietQR)
    // ══════════════════════════════════════════════════════════════

    @Override
    public PaymentCreateResponse createSepayPayment() {
        User user = getCurrentAuthenticatedUser();
        validateUserProfile(user);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = buildOrderFromCart(user, cart, PaymentMethod.SEPAY);
        Order saved = orderRepository.save(order);

        try {
            int totalAmount = saved.getTotalAmount().intValue();
            String bankId = sepayProperties.getBankId();
            String accountNumber = sepayProperties.getAccountNumber();
            String accountName = java.net.URLEncoder.encode(sepayProperties.getAccountName(), java.nio.charset.StandardCharsets.UTF_8);
            String orderCode = saved.getCode();

            // Link trang thanh toán của SePay (Checkout Page)
            String paymentUrl = String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&amount=%d&des=%s&accountName=%s",
                    bankId, accountNumber, totalAmount, orderCode, accountName);

            log.info("[SEPAY] Tạo link thanh toán thành công cho đơn {}", saved.getCode());

            return PaymentCreateResponse.builder()
                    .orderId(saved.getId())
                    .orderCode(saved.getCode())
                    .paymentUrl(paymentUrl)
                    .build();

        } catch (Exception e) {
            log.error("[SEPAY] Lỗi tạo link thanh toán: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo liên kết thanh toán SePay: " + e.getMessage());
        }
    }

    /**
     * Xử lý Webhook từ SePay.
     * SePay gửi HTTP POST với body JSON và header Authorization.
     */
    @Override
    public void handleSepayWebhook(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            
            int transferAmount = root.path("transferAmount").asInt(0);
            String content = root.path("content").asText("");
            String referenceCode = root.path("referenceCode").asText("");
            
            // Bỏ qua giao dịch chuyển tiền ra (out)
            if ("out".equals(root.path("transferType").asText(""))) {
                return;
            }

            // Tìm Mã đơn hàng (Format: TWINL...) trong nội dung chuyển khoản
            Pattern pattern = Pattern.compile("(TWINL\\d+)");
            Matcher matcher = pattern.matcher(content);
            
            if (!matcher.find()) {
                log.warn("[SEPAY Webhook] Không tìm thấy mã đơn hàng TWINL trong nội dung: {}", content);
                return;
            }
            
            String orderCode = matcher.group(1);

            // Tìm đơn hàng theo mã
            Order order = orderRepository.findByCode(orderCode)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Order not found for code: " + orderCode));

            // Đã thanh toán rồi thì bỏ qua
            if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
                log.info("[SEPAY Webhook] Đơn {} đã được thanh toán trước đó. Bỏ qua.", order.getCode());
                return;
            }

            // Kiểm tra số tiền chuyển có đủ không (Cho phép chuyển dư)
            int orderTotal = order.getTotalAmount().intValue();
            if (transferAmount < orderTotal) {
                log.warn("[SEPAY Webhook] Đơn {} chuyển thiếu tiền. Yêu cầu: {}, Nhận: {}", order.getCode(), orderTotal, transferAmount);
                return; // Có thể lưu trạng thái PARTIAL_PAID nếu thiết kế
            }

            // Cập nhật trạng thái đơn hàng
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentPaidAt(LocalDateTime.now());
            order.setPaymentTransactionNo(referenceCode);

            // Trừ tồn kho
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null && product.getStock() != null) {
                    int newStock = product.getStock() - item.getQuantity();
                    product.setStock(Math.max(newStock, 0));
                    productRepository.save(product);
                }
            }

            // Xóa giỏ hàng sau khi thanh toán thành công
            clearCart(order);
            orderRepository.save(order);
            log.info("[SEPAY Webhook] Đơn hàng {} thanh toán thành công.", order.getCode());
            
            notificationService.sendNotification(order.getUser(), "Thanh toán thành công", "Đơn hàng " + order.getCode() + " đã được thanh toán thành công qua SePay và đang chờ xác nhận.", "ORDER_STATUS");

            java.util.List<User> admins = userRepository.findByRoles_Name(com.twinl.entity.RoleName.ADMIN);
            for (User admin : admins) {
                notificationService.sendNotification(admin, "Đơn hàng mới", "Đơn hàng " + order.getCode() + " vừa được thanh toán thành công. Vui lòng kiểm tra và gán Shipper.", "NEW_ORDER_PAID");
            }

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("[SEPAY Webhook] Lỗi xử lý webhook: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SePay webhook: " + e.getMessage());
        }
    }

    private void validateUserProfile(User user) {
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing display name");
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing phone");
        }
        if (user.getAddress() == null || user.getAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing address");
        }
        if (user.getWardCode() == null || user.getWardCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing ward code");
        }
        if (user.getDistrictId() == null || user.getDistrictId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing district id");
        }
        if (user.getProvinceId() == null || user.getProvinceId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing province id");
        }
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

    private static class ValidationResult {
        private final String txnRef;
        private final String transactionNo;
        private final String amount;
        private final boolean isSuccess;

        private ValidationResult(String txnRef, String transactionNo, String amount, boolean isSuccess) {
            this.txnRef = txnRef;
            this.transactionNo = transactionNo;
            this.amount = amount;
            this.isSuccess = isSuccess;
        }
    }
}
