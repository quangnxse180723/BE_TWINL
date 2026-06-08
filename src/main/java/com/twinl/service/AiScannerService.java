package com.twinl.service;

import com.twinl.dto.response.AiAutoFillResponse;
import com.twinl.dto.response.AiScanResultResponse;
import com.twinl.dto.response.ImageQualityCheckResponse;
import com.twinl.dto.response.LegitCheckResponse;
import com.twinl.enums.AiScanType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface AiScannerService {
    AiScanResultResponse scanImage(List<MultipartFile> files, AiScanType type);
    LegitCheckResponse checkLegit(List<MultipartFile> files);
    ImageQualityCheckResponse checkImageQuality(MultipartFile file);
    AiAutoFillResponse autoFillFromImages(List<MultipartFile> files);

    /**
     * Pre-validate một ảnh trước khi đưa vào Legit Check thật.
     * Dùng Gemini Flash (siêu nhanh) để hỏi: "Ảnh này có đúng loại [slotType] không?"
     *
     * @param file     File ảnh cần kiểm tra
     * @param slotType Loại slot: "front" | "logo" | "neckTag" | "washTag"
     * @return Map { "valid": true/false, "message": "..." }
     */
    Map<String, Object> validateImageSlot(MultipartFile file, String slotType);
}

