package com.twinl.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegitCheckResponse {
    private String brand;
    private String riskLevel;  // LOW / HIGH / UNCERTAIN
    private List<String> redFlags;
    private String advice;
    private String rawData;
}
