package com.twinl.controller;

import com.twinl.dto.response.WalletResponse;
import com.twinl.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.twinl.dto.request.BankUpdateRequest;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(Authentication authentication) {
        String username = authentication.getName();
        WalletResponse response = walletService.getMyWallet(username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bank")
    public ResponseEntity<Void> updateBankAccount(
            Authentication authentication,
            @Valid @RequestBody BankUpdateRequest request) {
        String username = authentication.getName();
        walletService.updateBankAccount(username, request);
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PostMapping("/withdraw")
    public ResponseEntity<Void> requestWithdrawal(
            Authentication authentication,
            @RequestBody java.util.Map<String, java.math.BigDecimal> request) {
        String username = authentication.getName();
        java.math.BigDecimal amount = request.get("amount");
        if (amount == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Amount is required");
        }
        walletService.requestWithdrawal(username, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/withdrawals")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.twinl.dto.response.WithdrawalRequestResponse>> getPendingWithdrawals() {
        return ResponseEntity.ok(walletService.getPendingWithdrawals());
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/withdrawals/{id}/approve")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveWithdrawal(@org.springframework.web.bind.annotation.PathVariable Long id) {
        walletService.approveWithdrawal(id);
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PostMapping("/admin/withdrawals/{id}/reject")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectWithdrawal(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody java.util.Map<String, String> request) {
        String reason = request.getOrDefault("reason", "Không có lý do");
        walletService.rejectWithdrawal(id, reason);
        return ResponseEntity.ok().build();
    }
}
