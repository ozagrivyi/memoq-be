package com.memoq.backend.controller;

import com.memoq.backend.dto.LoginRequest;
import com.memoq.backend.dto.LoginResponse;
import com.memoq.backend.security.AdminProperties;
import com.memoq.backend.security.JwtProperties;
import com.memoq.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminProperties adminProperties;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        boolean usernameMatches = adminProperties.username().equals(request.username());
        boolean passwordMatches =
                usernameMatches && passwordEncoder.matches(request.password(), adminProperties.passwordHash());
        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid username or password.");
        }
        String token = jwtService.generateToken(request.username());
        return new LoginResponse(token, "Bearer", jwtProperties.expirationMinutes() * 60);
    }
}
