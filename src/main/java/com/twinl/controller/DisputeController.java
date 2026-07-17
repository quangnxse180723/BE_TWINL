package com.twinl.controller;

import com.twinl.dto.request.DisputeRequest;
import com.twinl.dto.response.DisputeResponse;
import com.twinl.service.DisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/orders/{orderId}/dispute")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN')")
    public ResponseEntity<DisputeResponse> createDispute(
            @PathVariable Long orderId,
            @Valid @RequestBody DisputeRequest request
    ) {
        return ResponseEntity.ok(disputeService.createDispute(orderId, request));
    }

    @GetMapping("/admin/disputes")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<DisputeResponse>> getAllDisputes(
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(disputeService.getAllDisputes(status, pageable));
    }

    @GetMapping("/admin/disputes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DisputeResponse> getDisputeById(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.getDisputeById(id));
    }

    @PostMapping("/admin/disputes/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable Long id,
            @RequestParam String resolution,
            @RequestParam(required = false, defaultValue = "") String note
    ) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, resolution, note));
    }
}
