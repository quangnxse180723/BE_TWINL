package com.twinl.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
	private Long id;
	private String title;
	private String message;
	private String type;
	private boolean isRead;
	private LocalDateTime createdAt;
}
