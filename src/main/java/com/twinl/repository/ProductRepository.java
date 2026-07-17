package com.twinl.repository;

import com.twinl.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
	org.springframework.data.domain.Page<Product> findBySellerId(Long sellerId, org.springframework.data.domain.Pageable pageable);

	long countBySellerIdAndStatus(Long sellerId, String status);

	@Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND TRIM(p.brand) != '' ORDER BY p.brand")
	List<String> findDistinctBrands();
}
