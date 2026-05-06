package com.multi.currency.wallet.application.usecase;

import com.multi.currency.wallet.application.dto.request.ReversalRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import com.multi.currency.wallet.domain.exception.AlreadyReversedException;
import com.multi.currency.wallet.domain.exception.InvalidAccountOperationException;
import com.multi.currency.wallet.domain.exception.TransactionNotFoundException;
import com.multi.currency.wallet.domain.model.*;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReversalUseCase – full coverage")
class ReversalUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReversalUseCase reversalUseCase;

    // ── shared fixtures ────────────────────────────────────────────────────────

    private UUID userId;
    private UUID accountId;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        account = buildAccount(accountId, userId, "500");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // execute() – DEPOSIT reversal
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("execute – DEPOSIT reversal")
    class DepositReversal {

        @Test
        @DisplayName("returns exactly one REVERSAL_DEBIT response")
        void returnsOneReversalDebitResponse() {
            Transaction deposit = makeDeposit(accountId, "200");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            List<TransactionResponse> results = reversalUseCase.execute(req(deposit.getId()), userId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).type()).isEqualTo(TransactionType.REVERSAL_DEBIT);
        }

        @Test
        @DisplayName("debits the account by the deposit amount")
        void debitsAccountByDepositAmount() {
            Transaction deposit = makeDeposit(accountId, "200");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(deposit.getId()), userId);

            // account started at 500 → 500 − 200 = 300
            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("300");
        }

        @Test
        @DisplayName("persists the updated account")
        void savesAccount() {
            Transaction deposit = makeDeposit(accountId, "200");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(deposit.getId()), userId);

            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("saved reversal Transaction references the original transaction ID")
        void savedReversalReferencesOriginalId() {
            Transaction deposit = makeDeposit(accountId, "200");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            reversalUseCase.execute(req(deposit.getId()), userId);
            verify(transactionRepository).save(captor.capture());

            assertThat(captor.getValue().getReferenceTransactionId()).isEqualTo(deposit.getId());
        }

        @Test
        @DisplayName("saved reversal Transaction carries the deposit amount")
        void savedReversalCarriesCorrectAmount() {
            Transaction deposit = makeDeposit(accountId, "150");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            reversalUseCase.execute(req(deposit.getId()), userId);
            verify(transactionRepository).save(captor.capture());

            assertThat(captor.getValue().getAmount().getAmount()).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("saved reversal has the account's post-debit balance as balanceAfter")
        void savedReversalHasCorrectBalanceAfter() {
            Transaction deposit = makeDeposit(accountId, "200");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            reversalUseCase.execute(req(deposit.getId()), userId);
            verify(transactionRepository).save(captor.capture());

            // 500 − 200 = 300
            assertThat(captor.getValue().getBalanceAfter().getAmount()).isEqualByComparingTo("300");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // execute() – TRANSFER_DEBIT reversal
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("execute – TRANSFER_DEBIT reversal")
    class TransferDebitReversal {

        @Test
        @DisplayName("returns REVERSAL_CREDIT for the source leg when no credit leg found")
        void returnsReversalCreditWhenNoCreditLeg() {
            UUID destId = UUID.randomUUID();
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of());

            List<TransactionResponse> results = reversalUseCase.execute(req(debit.getId()), userId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).type()).isEqualTo(TransactionType.REVERSAL_CREDIT);
        }

        @Test
        @DisplayName("credits the source account back by the transfer amount")
        void creditsSourceAccountBack() {
            UUID destId = UUID.randomUUID();
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of());

            reversalUseCase.execute(req(debit.getId()), userId);

            // account 500 + 100 = 600
            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("600");
        }

        @Test
        @DisplayName("also reverses the matching credit leg atomically when found")
        void reversesMatchingCreditLegWhenFound() {
            UUID destId = UUID.randomUUID();
            Account destAccount = buildAccount(destId, UUID.randomUUID(), "300");
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            // Credit leg matches: same relatedAccountId (source) and same amount
            Transaction credit = makeTransferCredit(destId, accountId, "100");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            // credit leg not yet reversed
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(false);
            stubFindAccount(destId, destAccount);

            List<TransactionResponse> results = reversalUseCase.execute(req(debit.getId()), userId);

            // Both legs reversed → 2 results
            assertThat(results).hasSize(2);
            assertThat(results).extracting(TransactionResponse::type)
                    .containsExactlyInAnyOrder(TransactionType.REVERSAL_CREDIT, TransactionType.REVERSAL_DEBIT);
        }

        @Test
        @DisplayName("debits destination account when credit leg reversed")
        void debitsDestinationAccountForCreditLeg() {
            UUID destId = UUID.randomUUID();
            Account destAccount = buildAccount(destId, UUID.randomUUID(), "300");
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            Transaction credit = makeTransferCredit(destId, accountId, "100");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(false);
            stubFindAccount(destId, destAccount);

            reversalUseCase.execute(req(debit.getId()), userId);

            // destAccount 300 − 100 = 200
            assertThat(destAccount.getBalance().getAmount()).isEqualByComparingTo("200");
        }

        @Test
        @DisplayName("saves destination account after debiting it")
        void savesDestinationAccount() {
            UUID destId = UUID.randomUUID();
            Account destAccount = buildAccount(destId, UUID.randomUUID(), "300");
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            Transaction credit = makeTransferCredit(destId, accountId, "100");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(false);
            stubFindAccount(destId, destAccount);

            reversalUseCase.execute(req(debit.getId()), userId);

            verify(accountRepository).save(destAccount);
        }

        @Test
        @DisplayName("skips credit leg reversal silently when it is already reversed")
        void skipsCreditLegAlreadyReversed() {
            UUID destId = UUID.randomUUID();
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            Transaction credit = makeTransferCredit(destId, accountId, "100");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            // credit leg already reversed → skip it
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(true);

            List<TransactionResponse> results = reversalUseCase.execute(req(debit.getId()), userId);

            // Only the source debit reversal is returned
            assertThat(results).hasSize(1);
            assertThat(results.get(0).type()).isEqualTo(TransactionType.REVERSAL_CREDIT);
        }

        @Test
        @DisplayName("throws AccountNotFoundException when destination account is missing during credit-leg reversal")
        void throwsWhenDestinationAccountMissingForCreditLeg() {
            UUID destId = UUID.randomUUID();
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            Transaction credit = makeTransferCredit(destId, accountId, "100");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(false);
            when(accountRepository.findById(destId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reversalUseCase.execute(req(debit.getId()), userId))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Destination account not found");
        }

        @Test
        @DisplayName("skips credit leg search entirely when debit has no relatedAccountId")
        void skipsSearchWhenNoRelatedAccountId() {
            // Build a TRANSFER_DEBIT with null relatedAccountId
            Transaction debit = Transaction.createTransferDebit(
                    accountId, null,
                    Money.of("50", "USD"), Money.of("450", "USD"), "No dest");
            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);

            List<TransactionResponse> results = reversalUseCase.execute(req(debit.getId()), userId);

            assertThat(results).hasSize(1);
            // findByAccountId should never be called since relatedAccountId is null
            verify(transactionRepository, never()).findByAccountId(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // execute() – TRANSFER_CREDIT reversal ← previously untested
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("execute – TRANSFER_CREDIT reversal")
    class TransferCreditReversal {

        @Test
        @DisplayName("returns exactly one REVERSAL_DEBIT response")
        void returnsOneReversalDebitResponse() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "80");
            stubFindTx(credit);
            stubNotYetReversed(credit.getId());
            stubFindAccount(accountId, account);

            List<TransactionResponse> results = reversalUseCase.execute(req(credit.getId()), userId);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).type()).isEqualTo(TransactionType.REVERSAL_DEBIT);
        }

        @Test
        @DisplayName("debits the destination account by the credit amount")
        void debitsDestinationAccountByAmount() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "80");
            stubFindTx(credit);
            stubNotYetReversed(credit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(credit.getId()), userId);

            // 500 − 80 = 420
            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("420");
        }

        @Test
        @DisplayName("saves account after debit")
        void savesAccount() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "80");
            stubFindTx(credit);
            stubNotYetReversed(credit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(credit.getId()), userId);

            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("saved reversal references the original credit transaction ID")
        void savedReversalReferencesOriginalId() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "80");
            stubFindTx(credit);
            stubNotYetReversed(credit.getId());
            stubFindAccount(accountId, account);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            reversalUseCase.execute(req(credit.getId()), userId);
            verify(transactionRepository).save(captor.capture());

            assertThat(captor.getValue().getReferenceTransactionId()).isEqualTo(credit.getId());
        }

        @Test
        @DisplayName("does NOT touch any other accounts or call findByAccountId")
        void doesNotSearchOtherAccounts() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "80");
            stubFindTx(credit);
            stubNotYetReversed(credit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(credit.getId()), userId);

            verify(transactionRepository, never()).findByAccountId(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // execute() – unsupported type ← previously untested
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("execute – unsupported transaction type")
    class UnsupportedType {

        /**
         * We fake a WITHDRAWAL type by constructing a stub Transaction whose
         * type is not one of DEPOSIT / TRANSFER_DEBIT / TRANSFER_CREDIT /
         * REVERSAL_CREDIT / REVERSAL_DEBIT so the else-branch is exercised.
         *
         * Because Transaction.createXxx factory methods don't cover WITHDRAWAL,
         * we use Mockito to spy the transaction and override getType().
         */
        @Test
        @DisplayName("throws InvalidAccountOperationException for WITHDRAWAL type")
        void throwsForWithdrawalType() {
            Transaction withdrawal = spy(makeDeposit(accountId, "50"));
            when(withdrawal.getType()).thenReturn(TransactionType.WITHDRAWAL);

            stubFindTx(withdrawal);
            stubNotYetReversed(withdrawal.getId());
            stubFindAccount(accountId, account);

            assertThatThrownBy(() -> reversalUseCase.execute(req(withdrawal.getId()), userId))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Unsupported transaction type for reversal")
                    .hasMessageContaining("WITHDRAWAL");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // execute() – guard checks
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("execute – guard checks")
    class GuardChecks {

        @Test
        @DisplayName("throws TransactionNotFoundException when original transaction does not exist")
        void throwsWhenTransactionNotFound() {
            UUID unknown = UUID.randomUUID();
            when(transactionRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reversalUseCase.execute(req(unknown), userId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining(unknown.toString());
        }

        @Test
        @DisplayName("throws AlreadyReversedException when transaction has already been reversed")
        void throwsWhenAlreadyReversed() {
            Transaction deposit = makeDeposit(accountId, "100");
            stubFindTx(deposit);
            when(transactionRepository.existsByReferenceTransactionId(deposit.getId())).thenReturn(true);

            assertThatThrownBy(() -> reversalUseCase.execute(req(deposit.getId()), userId))
                    .isInstanceOf(AlreadyReversedException.class)
                    .hasMessageContaining("already been reversed");
        }

        @Test
        @DisplayName("throws InvalidAccountOperationException when trying to reverse a REVERSAL_DEBIT")
        void throwsWhenTryingToReverseReversalDebit() {
            Transaction reversalDebit = Transaction.createReversal(
                    accountId, UUID.randomUUID(),
                    Money.of("100", "USD"), Money.of("400", "USD"),
                    TransactionType.DEPOSIT); // creates REVERSAL_DEBIT

            stubFindTx(reversalDebit);
            stubNotYetReversed(reversalDebit.getId());

            assertThatThrownBy(() -> reversalUseCase.execute(req(reversalDebit.getId()), userId))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("cannot itself be reversed");
        }

        @Test
        @DisplayName("throws InvalidAccountOperationException when trying to reverse a REVERSAL_CREDIT")
        void throwsWhenTryingToReverseReversalCredit() {
            Transaction reversalCredit = Transaction.createReversal(
                    accountId, UUID.randomUUID(),
                    Money.of("100", "USD"), Money.of("600", "USD"),
                    TransactionType.TRANSFER_DEBIT); // creates REVERSAL_CREDIT

            stubFindTx(reversalCredit);
            stubNotYetReversed(reversalCredit.getId());

            assertThatThrownBy(() -> reversalUseCase.execute(req(reversalCredit.getId()), userId))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("cannot itself be reversed");
        }

        @Test
        @DisplayName("throws AccountNotFoundException when the account for the transaction does not exist")
        void throwsWhenAccountNotFound() {
            Transaction deposit = makeDeposit(accountId, "100");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reversalUseCase.execute(req(deposit.getId()), userId))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(accountId.toString());
        }

        @Test
        @DisplayName("throws AccessDeniedException when requestingUserId does not match account owner")
        void throwsWhenUserDoesNotOwnAccount() {
            UUID otherUserId = UUID.randomUUID();
            Transaction deposit = makeDeposit(accountId, "100");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account); // account.userId == userId, not otherUserId

            assertThatThrownBy(() -> reversalUseCase.execute(req(deposit.getId()), otherUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("do not own");
        }

        @Test
        @DisplayName("does NOT call accountRepository.save when ownership check fails")
        void doesNotSaveWhenOwnershipFails() {
            UUID otherUserId = UUID.randomUUID();
            Transaction deposit = makeDeposit(accountId, "100");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            assertThatThrownBy(() -> reversalUseCase.execute(req(deposit.getId()), otherUserId));
            verify(accountRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // reverseTransfer()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reverseTransfer")
    class ReverseTransfer {

        // ── happy paths ───────────────────────────────────────────────────────

        @Test
        @DisplayName("debit leg passed in → delegates directly to execute() without extra lookup")
        void debitLegPassedIn_delegatesToExecute() {
            UUID destId = UUID.randomUUID();
            Transaction debit = makeTransferDebit(accountId, destId, "150");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of());

            List<TransactionResponse> results = reversalUseCase.reverseTransfer(debit.getId(), userId);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).type()).isEqualTo(TransactionType.REVERSAL_CREDIT);
        }

        @Test
        @DisplayName("credit leg passed in → resolves debit leg via findMatchingDebitLeg() then reverses")
        void creditLegPassedIn_resolvesDebitLegAndReverses() {
            UUID destId = accountId; // credit is ON this account
            UUID sourceId = UUID.randomUUID(); // debit is on source account
            Account sourceAccount = buildAccount(sourceId, userId, "400");

            Transaction credit = makeTransferCredit(destId, sourceId, "120");
            Transaction debit = makeTransferDebit(sourceId, destId, "120");

            // First call: load the credit leg
            when(transactionRepository.findById(credit.getId())).thenReturn(Optional.of(credit));
            // findMatchingDebitLeg searches source account's transactions
            when(transactionRepository.findByAccountId(sourceId)).thenReturn(List.of(debit));
            // Second call (inside execute): reload the debit leg
            when(transactionRepository.findById(debit.getId())).thenReturn(Optional.of(debit));
            stubNotYetReversed(debit.getId());
            stubFindAccount(sourceId, sourceAccount);
            // credit leg search from execute → reverseCreditLeg
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(false);
            // destination account for credit-leg reversal
            Account destAccount = buildAccount(destId, UUID.randomUUID(), "300");
            when(accountRepository.findById(destId)).thenReturn(Optional.of(destAccount));

            List<TransactionResponse> results = reversalUseCase.reverseTransfer(credit.getId(), userId);

            assertThat(results).isNotEmpty();
        }

        // ── failure paths ─────────────────────────────────────────────────────

        @Test
        @DisplayName("throws TransactionNotFoundException when the transaction ID does not exist")
        void throwsWhenTransactionNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(transactionRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reversalUseCase.reverseTransfer(unknownId, userId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }

        @Test
        @DisplayName("throws InvalidAccountOperationException when tx is a DEPOSIT (not a transfer)")
        void throwsWhenTransactionIsDeposit() {
            Transaction deposit = makeDeposit(accountId, "100");
            when(transactionRepository.findById(deposit.getId())).thenReturn(Optional.of(deposit));

            assertThatThrownBy(() -> reversalUseCase.reverseTransfer(deposit.getId(), userId))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("not a transfer transaction");
        }

        @Test
        @DisplayName("throws InvalidAccountOperationException when tx is a REVERSAL_DEBIT (not a transfer)")
        void throwsWhenTransactionIsReversalDebit() {
            Transaction reversal = Transaction.createReversal(
                    accountId, UUID.randomUUID(),
                    Money.of("100", "USD"), Money.of("400", "USD"),
                    TransactionType.DEPOSIT);
            when(transactionRepository.findById(reversal.getId())).thenReturn(Optional.of(reversal));

            assertThatThrownBy(() -> reversalUseCase.reverseTransfer(reversal.getId(), userId))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("not a transfer transaction");
        }

        @Test
        @DisplayName("throws InvalidAccountOperationException when credit leg has null relatedAccountId")
        void throwsWhenCreditLegHasNullRelatedAccountId() {
            // TRANSFER_CREDIT with null relatedAccountId
            Transaction credit = Transaction.createTransferCredit(
                    accountId, null,
                    Money.of("50", "USD"), Money.of("550", "USD"), "no source");

            when(transactionRepository.findById(credit.getId())).thenReturn(Optional.of(credit));

            assertThatThrownBy(() -> reversalUseCase.reverseTransfer(credit.getId(), userId))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Cannot locate matching debit leg");
        }

        @Test
        @DisplayName("throws TransactionNotFoundException when no matching debit leg found on source account")
        void throwsWhenMatchingDebitLegNotFound() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "75");

            when(transactionRepository.findById(credit.getId())).thenReturn(Optional.of(credit));
            // Source account has transactions but NONE matching a TRANSFER_DEBIT for this
            // amount/account
            Transaction unrelatedTx = makeDeposit(sourceId, "75");
            when(transactionRepository.findByAccountId(sourceId)).thenReturn(List.of(unrelatedTx));

            assertThatThrownBy(() -> reversalUseCase.reverseTransfer(credit.getId(), userId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining("Matching debit leg not found");
        }

        @Test
        @DisplayName("throws TransactionNotFoundException when source account has no transactions at all")
        void throwsWhenSourceAccountHasNoTransactions() {
            UUID sourceId = UUID.randomUUID();
            Transaction credit = makeTransferCredit(accountId, sourceId, "75");

            when(transactionRepository.findById(credit.getId())).thenReturn(Optional.of(credit));
            when(transactionRepository.findByAccountId(sourceId)).thenReturn(List.of());

            assertThatThrownBy(() -> reversalUseCase.reverseTransfer(credit.getId(), userId))
                    .isInstanceOf(TransactionNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Interaction / side-effect verifications
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("interaction verifications")
    class Interactions {

        @Test
        @DisplayName("exactly one transactionRepository.save() called for a DEPOSIT reversal")
        void exactlyOneSaveForDepositReversal() {
            Transaction deposit = makeDeposit(accountId, "100");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(deposit.getId()), userId);

            verify(transactionRepository, times(1)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("two transactionRepository.save() calls when both transfer legs are reversed")
        void twoSavesForFullTransferReversal() {
            UUID destId = UUID.randomUUID();
            Account destAccount = buildAccount(destId, UUID.randomUUID(), "200");
            Transaction debit = makeTransferDebit(accountId, destId, "100");
            Transaction credit = makeTransferCredit(destId, accountId, "100");

            stubFindTx(debit);
            stubNotYetReversed(debit.getId());
            stubFindAccount(accountId, account);
            when(transactionRepository.findByAccountId(destId)).thenReturn(List.of(credit));
            when(transactionRepository.existsByReferenceTransactionId(credit.getId())).thenReturn(false);
            stubFindAccount(destId, destAccount);

            reversalUseCase.execute(req(debit.getId()), userId);

            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }

        @Test
        @DisplayName("existsByReferenceTransactionId checked exactly once per execute() call")
        void idempotencyCheckCalledOnce() {
            Transaction deposit = makeDeposit(accountId, "50");
            stubFindTx(deposit);
            stubNotYetReversed(deposit.getId());
            stubFindAccount(accountId, account);

            reversalUseCase.execute(req(deposit.getId()), userId);

            verify(transactionRepository, times(1)).existsByReferenceTransactionId(deposit.getId());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Build an Account owned by the given user with the given starting balance. */
    private Account buildAccount(UUID id, UUID owner, String balance) {
        return new Account(
                id, generateAccountNumber(), owner,
                Money.of(balance, "USD"), "USD",
                AccountStatus.ACTIVE, Instant.now(), Instant.now());
    }

    private String generateAccountNumber() {
        return String.format("%012d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000_000L));
    }

    private Transaction makeDeposit(UUID forAccountId, String amount) {
        return Transaction.createDeposit(
                forAccountId,
                Money.of(amount, "USD"),
                Money.of("700", "USD"),
                "Test deposit");
    }

    private Transaction makeTransferDebit(UUID fromAccountId, UUID toAccountId, String amount) {
        return Transaction.createTransferDebit(
                fromAccountId, toAccountId,
                Money.of(amount, "USD"),
                Money.of("300", "USD"),
                "Transfer");
    }

    private Transaction makeTransferCredit(UUID toAccountId, UUID fromAccountId, String amount) {
        return Transaction.createTransferCredit(
                toAccountId, fromAccountId,
                Money.of(amount, "USD"),
                Money.of("400", "USD"),
                "Transfer");
    }

    private ReversalRequest req(UUID originalTransactionId) {
        return new ReversalRequest(originalTransactionId, "Test reason");
    }

    private void stubFindTx(Transaction tx) {
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
    }

    private void stubNotYetReversed(UUID txId) {
        when(transactionRepository.existsByReferenceTransactionId(txId)).thenReturn(false);
    }

    private void stubFindAccount(UUID id, Account acc) {
        when(accountRepository.findById(id)).thenReturn(Optional.of(acc));
    }
}