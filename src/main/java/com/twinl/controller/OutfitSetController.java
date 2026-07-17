package com.twinl.controller;

import com.twinl.dto.request.OutfitSetRequest;
import com.twinl.dto.response.OutfitSetResponse;
import com.twinl.service.OutfitSetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outfit-sets")
public class OutfitSetController {

	private final OutfitSetService outfitSetService;

	public OutfitSetController(OutfitSetService outfitSetService) {
		this.outfitSetService = outfitSetService;
	}

	// ── Public endpoints ─────────────────────────────────────────────────────────

	@GetMapping
	public ResponseEntity<List<OutfitSetResponse>> getAllActive() {
		return ResponseEntity.ok(outfitSetService.getAllActive());
	}

	@GetMapping("/{id}")
	public ResponseEntity<OutfitSetResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(outfitSetService.getById(id));
	}

	// ── Admin endpoints ───────────────────────────────────────────────────────────

	@GetMapping("/admin/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<OutfitSetResponse>> getAll() {
		return ResponseEntity.ok(outfitSetService.getAll());
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<OutfitSetResponse> create(@Valid @RequestBody OutfitSetRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(outfitSetService.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<OutfitSetResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody OutfitSetRequest request
	) {
		return ResponseEntity.ok(outfitSetService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		outfitSetService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/toggle")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<OutfitSetResponse> toggleActive(@PathVariable Long id) {
		return ResponseEntity.ok(outfitSetService.toggleActive(id));
	}
}
