package com.twinl.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinl.dto.response.AiAutoFillResponse;
import com.twinl.dto.response.AiScanResultResponse;
import com.twinl.dto.response.ImageQualityCheckResponse;
import com.twinl.dto.response.LegitCheckResponse;
import com.twinl.enums.AiScanType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.twinl.service.AiScannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.twinl.repository.CategoryRepository;
import com.twinl.repository.ColorRepository;
import com.twinl.entity.Category;
import com.twinl.entity.Color;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiScannerServiceImpl implements AiScannerService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    // Model nhanh: dùng cho INFO_CHECK (tra cứu & tìm kiếm)
    private static final String MODEL_FLASH = "gemini-3.1-flash-lite";

    // Model sâu: dùng cho SELL_LISTING (viết mô tả bán hàng)
    private static final String MODEL_PRO   = "gemini-3.1-pro";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;

    @Override
    public AiScanResultResponse scanImage(List<MultipartFile> files, AiScanType type) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            log.error("Chưa cấu hình API Key");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Tính năng AI thật yêu cầu bạn phải điền API Key (gemini.api.key) trong file cấu hình Backend.");
        }

        // Chọn model theo loại scan (y hệt logic bạn yêu cầu)
        String modelId;
        if (type == AiScanType.INFO_CHECK) {
            modelId = MODEL_FLASH; // Tốc độ siêu tốc cho tra cứu
        } else {
            modelId = MODEL_PRO;   // Phân tích sâu cho mô tả bán hàng
        }

        log.info("AI Scan type={}, model={}", type, modelId);

        try {
            String prompt = buildPrompt(type);

            List<Map<String, Object>> parts = new java.util.ArrayList<>();
            Map<String, Object> partText = new HashMap<>();
            partText.put("text", prompt);
            parts.add(partText);

            int imageCount = Math.min(files == null ? 0 : files.size(), 6);
            for (int i = 0; i < imageCount; i++) {
                MultipartFile f = files.get(i);
                String base64Image = Base64.getEncoder().encodeToString(f.getBytes());
                String mimeType = f.getContentType() != null ? f.getContentType() : "image/jpeg";

                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mimeType", mimeType);
                inlineData.put("data", base64Image);

                Map<String, Object> partImage = new HashMap<>();
                partImage.put("inlineData", inlineData);
                parts.add(partImage);
            }

            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", type == AiScanType.INFO_CHECK ? 0.1 : 0.4);
            requestBody.put("generationConfig", genConfig);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String[] fallbackModels = { modelId, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-flash-latest" };
            ResponseEntity<String> response = null;
            String lastError = "";

            for (String mId : fallbackModels) {
                try {
                    String geminiApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                            + mId
                            + ":generateContent?key=" + geminiApiKey;
                    log.info("Calling Gemini API with model {}: {}", mId, geminiApiUrl.replace(geminiApiKey, "***"));
                    response = restTemplate.postForEntity(geminiApiUrl, request, String.class);
                    break;
                } catch (Exception ex) {
                    log.warn("Lỗi gọi model {}, thử fallback. Chi tiết: {}", mId, ex.getMessage());
                    lastError = ex.getMessage();
                }
            }

            if (response == null) {
                throw new RuntimeException("Tất cả các model đều quá tải hoặc lỗi: " + lastError);
            }
            
            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            log.error("AI Scan Error", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lỗi gọi API Google Gemini: " + e.getMessage());
        }
    }

    @Override
    public LegitCheckResponse checkLegit(List<MultipartFile> files) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            log.error("Chưa cấu hình API Key");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Tính năng AI thật yêu cầu bạn phải điền API Key (gemini.api.key) trong file cấu hình Backend.");
        }

        log.info("Legit Check: {} ảnh được gửi lên", files == null ? 0 : files.size());

        String systemPromptText = "Bạn là một chuyên gia kiểm định (Authentication Expert) chuyên nghiệp cho các thương hiệu thời trang cao cấp. " +
            "Tôi cung cấp cho bạn các hình ảnh chi tiết của một sản phẩm.\n" +
            "Hãy soi kỹ Logo, Font chữ trên Tag, Đường kim mũi chỉ và tỷ lệ thiết kế. " +
            "So sánh chúng với dữ liệu về hàng chính hãng của thương hiệu này.\n" +
            "Hãy trả về JSON thuần (không markdown, không code block) theo đúng định dạng:\n" +
            "{\"brand\": \"Tên thương hiệu\", \"riskLevel\": \"LOW\", \"redFlags\": [\"danh sách điểm đáng ngờ\"], \"advice\": \"Lời khuyên cho người mua\"}\n" +
            "riskLevel chỉ được là: LOW (Rủi ro thấp/Khả năng cao là chính hãng), HIGH (Rủi ro cao/Nhiều dấu hiệu làm giả), hoặc UNCERTAIN (Không đủ dữ liệu).";

        try {
            List<Map<String, Object>> parts = new java.util.ArrayList<>();

            // Add system/user text part first
            Map<String, Object> partText = new HashMap<>();
            partText.put("text", systemPromptText);
            parts.add(partText);

            // Add up to 4 image parts
            int imageCount = Math.min(files == null ? 0 : files.size(), 4);
            for (int i = 0; i < imageCount; i++) {
                MultipartFile imgFile = files.get(i);
                String base64Image = Base64.getEncoder().encodeToString(imgFile.getBytes());
                String mimeType = imgFile.getContentType() != null ? imgFile.getContentType() : "image/jpeg";

                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mimeType", mimeType);
                inlineData.put("data", base64Image);

                Map<String, Object> partImage = new HashMap<>();
                partImage.put("inlineData", inlineData);
                parts.add(partImage);
            }

            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.1);
            requestBody.put("generationConfig", genConfig);

            String responseBody = callGeminiWithFallback(requestBody);

            return parseLegitCheckResponse(responseBody);

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Legit Check API Error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lỗi gọi API Google Gemini: " + e.getMessage());
        }
    }

    private LegitCheckResponse parseLegitCheckResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                // Strip markdown code blocks if AI ignores the instruction
                if (text.startsWith("```json")) {
                    text = text.replace("```json", "").replace("```", "").trim();
                } else if (text.startsWith("```")) {
                    text = text.replace("```", "").trim();
                }
                LegitCheckResponse result = objectMapper.readValue(text, LegitCheckResponse.class);
                result.setRawData(responseBody);
                return result;
            }
        } catch (Exception e) {
            log.warn("Lỗi parse Legit Check JSON từ AI", e);
        }

        return LegitCheckResponse.builder()
                .riskLevel("UNCERTAIN")
                .advice("Không thể phân tích được, vui lòng chụp ảnh rõ hơn")
                .rawData(responseBody)
                .build();
    }

    private String buildPrompt(AiScanType type) {
        if (type == AiScanType.INFO_CHECK) {
            return "Đây là hình ảnh một món đồ thời trang (quần áo, giày dép, túi xách...). " +
                   "Hãy phân tích và trả về CHÍNH XÁC định dạng JSON sau (không chứa markdown, chỉ trả về chuỗi JSON thô): " +
                   "{\"brand\": \"tên thương hiệu nếu có, hoặc 'Không xác định'\", " +
                   "\"material\": \"chất liệu chính\", " +
                   "\"style\": \"phong cách hoặc kiểu dáng\", " +
                   "\"estimatedPrice\": \"giá bán lẻ ước tính tiền Việt Nam, dạng chuỗi (VD: '~ 300.000 VNĐ')\"}";
        } else {
            // SELL_LISTING: prompt sâu hơn để viết mô tả bán hàng
            return "Đây là hình ảnh một món đồ thời trang secondhand. " +
                   "Hãy phân tích kỹ lưỡng và trả về CHÍNH XÁC định dạng JSON sau (không chứa markdown, chỉ trả về chuỗi JSON thô): " +
                   "{\"brand\": \"tên thương hiệu nếu có, hoặc 'Không xác định'\", " +
                   "\"material\": \"chất liệu chính\", " +
                   "\"style\": \"phong cách hoặc kiểu dáng chi tiết\", " +
                   "\"estimatedPrice\": \"giá bán lẻ ước tính tiền Việt Nam, dạng chuỗi (VD: '~ 300.000 VNĐ')\"}";
        }
    }

    private AiScanResultResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                // Loại bỏ code block markdown nếu AI vẫn trả về
                if (text.startsWith("```json")) {
                    text = text.replace("```json", "").replace("```", "").trim();
                } else if (text.startsWith("```")) {
                    text = text.replace("```", "").trim();
                }
                
                AiScanResultResponse result = objectMapper.readValue(text, AiScanResultResponse.class);
                result.setRawData(responseBody);
                return result;
            }
        } catch (Exception e) {
            log.warn("Lỗi parse JSON từ AI", e);
        }
        
        return AiScanResultResponse.builder()
                .brand("Không xác định")
                .material("Chưa rõ")
                .style("Đang phân tích...")
                .estimatedPrice("Liên hệ")
                .rawData(responseBody)
                .build();
    }

    @Override
    public ImageQualityCheckResponse checkImageQuality(MultipartFile file) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa cấu hình API Key");
        }
        String prompt = "Bạn là chuyên gia kiểm tra chất lượng ảnh sản phẩm thời trang cho sàn thương mại điện tử. " +
            "Hãy phân tích ảnh và trả về JSON thuần (không markdown, không code block) theo định dạng:\n" +
            "{\"passed\": true, \"status\": \"PASS\", \"passed_checks\": [\"Danh sách tiêu chí đạt\"], " +
            "\"failed_checks\": [\"Danh sách tiêu chí chưa đạt\"], \"advice\": \"Lời khuyên ngắn gọn\"}\n" +
            "Các tiêu chí: 1. Độ sắc nét (ảnh không bị mờ) 2. Ánh sáng đủ 3. Sản phẩm chiếm phần lớn khung hình 4. Nền ảnh gọn gàng.\n" +
            "status: PASS (tất cả đạt), WARN (1-2 chưa đạt), FAIL (3-4 chưa đạt). passed = true nếu PASS hoặc WARN.";
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64Image);
            Map<String, Object> partImage = new HashMap<>();
            partImage.put("inlineData", inlineData);
            Map<String, Object> partText = new HashMap<>();
            partText.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(partText, partImage));
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.1);
            requestBody.put("generationConfig", genConfig);
            String body = callGeminiWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(body);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                if (text.startsWith("```")) text = text.replaceAll("```json|```", "").trim();
                ImageQualityCheckResponse result = objectMapper.readValue(text, ImageQualityCheckResponse.class);
                result.setRawData(body);
                return result;
            }
        } catch (Exception e) {
            log.warn("Image quality check error", e);
        }
        return ImageQualityCheckResponse.builder()
            .passed(true).status("WARN")
            .advice("Không thể phân tích chất lượng ảnh, vui lòng thử lại")
            .rawData("").build();
    }

    @Override
    public AiAutoFillResponse autoFillFromImages(List<MultipartFile> files) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa cấu hình API Key");
        }
        String prompt = "Bạn là chuyên gia phân tích sản phẩm thời trang. Tôi cung cấp ảnh sản phẩm thời trang secondhand. " +
            "Hãy phân tích kỹ và trả về JSON thuần (không markdown) theo định dạng:\n" +
            "{\"name\": \"Tên sản phẩm ngắn gọn\", \"brand\": \"Thương hiệu hoặc Không xác định\", " +
            "\"category\": \"Tên danh mục phù hợp nhất (ví dụ: Áo thun, Áo khoác, Quần Jeans, Váy...)\", " +
            "\"style\": \"Phong cách thời trang bằng Tiếng Việt (VD: Đường phố, Cổ điển, Tối giản, Y2K, Thảo nguyên/Boho, Công sở, Năng động, Thể thao). KHÔNG dùng các từ miêu tả chung chung như 'thời thượng', 'nổi bật', 'cá tính'\", " +
            "\"gender\": \"Nam hoặc Nữ (bắt buộc phải là 1 trong 2, nếu unisex thì tuỳ kiểu dáng nghiêng về giới tính nào)\", " +
            "\"description\": \"Mô tả chi tiết tiếng Việt 2-4 câu, nêu đặc điểm nổi bật và chất liệu\", " +
            "\"estimatedPrice\": \"Số tiền VNĐ ước tính dạng số nguyên (VD: 350000)\", " +
            "\"material\": \"Chất liệu chính\", \"condition\": \"Tình trạng: Mới/Như mới/Tốt/Bình thường\", " +
            "\"conditionPercentage\": 90, " +
            "\"defects\": [\"MINT\", \"MINOR_FLAW\", \"STAINED\", \"MISSING_BUTTON\", \"TORN\", \"FADED\"], " +
            "\"color\": \"Màu sắc chủ đạo (VD: Đen, Trắng, Đỏ)\", " +
            "\"sizes\": [\"M\"], \"length\": 65, \"shoulder\": 45, \"chest\": 100, \"waist\": 0}\n" +
            "Lưu ý cho defects: Trả về một mảng chứa các mã lỗi khớp với hình ảnh. Nếu không có lỗi gì, trả về [\"MINT\"]. " +
            "Lưu ý cho conditionPercentage: Ước lượng độ mới theo phần trăm (số nguyên từ 50 đến 100). " +
            "Lưu ý cho sizes: Mảng các size dự đoán (S, M, L, XL...). Nếu không rõ trả về rỗng []. " +
            "Lưu ý cho các số đo (length, shoulder, chest, waist): Ước lượng số đo theo cm. Nếu không có thông tin hoặc không rõ, trả về 0. " +
            "Ước tính giá theo thị trường secondhand Việt Nam.";
        try {
            List<Map<String, Object>> parts = new java.util.ArrayList<>();
            Map<String, Object> partText = new HashMap<>();
            partText.put("text", prompt);
            parts.add(partText);
            int count = Math.min(files == null ? 0 : files.size(), 6);
            for (int i = 0; i < count; i++) {
                MultipartFile f = files.get(i);
                String base64 = Base64.getEncoder().encodeToString(f.getBytes());
                String mime = f.getContentType() != null ? f.getContentType() : "image/jpeg";
                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mimeType", mime);
                inlineData.put("data", base64);
                Map<String, Object> part = new HashMap<>();
                part.put("inlineData", inlineData);
                parts.add(part);
            }
            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.3);
            requestBody.put("generationConfig", genConfig);
            String body = callGeminiWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(body);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                if (text.startsWith("```")) text = text.replaceAll("```json|```", "").trim();
                AiAutoFillResponse result = objectMapper.readValue(text, AiAutoFillResponse.class);
                
                // --- Xử lý danh mục tự động ---
                if (result.getCategory() != null && !result.getCategory().trim().isEmpty()) {
                    String categoryName = result.getCategory().trim();
                    java.util.Optional<Category> optCat = categoryRepository.findAll().stream()
                            .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                            .findFirst();
                    if (optCat.isPresent()) {
                        result.setCategoryId(optCat.get().getId());
                        result.setCategory(optCat.get().getName());
                    } else {
                        // Tự động tạo danh mục mới
                        Category newCat = new Category();
                        newCat.setName(categoryName);

                        // Phân loại danh mục cha (Nam, Nữ, Thể thao)
                        String gender = result.getGender() != null ? result.getGender().trim().toLowerCase() : "";
                        String style = result.getStyle() != null ? result.getStyle().trim().toLowerCase() : "";
                        String parentName = "Nữ"; // Fallback mặc định
                        
                        if (style.contains("thể thao") || style.contains("sport")) {
                            parentName = "Thể thao";
                        } else if (gender.contains("nam") && !gender.contains("nữ")) {
                            parentName = "Nam";
                        } else if (gender.contains("nữ") || gender.contains("nu")) {
                            parentName = "Nữ";
                        }

                        if (parentName != null) {
                            final String pName = parentName;
                            java.util.Optional<Category> parentOpt = categoryRepository.findAll().stream()
                                    .filter(c -> c.getName().equalsIgnoreCase(pName) && c.getParent() == null)
                                    .findFirst();
                            if (parentOpt.isPresent()) {
                                newCat.setParent(parentOpt.get());
                            } else {
                                Category newParent = new Category();
                                newParent.setName(pName);
                                categoryRepository.save(newParent);
                                newCat.setParent(newParent);
                            }
                        }

                        categoryRepository.save(newCat);
                        result.setCategoryId(newCat.getId());
                        result.setCategory(newCat.getName());
                    }
                }
                
                // --- Xử lý màu sắc tự động ---
                if (result.getColor() != null && !result.getColor().trim().isEmpty()) {
                    String[] colorParts = result.getColor().split(",|\\s+và\\s+|-|/|&");
                    java.util.List<Long> cIds = new java.util.ArrayList<>();
                    java.util.List<String> cNames = new java.util.ArrayList<>();
                    
                    for (String part : colorParts) {
                        String cName = part.trim();
                        if (cName.isEmpty()) continue;
                        
                        // Viết hoa chữ cái đầu
                        cName = cName.substring(0, 1).toUpperCase() + cName.substring(1).toLowerCase();
                        
                        final String finalCName = cName;
                        java.util.Optional<Color> optColor = colorRepository.findAll().stream()
                                .filter(c -> c.getName().equalsIgnoreCase(finalCName))
                                .findFirst();
                                
                        if (optColor.isPresent()) {
                            cIds.add(optColor.get().getId());
                            cNames.add(optColor.get().getName());
                        } else {
                            Color newColor = new Color();
                            newColor.setName(finalCName);
                            colorRepository.save(newColor);
                            cIds.add(newColor.getId());
                            cNames.add(newColor.getName());
                        }
                    }
                    result.setColorIds(cIds);
                    result.setColorNames(cNames);
                }
                
                result.setRawData(body);
                return result;
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("AutoFill error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lỗi gọi API Google Gemini: " + e.getMessage());
        }
        return AiAutoFillResponse.builder().name("").brand("").description("").build();
    }

    /** Helper: gọi Gemini với fallback model chain, trả về response body dạng String */
    private String callGeminiWithFallback(Map<String, Object> requestBody) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String[] fallbackModels = { MODEL_FLASH, "gemini-3.1-flash-lite", "gemini-2.0-flash", "gemini-1.5-flash-latest" };
        String lastError = "";
        for (String mId : fallbackModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + mId + ":generateContent?key=" + geminiApiKey;
                log.info("Calling Gemini with model {}", mId);
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                return response.getBody();
            } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
                String errorBody = httpEx.getResponseBodyAsString();
                log.warn("Model {} failed with HTTP {}: {}", mId, httpEx.getStatusCode(), errorBody);
                lastError = "HTTP " + httpEx.getStatusCode() + ": " + errorBody;
            } catch (Exception ex) {
                log.warn("Model {} failed: {}", mId, ex.getMessage());
                lastError = ex.getMessage();
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tất cả Gemini model đều quá tải hoặc lỗi: " + lastError);
    }

    // ════════════════════════════════════════════════════════════════
    //  AI GUARD: Pre-validate ảnh upload theo từng slot
    //  Dùng Gemini Flash (siêu nhanh ~1-2s) để chặn ảnh rác
    // ════════════════════════════════════════════════════════════════

    /** Mô tả tiếng Việt cho từng slot – dùng trong prompt gửi cho AI */
    private static final Map<String, String> SLOT_DESCRIPTIONS = Map.of(
        "front",   "hình ảnh mặt trước của một sản phẩm thời trang (áo, quần, váy, túi xách, giày...). Bạn hãy dễ tính, chỉ cần có quần áo/phụ kiện thì trả lời YES",
        "back",    "hình ảnh mặt sau của một sản phẩm thời trang (lưu ý: đối với một số loại áo, váy, mặt sau có thể trông rất giống mặt trước. Hãy dễ tính, nếu là quần áo thì cứ trả lời YES)",
        "tag",     "hình ảnh có chứa mác thương hiệu, logo hoặc mác size của sản phẩm thời trang",
        "opt1",    "bất kỳ hình ảnh nào có liên quan đến quần áo, giày dép, phụ kiện thời trang. Hãy dễ tính, cứ có quần áo là trả lời YES",
        "opt2",    "bất kỳ hình ảnh nào có liên quan đến quần áo, giày dép, phụ kiện thời trang. Hãy dễ tính, cứ có quần áo là trả lời YES",
        "opt3",    "bất kỳ hình ảnh nào có liên quan đến quần áo, giày dép, phụ kiện thời trang. Hãy dễ tính, cứ có quần áo là trả lời YES"
    );

    /** Thông báo lỗi thân thiện khi ảnh sai slot */
    private static final Map<String, String> SLOT_ERROR_MESSAGES = Map.of(
        "front",   "Ảnh ô số 1 chưa đúng! Vui lòng chụp rõ mặt trước sản phẩm.",
        "back",    "Ảnh ô số 2 chưa đúng! Vui lòng chụp mặt sau sản phẩm.",
        "tag",     "Ảnh ô số 3 chưa đúng! Vui lòng chụp cận cảnh mác thương hiệu, logo hoặc size.",
        "opt1",    "Ảnh phụ 1 chưa rõ ràng.",
        "opt2",    "Ảnh phụ 2 chưa rõ ràng.",
        "opt3",    "Ảnh phụ 3 chưa rõ ràng."
    );

    @Override
    public Map<String, Object> validateImageSlot(MultipartFile file, String slotType) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            // Không có API key → cho qua để không block người dùng
            log.warn("[AI Guard] Không có Gemini API key, bỏ qua pre-validation cho slot: {}", slotType);
            return Map.of("valid", true, "message", "Ảnh được chấp nhận.");
        }

        String slotDesc = SLOT_DESCRIPTIONS.getOrDefault(slotType,
            "ảnh sản phẩm thời trang rõ nét");

        String prompt = String.format(
            "Nhìn vào ảnh này. Nhiệm vụ của bạn là kiểm duyệt ảnh rác. " +
            "Đây có phải là %s không?\n" +
            "Chỉ trả lời đúng 1 từ: YES hoặc NO.",
            slotDesc
        );

        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64Image);

            Map<String, Object> partImage = new HashMap<>();
            partImage.put("inlineData", inlineData);

            Map<String, Object> partText = new HashMap<>();
            partText.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(partText, partImage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            // Dùng temperature = 0 để AI trả lời chắc chắn YES/NO
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.0);
            genConfig.put("maxOutputTokens", 5); // Chỉ cần 1 từ
            requestBody.put("generationConfig", genConfig);

            String responseBody = callGeminiWithFallback(requestBody);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String answer = candidates.get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText("NO").trim().toUpperCase();

                boolean isValid = answer.startsWith("YES");
                log.info("[AI Guard] Slot={}, Answer='{}', Valid={}", slotType, answer, isValid);

                if (isValid) {
                    return Map.of("valid", true, "message", "Ảnh hợp lệ.");
                } else {
                    String errorMsg = SLOT_ERROR_MESSAGES.getOrDefault(slotType,
                        "Ảnh chưa đúng yêu cầu, vui lòng chụp lại.");
                    return Map.of("valid", false, "message", errorMsg);
                }
            }
        } catch (ResponseStatusException rse) {
            // Nếu AI service down → cho qua, không block người dùng
            log.warn("[AI Guard] AI service lỗi, bỏ qua validation: {}", rse.getMessage());
            return Map.of("valid", true, "message", "Bỏ qua kiểm tra (AI tạm thời không khả dụng).");
        } catch (Exception e) {
            log.warn("[AI Guard] Lỗi validate slot {}: {}", slotType, e.getMessage());
        }
        return Map.of("valid", true, "message", "Bỏ qua kiểm tra (lỗi hệ thống).");
    }

    @Override
    public String classifyImageSide(MultipartFile file) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            log.warn("[AI Classify] Không có Gemini API key, mặc định trả về FRONT_VALID");
            return "FRONT_VALID";
        }

        String prompt = "Vai trò (Role): Bạn là một AI chuyên gia kiểm định hình ảnh sản phẩm thời trang. Nhiệm vụ của bạn là phân loại hình ảnh người dùng tải lên thuộc phần nào của sản phẩm (\"Mặt trước\", \"Mặt sau\").\n" +
                "\n" +
                "Hướng dẫn cốt lõi (Core Instructions):\n" +
                "\n" +
                "Chấp nhận đa dạng ngữ cảnh (Context Flexibility): Sản phẩm có thể được chụp trải phẳng (flat-lay) HOẶC đang được mặc trên người người mẫu (on-body). Cả hai trường hợp đều hợp lệ. TUYỆT ĐỐI KHÔNG từ chối hình ảnh chỉ vì có sự xuất hiện của cơ thể người.\n" +
                "Bỏ qua yếu tố nhiễu (Noise Tolerance): Bỏ qua các nếp nhăn tự nhiên của vải, độ bóng của ánh đèn flash, hoặc việc khóa kéo chưa được kéo lên kín cổ. Không đánh rớt ảnh nếu tay người mẫu hoặc nếp gấp che khuất một phần nhỏ (dưới 20%) bề mặt áo.\n" +
                "Tiêu chí nhận diện Mặt trước (Front Recognition): Xác nhận là \"Mặt trước\" nếu hình ảnh có các đặc điểm sau:\n" +
                "Nhìn thấy phần ngực, bụng của người mặc (nếu ảnh on-body).\n" +
                "Nhìn thấy cổ áo phía trước, cằm hoặc mặt người mặc.\n" +
                "Có khóa kéo (zipper) ở giữa, hàng khuy áo, hoặc các chi tiết túi ngực, logo trước ngực.\n" +
                "Hiển thị rõ tối thiểu 70% tổng thể phần thân trước.\n" +
                "Tiêu chí nhận diện Mặt sau (Back Recognition): Xác nhận là \"Mặt sau\" nếu hình ảnh có các đặc điểm sau:\n" +
                "Nhìn thấy phần lưng, gáy hoặc phía sau đầu của người mặc.\n" +
                "Bề mặt thân áo liền mạch (không bị chia đôi bởi khóa kéo/khuy).\n" +
                "Có mũ (hood) rũ xuống lưng, hoặc các logo/đường may đặc trưng phía sau lưng áo.\n" +
                "Định dạng Đầu ra (Output Format): Chỉ trả về một trong 3 kết quả sau (không giải thích thêm):\n" +
                "\n" +
                "FRONT_VALID (Nhận diện thành công mặt trước)\n" +
                "BACK_VALID (Nhận diện thành công mặt sau)\n" +
                "INVALID_IMAGE (Chỉ dùng khi: Không phải quần áo / Quá mờ / Bị che khuất quá 50% không thể nhận diện).";

        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64Image);

            Map<String, Object> partImage = new HashMap<>();
            partImage.put("inlineData", inlineData);

            Map<String, Object> partText = new HashMap<>();
            partText.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(partText, partImage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            // Dùng temperature = 0 để AI trả lời chắc chắn format output
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.0);
            genConfig.put("maxOutputTokens", 10); 
            requestBody.put("generationConfig", genConfig);

            String responseBody = callGeminiWithFallback(requestBody);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String answer = candidates.get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText("INVALID_IMAGE").trim().toUpperCase();

                log.info("[AI Classify] Answer='{}'", answer);

                if (answer.contains("FRONT_VALID")) return "FRONT_VALID";
                if (answer.contains("BACK_VALID")) return "BACK_VALID";
                return "INVALID_IMAGE";
            }
        } catch (Exception e) {
            log.warn("[AI Classify] Lỗi classify image: {}", e.getMessage());
        }
        return "FRONT_VALID"; // Fallback to FRONT_VALID on error
    }
}
