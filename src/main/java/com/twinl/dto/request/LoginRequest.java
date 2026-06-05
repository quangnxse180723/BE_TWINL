package com.twinl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
	@Email
	@NotBlank
	@Size(max = 255)
	private String email;

	@NotBlank(message = "Mật khẩu không được để trống")
	@Size(min = 6, max = 72, message = "Mật khẩu tối thiểu là 6 ký tự")
	private String password;
}
