package com.twinl.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DisputeResponse {
    private Long id;
    private Long orderId;
    private String orderCode;
    private String requesterName;
    private String requesterEmail;
    private String reason;
    private String description;
    private List<String> evidenceImages;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
