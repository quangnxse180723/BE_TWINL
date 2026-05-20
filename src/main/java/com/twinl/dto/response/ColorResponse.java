package com.twinl.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ColorResponse {
	private Long id;
	private String name;
}
