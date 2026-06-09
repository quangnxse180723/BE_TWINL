package com.twinl.service;

import com.twinl.dto.request.LoginRequest;
import com.twinl.dto.request.RegisterRequest;
import com.twinl.dto.response.AuthResponse;

import com.twinl.dto.request.GoogleLoginRequest;

public interface AuthService {
	AuthResponse register(RegisterRequest request);
	AuthResponse login(LoginRequest request);
	AuthResponse googleLogin(GoogleLoginRequest request);
}
