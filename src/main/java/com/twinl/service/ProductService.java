package com.twinl.service;

import com.twinl.dto.request.ProductRequest;
import com.twinl.dto.response.ProductResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
	Page<ProductResponse> getProducts(
			String search,
			String category,
			String brand,
			String gender,
			String size,
			String color,
			Boolean inStock,
			String minPrice,
			String maxPrice,
			int page,
			int sizePage
	);

	ProductResponse getProductById(Long id);
	ProductResponse createProduct(ProductRequest request);
	ProductResponse updateProduct(Long id, ProductRequest request);
	void deleteProduct(Long id);
	List<String> uploadProductImages(List<MultipartFile> files);
}
