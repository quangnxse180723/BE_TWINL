package com.twinl.repository;

import com.twinl.entity.OutfitSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutfitSetRepository extends JpaRepository<OutfitSet, Long> {
	List<OutfitSet> findByActiveTrueOrderByCreatedAtDesc();
	long countByActiveTrue();
}
