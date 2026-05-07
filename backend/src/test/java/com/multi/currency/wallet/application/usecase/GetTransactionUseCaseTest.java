package com.multi.currency.wallet.application.usecase;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTransactionUseCase")
class GetTransactionUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GetTransactionUseCase getTransactionUseCase;

    private UUID userId;
    private String accountNumber;
    private Account account;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        accountNumber = "123456789012";
        account = new Account(accountId, accountNumber, userId,
                Money.zero("USD"), "USD", AccountStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Nested
    @DisplayName("execute – paginated by accountId")
    class ExecutePaginated {

        @Test
        void returnsPaginatedTransactionsForAccount() {
            PageRequest pageable = PageRequest.of(0, 10);
            Transaction tx = Transaction.createDeposit(accountId, Money.of("100", "USD"), Money.of("100", "USD"), null);
            Page<Transaction> page = new PageImpl<>(List.of(tx));

            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
            when(transactionRepository.findByAccountId(accountId, pageable)).thenReturn(page);

            Page<TransactionResponse> result = getTransactionUseCase.execute(accountNumber, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).type()).isEqualTo(TransactionType.DEPOSIT);
        }

        @Test
        void throwsWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> getTransactionUseCase.execute(accountNumber, PageRequest.of(0, 10)))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("executeForUser – all transactions")
    class ExecuteForUser {

        @Test
        void returnsAllTransactionsAcrossAllUserAccounts() {
            Account acc2 = new Account(UUID.randomUUID(), "999999999999", userId,
                    Money.zero("EUR"), "EUR", AccountStatus.ACTIVE, Instant.now(), Instant.now());

            when(accountRepository.findByUserId(userId)).thenReturn(List.of(account, acc2));

            Transaction tx1 = Transaction.createDeposit(accountId, Money.of("50", "USD"), Money.of("50", "USD"), null);
            Transaction tx2 = Transaction.createDeposit(acc2.getId(), Money.of("80", "EUR"), Money.of("80", "EUR"),
                    null);

            when(transactionRepository.findByAccountId(account.getId())).thenReturn(List.of(tx1));
            when(transactionRepository.findByAccountId(acc2.getId())).thenReturn(List.of(tx2));

            List<TransactionResponse> result = getTransactionUseCase.executeForUser(userId);

            assertThat(result).hasSize(2);
        }

        @Test
        void returnsEmptyListWhenUserHasNoAccounts() {
            when(accountRepository.findByUserId(userId)).thenReturn(List.of());
            assertThat(getTransactionUseCase.executeForUser(userId)).isEmpty();
        }
    }
}
