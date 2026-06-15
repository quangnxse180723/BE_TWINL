package com.twinl.service;

import com.twinl.dto.request.LoginRequest;
import com.twinl.dto.request.RegisterRequest;
import com.twinl.dto.response.AuthResponse;

import com.twinl.dto.request.GoogleLoginRequest;

import com.twinl.dto.request.SendOtpRequest;
import com.twinl.dto.request.ForgotPasswordRequest;
import com.twinl.dto.request.ResetPasswordRequest;

public interface AuthService {
	void sendOtp(SendOtpRequest request);
	void sendForgotPasswordOtp(ForgotPasswordRequest request);
	void resetPassword(ResetPasswordRequest request);
	AuthResponse register(RegisterRequest request);
	AuthResponse login(LoginRequest request);
	AuthResponse googleLogin(GoogleLoginRequest request);
}
