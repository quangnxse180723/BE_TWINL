package com.twinl.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
	private Long id;
	private String displayName;
	private String email;
	private String avatarUrl;
	private String phone;
	private String address;
	private String gender;
	private LocalDate dateOfBirth;
	private Boolean active;
	private List<String> roles;
}
