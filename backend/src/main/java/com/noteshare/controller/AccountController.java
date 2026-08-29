package com.noteshare.controller;

import com.noteshare.dto.AccountVisibilityRequest;
import com.noteshare.dto.DeleteAccountRequest;
import com.noteshare.security.AuthContext;
import com.noteshare.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(HttpServletRequest request, @Valid @RequestBody DeleteAccountRequest body) {
        Long userId = AuthContext.requireUserId(request);
        accountService.deleteAccount(userId, body.password());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/account/visibility")
    public Map<String, String> getVisibility(HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        return Map.of("visibility", accountService.getVisibility(userId).name());
    }

    @PutMapping("/account/visibility")
    public Map<String, String> setVisibility(HttpServletRequest request, @Valid @RequestBody AccountVisibilityRequest body) {
        Long userId = AuthContext.requireUserId(request);
        accountService.setVisibility(userId, body.visibility());
        return Map.of("visibility", accountService.getVisibility(userId).name());
    }
}
