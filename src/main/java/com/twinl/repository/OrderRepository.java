package com.twinl.repository;

import com.twinl.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	Page<Order> findAll(Pageable pageable);

	List<Order> findTop5ByOrderByCreatedAtDesc();

	@Query("select coalesce(sum(o.totalAmount), 0) from Order o")
	BigDecimal sumTotalAmount();
}
