package com.multi.currency.wallet.application.usecase;

import com.multi.currency.wallet.application.dto.request.TransferRequest;
import com.multi.currency.wallet.application.dto.response.TransferResponse;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import com.multi.currency.wallet.domain.model.*;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;
import com.multi.currency.wallet.domain.service.TransferDomainService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferUseCase")
class TransferUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferDomainService transferDomainService;

    @InjectMocks
    private TransferUseCase transferUseCase;

    private UUID userId;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        source = buildAccount("100000000001", userId, "USD", "500");
        destination = buildAccount("200000000002", UUID.randomUUID(), "USD", "100");
    }

    @Nested
    @DisplayName("execute – happy path")
    class HappyPath {

        @Test
        void persistsBothAccountsAndTransactions() {
            TransferRequest request = new TransferRequest("100000000001", "200000000002", new BigDecimal("200"), "USD",
                    "Rent");
            Transaction debitTx = Transaction.createTransferDebit(source.getId(), destination.getId(),
                    Money.of("200", "USD"), source.getBalance(), "Rent");
            Transaction creditTx = Transaction.createTransferCredit(destination.getId(), source.getId(),
                    Money.of("200", "USD"), destination.getBalance(), "Rent");

            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(source));
            when(accountRepository.findByAccountNumber("200000000002")).thenReturn(Optional.of(destination));
            when(transferDomainService.transfer(any(), any(), any(), any())).thenReturn(List.of(debitTx, creditTx));

            TransferResponse response = transferUseCase.execute(request, userId);

            verify(accountRepository).save(source);
            verify(accountRepository).save(destination);
            verify(transactionRepository).saveAll(anyList());

            assertThat(response.sourceAccountNumber()).isEqualTo("100000000001");
            assertThat(response.destinationAccountNumber()).isEqualTo("200000000002");
            assertThat(response.debitTransaction().type()).isEqualTo(TransactionType.TRANSFER_DEBIT);
            assertThat(response.creditTransaction().type()).isEqualTo(TransactionType.TRANSFER_CREDIT);
        }
    }

    @Nested
    @DisplayName("execute – failure cases")
    class FailureCases {

        @Test
        void throwsWhenSourceAccountNotFound() {
            TransferRequest request = new TransferRequest("XXXX", "200000000002", new BigDecimal("100"), "USD", null);
            when(accountRepository.findByAccountNumber("XXXX")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferUseCase.execute(request, userId))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Source account not found");
        }

        @Test
        void throwsWhenDestinationAccountNotFound() {
            TransferRequest request = new TransferRequest("100000000001", "YYYY", new BigDecimal("100"), "USD", null);
            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(source));
            when(accountRepository.findByAccountNumber("YYYY")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transferUseCase.execute(request, userId))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Destination account not found");
        }

        @Test
        void throwsAccessDeniedWhenUserDoesNotOwnSourceAccount() {
            UUID otherUser = UUID.randomUUID();
            TransferRequest request = new TransferRequest("100000000001", "200000000002", new BigDecimal("100"), "USD",
                    null);
            when(accountRepository.findByAccountNumber("100000000001")).thenReturn(Optional.of(source));

            assertThatThrownBy(() -> transferUseCase.execute(request, otherUser))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Account buildAccount(String number, UUID owner, String currency, String balance) {
        return new Account(
                UUID.randomUUID(),
                number,
                owner,
                Money.of(balance, currency),
                currency,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now());
    }
}