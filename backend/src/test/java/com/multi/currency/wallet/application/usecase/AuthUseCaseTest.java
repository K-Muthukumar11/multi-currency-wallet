package com.multi.currency.wallet.application.usecase;

import com.multi.currency.wallet.application.dto.request.LoginRequest;
import com.multi.currency.wallet.application.dto.request.RegisterRequest;
import com.multi.currency.wallet.application.dto.response.AuthResponse;
import com.multi.currency.wallet.domain.exception.DomainException;
import com.multi.currency.wallet.domain.model.User;
import com.multi.currency.wallet.domain.repository.UserRepository;
import com.multi.currency.wallet.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUseCase")
class AuthUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthUseCase authUseCase;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void registersNewUserAndReturnsAuthResponse() {
            RegisterRequest request = new RegisterRequest("john@example.com", "secret123", "John Doe");
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");

            AuthResponse response = authUseCase.register(request);

            assertThat(response.token()).isEqualTo("mock-jwt-token");
            assertThat(response.email()).isEqualTo("john@example.com");
            assertThat(response.fullName()).isEqualTo("John Doe");
            verify(userRepository).save(any(User.class));
        }

        @Test
        void throwsWhenEmailAlreadyRegistered() {
            RegisterRequest request = new RegisterRequest("existing@example.com", "pass", "Name");
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authUseCase.register(request))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Email already registered");
        }

        @Test
        void doesNotSaveUserWhenEmailIsDuplicate() {
            RegisterRequest request = new RegisterRequest("dup@example.com", "pass", "Name");
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authUseCase.register(request));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private User storedUser;
        private String rawPassword;

        @BeforeEach
        void setUp() {
            rawPassword = "myPassword";
            String hash = encoder.encode(rawPassword);
            storedUser = new User(
                    UUID.randomUUID(),
                    "user@example.com",
                    hash,
                    "Test User",
                    com.multi.currency.wallet.domain.model.UserRole.USER,
                    Instant.now());
        }

        @Test
        void returnsAuthResponseForValidCredentials() {
            LoginRequest request = new LoginRequest("user@example.com", rawPassword);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(storedUser));
            when(jwtService.generateToken(anyString(), anyString())).thenReturn("valid-token");

            AuthResponse response = authUseCase.login(request);

            assertThat(response.token()).isEqualTo("valid-token");
            assertThat(response.email()).isEqualTo("user@example.com");
        }

        @Test
        void throwsBadCredentialsWhenEmailNotFound() {
            LoginRequest request = new LoginRequest("ghost@example.com", "pass");
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authUseCase.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void throwsBadCredentialsWhenPasswordIsWrong() {
            LoginRequest request = new LoginRequest("user@example.com", "wrongPassword");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(storedUser));

            assertThatThrownBy(() -> authUseCase.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
