// ============================================================
// tests/04_transfer.test.js — Transfer API tests
// Core business rule: atomic debit+credit, same-currency only
// ============================================================

import { check, group } from 'k6';
import http from 'k6/http';
import {
  registerAndLogin,
  setupAccount,
  setupDeposit,
  transfer,
  getMyTransactions,
} from '../helpers/http.js';
import { THRESHOLDS, BASE_URL } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

export default function () {
  // ── Setup two users with funded USD accounts ──────────────
  const { token: tokenA } = registerAndLogin('_txfA');
  const accA = setupAccount(tokenA, 'USD');
  setupDeposit(tokenA, accA.accountNumber, 1000, 'USD');

  const { token: tokenB } = registerAndLogin('_txfB');
  const accB = setupAccount(tokenB, 'USD');

  // ── Happy-path: Basic transfer ────────────────────────────
  group('TRF-01 | Transfer funds between two USD accounts', () => {
    const res  = transfer(tokenA, accA.accountNumber, accB.accountNumber, 200, 'USD', 'First transfer');
    const body = JSON.parse(res.body);
    check(res, {
      'transfer returns 201':            (r) => r.status === 201,
      'debit txn present':               () => !!body.debitTransaction,
      'credit txn present':              () => !!body.creditTransaction,
      'debit type is TRANSFER_DEBIT':    () => body.debitTransaction.type === 'TRANSFER_DEBIT',
      'credit type is TRANSFER_CREDIT':  () => body.creditTransaction.type === 'TRANSFER_CREDIT',
      'debit amount is 200':             () => body.debitTransaction.amount === 200,
      'credit amount is 200':            () => body.creditTransaction.amount === 200,
      'debit status is COMPLETED':       () => body.debitTransaction.status === 'COMPLETED',
      'credit status is COMPLETED':      () => body.creditTransaction.status === 'COMPLETED',
    });
  });

  // ── Happy-path: Verify sender balance after transfer ──────
  group('TRF-02 | Sender balance decremented correctly', () => {
    // Sender started with 1000, transferred 200 in TRF-01
    const res  = transfer(tokenA, accA.accountNumber, accB.accountNumber, 100, 'USD', 'Second transfer');
    const body = JSON.parse(res.body);
    check(res, {
      'second transfer returns 201':              (r) => r.status === 201,
      'sender balanceAfter reflects deduction':   () => body.debitTransaction.balanceAfter === 700,
    });
  });

  // ── Happy-path: Exact balance transfer (edge boundary) ───
  group('TRF-03 | Transfer exact remaining balance succeeds', () => {
    // accA now has 700 (1000-200-100)
    const res = transfer(tokenA, accA.accountNumber, accB.accountNumber, 700, 'USD', 'Drain account');
    check(res, {
      'exact balance transfer returns 201': (r) => r.status === 201,
    });
  });

  // ── Happy-path: 3 decimal precision in transfer ───────────
  group('TRF-04 | Transfer with 3-decimal precision', () => {
    // Re-fund accA
    setupDeposit(tokenA, accA.accountNumber, 500, 'USD');
    const res  = transfer(tokenA, accA.accountNumber, accB.accountNumber, 10.005, 'USD', '3dp transfer');
    const body = JSON.parse(res.body);
    check(res, {
      '3dp transfer returns 201':           (r) => r.status === 201,
      'debit amount is 10.005':             () => body.debitTransaction.amount === 10.005,
    });
  });

  // ── Edge: Insufficient funds ──────────────────────────────
  group('TRF-05 | Transfer more than available balance returns 422', () => {
    // accA has 500 - 10.005 = 489.995; try to transfer 10000
    const res = transfer(tokenA, accA.accountNumber, accB.accountNumber, 10000, 'USD', 'overdraft attempt');
    check(res, {
      'insufficient funds returns 422': (r) => r.status === 422,
    });
  });

  // ── Edge: Cross-currency transfer is rejected ─────────────
  group('TRF-06 | Cross-currency transfer is rejected', () => {
    const { token: tokenC } = registerAndLogin('_txfC');
    const accC = setupAccount(tokenC, 'EUR');
    setupDeposit(tokenC, accC.accountNumber, 1000, 'EUR');

    // Try to transfer EUR from EUR account to USD account
    const res = transfer(tokenC, accC.accountNumber, accB.accountNumber, 100, 'EUR', 'cross-currency');
    check(res, {
      'cross-currency transfer returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Edge: Currency code in request mismatches account ────
  group('TRF-07 | Wrong currencyCode in request body returns 4xx', () => {
    setupDeposit(tokenA, accA.accountNumber, 100, 'USD');
    const res = transfer(tokenA, accA.accountNumber, accB.accountNumber, 50, 'EUR');
    check(res, {
      'currency mismatch returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Edge: Self-transfer (same account) ────────────────────
  group('TRF-08 | Self-transfer is rejected', () => {
    const res = transfer(tokenA, accA.accountNumber, accA.accountNumber, 50, 'USD', 'self-transfer');
    check(res, {
      'self-transfer returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Edge: Non-existent source account ─────────────────────
  group('TRF-09 | Non-existent source account returns 404', () => {
    const res = transfer(tokenA, 'FAKE-SOURCE-000', accB.accountNumber, 50, 'USD');
    check(res, {
      'non-existent source returns 404': (r) => r.status === 404,
    });
  });

  // ── Edge: Non-existent destination account ────────────────
  group('TRF-10 | Non-existent destination account returns 404', () => {
    const res = transfer(tokenA, accA.accountNumber, 'FAKE-DEST-000', 50, 'USD');
    check(res, {
      'non-existent destination returns 404': (r) => r.status === 404,
    });
  });

  // ── Edge: Zero amount ─────────────────────────────────────
  group('TRF-11 | Zero amount transfer is rejected', () => {
    const res = transfer(tokenA, accA.accountNumber, accB.accountNumber, 0, 'USD');
    check(res, {
      'zero amount returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Negative amount ─────────────────────────────────
  group('TRF-12 | Negative amount transfer is rejected', () => {
    const res = transfer(tokenA, accA.accountNumber, accB.accountNumber, -50, 'USD');
    check(res, {
      'negative amount returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Missing source account number ───────────────────
  group('TRF-13 | Missing sourceAccountNumber returns 400', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/transfers`,
      JSON.stringify({ destinationAccountNumber: accB.accountNumber, amount: 50, currencyCode: 'USD' }),
      { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${tokenA}` } },
    );
    check(res, {
      'missing source returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Unauthenticated transfer ────────────────────────
  group('TRF-14 | Transfer without auth token returns 401/403', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/transfers`,
      JSON.stringify({ sourceAccountNumber: accA.accountNumber, destinationAccountNumber: accB.accountNumber, amount: 50, currencyCode: 'USD' }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(res, {
      'no-token transfer returns 401 or 403': (r) => r.status === 401 || r.status === 403,
    });
  });

  // ── Atomicity: Verify both legs appear in transaction list
  group('TRF-15 | Atomicity — both debit and credit appear in ledger', () => {
    setupDeposit(tokenA, accA.accountNumber, 200, 'USD');
    transfer(tokenA, accA.accountNumber, accB.accountNumber, 50, 'USD', 'atomicity check');

    const resA = getMyTransactions(tokenA);
    const resB = getMyTransactions(tokenB);
    const txnsA = JSON.parse(resA.body);
    const txnsB = JSON.parse(resB.body);

    const hasDebit  = txnsA.some((t) => t.type === 'TRANSFER_DEBIT');
    const hasCredit = txnsB.some((t) => t.type === 'TRANSFER_CREDIT');

    check(resA, {
      'sender has TRANSFER_DEBIT entry': () => hasDebit,
    });
    check(resB, {
      'receiver has TRANSFER_CREDIT entry': () => hasCredit,
    });
  });
}
