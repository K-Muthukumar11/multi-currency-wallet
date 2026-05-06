package com.multi.currency.wallet.application.usecase;

import com.multi.currency.wallet.application.dto.request.DepositRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import com.multi.currency.wallet.domain.model.*;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositUseCase")
class DepositUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DepositUseCase depositUseCase;

    private UUID userId;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        account = new Account(
                UUID.randomUUID(),
                "100000000001",
                userId,
                Money.zero("USD"),
                "USD",
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now());
    }

    @Nested
    @DisplayName("execute – happy path")
    class HappyPath {

        @Test
        void savesAccountAndTransactionAndReturnsResponse() {
            DepositRequest request = new DepositRequest("100000000001", new BigDecimal("200"), "USD", "Salary");
            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(account));

            TransactionResponse response = depositUseCase.execute(request, userId);

            verify(accountRepository).save(account);
            verify(transactionRepository).save(any());
            assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
        }

        @Test
        void creditsCorrectAmountToAccount() {
            DepositRequest request = new DepositRequest("100000000001", new BigDecimal("350"), "USD", null);
            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(account));

            depositUseCase.execute(request, userId);

            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("350");
        }
    }

    @Nested
    @DisplayName("execute – failure cases")
    class FailureCases {

        @Test
        void throwsAccountNotFoundWhenAccountDoesNotExist() {
            DepositRequest request = new DepositRequest("999999999999", new BigDecimal("100"), "USD", null);
            when(accountRepository.findByAccountNumber("999999999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> depositUseCase.execute(request, userId))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        void throwsAccessDeniedWhenUserDoesNotOwnAccount() {
            UUID otherUserId = UUID.randomUUID();
            DepositRequest request = new DepositRequest("100000000001", new BigDecimal("100"), "USD", null);
            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> depositUseCase.execute(request, otherUserId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
