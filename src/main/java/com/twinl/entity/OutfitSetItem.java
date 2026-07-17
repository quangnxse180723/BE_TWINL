package com.twinl.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "outfit_set_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitSetItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "outfit_set_id", nullable = false)
	private OutfitSet outfitSet;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	// Vai trò trong set: "áo", "quần", "túi", "mũ", "giày", "phụ kiện"...
	@Column(length = 50)
	private String role;

	// Thứ tự hiển thị trong set
	@Column(name = "display_order", nullable = false)
	@Builder.Default
	private Integer displayOrder = 0;
}
