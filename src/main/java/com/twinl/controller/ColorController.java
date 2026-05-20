package com.twinl.controller;

import com.twinl.dto.response.ColorResponse;
import com.twinl.service.ColorService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/colors")
public class ColorController {
	private final ColorService colorService;

	public ColorController(ColorService colorService) {
		this.colorService = colorService;
	}

	@GetMapping
	public ResponseEntity<List<ColorResponse>> getColors() {
		return ResponseEntity.ok(colorService.getAllColors());
	}
}
