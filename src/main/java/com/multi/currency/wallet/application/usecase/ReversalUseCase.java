package com.multi.currency.wallet.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.multi.currency.wallet.application.dto.request.ReversalRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import com.multi.currency.wallet.domain.exception.AlreadyReversedException;
import com.multi.currency.wallet.domain.exception.InvalidAccountOperationException;
import com.multi.currency.wallet.domain.exception.TransactionNotFoundException;
import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.Transaction;
import com.multi.currency.wallet.domain.model.TransactionType;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Use Case: Reversal Transaction
 *
 * Business Rules Enforced:
 * 1. Transactions are immutable – we create a NEW reversal record referencing
 * the original.
 * 2. Each original transaction can only be reversed once (idempotency guard).
 * 3. Only the owner of the account may initiate a reversal.
 * 4. Reversals are atomic: if the original was a transfer (two legs), both legs
 * are reversed
 * or neither is (wrapped in a single @Transactional boundary).
 * 5. DEPOSIT reversal: debit the account (undo the credit).
 * 6. TRANSFER_DEBIT reversal: credit back the source account.
 * 7. TRANSFER_CREDIT reversal: debit the destination account (funds returned to
 * source).
 * Full transfer reversal reverses both legs atomically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReversalUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Reverse a single transaction (DEPOSIT or one leg of a TRANSFER).
     * For a full transfer reversal, use {@link #reverseTransfer}.
     */
    @Transactional
    public List<TransactionResponse> execute(ReversalRequest request, UUID requestingUserId) {
        UUID originalId = request.originalTransactionId();
        log.info("Reversal requested for transaction {} by user {}", originalId, requestingUserId);

        // 1. Load original transaction
        Transaction original = transactionRepository.findById(originalId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + originalId));

        // 2. Guard: already reversed?
        if (transactionRepository.existsByReferenceTransactionId(originalId)) {
            throw new AlreadyReversedException("Transaction " + originalId + " has already been reversed");
        }

        // 3. Guard: only reversible types
        TransactionType type = original.getType();
        if (type == TransactionType.REVERSAL_CREDIT || type == TransactionType.REVERSAL_DEBIT) {
            throw new InvalidAccountOperationException("A reversal transaction cannot itself be reversed");
        }

        // 4. Load the account the original transaction belongs to
        Account account = accountRepository.findById(original.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + original.getAccountId()));

        // 5. Ownership check
        if (!account.getUserId().equals(requestingUserId)) {
            throw new AccessDeniedException("You do not own the account associated with this transaction");
        }

        List<Transaction> reversals = new ArrayList<>();

        if (type == TransactionType.DEPOSIT) {
            // Reverse a deposit: debit the account back
            account.debit(original.getAmount());
            accountRepository.save(account);

            Transaction reversal = Transaction.createReversal(
                    account.getId(), originalId, original.getAmount(),
                    account.getBalance(), type);
            transactionRepository.save(reversal);
            reversals.add(reversal);
            log.info("Deposit reversal created: {}", reversal.getId());

        } else if (type == TransactionType.TRANSFER_DEBIT) {
            // Reverse source-side of a transfer: credit the source account back
            account.credit(original.getAmount());
            accountRepository.save(account);

            Transaction reversal = Transaction.createReversal(
                    account.getId(), originalId, original.getAmount(),
                    account.getBalance(), type);
            transactionRepository.save(reversal);
            reversals.add(reversal);
            log.info("Transfer-debit reversal created: {}", reversal.getId());

            // Also find and reverse the corresponding credit leg if it hasn't been reversed
            if (original.getRelatedAccountId() != null) {
                reversals.addAll(reverseCreditLeg(original, requestingUserId));
            }

        } else if (type == TransactionType.TRANSFER_CREDIT) {
            // Reverse destination-side: debit the destination account
            account.debit(original.getAmount());
            accountRepository.save(account);

            Transaction reversal = Transaction.createReversal(
                    account.getId(), originalId, original.getAmount(),
                    account.getBalance(), type);
            transactionRepository.save(reversal);
            reversals.add(reversal);
            log.info("Transfer-credit reversal created: {}", reversal.getId());

        } else {
            throw new InvalidAccountOperationException("Unsupported transaction type for reversal: " + type);
        }

        return reversals.stream().map(TransactionResponse::from).toList();
    }

    /**
     * Atomically reverse both legs of a transfer given either leg's transaction ID.
     * Useful when the caller wants to undo an entire transfer in one API call.
     */
    @Transactional
    public List<TransactionResponse> reverseTransfer(UUID transactionId, UUID requestingUserId) {
        log.info("Full transfer reversal requested for tx {} by user {}", transactionId, requestingUserId);

        Transaction leg = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));

        // Normalise: find the DEBIT leg (source side), regardless of which leg was
        // passed
        Transaction debitLeg;
        if (leg.getType() == TransactionType.TRANSFER_DEBIT) {
            debitLeg = leg;
        } else if (leg.getType() == TransactionType.TRANSFER_CREDIT) {
            // The related account's debit transaction references this account as
            // relatedAccountId
            // We search for a TRANSFER_DEBIT on the related account pointing here
            debitLeg = findMatchingDebitLeg(leg);
        } else {
            throw new InvalidAccountOperationException(
                    "Transaction " + transactionId + " is not a transfer transaction");
        }

        // Delegate to the standard reversal (which handles both legs)
        return execute(new ReversalRequest(debitLeg.getId(), "Full transfer reversal"), requestingUserId);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<Transaction> reverseCreditLeg(Transaction debitLeg, UUID requestingUserId) {
        List<Transaction> results = new ArrayList<>();
        UUID destinationAccountId = debitLeg.getRelatedAccountId();
        if (destinationAccountId == null)
            return results;

        // Find the matching TRANSFER_CREDIT on the destination account
        List<Transaction> destinationTxs = transactionRepository.findByAccountId(destinationAccountId);
        Transaction creditLeg = destinationTxs.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_CREDIT
                        && debitLeg.getId().equals(t.getReferenceTransactionId() != null
                                ? t.getReferenceTransactionId()
                                : null)
                        || (t.getType() == TransactionType.TRANSFER_CREDIT
                                && t.getRelatedAccountId() != null
                                && t.getRelatedAccountId().equals(debitLeg.getAccountId())
                                && t.getAmount().equals(debitLeg.getAmount())))
                .findFirst()
                .orElse(null);

        if (creditLeg == null)
            return results;

        // Skip if already reversed
        if (transactionRepository.existsByReferenceTransactionId(creditLeg.getId())) {
            log.warn("Credit leg {} already reversed; skipping", creditLeg.getId());
            return results;
        }

        Account destinationAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(
                        () -> new AccountNotFoundException("Destination account not found: " + destinationAccountId));

        destinationAccount.debit(creditLeg.getAmount());
        accountRepository.save(destinationAccount);

        Transaction reversal = Transaction.createReversal(
                destinationAccount.getId(), creditLeg.getId(),
                creditLeg.getAmount(), destinationAccount.getBalance(),
                creditLeg.getType());
        transactionRepository.save(reversal);
        results.add(reversal);
        log.info("Paired credit-leg reversal created: {}", reversal.getId());
        return results;
    }

    private Transaction findMatchingDebitLeg(Transaction creditLeg) {
        if (creditLeg.getRelatedAccountId() == null) {
            throw new InvalidAccountOperationException(
                    "Cannot locate matching debit leg for transaction: " + creditLeg.getId());
        }
        List<Transaction> sourceTxs = transactionRepository.findByAccountId(creditLeg.getRelatedAccountId());
        return sourceTxs.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_DEBIT
                        && t.getRelatedAccountId() != null
                        && t.getRelatedAccountId().equals(creditLeg.getAccountId())
                        && t.getAmount().equals(creditLeg.getAmount()))
                .findFirst()
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Matching debit leg not found for credit transaction: " + creditLeg.getId()));
    }
}
