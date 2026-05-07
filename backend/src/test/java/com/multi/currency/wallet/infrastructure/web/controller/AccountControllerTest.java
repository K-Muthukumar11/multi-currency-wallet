package com.multi.currency.wallet.infrastructure.web.controller;

import com.multi.currency.wallet.application.dto.request.CreateAccountRequest;
import com.multi.currency.wallet.application.dto.response.AccountResponse;
import com.multi.currency.wallet.application.usecase.CreateAccountUseCase;
import com.multi.currency.wallet.domain.model.User;
import com.multi.currency.wallet.domain.model.UserPrincipal;
import com.multi.currency.wallet.domain.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController")
class AccountControllerTest {

    @Mock
    private CreateAccountUseCase createAccountUseCase;

    @InjectMocks
    private AccountController controller;

    private Authentication authentication;
    private UserPrincipal principal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User(userId, "test@example.com", "password-hash", "Test User", UserRole.USER, Instant.now());
        principal = new UserPrincipal(user);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    void createAccountReturnsCreatedResponse() {
        CreateAccountRequest request = new CreateAccountRequest("USD");
        AccountResponse response = new AccountResponse(UUID.randomUUID(), "123456789012", userId,
                BigDecimal.ZERO, "USD", null, Instant.now());

        when(createAccountUseCase.execute(userId, request)).thenReturn(response);

        ResponseEntity<AccountResponse> result = controller.createAccount(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(createAccountUseCase).execute(userId, request);
    }

    @Test
    void getMyAccountsReturnsListOfAccounts() {
        AccountResponse response = new AccountResponse(UUID.randomUUID(), "123456789012", userId,
                BigDecimal.ZERO, "USD", null, Instant.now());
        List<AccountResponse> accounts = List.of(response);

        when(createAccountUseCase.getAccountForUser(userId)).thenReturn(accounts);

        ResponseEntity<List<AccountResponse>> result = controller.getMyAccounts(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(accounts);
        verify(createAccountUseCase).getAccountForUser(userId);
    }
}
