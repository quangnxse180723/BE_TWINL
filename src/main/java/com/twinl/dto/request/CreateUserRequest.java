package com.twinl.dto.request;

import com.twinl.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
	@NotBlank
	@Size(max = 120)
	private String displayName;

	@NotBlank
	@Email
	@Size(max = 120)
	private String email;

	@NotBlank
	@Size(min = 6, max = 120)
	private String password;

	private RoleName role;

	@Size(max = 20)
	private String phone;

	@Size(max = 255)
	private String address;

	@Size(max = 20)
	private String gender;

	private LocalDate dateOfBirth;
}
