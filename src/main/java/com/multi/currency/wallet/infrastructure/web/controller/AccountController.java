package com.multi.currency.wallet.infrastructure.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.multi.currency.wallet.application.dto.request.CreateAccountRequest;
import com.multi.currency.wallet.application.dto.response.AccountResponse;

@RestController
public class AccountController {

    public ResponseEntity<AccountResponse> createAccount(CreateAccountRequest request, Authentication authentication) {
        return null;
    }

    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        return null;
    }

}
