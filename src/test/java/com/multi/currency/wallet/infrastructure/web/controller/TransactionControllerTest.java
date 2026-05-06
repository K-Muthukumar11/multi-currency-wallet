package com.multi.currency.wallet.infrastructure.web.controller;

import com.multi.currency.wallet.application.dto.request.DepositRequest;
import com.multi.currency.wallet.application.dto.request.ReversalRequest;
import com.multi.currency.wallet.application.dto.request.TransferRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.application.dto.response.TransferResponse;
import com.multi.currency.wallet.application.usecase.DepositUseCase;
import com.multi.currency.wallet.application.usecase.GetTransactionUseCase;
import com.multi.currency.wallet.application.usecase.ReversalUseCase;
import com.multi.currency.wallet.application.usecase.TransferUseCase;
import com.multi.currency.wallet.domain.model.TransactionType;
import com.multi.currency.wallet.domain.model.TransactionStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController")
class TransactionControllerTest {

        @Mock
        private DepositUseCase depositUseCase;

        @Mock
        private TransferUseCase transferUseCase;

        @Mock
        private GetTransactionUseCase getTransactionUseCase;

        @Mock
        private ReversalUseCase reversalUseCase;

        @InjectMocks
        private TransactionController controller;

        private Authentication authentication;
        private UserPrincipal principal;
        private UUID userId;
        private UUID accountId;
        private String accountNumber = "123456789012";

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                accountId = UUID.randomUUID();
                User user = new User(userId, "test@example.com", "password-hash", "Test User", UserRole.USER,
                                Instant.now());
                principal = new UserPrincipal(user);
                authentication = mock(Authentication.class);
                lenient().when(authentication.getPrincipal()).thenReturn(principal);
        }

        @Test
        void depositReturnsCreatedResponse() {
                DepositRequest request = new DepositRequest(accountNumber, BigDecimal.valueOf(125.50), "USD", "top-up");
                TransactionResponse response = new TransactionResponse(UUID.randomUUID(), accountId,
                                TransactionType.DEPOSIT,
                                BigDecimal.valueOf(125.50), "USD", BigDecimal.valueOf(125.50), "top-up", null, null,
                                TransactionStatus.COMPLETED, Instant.now());

                when(depositUseCase.execute(request, userId)).thenReturn(response);

                ResponseEntity<TransactionResponse> result = controller.deposit(request, authentication);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(result.getBody()).isEqualTo(response);
                verify(depositUseCase).execute(request, userId);
        }

        @Test
        void transferReturnsCreatedResponse() {
                TransferRequest request = new TransferRequest(accountNumber, "987654321098", BigDecimal.valueOf(42.00),
                                "USD",
                                "payment");
                TransactionResponse debit = new TransactionResponse(UUID.randomUUID(), accountId,
                                TransactionType.TRANSFER_DEBIT,
                                BigDecimal.valueOf(42.00), "USD", BigDecimal.valueOf(58.00), "payment", null,
                                UUID.randomUUID(),
                                TransactionStatus.COMPLETED, Instant.now());
                TransactionResponse credit = new TransactionResponse(UUID.randomUUID(), UUID.randomUUID(),
                                TransactionType.TRANSFER_CREDIT,
                                BigDecimal.valueOf(42.00), "USD", BigDecimal.valueOf(142.00), "payment", null,
                                accountId,
                                TransactionStatus.COMPLETED, Instant.now());
                TransferResponse response = new TransferResponse(debit, credit, request.sourceAccountNumber(),
                                request.destinationAccountNumber());

                when(transferUseCase.execute(request, userId)).thenReturn(response);

                ResponseEntity<TransferResponse> result = controller.transfer(request, authentication);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(result.getBody()).isEqualTo(response);
                verify(transferUseCase).execute(request, userId);
        }

        @Test
        void getAccountTransactionReturnsPageOfTransactions() {
                PageRequest pageable = PageRequest.of(0, 20);
                TransactionResponse response = new TransactionResponse(UUID.randomUUID(), accountId,
                                TransactionType.DEPOSIT,
                                BigDecimal.valueOf(100), "USD", BigDecimal.valueOf(100), "deposit", null, null,
                                TransactionStatus.COMPLETED, Instant.now());
                Page<TransactionResponse> page = new PageImpl<>(List.of(response));

                when(getTransactionUseCase.execute(accountNumber, pageable)).thenReturn(page);

                ResponseEntity<Page<TransactionResponse>> result = controller.getAccountTransaction(accountNumber,
                                pageable,
                                authentication);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isEqualTo(page);
                verify(getTransactionUseCase).execute(accountNumber, pageable);
        }

        @Test
        void getMyTransactionsReturnsListOfTransactions() {
                TransactionResponse response = new TransactionResponse(UUID.randomUUID(), accountId,
                                TransactionType.DEPOSIT,
                                BigDecimal.valueOf(15), "USD", BigDecimal.valueOf(15), "gift", null, null,
                                TransactionStatus.COMPLETED, Instant.now());
                List<TransactionResponse> transactions = List.of(response);

                when(getTransactionUseCase.executeForUser(userId)).thenReturn(transactions);

                ResponseEntity<List<TransactionResponse>> result = controller.getMyTransactions(authentication);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isEqualTo(transactions);
                verify(getTransactionUseCase).executeForUser(userId);
        }

        @Test
        void reverseReturnsCreatedResponse() {
                ReversalRequest request = new ReversalRequest(UUID.randomUUID(), "mistaken deposit");
                TransactionResponse reversal = new TransactionResponse(UUID.randomUUID(), accountId,
                                TransactionType.REVERSAL_DEBIT,
                                BigDecimal.valueOf(15), "USD", BigDecimal.valueOf(85), "mistaken deposit",
                                request.originalTransactionId(), null,
                                TransactionStatus.COMPLETED, Instant.now());
                List<TransactionResponse> response = List.of(reversal);

                when(reversalUseCase.execute(request, userId)).thenReturn(response);

                ResponseEntity<List<TransactionResponse>> result = controller.reverse(request, authentication);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(result.getBody()).isEqualTo(response);
                verify(reversalUseCase).execute(request, userId);
        }

        @Test
        void reverseTransferReturnsCreatedResponse() {
                UUID transactionId = UUID.randomUUID();
                TransactionResponse reversal = new TransactionResponse(UUID.randomUUID(), accountId,
                                TransactionType.REVERSAL_CREDIT,
                                BigDecimal.valueOf(20), "USD", BigDecimal.valueOf(80), "transfer reversal", null,
                                UUID.randomUUID(),
                                TransactionStatus.COMPLETED, Instant.now());
                List<TransactionResponse> response = List.of(reversal);

                when(reversalUseCase.reverseTransfer(transactionId, userId)).thenReturn(response);

                ResponseEntity<List<TransactionResponse>> result = controller.reverseTransfer(transactionId,
                                authentication);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(result.getBody()).isEqualTo(response);
                verify(reversalUseCase).reverseTransfer(transactionId, userId);
        }

        @Test
        void deleteTransactionReturnsMethodNotAllowed() {
                UUID transactionId = UUID.randomUUID();

                ResponseEntity<Void> result = controller.deleteTransaction(transactionId);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
                assertThat(result.getBody()).isNull();
        }
}
