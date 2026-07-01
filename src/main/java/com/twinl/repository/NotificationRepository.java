package com.twinl.repository;

import com.twinl.entity.Notification;
import com.twinl.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByUserOrderByCreatedAtDesc(User user);

	long countByUserAndIsReadFalse(User user);

}
