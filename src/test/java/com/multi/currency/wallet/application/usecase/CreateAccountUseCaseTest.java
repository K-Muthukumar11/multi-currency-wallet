package com.multi.currency.wallet.application.usecase;

import com.multi.currency.wallet.application.dto.request.CreateAccountRequest;
import com.multi.currency.wallet.application.dto.response.AccountResponse;
import com.multi.currency.wallet.domain.model.*;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountUseCase")
class CreateAccountUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CreateAccountUseCase createAccountUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("execute – create account")
    class Execute {

        @Test
        void savesAccountAndReturnsResponse() {
            CreateAccountRequest request = new CreateAccountRequest("USD");

            AccountResponse response = createAccountUseCase.execute(userId, request);

            verify(accountRepository).save(any(Account.class));
            assertThat(response.currencyCode()).isEqualTo("USD");
            assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void createsAccountBelongingToUser() {
            CreateAccountRequest request = new CreateAccountRequest("EUR");
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

            createAccountUseCase.execute(userId, request);

            verify(accountRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("getAccountForUser")
    class GetAccountForUser {

        @Test
        void returnsAllAccountsForUser() {
            Account a1 = Account.open(userId, "USD");
            Account a2 = Account.open(userId, "EUR");
            when(accountRepository.findByUserId(userId)).thenReturn(List.of(a1, a2));

            List<AccountResponse> result = createAccountUseCase.getAccountForUser(userId);

            assertThat(result).hasSize(2);
        }

        @Test
        void returnsEmptyListWhenNoAccounts() {
            when(accountRepository.findByUserId(userId)).thenReturn(List.of());
            assertThat(createAccountUseCase.getAccountForUser(userId)).isEmpty();
        }
    }
}
