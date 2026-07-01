package com.twinl.service;

import com.twinl.dto.response.NotificationResponse;
import com.twinl.entity.User;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
	void sendNotification(User user, String title, String message, String type);

	List<NotificationResponse> getUserNotifications(String username);

	long getUnreadCount(String username);

	void markAsRead(Long notificationId, String username);

	void markAllAsRead(String username);

	SseEmitter subscribe(String username);

	int getOnlineUserCount();
}
