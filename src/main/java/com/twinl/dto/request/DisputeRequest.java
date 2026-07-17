package com.twinl.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class DisputeRequest {
    @NotBlank(message = "Lý do không được để trống")
    private String reason;

    @NotBlank(message = "Mô tả chi tiết không được để trống")
    private String description;

    private List<String> evidenceImages;
}
