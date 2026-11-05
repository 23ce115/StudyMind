package com.studymind.backend.controller;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.studymind.backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUser me;

    public AuthController(AuthService auth, CurrentUser me) { this.auth = auth; this.me = me; }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) { return auth.register(req); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }

    @GetMapping("/me")
    public UserResponse me() { return UserResponse.from(auth.get(me.id())); }

    /** Change name, program or the daily study goal (minutes). */
    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest req) { return auth.updateProfile(me.id(), req); }
}
