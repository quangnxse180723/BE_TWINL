package com.twinl.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.twinl.dto.request.CreateUserRequest;
import com.twinl.dto.request.UpdateProfileRequest;
import com.twinl.dto.request.UpdateUserRequest;
import com.twinl.dto.request.UpdateUserStatusRequest;
import com.twinl.dto.response.UserResponse;
import com.twinl.entity.Role;
import com.twinl.entity.RoleName;
import com.twinl.entity.User;
import com.twinl.repository.RoleRepository;
import com.twinl.repository.UserRepository;
import com.twinl.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final Cloudinary cloudinary;

	public UserServiceImpl(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder,
			Cloudinary cloudinary
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.cloudinary = cloudinary;
	}

	@Override
	public UserResponse getCurrentUser() {
		return toUserResponse(getCurrentAuthenticatedUser());
	}

	@Override
	public UserResponse updateCurrentUser(UpdateProfileRequest request) {
		User user = getCurrentAuthenticatedUser();
		if (request.getDisplayName() != null) {
			user.setDisplayName(request.getDisplayName());
		}
		if (request.getPhone() != null) {
			user.setPhone(request.getPhone());
		}
		if (request.getAddress() != null) {
			user.setAddress(request.getAddress());
		}
		if (request.getWardCode() != null) {
			user.setWardCode(request.getWardCode());
		}
		if (request.getDistrictId() != null) {
			user.setDistrictId(request.getDistrictId());
		}
		if (request.getProvinceId() != null) {
			user.setProvinceId(request.getProvinceId());
		}
		if (request.getGender() != null) {
			user.setGender(request.getGender());
		}
		if (request.getDateOfBirth() != null) {
			user.setDateOfBirth(request.getDateOfBirth());
		}

		User savedUser = userRepository.save(user);
		return toUserResponse(savedUser);
	}

	@Override
	public void changePassword(com.twinl.dto.request.ChangePasswordRequest request) {
		User user = getCurrentAuthenticatedUser();
		if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác");
		}
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
	}

	@Override
	public UserResponse updateAvatar(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
		}

		long maxSizeBytes = 10L * 1024 * 1024;
		if (file.getSize() > maxSizeBytes) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must be <= 10MB");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
		}

		User user = getCurrentAuthenticatedUser();
		try {
			Map<?, ?> uploadResult = cloudinary.uploader().upload(
					file.getBytes(),
					ObjectUtils.asMap(
							"folder", "twinl/avatars",
							"resource_type", "image"
					)
			);
			Object secureUrl = uploadResult.get("secure_url");
			if (secureUrl == null) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing upload URL");
			}
			user.setAvatarUrl(secureUrl.toString());
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar");
		}

		User savedUser = userRepository.save(user);
		return toUserResponse(savedUser);
	}

	@Override
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream()
				.map(this::toUserResponse)
				.collect(Collectors.toList());
	}

	@Override
	public UserResponse createUser(CreateUserRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
		}

		RoleName roleName = request.getRole() == null ? RoleName.USER : request.getRole();
		if (roleName == RoleName.ADMIN) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create admin user");
		}

		Role role = roleRepository.findByName(roleName)
				.orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

		User user = User.builder()
				.displayName(request.getDisplayName())
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.phone(request.getPhone())
				.address(request.getAddress())
				.wardCode(request.getWardCode())
				.districtId(request.getDistrictId())
				.provinceId(request.getProvinceId())
				.gender(request.getGender())
				.dateOfBirth(request.getDateOfBirth())
				.active(true)
				.build();
		user.getRoles().clear();
		user.getRoles().add(role);

		User savedUser = userRepository.save(user);
		return toUserResponse(savedUser);
	}

	@Override
	public UserResponse updateUser(Long id, UpdateUserRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		ensureNotAdmin(user);

		if (request.getDisplayName() != null) {
			user.setDisplayName(request.getDisplayName());
		}
		if (request.getPhone() != null) {
			user.setPhone(request.getPhone());
		}
		if (request.getAddress() != null) {
			user.setAddress(request.getAddress());
		}
		if (request.getWardCode() != null) {
			user.setWardCode(request.getWardCode());
		}
		if (request.getDistrictId() != null) {
			user.setDistrictId(request.getDistrictId());
		}
		if (request.getProvinceId() != null) {
			user.setProvinceId(request.getProvinceId());
		}
		if (request.getGender() != null) {
			user.setGender(request.getGender());
		}
		if (request.getDateOfBirth() != null) {
			user.setDateOfBirth(request.getDateOfBirth());
		}
		if (request.getRole() != null) {
			if (request.getRole() == RoleName.ADMIN) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign admin role");
			}
			Role role = roleRepository.findByName(request.getRole())
					.orElseGet(() -> roleRepository.save(Role.builder().name(request.getRole()).build()));
			user.getRoles().clear();
			user.getRoles().add(role);
		}

		User savedUser = userRepository.save(user);
		return toUserResponse(savedUser);
	}

	@Override
	public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		ensureNotAdmin(user);
		user.setActive(Boolean.TRUE.equals(request.getActive()));
		User savedUser = userRepository.save(user);
		return toUserResponse(savedUser);
	}

	private User getCurrentAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
		}

		String email = authentication.getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private UserResponse toUserResponse(User user) {
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
				.roles(user.getRoles().stream()
						.map(role -> role.getName().name())
						.collect(Collectors.toList()))
				.build();
	}

	private void ensureNotAdmin(User user) {
		if (hasRole(user, RoleName.ADMIN)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify admin user");
		}
	}

	private boolean hasRole(User user, RoleName roleName) {
		return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
	}
}
