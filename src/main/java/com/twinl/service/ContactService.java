package com.twinl.service;

import com.twinl.dto.request.ContactRequest;
import com.twinl.dto.response.ContactResponse;

public interface ContactService {
	ContactResponse create(ContactRequest request);
}
