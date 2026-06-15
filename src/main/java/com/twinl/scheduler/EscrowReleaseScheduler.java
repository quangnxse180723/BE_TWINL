package com.twinl.scheduler;

import com.twinl.entity.Order;
import com.twinl.entity.OrderStatus;
import com.twinl.repository.OrderRepository;
import com.twinl.service.NotificationService;
import com.twinl.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscrowReleaseScheduler {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    // Chạy ngầm định kỳ 5 phút một lần
    @Scheduled(fixedDelay = 300000)
    public void processEscrowReleases() {
        log.info("[ESCROW JOB] Bắt đầu quét các đơn hàng cần giải ngân tiền...");

        List<Order> eligibleOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .filter(o -> Boolean.FALSE.equals(o.getEscrowReleased()))
                .filter(o -> o.getDeliveredAt() != null)
                // Điều kiện: Đã qua 24h kể từ lúc giao hàng thành công
                .filter(o -> o.getDeliveredAt().plusHours(24).isBefore(LocalDateTime.now()))
                .toList();

        if (eligibleOrders.isEmpty()) {
            log.info("[ESCROW JOB] Không có đơn hàng nào cần giải ngân trong đợt này.");
            return;
        }

        for (Order order : eligibleOrders) {
            try {
                // Giải ngân tiền vào ví thực cho người bán
                walletService.releaseEscrow(order);
                
                // Đánh dấu đã giải ngân và chuyển trạng thái sang COMPLETED
                order.setEscrowReleased(true);
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                
                log.info("[ESCROW JOB] Đã giải ngân thành công cho đơn hàng: {}", order.getCode());
                
                // (Tùy chọn: Gửi thông báo cho từng người bán trong đơn, tạm thời log lại là đủ an toàn)
            } catch (Exception e) {
                log.error("[ESCROW JOB] Lỗi giải ngân cho đơn hàng {}: {}", order.getCode(), e.getMessage());
            }
        }
        
        log.info("[ESCROW JOB] Hoàn thành quét. Tổng số đơn giải ngân: {}", eligibleOrders.size());
    }
}
