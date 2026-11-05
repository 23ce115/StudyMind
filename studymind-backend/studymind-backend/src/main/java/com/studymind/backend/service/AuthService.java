package com.studymind.backend.service;

import com.studymind.backend.dto.Dtos.*;
import com.studymind.backend.exception.ApiException;
import com.studymind.backend.model.User;
import com.studymind.backend.repository.UserRepository;
import com.studymind.backend.security.JwtUtil;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists");
        User u = new User();
        u.email = email;
        u.passwordHash = encoder.encode(req.password());
        u.name = req.name().trim();
        u.program = req.program() == null || req.program().isBlank() ? null : req.program().trim();
        u = users.save(u);
        return new AuthResponse(jwt.generateToken(u.id, u.email), UserResponse.from(u));
    }

    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(normalize(req.email()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(req.password(), u.passwordHash)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return new AuthResponse(jwt.generateToken(u.id, u.email), UserResponse.from(u));
    }

    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in again"));
    }

    public UserResponse updateProfile(UUID id, UpdateProfileRequest req) {
        User u = get(id);
        if (req.name() != null && !req.name().isBlank()) u.name = req.name().trim();
        if (req.program() != null) u.program = req.program().isBlank() ? null : req.program().trim();
        if (req.dailyGoalMinutes() != null) u.dailyGoalMinutes = req.dailyGoalMinutes();
        return UserResponse.from(users.save(u));
    }
}
