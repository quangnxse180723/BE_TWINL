package com.twinl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    private String otp;

    @Size(min = 6, message = "Mật khẩu tối thiểu là 6 ký tự")
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;
}
