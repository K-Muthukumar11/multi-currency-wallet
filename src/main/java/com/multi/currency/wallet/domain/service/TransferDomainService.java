package com.multi.currency.wallet.domain.service;

import java.util.List;

import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.Money;
import com.multi.currency.wallet.domain.model.Transaction;

public class TransferDomainService {

    public List<Transaction> transfer(Account source, Account destination, Money amount, String description) {
        return null;
    }
}
