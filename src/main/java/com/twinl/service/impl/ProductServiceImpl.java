package com.twinl.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.twinl.dto.request.ProductRequest;
import com.twinl.dto.response.ProductResponse;
import com.twinl.entity.Category;
import com.twinl.entity.Color;
import com.twinl.entity.Product;
import com.twinl.repository.CategoryRepository;
import com.twinl.repository.ColorRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.service.ProductService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductServiceImpl implements ProductService {
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ColorRepository colorRepository;
	private final Cloudinary cloudinary;

	public ProductServiceImpl(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			ColorRepository colorRepository,
			Cloudinary cloudinary
	) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.colorRepository = colorRepository;
		this.cloudinary = cloudinary;
	}

	@Override
	public Page<ProductResponse> getProducts(
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
	) {
		Specification<Product> spec = (root, query, cb) -> cb.conjunction();

		if (search != null && !search.isBlank()) {
			String keyword = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
			spec = spec.and((root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("name")), keyword),
					cb.like(cb.lower(root.get("description")), keyword),
					cb.like(cb.lower(root.get("brand")), keyword),
					cb.like(cb.lower(root.join("category", JoinType.LEFT).get("name")), keyword)
			));
		}

		if (category != null && !category.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.join("category", JoinType.LEFT).get("name"), category));
		}

		if (brand != null && !brand.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("brand"), brand));
		}

		if (gender != null && !gender.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
		}

		if (size != null && !size.isBlank()) {
			spec = spec.and((root, query, cb) -> root.join("sizes").in(size));
		}

		if (color != null && !color.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.join("colors", JoinType.LEFT).get("name"), color));
		}

		if (inStock != null && inStock) {
			spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("stock"), 0));
		}

		Optional<BigDecimal> min = parsePrice(minPrice);
		Optional<BigDecimal> max = parsePrice(maxPrice);

		if (min.isPresent()) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min.get()));
		}

		if (max.isPresent()) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max.get()));
		}

		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return productRepository.findAll(spec, pageable).map(this::toResponse);
	}

	@Override
	public ProductResponse getProductById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
		return toResponse(product);
	}

	@Override
	public ProductResponse createProduct(ProductRequest request) {
		validateImageUrls(request.getImageUrls());
		Product product = Product.builder()
				.name(request.getName())
				.description(request.getDescription())
				.price(request.getPrice())
				.category(requireCategory(request.getCategoryId()))
				.brand(request.getBrand())
				.gender(request.getGender())
				.imageUrls(request.getImageUrls() == null ? new ArrayList<>() : request.getImageUrls())
				.status(request.getStatus() == null ? "ACTIVE" : request.getStatus())
				.style(request.getStyle())
				.stock(request.getStock())
				.sizes(request.getSizes() == null ? new java.util.HashSet<>() : request.getSizes())
				.colors(requireColors(request.getColorIds()))
				.build();

		Product saved = productRepository.save(product);
		return toResponse(saved);
	}

	@Override
	public ProductResponse updateProduct(Long id, ProductRequest request) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

		product.setName(request.getName());
		product.setDescription(request.getDescription());
		product.setPrice(request.getPrice());
		product.setCategory(requireCategory(request.getCategoryId()));
		product.setBrand(request.getBrand());
		product.setGender(request.getGender());
		if (request.getImageUrls() != null) {
			validateImageUrls(request.getImageUrls());
			product.setImageUrls(request.getImageUrls());
		}
		if (request.getStatus() != null) {
			product.setStatus(request.getStatus());
		}
		if (request.getStyle() != null) {
			product.setStyle(request.getStyle());
		}
		product.setStock(request.getStock());
		product.setSizes(request.getSizes() == null ? new java.util.HashSet<>() : request.getSizes());
		product.setColors(requireColors(request.getColorIds()));

		Product saved = productRepository.save(product);
		return toResponse(saved);
	}

	@Override
	public void deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
		}
		productRepository.deleteById(id);
	}

	@Override
	public List<String> uploadProductImages(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Files are required");
		}
		if (files.size() < 3 || files.size() > 6) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload 3 to 6 images");
		}

		List<String> urls = new ArrayList<>();
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}
			long maxSizeBytes = 10L * 1024 * 1024;
			if (file.getSize() > maxSizeBytes) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must be <= 10MB");
			}
			String contentType = file.getContentType();
			if (contentType == null || !contentType.startsWith("image/")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
			}

			try {
				Map<?, ?> uploadResult = cloudinary.uploader().upload(
						file.getBytes(),
						ObjectUtils.asMap(
								"folder", "twinl/products",
								"resource_type", "image"
						)
				);
				Object secureUrl = uploadResult.get("secure_url");
				if (secureUrl == null) {
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing upload URL");
				}
				urls.add(secureUrl.toString());
			} catch (Exception ex) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image");
			}
		}

		if (urls.size() < 3) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload 3 to 6 images");
		}
		return urls;
	}

	private ProductResponse toResponse(Product product) {
		return ProductResponse.builder()
				.id(product.getId())
				.name(product.getName())
				.description(product.getDescription())
				.price(product.getPrice())
				.categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
				.category(product.getCategory() != null ? product.getCategory().getName() : null)
				.brand(product.getBrand())
				.gender(product.getGender())
				.imageUrls(product.getImageUrls())
				.status(product.getStatus())
				.style(product.getStyle())
				.stock(product.getStock())
				.sizes(product.getSizes())
				.colorIds(product.getColors().stream().map(Color::getId).collect(Collectors.toSet()))
				.colors(product.getColors().stream().map(Color::getName).collect(Collectors.toSet()))
				.createdAt(product.getCreatedAt())
				.updatedAt(product.getUpdatedAt())
				.build();
	}

	private Category requireCategory(Long categoryId) {
		if (categoryId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category id is required");
		}
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
	}

	private Set<Color> requireColors(Set<Long> colorIds) {
		if (colorIds == null || colorIds.isEmpty()) {
			return new java.util.HashSet<>();
		}
		List<Color> colors = colorRepository.findAllById(colorIds);
		if (colors.size() != colorIds.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Color not found");
		}
		return new java.util.HashSet<>(colors);
	}

	private void validateImageUrls(List<String> imageUrls) {
		if (imageUrls == null || imageUrls.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URLs are required");
		}
		if (imageUrls.size() < 3 || imageUrls.size() > 6) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload 3 to 6 images");
		}
	}

	private Optional<BigDecimal> parsePrice(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(new BigDecimal(value.trim()));
		} catch (NumberFormatException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid price value");
		}
	}
}
