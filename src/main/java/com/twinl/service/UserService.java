package com.twinl.service;

import com.twinl.dto.request.CreateUserRequest;
import com.twinl.dto.request.UpdateProfileRequest;
import com.twinl.dto.request.UpdateUserRequest;
import com.twinl.dto.request.UpdateUserStatusRequest;
import com.twinl.dto.request.ChangePasswordRequest;
import com.twinl.dto.response.UserResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
	UserResponse getCurrentUser();
	UserResponse updateCurrentUser(UpdateProfileRequest request);
	void changePassword(ChangePasswordRequest request);
	UserResponse updateAvatar(MultipartFile file);
	List<UserResponse> getAllUsers();

	com.twinl.dto.response.UserStatsResponse getUserStats(Long userId);
	UserResponse createUser(CreateUserRequest request);
	UserResponse updateUser(Long id, UpdateUserRequest request);
	UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request);
}
