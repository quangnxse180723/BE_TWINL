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
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "INFO_CHECK") AiScanType type
    ) {
        return ResponseEntity.ok(aiScannerService.scanImage(file, type));
    }

    /**
     * POST /api/v1/ai/legit-check
     * @param files up to 3 images (overall front, logo close-up, tag/label)
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
}
