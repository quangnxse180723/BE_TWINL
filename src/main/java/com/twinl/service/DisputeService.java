package com.twinl.service;

import com.twinl.dto.request.DisputeRequest;
import com.twinl.dto.response.DisputeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DisputeService {
    DisputeResponse createDispute(Long orderId, DisputeRequest request);
    Page<DisputeResponse> getAllDisputes(String status, Pageable pageable);
    DisputeResponse getDisputeById(Long id);
    DisputeResponse resolveDispute(Long id, String resolution, String note);
}
