package com.twinl.service;

import com.twinl.entity.Order;
import com.twinl.entity.OrderStatus;
import com.twinl.entity.Wallet;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.repository.UserRepository;
import com.twinl.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BusinessAnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public BusinessAnalyticsService(OrderRepository orderRepository,
                                    ProductRepository productRepository,
                                    UserRepository userRepository,
                                    WalletRepository walletRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isEmpty()) {
            return isStart ? LocalDateTime.now().minusDays(30) : LocalDateTime.now();
        }
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            return isStart ? date.atStartOfDay() : date.atTime(java.time.LocalTime.MAX);
        } catch (Exception e) {
            return isStart ? LocalDateTime.now().minusDays(30) : LocalDateTime.now();
        }
    }

    public Map<String, Object> getDashboardData(String start, String end) {
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);

        List<Order> orders = orderRepository.findAllByCreatedAtBetween(startDate, endDate);
        
        // 1. Metrics
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orders.size();
        long ordersSuccess = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED).count();
        long ordersShipping = orders.stream().filter(o -> o.getStatus() == OrderStatus.ASSIGNED || o.getStatus() == OrderStatus.PICKED_UP).count();
        long ordersPending = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long ordersCancelled = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELED).count();

        long newUsers = userRepository.count();
        long newUsersToday = newUsers > 30 ? newUsers / 30 : newUsers; // Mock new users today
        long activeProducts = productRepository.count(); // Có thể filter theo active state nếu có

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRevenue", totalRevenue);
        metrics.put("revenueTrend", "+18.2%"); // Giả lập
        metrics.put("totalOrders", totalOrders);
        metrics.put("ordersTrend", "+12.5%"); // Giả lập
        metrics.put("ordersSuccess", ordersSuccess);
        metrics.put("ordersShipping", ordersShipping);
        metrics.put("ordersPending", ordersPending);
        metrics.put("ordersCancelled", ordersCancelled);
        metrics.put("newUsers", newUsers);
        metrics.put("usersTrend", "+5.4%"); // Giả lập
        metrics.put("newUsersToday", newUsersToday);
        metrics.put("activeProducts", activeProducts);

        // 2. Finance
        // Tìm admin wallet đầu tiên để lấy số liệu thực, nếu ko có thì tính giả lập từ Order
        Wallet adminWallet = walletRepository.findAll().stream().findFirst().orElse(null);
        Map<String, Object> finance = new HashMap<>();
        if (adminWallet != null) {
            finance.put("escrowBalance", adminWallet.getEscrowBalance());
            finance.put("platformCommission", adminWallet.getTotalCommission());
            finance.put("readyToPay", adminWallet.getBalance());
        } else {
            // Tính ảo
            BigDecimal escrow = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ready = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal comm = ready.multiply(new BigDecimal("0.015"));
            
            finance.put("escrowBalance", escrow);
            finance.put("platformCommission", comm);
            finance.put("readyToPay", ready);
        }

        // 3. Sales Chart (Biểu đồ doanh thu theo ngày)
        Map<String, BigDecimal> revByDate = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        log -> String.format("%d Thg %d", log.getCreatedAt().getDayOfMonth(), log.getCreatedAt().getMonthValue()),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ));

        long daysBetween = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
        if (daysBetween < 0 || daysBetween > 100) daysBetween = 30;

        List<Map<String, Object>> salesChart = new ArrayList<>();
        for (long i = daysBetween; i >= 0; i--) {
            LocalDateTime day = endDate.minusDays(i);
            String name = String.format("%d Thg %d", day.getDayOfMonth(), day.getMonthValue());
            BigDecimal uv = revByDate.getOrDefault(name, BigDecimal.ZERO);
            Map<String, Object> point = new HashMap<>();
            point.put("name", name);
            point.put("uv", uv);
            salesChart.add(point);
        }

        // 4. Top Categories
        Map<String, Long> categoryCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getProduct() != null && item.getProduct().getCategory() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory().getName(),
                        Collectors.summingLong(item -> item.getQuantity() != null ? item.getQuantity().longValue() : 1L)
                ));

        long totalItemSold = categoryCount.values().stream().mapToLong(Long::longValue).sum();

        List<Map<String, Object>> topCategories = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(4)
                .map(entry -> {
                    long count = entry.getValue();
                    long percent = totalItemSold == 0 ? 0 : (count * 100) / totalItemSold;
                    Map<String, Object> catMap = new HashMap<>();
                    catMap.put("name", entry.getKey());
                    catMap.put("orders", count);
                    catMap.put("percent", percent);
                    return catMap;
                })
                .collect(Collectors.toList());

        // 5. Shipping
        long totalShippingOrDelivered = ordersSuccess + ordersShipping + ordersCancelled;
        double rate = totalShippingOrDelivered == 0 ? 0 : (double) ordersSuccess / totalShippingOrDelivered * 100;
        
        Map<String, Object> shipping = new HashMap<>();
        shipping.put("successRate", String.format("%.1f%%", rate));
        shipping.put("avgDeliveryDays", "1.8 Ngày");
        shipping.put("delayedOrdersCount", orders.stream().filter(o -> (o.getStatus() == OrderStatus.ASSIGNED || o.getStatus() == OrderStatus.PICKED_UP) && o.getCreatedAt().isBefore(LocalDateTime.now().minusDays(2))).count());
        shipping.put("returnedOrdersCount", orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELED).count());

        // 6. Attention Orders
        List<Map<String, Object>> attentionOrders = orders.stream()
                .filter(o -> ((o.getStatus() == OrderStatus.ASSIGNED || o.getStatus() == OrderStatus.PICKED_UP) && o.getCreatedAt().isBefore(LocalDateTime.now().minusDays(2)))
                          || o.getStatus() == OrderStatus.CANCELED)
                .limit(5)
                .map(o -> {
                    Map<String, Object> ao = new HashMap<>();
                    ao.put("code", o.getCode());
                    ao.put("customer", o.getCustomerName());
                    ao.put("issue", o.getStatus() == OrderStatus.CANCELED ? "Hoàn trả" : "Quá hạn 48h");
                    ao.put("shipper", o.getShipper() != null ? o.getShipper().getDisplayName() : "Chưa có");
                    ao.put("status", o.getStatus() == OrderStatus.CANCELED ? "DISPUTE" : "DELAY");
                    return ao;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("metrics", metrics);
        response.put("finance", finance);
        response.put("salesChart", salesChart);
        response.put("topCategories", topCategories);
        response.put("shipping", shipping);
        response.put("attentionOrders", attentionOrders);
        return response;
    }
}
