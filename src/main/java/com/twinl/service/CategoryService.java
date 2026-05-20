package com.twinl.service;

import com.twinl.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
	List<CategoryResponse> getAllCategories();
}
