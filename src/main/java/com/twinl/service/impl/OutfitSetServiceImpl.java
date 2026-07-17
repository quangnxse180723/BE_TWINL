package com.twinl.service.impl;

import com.twinl.dto.request.OutfitSetRequest;
import com.twinl.dto.response.OutfitSetResponse;
import com.twinl.entity.OutfitSet;
import com.twinl.entity.OutfitSetItem;
import com.twinl.entity.Product;
import com.twinl.repository.OutfitSetRepository;
import com.twinl.repository.ProductRepository;
import com.twinl.service.OutfitSetService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OutfitSetServiceImpl implements OutfitSetService {

	private final OutfitSetRepository outfitSetRepository;
	private final ProductRepository productRepository;

	public OutfitSetServiceImpl(OutfitSetRepository outfitSetRepository,
			ProductRepository productRepository) {
		this.outfitSetRepository = outfitSetRepository;
		this.productRepository = productRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<OutfitSetResponse> getAllActive() {
		return outfitSetRepository.findByActiveTrueOrderByCreatedAtDesc()
				.stream().map(this::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<OutfitSetResponse> getAll() {
		return outfitSetRepository.findAll().stream()
				.map(this::toResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public OutfitSetResponse getById(Long id) {
		OutfitSet set = outfitSetRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bộ set id=" + id));
		return toResponse(set);
	}

	@Override
	public OutfitSetResponse create(OutfitSetRequest request) {
		OutfitSet set = OutfitSet.builder()
				.name(request.getName())
				.description(request.getDescription())
				.coverImageUrl(request.getCoverImageUrl())
				.styleTag(request.getStyleTag())
				.discountTwoItems(request.getDiscountTwoItems() != null ? request.getDiscountTwoItems() : 5)
				.discountThresholdLow(request.getDiscountThresholdLow() != null ? request.getDiscountThresholdLow() : 8)
				.discountThresholdHigh(request.getDiscountThresholdHigh() != null ? request.getDiscountThresholdHigh() : 10)
				.active(request.getActive() != null ? request.getActive() : true)
				.build();

		buildItems(set, request.getItems());
		return toResponse(outfitSetRepository.save(set));
	}

	@Override
	public OutfitSetResponse update(Long id, OutfitSetRequest request) {
		OutfitSet set = outfitSetRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bộ set id=" + id));

		set.setName(request.getName());
		set.setDescription(request.getDescription());
		set.setCoverImageUrl(request.getCoverImageUrl());
		set.setStyleTag(request.getStyleTag());
		if (request.getDiscountTwoItems() != null) set.setDiscountTwoItems(request.getDiscountTwoItems());
		if (request.getDiscountThresholdLow() != null) set.setDiscountThresholdLow(request.getDiscountThresholdLow());
		if (request.getDiscountThresholdHigh() != null) set.setDiscountThresholdHigh(request.getDiscountThresholdHigh());
		if (request.getActive() != null) set.setActive(request.getActive());

		// Replace all items
		set.getItems().clear();
		buildItems(set, request.getItems());

		return toResponse(outfitSetRepository.save(set));
	}

	@Override
	public void delete(Long id) {
		OutfitSet set = outfitSetRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bộ set id=" + id));
		outfitSetRepository.delete(set);
	}

	@Override
	public OutfitSetResponse toggleActive(Long id) {
		OutfitSet set = outfitSetRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bộ set id=" + id));
		set.setActive(!set.getActive());
		return toResponse(outfitSetRepository.save(set));
	}

	// ── Helpers ──────────────────────────────────────────────────────────────────

	private void buildItems(OutfitSet set, List<OutfitSetRequest.OutfitSetItemRequest> itemRequests) {
		if (itemRequests == null) return;
		int order = 0;
		for (OutfitSetRequest.OutfitSetItemRequest ir : itemRequests) {
			Product product = productRepository.findById(ir.getProductId())
					.orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm id=" + ir.getProductId()));
			OutfitSetItem item = OutfitSetItem.builder()
					.outfitSet(set)
					.product(product)
					.role(ir.getRole())
					.displayOrder(ir.getDisplayOrder() != null ? ir.getDisplayOrder() : order)
					.build();
			set.getItems().add(item);
			order++;
		}
	}

	private OutfitSetResponse toResponse(OutfitSet set) {
		BigDecimal totalPrice = set.getItems().stream()
				.map(i -> i.getProduct().getPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		List<OutfitSetResponse.OutfitSetItemResponse> itemResponses = set.getItems().stream()
				.map(item -> {
					Product p = item.getProduct();
					String imageUrl = (p.getImageUrls() != null && !p.getImageUrls().isEmpty())
							? p.getImageUrls().get(0) : null;
					return OutfitSetResponse.OutfitSetItemResponse.builder()
							.id(item.getId())
							.productId(p.getId())
							.productName(p.getName())
							.productBrand(p.getBrand())
							.productPrice(p.getPrice())
							.productImageUrl(imageUrl)
							.productStatus(p.getStatus())
							.productStock(p.getStock())
							.role(item.getRole())
							.displayOrder(item.getDisplayOrder())
							.build();
				})
				.collect(Collectors.toList());

		return OutfitSetResponse.builder()
				.id(set.getId())
				.name(set.getName())
				.description(set.getDescription())
				.coverImageUrl(set.getCoverImageUrl())
				.styleTag(set.getStyleTag())
				.discountTwoItems(set.getDiscountTwoItems())
				.discountThresholdLow(set.getDiscountThresholdLow())
				.discountThresholdHigh(set.getDiscountThresholdHigh())
				.discountPriceThreshold(set.getDiscountPriceThreshold())
				.active(set.getActive())
				.itemCount(set.getItems().size())
				.totalPrice(totalPrice)
				.createdAt(set.getCreatedAt())
				.items(itemResponses)
				.build();
	}
}
