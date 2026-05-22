package com.twinl.repository;

import com.twinl.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	Page<Order> findAll(Pageable pageable);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	Optional<Order> findByCode(String code);

	Optional<Order> findByCodeAndUserId(String code, Long userId);

	List<Order> findTop5ByOrderByCreatedAtDesc();

	@Query("select coalesce(sum(o.totalAmount), 0) from Order o")
	BigDecimal sumTotalAmount();
}
