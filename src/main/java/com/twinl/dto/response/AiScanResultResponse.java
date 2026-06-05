package com.twinl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScanResultResponse {
    private String brand;
    private String material;
    private String style;
    private String estimatedPrice;
    private String rawData; // Để debug nếu cần
}
