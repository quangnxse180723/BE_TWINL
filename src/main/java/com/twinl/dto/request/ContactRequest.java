package com.twinl.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {
	@NotBlank
	@Size(max = 120)
	private String name;

	@NotBlank
	@Email
	@Size(max = 120)
	private String email;

	@Size(max = 20)
	private String phone;

	@NotBlank
	@Size(max = 2000)
	private String message;
}
