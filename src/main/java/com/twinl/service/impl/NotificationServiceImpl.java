package com.twinl.service.impl;

import com.twinl.dto.response.NotificationResponse;
import com.twinl.entity.Notification;
import com.twinl.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.twinl.repository.NotificationRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.NotificationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	@Override
	@Transactional
	public void sendNotification(User user, String title, String message, String type) {
		Notification notification = Notification.builder()
				.user(user)
				.title(title)
				.message(message)
				.type(type)
				.isRead(false)
				.build();
		notification = notificationRepository.save(notification);

		NotificationResponse response = mapToResponse(notification);

		// Nếu User đang kết nối SSE thì push ngay lập tức
		SseEmitter emitter = emitters.get(user.getEmail());
		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event()
						.name("notification")
						.data(response));
			} catch (IOException e) {
				emitters.remove(user.getEmail());
				log.warn("Lỗi khi push notification qua SSE cho user: {}", user.getEmail());
			}
		}
	}

	@Override
	public List<NotificationResponse> getUserNotifications(String username) {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

		return notificationRepository.findByUserOrderByCreatedAtDesc(user)
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	@Override
	public long getUnreadCount(String username) {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
		return notificationRepository.countByUserAndIsReadFalse(user);
	}

	@Override
	@Transactional
	public void markAsRead(Long notificationId, String username) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));

		if (!notification.getUser().getEmail().equals(username)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đánh dấu thông báo này");
		}

		notification.setRead(true);
		notificationRepository.save(notification);
	}

	@Override
	@Transactional
	public void markAllAsRead(String username) {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
		notificationRepository.markAllAsReadByUser(user);
	}

	@Override
	public SseEmitter subscribe(String username) {
		SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // Timeout 1 giờ
		emitters.put(username, emitter);

		emitter.onCompletion(() -> emitters.remove(username));
		emitter.onTimeout(() -> emitters.remove(username));
		emitter.onError((e) -> emitters.remove(username));

		// Gửi event khởi tạo
		try {
			emitter.send(SseEmitter.event().name("init").data("Connected"));
		} catch (IOException e) {
			emitters.remove(username);
		}

		return emitter;
	}

	private NotificationResponse mapToResponse(Notification notification) {
		return NotificationResponse.builder()
				.id(notification.getId())
				.title(notification.getTitle())
				.message(notification.getMessage())
				.type(notification.getType())
				.isRead(notification.isRead())
				.createdAt(notification.getCreatedAt())
				.build();
	}

	@Override
	public int getOnlineUserCount() {
		return emitters.size();
	}

	@org.springframework.scheduling.annotation.Scheduled(fixedRate = 10000)
	public void keepAliveAndClean() {
		emitters.forEach((username, emitter) -> {
			try {
				emitter.send(SseEmitter.event().name("ping").data("Heartbeat"));
			} catch (Exception e) {
				emitters.remove(username);
			}
		});
	}
}
