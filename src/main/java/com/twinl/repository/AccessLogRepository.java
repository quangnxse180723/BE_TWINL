package com.twinl.repository;

import com.twinl.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    long countByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    long countByStatusAndCreatedAtAfter(String status, LocalDateTime date);
    long countByStatusAndCreatedAtBetween(String status, LocalDateTime startDate, LocalDateTime endDate);
    
    List<AccessLog> findAllByCreatedAtAfter(LocalDateTime date);
    List<AccessLog> findAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<AccessLog> findTop10ByOrderByCreatedAtDesc();
}
