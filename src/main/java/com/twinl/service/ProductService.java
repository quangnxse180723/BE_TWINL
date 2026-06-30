package com.twinl.service;

import com.twinl.dto.request.ProductRequest;
import com.twinl.dto.response.ProductResponse;
import com.twinl.dto.response.SellerProfileResponse;
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
			String style,
			String excludeStyle,
			Integer minCondition,
			Integer maxCondition,
			java.util.List<String> defects,
			String status,
			String sortBy,
			int page,
			int sizePage
	);

	ProductResponse getProductById(Long id);
	ProductResponse createProduct(ProductRequest request);
	ProductResponse createSellerProduct(ProductRequest request, String username);
	Page<ProductResponse> getProductsBySeller(String username, int page, int sizePage);
	ProductResponse updateProduct(Long id, ProductRequest request);
	ProductResponse updateProductStatus(Long id, String status);
	void deleteProduct(Long id);
	List<String> uploadProductImages(List<MultipartFile> files);
	SellerProfileResponse getSellerProfile(Long sellerId);
}
