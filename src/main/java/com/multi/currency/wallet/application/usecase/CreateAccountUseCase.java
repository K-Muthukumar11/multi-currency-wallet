package com.multi.currency.wallet.application.usecase;

import java.util.List;
import java.util.UUID;

import com.multi.currency.wallet.application.dto.request.CreateAccountRequest;
import com.multi.currency.wallet.application.dto.response.AccountResponse;

public class CreateAccountUseCase {

    public AccountResponse execute(UUID userId, CreateAccountRequest request) {
        return null;
    }

    public List<AccountResponse> getAccountForUser(UUID userUuid) {
        return null;
    }

}