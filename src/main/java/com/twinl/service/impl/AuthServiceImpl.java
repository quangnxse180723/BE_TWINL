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
import jakarta.servlet.http.HttpServletRequest;
import com.twinl.service.AnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.twinl.dto.request.GoogleLoginRequest;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import java.util.UUID;
@Service
public class AuthServiceImpl implements AuthService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final HttpServletRequest httpServletRequest;
	private final AnalyticsService analyticsService;
	private final com.twinl.repository.OtpRepository otpRepository;
	private final com.twinl.service.EmailService emailService;

	public AuthServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			HttpServletRequest httpServletRequest,
			AnalyticsService analyticsService,
			com.twinl.repository.OtpRepository otpRepository,
			com.twinl.service.EmailService emailService
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.httpServletRequest = httpServletRequest;
		this.analyticsService = analyticsService;
		this.otpRepository = otpRepository;
		this.emailService = emailService;
	}

	@Override
	public void sendOtp(com.twinl.dto.request.SendOtpRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
		}
		
		String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));
		java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(5);

		com.twinl.entity.OtpEntity otpEntity = otpRepository.findByEmail(request.getEmail())
				.orElse(new com.twinl.entity.OtpEntity());
		otpEntity.setEmail(request.getEmail());
		otpEntity.setOtpCode(otpCode);
		otpEntity.setExpiresAt(expiresAt);
		otpRepository.save(otpEntity);

		emailService.sendOtpEmail(request.getEmail(), otpCode);
	}

	@Override
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		com.twinl.entity.OtpEntity otpEntity = otpRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ hoặc chưa được gửi"));

		if (!otpEntity.getOtpCode().equals(request.getOtp())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP không chính xác");
		}

		if (otpEntity.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
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
		otpRepository.delete(otpEntity);

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
				if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
					analyticsService.logAccess(httpServletRequest, "FAILED", existingUser.getId());
					throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu");
				}
				analyticsService.logAccess(httpServletRequest, "FAILED", existingUser.getId());
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
			if (existingUser == null || !passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
				analyticsService.logAccess(httpServletRequest, "FAILED", existingUser != null ? existingUser.getId() : null);
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu");
			}
			analyticsService.logAccess(httpServletRequest, "FAILED", existingUser.getId());
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"Tài khoản của bạn đã bị khóa, vui lòng liên hệ admin qua gmail: twinl2hand@gmail.com"
			);
		} catch (BadCredentialsException ex) {
			analyticsService.logAccess(httpServletRequest, "FAILED", existingUser != null ? existingUser.getId() : null);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu");
		} catch (Exception ex) {
			analyticsService.logAccess(httpServletRequest, "FAILED", existingUser != null ? existingUser.getId() : null);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu");
		}

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai email hoặc mật khẩu"));

		analyticsService.logAccess(httpServletRequest, "SUCCESS", user.getId());

		String token = jwtService.generateToken(user);
		return AuthResponse.builder()
				.accessToken(token)
				.tokenType("Bearer")
				.user(toUserResponse(user))
				.build();
	}

	@Value("${google.client.id:YOUR_GOOGLE_CLIENT_ID}")
	private String googleClientId;

	@Override
	public AuthResponse googleLogin(GoogleLoginRequest request) {
		try {
			org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
			org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
			headers.setBearerAuth(request.getIdToken()); // The FE sends the access_token in idToken field
			org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("parameters", headers);
			
			org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(
					"https://www.googleapis.com/oauth2/v3/userinfo", 
					org.springframework.http.HttpMethod.GET, 
					entity, 
					java.util.Map.class
			);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				java.util.Map<String, Object> payload = response.getBody();
				String email = (String) payload.get("email");
				String name = (String) payload.get("name");
				String pictureUrl = (String) payload.get("picture");

				User user = userRepository.findByEmail(email).orElse(null);
				if (user == null) {
					Role userRole = roleRepository.findByName(RoleName.USER)
							.orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.USER).build()));

					user = User.builder()
							.email(email)
							.displayName(name != null && !name.isEmpty() ? name : "Người dùng Google")
							.avatarUrl(pictureUrl)
							.password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password for google users
							.build();
					user.getRoles().add(userRole);
					user = userRepository.save(user);
				}

				boolean isActive = user.getActive() == null || Boolean.TRUE.equals(user.getActive());
				if (!isActive) {
					analyticsService.logAccess(httpServletRequest, "FAILED", user.getId());
					throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
				}

				analyticsService.logAccess(httpServletRequest, "SUCCESS", user.getId());
				String token = jwtService.generateToken(user);

				return AuthResponse.builder()
						.accessToken(token)
						.tokenType("Bearer")
						.user(toUserResponse(user))
						.build();
			} else {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID token.");
			}
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login failed: " + e.getMessage());
		}
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
				.wardCode(user.getWardCode())
				.districtId(user.getDistrictId())
				.provinceId(user.getProvinceId())
				.gender(user.getGender())
				.dateOfBirth(user.getDateOfBirth())
				.active(isActive)
				.roles(roles)
				.build();
	}
}
