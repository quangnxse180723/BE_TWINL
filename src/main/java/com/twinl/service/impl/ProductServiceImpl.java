package com.twinl.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.twinl.dto.request.ProductRequest;
import com.twinl.dto.response.ProductResponse;
import com.twinl.dto.response.SellerProfileResponse;
import com.twinl.entity.Category;
import com.twinl.entity.Color;
import com.twinl.entity.Product;
import com.twinl.entity.User;
import com.twinl.repository.CategoryRepository;
import com.twinl.repository.ColorRepository;
import com.twinl.repository.OrderRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.repository.ShopReviewRepository;
import com.twinl.repository.UserRepository;
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
	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final ShopReviewRepository shopReviewRepository;
	private final Cloudinary cloudinary;
	private final com.twinl.service.NotificationService notificationService;

	public ProductServiceImpl(
			ProductRepository productRepository,
			CategoryRepository categoryRepository,
			ColorRepository colorRepository,
			UserRepository userRepository,
			OrderRepository orderRepository,
			ShopReviewRepository shopReviewRepository,
			Cloudinary cloudinary,
			com.twinl.service.NotificationService notificationService
	) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.colorRepository = colorRepository;
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.shopReviewRepository = shopReviewRepository;
		this.cloudinary = cloudinary;
		this.notificationService = notificationService;
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
			String style,
			String excludeStyle,
			Integer minCondition,
			Integer maxCondition,
			java.util.List<String> defects,
			String status,
			String sortBy,
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
			spec = spec.and((root, query, cb) -> {
				jakarta.persistence.criteria.Join<Object, Object> categoryJoin = root.join("category", jakarta.persistence.criteria.JoinType.LEFT);
				jakarta.persistence.criteria.Join<Object, Object> parentJoin = categoryJoin.join("parent", jakarta.persistence.criteria.JoinType.LEFT);
				return cb.or(
						cb.equal(categoryJoin.get("name"), category),
						cb.equal(parentJoin.get("name"), category)
				);
			});
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

		if (style != null && !style.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("style"), style));
		}

		if (excludeStyle != null && !excludeStyle.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.or(
					cb.notEqual(root.get("style"), excludeStyle),
					cb.isNull(root.get("style"))
			));
		}

		if (minCondition != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("conditionPercentage"), minCondition));
		}

		if (maxCondition != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("conditionPercentage"), maxCondition));
		}

		if (defects != null && !defects.isEmpty()) {
			List<com.twinl.entity.DefectType> defectTypes = defects.stream()
					.map(d -> {
						try {
							return com.twinl.entity.DefectType.valueOf(d);
						} catch (IllegalArgumentException e) {
							return null;
						}
					})
					.filter(java.util.Objects::nonNull)
					.toList();
			if (!defectTypes.isEmpty()) {
				spec = spec.and((root, query, cb) -> root.join("defects").in(defectTypes));
			}
		}

		if (status != null && !status.isBlank()) {
			if (!"ALL".equalsIgnoreCase(status)) {
				spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toUpperCase()));
			}
		} else {
			spec = spec.and((root, query, cb) -> cb.or(
					cb.equal(root.get("status"), "ACTIVE"),
					cb.isNull(root.get("status"))
			));
		}

		Sort sort = Sort.by("createdAt").descending();
		if ("price_asc".equals(sortBy)) {
			sort = Sort.by("price").ascending();
		} else if ("price_desc".equals(sortBy)) {
			sort = Sort.by("price").descending();
		} else if ("newest".equals(sortBy)) {
			sort = Sort.by("createdAt").descending();
		}

		PageRequest pageable = PageRequest.of(page, sizePage, sort);
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
				.conditionPercentage(request.getConditionPercentage() != null ? request.getConditionPercentage() : 100)
				.length(request.getLength())
				.shoulder(request.getShoulder())
				.chest(request.getChest())
				.waist(request.getWaist())
				.defects(request.getDefects() == null ? new java.util.HashSet<>() : request.getDefects())
				.build();

		Product saved = productRepository.save(product);
		return toResponse(saved);
	}

	@Override
	public ProductResponse createSellerProduct(ProductRequest request, String username) {
		User seller = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

		validateImageUrls(request.getImageUrls());
		Product product = Product.builder()
				.name(request.getName())
				.description(request.getDescription())
				.price(request.getPrice())
				.category(requireCategory(request.getCategoryId()))
				.brand(request.getBrand())
				.gender(request.getGender())
				.imageUrls(request.getImageUrls() == null ? new ArrayList<>() : request.getImageUrls())
				.status("PENDING")
				.style(request.getStyle())
				.stock(request.getStock())
				.sizes(request.getSizes() == null ? new java.util.HashSet<>() : request.getSizes())
				.colors(requireColors(request.getColorIds()))
				.conditionPercentage(request.getConditionPercentage() != null ? request.getConditionPercentage() : 100)
				.length(request.getLength())
				.shoulder(request.getShoulder())
				.chest(request.getChest())
				.waist(request.getWaist())
				.defects(request.getDefects() == null ? new java.util.HashSet<>() : request.getDefects())
				.seller(seller)
				.build();

		Product saved = productRepository.save(product);

		java.util.List<User> admins = userRepository.findByRoles_Name(com.twinl.entity.RoleName.ADMIN);
		for (User admin : admins) {
			notificationService.sendNotification(admin, "Sản phẩm mới chờ duyệt", "Người bán " + seller.getDisplayName() + " vừa đăng sản phẩm mới: " + saved.getName(), "NEW_PRODUCT_PENDING");
		}

		return toResponse(saved);
	}

	@Override
	public Page<ProductResponse> getProductsBySeller(String username, int page, int sizePage) {
		User seller = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));
		PageRequest pageable = PageRequest.of(page, sizePage, Sort.by("createdAt").descending());
		return productRepository.findBySellerId(seller.getId(), pageable).map(this::toResponse);
	}

	@Override
	public ProductResponse updateProductStatus(Long id, String status) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
		
		product.setStatus(status.toUpperCase());
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
		product.setConditionPercentage(request.getConditionPercentage() != null ? request.getConditionPercentage() : 100);
		product.setLength(request.getLength());
		product.setShoulder(request.getShoulder());
		product.setChest(request.getChest());
		product.setWaist(request.getWaist());
		product.setDefects(request.getDefects() == null ? new java.util.HashSet<>() : request.getDefects());

		Product updated = productRepository.save(product);
		return toResponse(updated);
	}

	@Override
	public void deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
		}
		productRepository.deleteById(id);
	}

	@Override
	public SellerProfileResponse getSellerProfile(Long sellerId) {
		User seller = userRepository.findById(sellerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));
		long productCount = productRepository.countBySellerIdAndStatus(sellerId, "ACTIVE");
		long soldCount = orderRepository.countSoldItemsBySellerId(sellerId);
		
		Double averageRating = shopReviewRepository.getAverageRatingByShopId(sellerId);
		if (averageRating == null) {
			averageRating = 0.0;
		}
		long reviewCount = shopReviewRepository.countByShopId(sellerId);

		return SellerProfileResponse.builder()
				.id(seller.getId())
				.displayName(seller.getDisplayName())
				.avatarUrl(seller.getAvatarUrl())
				.productCount(productCount)
				.soldCount(soldCount)
				.averageRating(averageRating)
				.reviewCount(reviewCount)
				.build();
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
				.sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
				.sellerName(product.getSeller() != null ? product.getSeller().getDisplayName() : null)
				.sellerAvatarUrl(product.getSeller() != null ? product.getSeller().getAvatarUrl() : null)
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
				.colorIds(product.getColors().stream().map(c -> c.getId()).collect(Collectors.toSet()))
				.colors(product.getColors().stream().map(c -> c.getName()).collect(Collectors.toSet()))
				.conditionPercentage(product.getConditionPercentage())
				.length(product.getLength())
				.shoulder(product.getShoulder())
				.chest(product.getChest())
				.waist(product.getWaist())
				.defects(product.getDefects())
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
