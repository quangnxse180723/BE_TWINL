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
}
