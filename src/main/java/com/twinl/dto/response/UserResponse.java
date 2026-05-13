package com.twinl.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
	private Long id;
	private String displayName;
	private String email;
	private List<String> roles;
}
