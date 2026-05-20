package com.twinl.service.impl;

import com.twinl.dto.response.CategoryResponse;
import com.twinl.repository.CategoryRepository;
import com.twinl.service.CategoryService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {
	private final CategoryRepository categoryRepository;

	public CategoryServiceImpl(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Override
	public List<CategoryResponse> getAllCategories() {
		return categoryRepository.findAll().stream()
				.map(category -> CategoryResponse.builder()
						.id(category.getId())
						.name(category.getName())
						.build())
				.collect(Collectors.toList());
	}
}
