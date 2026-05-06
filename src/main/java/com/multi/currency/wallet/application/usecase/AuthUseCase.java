package com.multi.currency.wallet.application.usecase;

import org.springframework.stereotype.Service;

import com.multi.currency.wallet.application.dto.request.LoginRequest;
import com.multi.currency.wallet.application.dto.request.RegisterRequest;
import com.multi.currency.wallet.application.dto.response.AuthResponse;
import com.multi.currency.wallet.domain.repository.UserRepository;
import com.multi.currency.wallet.infrastructure.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthUseCase {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        return null;
    }

    public AuthResponse login(LoginRequest request) {
        return null;
    }
}
