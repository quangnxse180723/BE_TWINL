package com.twinl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
	@NotBlank(message = "Tên hiển thị không được để trống")
	private String displayName;

	@Email(message = "Email không hợp lệ")
	@NotBlank(message = "Email không được để trống")
	private String email;

	@Size(min = 6, message = "Mật khẩu tối thiểu là 6 ký tự")
	@NotBlank(message = "Mật khẩu không được để trống")
	private String password;

	@NotBlank(message = "Mã OTP không được để trống")
	private String otp;
}
