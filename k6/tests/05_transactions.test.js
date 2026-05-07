// ============================================================
// tests/05_transactions.test.js — Transaction listing tests
// ============================================================

import { check, group } from 'k6';
import http from 'k6/http';
import {
  registerAndLogin,
  setupAccount,
  setupDeposit,
  transfer,
  getMyTransactions,
  getAccountTransactions,
} from '../helpers/http.js';
import { THRESHOLDS, BASE_URL } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

export default function () {
  const { token } = registerAndLogin('_txn');
  const acc       = setupAccount(token, 'USD');
  const accNum    = acc.accountNumber;

  // Seed some transactions
  setupDeposit(token, accNum, 500,   'USD');
  setupDeposit(token, accNum, 250,   'USD');

  // ── Happy-path: GET /transactions (all user transactions) ─
  group('TXN-01 | Authenticated user can list own transactions', () => {
    const res  = getMyTransactions(token);
    const body = JSON.parse(res.body);
    check(res, {
      'list transactions returns 200':   (r) => r.status === 200,
      'response is array':               () => Array.isArray(body),
      'at least 2 transactions present': () => body.length >= 2,
    });
  });

  // ── Happy-path: GET /accounts/{id}/transactions (paginated)
  group('TXN-02 | Get transactions by account ID (paginated)', () => {
    const res  = getAccountTransactions(token, accNum);
    const body = JSON.parse(res.body);
    check(res, {
      'account txns returns 200':     (r) => r.status === 200,
      'has content field (Page)':     () => body.content !== undefined,
      'content is array':             () => Array.isArray(body.content),
      'has totalElements':            () => body.totalElements !== undefined,
    });
  });

  // ── Happy-path: Transaction fields are complete ───────────
  group('TXN-03 | Transaction response has all required fields', () => {
    const res  = getMyTransactions(token);
    const txn  = JSON.parse(res.body)[0];
    check(res, {
      'txn has id':                   () => !!txn.id,
      'txn has accountId':            () => !!txn.accountId,
      'txn has type':                 () => !!txn.type,
      'txn has amount':               () => txn.amount !== undefined,
      'txn has currencyCode':         () => !!txn.currencyCode,
      'txn has balanceAfter':         () => txn.balanceAfter !== undefined,
      'txn has status':               () => !!txn.status,
      'txn has createdAt':            () => !!txn.createdAt,
    });
  });

  // ── Happy-path: Transfer transactions appear for both users
  group('TXN-04 | Transfer creates ledger entries for both parties', () => {
    const { token: tokenB } = registerAndLogin('_txn_b');
    const accB = setupAccount(tokenB, 'USD');
    transfer(token, accNum, accB.accountNumber, 100, 'USD', 'ledger check');

    const resA = getMyTransactions(token);
    const resB = getMyTransactions(tokenB);

    const typesA = JSON.parse(resA.body).map((t) => t.type);
    const typesB = JSON.parse(resB.body).map((t) => t.type);

    check(resA, {
      'sender ledger has TRANSFER_DEBIT':   () => typesA.includes('TRANSFER_DEBIT'),
    });
    check(resB, {
      'receiver ledger has TRANSFER_CREDIT': () => typesB.includes('TRANSFER_CREDIT'),
    });
  });

  // ── Happy-path: Pagination parameters are respected ───────
  group('TXN-05 | Pagination: page size is respected', () => {
    // Add more deposits to ensure >1 page
    for (let i = 0; i < 5; i++) {
      setupDeposit(token, accNum, 10, 'USD');
    }
    const res  = http.get(
      `${BASE_URL}/api/v1/accounts/${accNum}/transactions?size=3&page=0`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    const body = JSON.parse(res.body);
    check(res, {
      'page=0 size=3 returns 200':      (r) => r.status === 200,
      'content has at most 3 items':    () => body.content.length <= 3,
    });
  });

  // ── Edge: Transaction sorted by createdAt DESC (default) ──
  group('TXN-06 | Transactions sorted newest-first by default', () => {
    const res   = getAccountTransactions(token, accNum);
    const items = JSON.parse(res.body).content;
    if (items.length >= 2) {
      const first  = new Date(items[0].createdAt).getTime();
      const second = new Date(items[1].createdAt).getTime();
      check(res, {
        'first txn is newer or equal to second': () => first >= second,
      });
    } else {
      check(res, { 'not enough txns to test sort (skip)': () => true });
    }
  });

  // ── Edge: Unauthenticated list is rejected ─────────────────
  group('TXN-07 | Listing transactions without auth returns 401/403', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/transactions`,
      { headers: {} },
    );
    check(res, {
      'no-token list returns 401 or 403': (r) => r.status === 401 || r.status === 403,
    });
  });

  // ── Edge: Non-existent account ID returns 404 ────────────
  group('TXN-08 | Transactions for non-existent account number returns 404', () => {
      const fakeAccountNumber = '000000000000';
      const res = getAccountTransactions(token, fakeAccountNumber);
      check(res, {
        'fake accountNumber returns 404': (r) => r.status === 404,
      });
  });

  // ── Edge: Immutability — no DELETE endpoint ───────────────
  group('TXN-09 | No DELETE /transactions endpoint (immutability)', () => {
      const txns  = JSON.parse(getMyTransactions(token).body);
      const txnId = txns[0]?.id || '00000000-0000-0000-0000-000000000000';

      const res = http.del(
          `${BASE_URL}/api/v1/transactions/${txnId}`,
          null,
          { headers: { Authorization: `Bearer ${token}` } },
      );
      check(res, {
          'DELETE /transactions/{id} returns 404 or 405': (r) => r.status === 404 || r.status === 405,
      });
  });
}
