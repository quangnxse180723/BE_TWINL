package com.twinl.service.impl;

import com.twinl.dto.request.ContactRequest;
import com.twinl.dto.response.ContactResponse;
import com.twinl.entity.ContactMessage;
import com.twinl.repository.ContactMessageRepository;
import com.twinl.service.ContactService;
import org.springframework.stereotype.Service;

@Service
public class ContactServiceImpl implements ContactService {
	private final ContactMessageRepository contactMessageRepository;

	public ContactServiceImpl(ContactMessageRepository contactMessageRepository) {
		this.contactMessageRepository = contactMessageRepository;
	}

	@Override
	public ContactResponse create(ContactRequest request) {
		ContactMessage message = ContactMessage.builder()
				.name(request.getName())
				.email(request.getEmail())
				.phone(request.getPhone())
				.message(request.getMessage())
				.build();

		ContactMessage saved = contactMessageRepository.save(message);
		return ContactResponse.builder()
				.id(saved.getId())
				.name(saved.getName())
				.email(saved.getEmail())
				.phone(saved.getPhone())
				.message(saved.getMessage())
				.createdAt(saved.getCreatedAt())
				.build();
	}
}
