package com.twinl.repository;

import com.twinl.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	Page<Order> findAll(Pageable pageable);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	Page<Order> findByUserIdAndStatusIn(Long userId, List<com.twinl.entity.OrderStatus> statuses, Pageable pageable);

	Optional<Order> findByCode(String code);

	Optional<Order> findByCodeAndUserId(String code, Long userId);

	List<Order> findTop5ByOrderByCreatedAtDesc();
	
	List<Order> findAllByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :start AND :end")
	long countByStatusAndCreatedAtBetween(@Param("status") com.twinl.entity.OrderStatus status, @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

	Page<Order> findByShipperId(Long shipperId, Pageable pageable);

	long countByBuyerId(Long buyerId);

	@Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product.seller.id = :sellerId AND o.status = 'COMPLETED'")
	long countBySellerId(@Param("sellerId") Long sellerId);

	@Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.seller.id = :sellerId")
	Page<Order> findOrdersBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

	@Query("select coalesce(sum(o.totalAmount), 0) from Order o")
	BigDecimal sumTotalAmount();

    /** Dùng để tra cứu đơn hàng PayOS theo orderCode số (được lưu vào paymentTxnRef) */
    Optional<Order> findByPaymentTxnRef(String paymentTxnRef);

	@Query("SELECT COALESCE(SUM(i.quantity), 0L) FROM OrderItem i WHERE i.product.seller.id = :sellerId AND i.order.status IN (com.twinl.entity.OrderStatus.DELIVERED, com.twinl.entity.OrderStatus.COMPLETED)")
	long countSoldItemsBySellerId(@Param("sellerId") Long sellerId);

	@Query("SELECT COUNT(o) FROM Order o JOIN o.items i WHERE o.user.id = :userId AND i.product.seller.id = :sellerId AND o.status IN (com.twinl.entity.OrderStatus.DELIVERED, com.twinl.entity.OrderStatus.COMPLETED)")
	long countDeliveredOrdersByUserIdAndSellerId(@Param("userId") Long userId, @Param("sellerId") Long sellerId);
}
