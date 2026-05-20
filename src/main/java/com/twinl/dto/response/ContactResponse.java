package com.twinl.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactResponse {
	private Long id;
	private String name;
	private String email;
	private String phone;
	private String message;
	private LocalDateTime createdAt;
}
