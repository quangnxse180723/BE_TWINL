package com.twinl.controller;

import com.twinl.dto.response.AiAutoFillResponse;
import com.twinl.dto.response.AiScanResultResponse;
import com.twinl.dto.response.ImageQualityCheckResponse;
import com.twinl.dto.response.LegitCheckResponse;
import com.twinl.enums.AiScanType;
import com.twinl.service.AiScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiScannerController {

    private final AiScannerService aiScannerService;

    /**
     * POST /api/v1/ai/scan
     * @param file ảnh cần quét
     * @param type INFO_CHECK (tra cứu - dùng Flash) hoặc SELL_LISTING (bán hàng - dùng Pro)
     *             Mặc định là INFO_CHECK nếu không truyền
     */
    @PostMapping("/scan")
    public ResponseEntity<AiScanResultResponse> scanImage(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "type", defaultValue = "INFO_CHECK") AiScanType type
    ) {
        return ResponseEntity.ok(aiScannerService.scanImage(files, type));
    }

    /**
     * POST /api/v1/ai/legit-check
     * @param files up to 4 images (front, logo, neckTag, washTag)
     */
    @PostMapping("/legit-check")
    public ResponseEntity<LegitCheckResponse> checkLegit(
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(aiScannerService.checkLegit(files));
    }

    @PostMapping("/image-quality")
    public ResponseEntity<ImageQualityCheckResponse> checkImageQuality(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(aiScannerService.checkImageQuality(file));
    }

    @PostMapping("/autofill")
    public ResponseEntity<AiAutoFillResponse> autoFill(
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(aiScannerService.autoFillFromImages(files));
    }

    /**
     * POST /api/v1/ai/validate-slot
     *
     * AI Guard: Dùng Gemini Flash để kiểm tra xem ảnh có đúng loại không.
     * Frontend gọi ngay sau khi user chọn ảnh (before full legit check).
     *
     * @param file     File ảnh vừa chọn
     * @param slotType "front" | "logo" | "neckTag" | "washTag"
     * @return { "valid": true/false, "message": "Thông báo thân thiện" }
     */
    @PostMapping("/validate-slot")
    public ResponseEntity<Map<String, Object>> validateSlot(
            @RequestParam("file") MultipartFile file,
            @RequestParam("slotType") String slotType
    ) {
        return ResponseEntity.ok(aiScannerService.validateImageSlot(file, slotType));
    }

    /**
     * POST /api/v1/ai/classify-side
     *
     * Phân loại hình ảnh sản phẩm ("Mặt trước", "Mặt sau")
     *
     * @param file File ảnh cần kiểm tra
     * @return Dạng Map chứa thuộc tính "result" có giá trị: FRONT_VALID, BACK_VALID, hoặc INVALID_IMAGE
     */
    @PostMapping("/classify-side")
    public ResponseEntity<Map<String, String>> classifySide(
            @RequestParam("file") MultipartFile file
    ) {
        String result = aiScannerService.classifyImageSide(file);
        return ResponseEntity.ok(Map.of("result", result));
    }
}

