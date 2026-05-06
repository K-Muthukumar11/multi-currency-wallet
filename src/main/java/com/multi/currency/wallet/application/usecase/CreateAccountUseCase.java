package com.multi.currency.wallet.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.multi.currency.wallet.application.dto.request.CreateAccountRequest;
import com.multi.currency.wallet.application.dto.response.AccountResponse;
import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse execute(UUID userId, CreateAccountRequest request) {
        Account account = Account.open(userId, request.currencyCode());
        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountForUser(UUID userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }
}
