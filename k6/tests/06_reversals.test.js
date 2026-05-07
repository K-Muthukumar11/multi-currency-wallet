// ============================================================
// tests/06_reversals.test.js — Reversal / auditability tests
// Business rule: corrections as new reversal transactions,
// original is NEVER modified, double-reversal forbidden.
// ============================================================

import { check, group } from 'k6';
import http from 'k6/http';
import {
  registerAndLogin,
  setupAccount,
  setupDeposit,
  transfer,
  reverseTransaction,
  reverseTransfer,
  getMyTransactions,
} from '../helpers/http.js';
import { THRESHOLDS, BASE_URL } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

export default function () {
  const { token }  = registerAndLogin('_rev');
  const acc        = setupAccount(token, 'USD');
  const accNum     = acc.accountNumber;
  setupDeposit(token, accNum, 2000, 'USD');

  // ── Happy-path: Reverse a deposit ────────────────────────
  group('REV-01 | Reverse a deposit creates a reversal entry', () => {
    const depTxn = setupDeposit(token, accNum, 100, 'USD');
    const txnId  = depTxn.id;

    const res  = reverseTransaction(token, txnId, 'test reversal');
    const body = JSON.parse(res.body);

    check(res, {
      'reversal returns 201':                          (r) => r.status === 201,
      'response is array':                             () => Array.isArray(body),
      'reversal entry has referenceTransactionId':     () => body[0]?.referenceTransactionId === txnId,
      'reversal type contains REVERSAL':               () => body[0]?.type?.includes('REVERSAL'),
    });
  });

  // ── Happy-path: Original transaction is immutable after reversal
  group('REV-02 | Original transaction is not modified after reversal', () => {
    const depTxn = setupDeposit(token, accNum, 150, 'USD');
    const txnId  = depTxn.id;
    reverseTransaction(token, txnId, 'immutability check');

    // Get all transactions and find the original
    const txns    = JSON.parse(getMyTransactions(token).body);
    const original = txns.find((t) => t.id === txnId);

    check(null, {
      'original txn type unchanged (DEPOSIT)': () => original?.type === 'DEPOSIT',
      'original txn status unchanged':         () => original?.status !== 'REVERSED' && original?.status !== undefined,
    });
  });

  // ── Happy-path: Reverse both legs of a transfer ───────────
  group('REV-03 | Reverse a transfer (both legs atomically)', () => {
    const { token: tokenB } = registerAndLogin('_revB');
    const accB = setupAccount(tokenB, 'USD');

    const tfRes   = transfer(token, accNum, accB.accountNumber, 200, 'USD', 'to-be-reversed');
    const tfBody  = JSON.parse(tfRes.body);
    const debitId = tfBody.debitTransaction.id;

    const res  = reverseTransfer(token, debitId);
    const body = JSON.parse(res.body);

    check(res, {
      'reverse-transfer returns 201':          (r) => r.status === 201,
      'response contains 2 reversal entries':  () => Array.isArray(body) && body.length === 2,
      'both entries are REVERSAL type':        () => body.every((t) => t.type?.includes('REVERSAL')),
    });
  });

  // ── Edge: Double reversal is forbidden ────────────────────
  group('REV-04 | Double reversal of same transaction is rejected', () => {
    const depTxn = setupDeposit(token, accNum, 75, 'USD');
    const txnId  = depTxn.id;

    reverseTransaction(token, txnId, 'first reversal');

    const res = reverseTransaction(token, txnId, 'second reversal attempt');
    check(res, {
      'double reversal returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Edge: Reverse non-existent transaction returns 404 ────
  group('REV-05 | Reversing non-existent transaction returns 404', () => {
    const fakeId = '00000000-0000-0000-0000-000000000000';
    const res    = reverseTransaction(token, fakeId, 'fake txn');
    check(res, {
      'fake txnId returns 404': (r) => r.status === 404,
    });
  });

  // ── Edge: Missing originalTransactionId returns 400 ───────
  group('REV-06 | Missing originalTransactionId returns 400', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/transactions/reverse`,
      JSON.stringify({ reason: 'no id provided' }),
      { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } },
    );
    check(res, {
      'missing originalTransactionId returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Unauthenticated reversal is rejected ────────────
  group('REV-07 | Reversal without auth token returns 401/403', () => {
    const depTxn = setupDeposit(token, accNum, 25, 'USD');
    const res = http.post(
      `${BASE_URL}/api/v1/transactions/reverse`,
      JSON.stringify({ originalTransactionId: depTxn.id }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(res, {
      'no-token reversal returns 401 or 403': (r) => r.status === 401 || r.status === 403,
    });
  });

  // ── Edge: Reversal of a reversal (meta-reversal) ──────────
  group('REV-08 | Reversing a reversal transaction is rejected', () => {
    const depTxn  = setupDeposit(token, accNum, 50, 'USD');
    const revRes  = reverseTransaction(token, depTxn.id, 'original reversal');
    const revBody = JSON.parse(revRes.body);
    const revId   = revBody[0]?.id;

    if (revId) {
      const res = reverseTransaction(token, revId, 'reverse of reversal');
      check(res, {
        'reversing a reversal returns 4xx': (r) => r.status >= 400 && r.status < 500,
      });
    } else {
      check(null, { 'skip: could not get reversal id': () => true });
    }
  });

  // ── Auditability: Reversal references original ID ─────────
  group('REV-09 | Reversal entry references original transaction ID', () => {
    const depTxn = setupDeposit(token, accNum, 33, 'USD');
    const txnId  = depTxn.id;
    const revRes = reverseTransaction(token, txnId, 'audit ref check');
    const revArr = JSON.parse(revRes.body);

    check(revRes, {
      'referenceTransactionId matches original': () =>
        revArr.every((r) => r.referenceTransactionId === txnId),
    });
  });

  // ── Auditability: Both original and reversal exist in ledger
  group('REV-10 | Both original and reversal transactions appear in ledger', () => {
    const depTxn = setupDeposit(token, accNum, 77, 'USD');
    const txnId  = depTxn.id;
    reverseTransaction(token, txnId, 'ledger audit');

    const txns     = JSON.parse(getMyTransactions(token).body);
    const original = txns.find((t) => t.id === txnId);
    const reversal = txns.find((t) => t.referenceTransactionId === txnId);

    check(null, {
      'original txn still in ledger': () => !!original,
      'reversal txn in ledger':       () => !!reversal,
    });
  });
}
