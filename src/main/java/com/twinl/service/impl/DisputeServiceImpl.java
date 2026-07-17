package com.twinl.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinl.dto.request.DisputeRequest;
import com.twinl.dto.response.DisputeResponse;
import com.twinl.entity.Dispute;
import com.twinl.entity.Order;
import com.twinl.entity.OrderStatus;
import com.twinl.entity.User;
import com.twinl.repository.DisputeRepository;
import com.twinl.repository.OrderRepository;
import com.twinl.service.DisputeService;
import com.twinl.service.NotificationService;
import com.twinl.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DisputeResponse createDispute(Long orderId, DisputeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (!order.getUser().getEmail().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể yêu cầu trả hàng khi đơn hàng ở trạng thái ĐÃ GIAO (DELIVERED)");
        }

        if (disputeRepository.findByOrderId(orderId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng này đã có yêu cầu trả hàng");
        }

        String imagesJson = "[]";
        if (request.getEvidenceImages() != null) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.getEvidenceImages());
            } catch (JsonProcessingException e) {
                log.error("Failed to parse images", e);
            }
        }

        Dispute dispute = Dispute.builder()
                .order(order)
                .requester(order.getUser())
                .reason(request.getReason())
                .description(request.getDescription())
                .evidenceImages(imagesJson)
                .status("PENDING")
                .build();

        dispute = disputeRepository.save(dispute);

        order.setStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);

        // Notify Seller
        notificationService.sendNotification(
            order.getItems().iterator().next().getProduct().getSeller(),
            "Yêu cầu trả hàng",
            "Người mua " + order.getUser().getDisplayName() + " đã yêu cầu trả hàng cho đơn " + order.getCode(),
            "DISPUTE"
        );

        return toResponse(dispute);
    }

    @Override
    public Page<DisputeResponse> getAllDisputes(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return disputeRepository.findAll(pageable).map(this::toResponse);
        }
        return disputeRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Override
    public DisputeResponse getDisputeById(Long id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));
        return toResponse(dispute);
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long id, String resolution, String note) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));

        if (!"PENDING".equals(dispute.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu này đã được giải quyết");
        }

        Order order = dispute.getOrder();

        if ("ACCEPT".equalsIgnoreCase(resolution)) {
            dispute.setStatus("ACCEPTED");
            order.setStatus(OrderStatus.RETURNED);
            walletService.refundBuyer(order);

            // Notify Buyer & Seller
            notificationService.sendNotification(
                order.getUser(), "Chấp nhận trả hàng", 
                "Yêu cầu trả hàng của đơn " + order.getCode() + " đã được chấp nhận. Tiền đã được hoàn vào ví của bạn.", "DISPUTE_ACCEPTED"
            );
            notificationService.sendNotification(
                order.getItems().iterator().next().getProduct().getSeller(), "Đơn hàng bị hoàn trả", 
                "Admin đã chấp nhận yêu cầu trả hàng của đơn " + order.getCode() + ". Ghi chú: " + note, "DISPUTE_ACCEPTED"
            );
            
        } else if ("REJECT".equalsIgnoreCase(resolution)) {
            dispute.setStatus("REJECTED");
            order.setStatus(OrderStatus.COMPLETED);
            
            // Giải ngân cho người bán
            try {
                walletService.releaseEscrow(order);
                order.setEscrowReleased(true);
            } catch (Exception e) {
                log.error("Lỗi giải ngân khi từ chối dispute: ", e);
            }

            notificationService.sendNotification(
                order.getUser(), "Từ chối trả hàng", 
                "Yêu cầu trả hàng của đơn " + order.getCode() + " đã bị từ chối. Ghi chú: " + note, "DISPUTE_REJECTED"
            );
            notificationService.sendNotification(
                order.getItems().iterator().next().getProduct().getSeller(), "Yêu cầu trả hàng bị từ chối", 
                "Admin đã từ chối yêu cầu trả hàng của người mua đối với đơn " + order.getCode() + ". Tiền đã được giải ngân vào ví.", "DISPUTE_REJECTED"
            );
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hành động không hợp lệ");
        }

        orderRepository.save(order);
        dispute = disputeRepository.save(dispute);
        return toResponse(dispute);
    }

    private DisputeResponse toResponse(Dispute dispute) {
        List<String> images = new ArrayList<>();
        if (dispute.getEvidenceImages() != null && !dispute.getEvidenceImages().isBlank()) {
            try {
                images = objectMapper.readValue(dispute.getEvidenceImages(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse images", e);
            }
        }

        return DisputeResponse.builder()
                .id(dispute.getId())
                .orderId(dispute.getOrder().getId())
                .orderCode(dispute.getOrder().getCode())
                .requesterName(dispute.getRequester().getDisplayName())
                .requesterEmail(dispute.getRequester().getEmail())
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .evidenceImages(images)
                .status(dispute.getStatus())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }
}
