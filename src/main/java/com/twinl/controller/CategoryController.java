package com.twinl.controller;

import com.twinl.dto.response.CategoryResponse;
import com.twinl.service.CategoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public ResponseEntity<List<CategoryResponse>> getCategories() {
		return ResponseEntity.ok(categoryService.getAllCategories());
	}

	@org.springframework.web.bind.annotation.PostMapping
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CategoryResponse> createCategory(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.twinl.dto.request.CategoryRequest request) {
		return ResponseEntity.ok(categoryService.createCategory(request));
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CategoryResponse> updateCategory(@org.springframework.web.bind.annotation.PathVariable Long id, @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.twinl.dto.request.CategoryRequest request) {
		return ResponseEntity.ok(categoryService.updateCategory(id, request));
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteCategory(@org.springframework.web.bind.annotation.PathVariable Long id) {
		categoryService.deleteCategory(id);
		return ResponseEntity.ok().build();
	}
}
