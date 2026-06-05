package com.twinl.controller;


import com.twinl.dto.response.NotificationResponse;
import com.twinl.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<List<NotificationResponse>> getNotifications(
			@AuthenticationPrincipal UserDetails userDetails) {
		List<NotificationResponse> data = notificationService.getUserNotifications(userDetails.getUsername());
		return ResponseEntity.ok(data);
	}

	@GetMapping("/unread-count")
	public ResponseEntity<Long> getUnreadCount(
			@AuthenticationPrincipal UserDetails userDetails) {
		long count = notificationService.getUnreadCount(userDetails.getUsername());
		return ResponseEntity.ok(count);
	}

	@PutMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(
			@PathVariable Long id,
			@AuthenticationPrincipal UserDetails userDetails) {
		notificationService.markAsRead(id, userDetails.getUsername());
		return ResponseEntity.ok().build();
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamNotifications(@AuthenticationPrincipal UserDetails userDetails) {
		return notificationService.subscribe(userDetails.getUsername());
	}
}
