package com.twinl.dto.response;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImageQualityCheckResponse {
    private boolean passed;
    private String status;               // PASS / WARN / FAIL
    private List<String> passed_checks;
    private List<String> failed_checks;
    private String advice;
    private String rawData;
}
