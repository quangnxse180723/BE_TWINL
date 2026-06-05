package com.twinl.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 120)
	private String customerName;

	@Column(nullable = false, length = 120)
	private String customerEmail;

	@Column(length = 20)
	private String customerPhone;

	@Column(length = 255)
	private String shippingAddress;

	// Giữ lại các trường địa lý để Shipper biết địa chỉ chi tiết
	@Column(length = 20)
	private String shippingWardCode;

	private Integer shippingDistrictId;

	private Integer shippingProvinceId;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private PaymentStatus paymentStatus;

	@Column(length = 50)
	private String paymentTxnRef;

	@Column(length = 50)
	private String paymentTransactionNo;

	private LocalDateTime paymentPaidAt;

	// Shipper nội bộ được gán xử lý đơn
	@ManyToOne
	@JoinColumn(name = "shipper_id")
	private User shipper;

	// Thời điểm giao hàng thành công — dùng để kích hoạt bộ đếm Escrow 48h
	private LocalDateTime deliveredAt;

	// Ghi chú từ Shipper khi cập nhật trạng thái
	@Column(length = 500)
	private String note;

	@Column(nullable = false)
	@Builder.Default
	private Boolean escrowReleased = false;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private Set<OrderItem> items = new HashSet<>();

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
