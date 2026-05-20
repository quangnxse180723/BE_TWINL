package com.twinl.controller;

import com.twinl.dto.request.CreateUserRequest;
import com.twinl.dto.request.UpdateProfileRequest;
import com.twinl.dto.request.UpdateUserRequest;
import com.twinl.dto.request.UpdateUserStatusRequest;
import com.twinl.dto.response.UserResponse;
import com.twinl.service.UserService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getCurrentUser() {
		return ResponseEntity.ok(userService.getCurrentUser());
	}

	@PutMapping("/me")
	public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(userService.updateCurrentUser(request));
	}

	@PostMapping("/me/avatar")
	public ResponseEntity<UserResponse> updateAvatar(@RequestParam("file") MultipartFile file) {
		return ResponseEntity.ok(userService.updateAvatar(file));
	}

	@GetMapping
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.ok(userService.createUser(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<UserResponse> updateUser(
			@PathVariable Long id,
			@RequestBody UpdateUserRequest request
	) {
		return ResponseEntity.ok(userService.updateUser(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<UserResponse> updateUserStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateUserStatusRequest request
	) {
		return ResponseEntity.ok(userService.updateUserStatus(id, request));
	}
}
