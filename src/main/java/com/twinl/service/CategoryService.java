package com.twinl.service;

import com.twinl.dto.response.CategoryResponse;
import java.util.List;

import com.twinl.dto.request.CategoryRequest;

public interface CategoryService {
	List<CategoryResponse> getAllCategories();
	CategoryResponse createCategory(CategoryRequest request);
	CategoryResponse updateCategory(Long id, CategoryRequest request);
	void deleteCategory(Long id);
}
