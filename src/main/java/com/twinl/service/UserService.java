package com.twinl.service;

import com.twinl.dto.response.UserResponse;
import java.util.List;

public interface UserService {
	UserResponse getCurrentUser();
	List<UserResponse> getAllUsers();
}
