package com.twinl.service.impl;

import com.twinl.dto.response.ColorResponse;
import com.twinl.repository.ColorRepository;
import com.twinl.service.ColorService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ColorServiceImpl implements ColorService {
	private final ColorRepository colorRepository;

	public ColorServiceImpl(ColorRepository colorRepository) {
		this.colorRepository = colorRepository;
	}

	@Override
	public List<ColorResponse> getAllColors() {
		return colorRepository.findAll().stream()
				.map(color -> ColorResponse.builder()
						.id(color.getId())
						.name(color.getName())
						.build())
				.collect(Collectors.toList());
	}
}
