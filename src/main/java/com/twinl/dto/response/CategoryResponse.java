package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {
	private Long id;
	private String name;
	private Long parentId;
	private java.util.List<CategoryResponse> children;
}
