package com.twinl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
	@NotBlank
	private String displayName;

	@Email
	@NotBlank
	private String email;

	@Size(min = 6)
	@NotBlank
	private String password;
}
