package com.exemple.utilisateursservice.controllers;

import com.exemple.utilisateursservice.dto.*;
import com.exemple.utilisateursservice.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) { return ResponseEntity.ok(authService.register(req)); }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) { return ResponseEntity.ok(authService.login(req)); }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest req) { return ResponseEntity.ok(authService.refresh(req)); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String email) { authService.logout(email); return ResponseEntity.noContent().build(); }
}
