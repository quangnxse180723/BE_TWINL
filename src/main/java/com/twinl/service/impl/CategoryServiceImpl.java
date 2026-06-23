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
		return categoryRepository.findByParentIsNull().stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	private CategoryResponse mapToResponse(com.twinl.entity.Category category) {
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.parentId(category.getParent() != null ? category.getParent().getId() : null)
				.children(category.getChildren() != null ? category.getChildren().stream().map(this::mapToResponse).collect(Collectors.toList()) : new java.util.ArrayList<>())
				.build();
	}
	@Override
	public CategoryResponse createCategory(com.twinl.dto.request.CategoryRequest request) {
		com.twinl.entity.Category category = new com.twinl.entity.Category();
		category.setName(request.getName());
		if (request.getParentId() != null) {
			com.twinl.entity.Category parent = categoryRepository.findById(request.getParentId())
					.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent category not found"));
			category.setParent(parent);
		}
		return mapToResponse(categoryRepository.save(category));
	}

	@Override
	public CategoryResponse updateCategory(Long id, com.twinl.dto.request.CategoryRequest request) {
		com.twinl.entity.Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Category not found"));
		category.setName(request.getName());
		if (request.getParentId() != null) {
			if (request.getParentId().equals(id)) {
				throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot set parent to self");
			}
			com.twinl.entity.Category parent = categoryRepository.findById(request.getParentId())
					.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent category not found"));
			category.setParent(parent);
		} else {
			category.setParent(null);
		}
		return mapToResponse(categoryRepository.save(category));
	}

	@Override
	public void deleteCategory(Long id) {
		try {
			categoryRepository.deleteById(id);
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST,
					"Không thể xóa danh mục này vì đang có sản phẩm phụ thuộc."
			);
		}
	}
}
