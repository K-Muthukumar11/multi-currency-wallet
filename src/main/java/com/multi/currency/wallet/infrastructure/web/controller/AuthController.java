package com.multi.currency.wallet.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.multi.currency.wallet.application.dto.request.LoginRequest;
import com.multi.currency.wallet.application.dto.request.RegisterRequest;
import com.multi.currency.wallet.application.dto.response.AuthResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public ResponseEntity<AuthResponse> register(RegisterRequest request) {
        return null;
    }

    public ResponseEntity<AuthResponse> login(LoginRequest request) {
        return null;
    }
}
