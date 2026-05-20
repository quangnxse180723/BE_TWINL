package com.twinl.service.impl;

import com.twinl.config.JwtService;
import com.twinl.dto.request.LoginRequest;
import com.twinl.dto.request.RegisterRequest;
import com.twinl.dto.response.AuthResponse;
import com.twinl.dto.response.UserResponse;
import com.twinl.entity.Role;
import com.twinl.entity.RoleName;
import com.twinl.entity.User;
import com.twinl.repository.RoleRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.AuthService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Override
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		Role userRole = roleRepository.findByName(RoleName.USER)
				.orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.USER).build()));

		User user = User.builder()
				.displayName(request.getDisplayName())
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.build();
		user.getRoles().add(userRole);

		User savedUser = userRepository.save(user);
		String token = jwtService.generateToken(savedUser);

		return AuthResponse.builder()
				.accessToken(token)
				.tokenType("Bearer")
				.user(toUserResponse(savedUser))
				.build();
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
		if (existingUser != null) {
			boolean isActive = existingUser.getActive() == null || Boolean.TRUE.equals(existingUser.getActive());
			if (!isActive) {
				throw new ResponseStatusException(
						HttpStatus.FORBIDDEN,
						"Tài khoản của bạn đã bị khóa, vui lòng liên hệ admin qua gmail: twinl2hand@gmail.com"
				);
			}
		}

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
			);
		} catch (DisabledException ex) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"Tài khoản của bạn đã bị khóa, vui lòng liên hệ admin qua gmail: twinl2hand@gmail.com"
			);
		} catch (BadCredentialsException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		String token = jwtService.generateToken(user);
		return AuthResponse.builder()
				.accessToken(token)
				.tokenType("Bearer")
				.user(toUserResponse(user))
				.build();
	}

	private UserResponse toUserResponse(User user) {
		List<String> roles = user.getRoles().stream()
				.map(role -> role.getName().name())
				.collect(Collectors.toList());
		boolean isActive = user.getActive() == null || Boolean.TRUE.equals(user.getActive());

		return UserResponse.builder()
				.id(user.getId())
				.displayName(user.getDisplayName())
				.email(user.getEmail())
				.avatarUrl(user.getAvatarUrl())
				.phone(user.getPhone())
				.address(user.getAddress())
				.gender(user.getGender())
				.dateOfBirth(user.getDateOfBirth())
				.active(isActive)
				.roles(roles)
				.build();
	}
}
