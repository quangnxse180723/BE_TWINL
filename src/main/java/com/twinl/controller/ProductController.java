package com.twinl.controller;

import com.twinl.dto.request.ProductRequest;
import com.twinl.dto.response.ProductResponse;
import com.twinl.dto.response.SellerProfileResponse;
import com.twinl.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public ResponseEntity<Page<ProductResponse>> getProducts(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String brand,
			@RequestParam(required = false) String gender,
			@RequestParam(required = false) String size,
			@RequestParam(required = false) String color,
			@RequestParam(required = false) Boolean inStock,
			@RequestParam(required = false) String minPrice,
			@RequestParam(required = false) String maxPrice,
			@RequestParam(required = false) String style,
			@RequestParam(required = false) String excludeStyle,
			@RequestParam(required = false) Integer minCondition,
			@RequestParam(required = false) Integer maxCondition,
			@RequestParam(required = false) java.util.List<String> defects,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String sortBy,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int sizePage
	) {
		return ResponseEntity.ok(productService.getProducts(
				search,
				category,
				brand,
				gender,
				size,
				color,
				inStock,
				minPrice,
				maxPrice,
				style,
				excludeStyle,
				minCondition,
				maxCondition,
				defects,
				status,
				sortBy,
				page,
				sizePage
		));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
		return ResponseEntity.ok(productService.getProductById(id));
	}

	@PostMapping
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
	}

	@PostMapping("/images")
	public ResponseEntity<List<String>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
		return ResponseEntity.ok(productService.uploadProductImages(files));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProductResponse> updateProduct(
			@PathVariable Long id,
			@Valid @RequestBody ProductRequest request
	) {
		return ResponseEntity.ok(productService.updateProduct(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ProductResponse> updateProductStatus(
			@PathVariable Long id,
			@RequestParam String status
	) {
		return ResponseEntity.ok(productService.updateProductStatus(id, status));
	}

	@GetMapping("/sellers/{sellerId}/profile")
	public ResponseEntity<SellerProfileResponse> getSellerProfile(@PathVariable Long sellerId) {
		return ResponseEntity.ok(productService.getSellerProfile(sellerId));
	}
}
