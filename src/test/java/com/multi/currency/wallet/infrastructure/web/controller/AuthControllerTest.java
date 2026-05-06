package com.multi.currency.wallet.infrastructure.web.controller;

import com.multi.currency.wallet.application.dto.request.LoginRequest;
import com.multi.currency.wallet.application.dto.request.RegisterRequest;
import com.multi.currency.wallet.application.dto.response.AuthResponse;
import com.multi.currency.wallet.application.usecase.AuthUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthUseCase authUseCase;

    @InjectMocks
    private AuthController controller;

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User");
        AuthResponse response = new AuthResponse("token-abc", UUID.randomUUID(), "user@example.com", "Test User");

        when(authUseCase.register(request)).thenReturn(response);

        ResponseEntity<AuthResponse> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authUseCase).register(request);
    }

    @Test
    void loginReturnsOkResponse() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        AuthResponse response = new AuthResponse("token-xyz", UUID.randomUUID(), "user@example.com", "Test User");

        when(authUseCase.login(request)).thenReturn(response);

        ResponseEntity<AuthResponse> result = controller.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(authUseCase).login(request);
    }
}
