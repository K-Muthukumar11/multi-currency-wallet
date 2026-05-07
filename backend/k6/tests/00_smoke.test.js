// ============================================================
// tests/00_smoke.test.js — End-to-end smoke (single VU)
// Fast sanity check: register → account → deposit → reverse
// → re-deposit → transfer → list transactions → reverse transfer
// → double-reversal guard.  All in one run.
// ============================================================

import { check, group } from 'k6';
import {
  register, login,
  createAccount, getMyAccounts,
  deposit, transfer,
  getMyTransactions,
  reverseTransaction, reverseTransfer,
} from '../helpers/http.js';
import { THRESHOLDS } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

export default function () {
  const ts    = Date.now();
  const email = `smoke_${ts}@wallet.test`;
  const pass  = 'Smoke1234!';

  let token, accA, accB, txnId, debitTxnId;

  // ── Step 1: Register & Login ──────────────────────────────
  group('SMOKE-01 | Register', () => {
    const res = register(email, pass, 'Smoke User');
    check(res, { 'register 201': (r) => r.status === 201 });
  });

  group('SMOKE-02 | Login', () => {
    const res = login(email, pass);
    check(res, { 'login 200': (r) => r.status === 200 });
    token = JSON.parse(res.body).token;
  });

  // ── Step 2: Create accounts ───────────────────────────────
  group('SMOKE-03 | Create USD account A', () => {
    const res = createAccount(token, 'USD');
    check(res, { 'create USD account 201': (r) => r.status === 201 });
    accA = JSON.parse(res.body);
  });

  group('SMOKE-04 | Create USD account B', () => {
    const res = createAccount(token, 'USD');
    check(res, { 'create USD account B 201': (r) => r.status === 201 });
    accB = JSON.parse(res.body);
  });

  // ── Step 3: Deposit ───────────────────────────────────────
  group('SMOKE-05 | Deposit 1000 USD into account A', () => {
    const res  = deposit(token, accA.accountNumber, 1000, 'USD', 'Smoke seed');
    const body = JSON.parse(res.body);
    check(res, {
      'deposit 201':      (r) => r.status === 201,
      'balance is 1000':  () => parseFloat(body.balanceAfter) === 1000,
      'type is DEPOSIT':  () => body.type === 'DEPOSIT',
    });
    txnId = body.id;
  });

  // ── Step 4: Reverse the deposit BEFORE any transfer ───────
  // Balance is still 1000 here so the reversal succeeds.
  group('SMOKE-06 | Reverse the deposit', () => {
    const res  = reverseTransaction(token, txnId, 'Smoke reversal');
    const body = JSON.parse(res.body);
    check(res, {
      'reversal 201':                       (r) => r.status === 201,
      'reversal references original txnId': () => body[0]?.referenceTransactionId === txnId,
    });
  });

  // ── Step 5: Double-reversal guard ─────────────────────────
  // A reversal record now exists for txnId, so this must be rejected.
  group('SMOKE-07 | Double-reversal of deposit is rejected', () => {
    const res = reverseTransaction(token, txnId, 'second reversal');
    check(res, {
      'double reversal 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Step 6: Re-deposit so account A has funds for transfer ─
  group('SMOKE-08 | Re-deposit 1000 USD into account A', () => {
    const res  = deposit(token, accA.accountNumber, 1000, 'USD', 'Smoke re-seed');
    const body = JSON.parse(res.body);
    check(res, {
      're-deposit 201':    (r) => r.status === 201,
      'balance is 1000':   () => parseFloat(body.balanceAfter) === 1000,
    });
  });

  // ── Step 7: Transfer ──────────────────────────────────────
  group('SMOKE-09 | Transfer 300 USD A → B', () => {
    const res  = transfer(token, accA.accountNumber, accB.accountNumber, 300, 'USD', 'Smoke transfer');
    const body = JSON.parse(res.body);
    check(res, {
      'transfer 201':               (r) => r.status === 201,
      'debit balanceAfter is 700':  () => parseFloat(body.debitTransaction.balanceAfter) === 700,
      'credit balanceAfter is 300': () => parseFloat(body.creditTransaction.balanceAfter) === 300,
    });
    debitTxnId = body.debitTransaction.id;
  });

  // ── Step 8: List transactions ─────────────────────────────
  group('SMOKE-10 | List my transactions', () => {
    const res  = getMyTransactions(token);
    const txns = JSON.parse(res.body);
    check(res, {
      'list 200':                 (r) => r.status === 200,
      'at least 3 entries':       () => txns.length >= 3,
      'has DEPOSIT type':         () => txns.some((t) => t.type === 'DEPOSIT'),
      'has TRANSFER_DEBIT type':  () => txns.some((t) => t.type === 'TRANSFER_DEBIT'),
      'has TRANSFER_CREDIT type': () => txns.some((t) => t.type === 'TRANSFER_CREDIT'),
    });
  });

  // ── Step 9: Reverse transfer (both legs) ──────────────────
  group('SMOKE-11 | Reverse the transfer atomically', () => {
    const res  = reverseTransfer(token, debitTxnId);
    const body = JSON.parse(res.body);
    check(res, {
      'reverse-transfer 201':        (r) => r.status === 201,
      '2 reversal entries returned': () => body.length === 2,
    });
  });
}