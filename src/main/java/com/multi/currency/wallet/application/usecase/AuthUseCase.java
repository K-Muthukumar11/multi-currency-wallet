package com.multi.currency.wallet.application.usecase;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.multi.currency.wallet.application.dto.request.LoginRequest;
import com.multi.currency.wallet.application.dto.request.RegisterRequest;
import com.multi.currency.wallet.application.dto.response.AuthResponse;
import com.multi.currency.wallet.domain.exception.DomainException;
import com.multi.currency.wallet.domain.model.User;
import com.multi.currency.wallet.domain.repository.UserRepository;
import com.multi.currency.wallet.infrastructure.security.JwtService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthUseCase {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DomainException("Email already registered: " + request.email());
        }
        String hash = encoder.encode(request.password());
        User user = User.register(request.email(), hash, request.fullName());
        userRepository.save(user);
        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid Credentials"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Inavlid Credentials");
        }
        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }
}
