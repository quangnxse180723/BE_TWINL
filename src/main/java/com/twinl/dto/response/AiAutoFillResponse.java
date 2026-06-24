package com.twinl.dto.response;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AiAutoFillResponse {
    private String name;
    private String category;
    private Long categoryId;
    private String brand;
    private String style;
    private String gender;
    private String description;
    private String estimatedPrice;
    private String material;
    private String condition;
    private Integer conditionPercentage;
    private java.util.List<String> defects;
    private String color;
    private java.util.List<Long> colorIds;
    private java.util.List<String> colorNames;
    private java.util.List<String> sizes;
    private Float length;
    private Float shoulder;
    private Float chest;
    private Float waist;
    private String rawData;
}
