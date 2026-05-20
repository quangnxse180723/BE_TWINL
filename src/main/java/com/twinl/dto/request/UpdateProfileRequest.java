package com.twinl.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
	@Size(max = 120)
	private String displayName;

	@Size(max = 20)
	private String phone;

	@Size(max = 255)
	private String address;

	@Size(max = 20)
	private String gender;

	private LocalDate dateOfBirth;
}
