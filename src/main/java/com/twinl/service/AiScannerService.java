package com.twinl.service;

import com.twinl.dto.response.AiAutoFillResponse;
import com.twinl.dto.response.AiScanResultResponse;
import com.twinl.dto.response.ImageQualityCheckResponse;
import com.twinl.dto.response.LegitCheckResponse;
import com.twinl.enums.AiScanType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AiScannerService {
    AiScanResultResponse scanImage(MultipartFile file, AiScanType type);
    LegitCheckResponse checkLegit(List<MultipartFile> files);
    ImageQualityCheckResponse checkImageQuality(MultipartFile file);
    AiAutoFillResponse autoFillFromImages(List<MultipartFile> files);
}
