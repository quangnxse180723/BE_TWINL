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
	private final com.twinl.service.EmailService emailService;

	public ContactServiceImpl(ContactMessageRepository contactMessageRepository, com.twinl.service.EmailService emailService) {
		this.contactMessageRepository = contactMessageRepository;
		this.emailService = emailService;
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
		
		try {
			emailService.sendContactConfirmation(saved.getEmail(), saved.getName());
			emailService.sendContactNotificationToAdmin(saved.getName(), saved.getEmail(), saved.getPhone(), saved.getMessage());
		} catch (Exception e) {
			// Ignore email errors to not block the contact form submission
			System.err.println("Failed to send contact emails: " + e.getMessage());
		}
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
