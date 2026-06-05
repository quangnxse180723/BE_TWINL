package com.twinl.dto.request;

import com.twinl.entity.RoleName;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
	@Size(max = 120)
	private String displayName;

	private RoleName role;

	@Size(max = 20)
	private String phone;

	@Size(max = 255)
	private String address;

	@Size(max = 20)
	private String wardCode;

	private Integer districtId;

	private Integer provinceId;

	@Size(max = 20)
	private String gender;

	private LocalDate dateOfBirth;
}
