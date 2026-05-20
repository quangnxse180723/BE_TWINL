package com.twinl.repository;

import com.twinl.dto.response.TopProductResponse;
import com.twinl.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
	@Query(
			"select new com.twinl.dto.response.TopProductResponse(p.id, p.name, sum(oi.quantity)) " +
			"from OrderItem oi join oi.product p " +
			"group by p.id, p.name " +
			"order by sum(oi.quantity) desc"
	)
	List<TopProductResponse> findTopProducts();
}
