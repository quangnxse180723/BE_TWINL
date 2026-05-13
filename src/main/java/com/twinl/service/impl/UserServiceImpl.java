package com.twinl.service.impl;

import com.twinl.dto.response.UserResponse;
import com.twinl.entity.User;
import com.twinl.repository.UserRepository;
import com.twinl.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;

	public UserServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserResponse getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
		}

		String email = authentication.getName();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		return toUserResponse(user);
	}

	@Override
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream()
				.map(this::toUserResponse)
				.collect(Collectors.toList());
	}

	private UserResponse toUserResponse(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.displayName(user.getDisplayName())
				.email(user.getEmail())
				.roles(user.getRoles().stream()
						.map(role -> role.getName().name())
						.collect(Collectors.toList()))
				.build();
	}
}
