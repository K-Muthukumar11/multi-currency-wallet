// ============================================================
// tests/03_deposit.test.js — Deposit API tests
// ============================================================

import { check, group } from 'k6';
import http from 'k6/http';
import {
  registerAndLogin,
  setupAccount,
  deposit,
} from '../helpers/http.js';
import { THRESHOLDS, BASE_URL } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

export default function () {
  const { token } = registerAndLogin('_dep');
  const account   = setupAccount(token, 'USD');
  const accNum    = account.accountNumber;

  // ── Happy-path: Standard deposit ─────────────────────────
  group('DEP-01 | Deposit whole-number amount', () => {
    const res = deposit(token, accNum, 500, 'USD', 'Initial funding');
    const body = JSON.parse(res.body);
    check(res, {
      'deposit returns 201':          (r) => r.status === 201,
      'type is DEPOSIT':              () => body.type === 'DEPOSIT',
      'amount equals 500':            () => body.amount === 500,
      'currencyCode is USD':          () => body.currencyCode === 'USD',
      'status is COMPLETED':          () => body.status === 'COMPLETED',
      'balanceAfter is 500':          () => body.balanceAfter === 500,
      'accountId present':            () => !!body.accountId,
      'id (txnId) present':           () => !!body.id,
      'createdAt present':            () => !!body.createdAt,
    });
  });

  // ── Happy-path: 3 decimal precision (per problem statement)
  group('DEP-02 | Deposit with 3-decimal-place precision', () => {
    const res = deposit(token, accNum, 100.005, 'USD', '3 decimal deposit');
    const body = JSON.parse(res.body);
    check(res, {
      'deposit 3-decimal returns 201': (r) => r.status === 201,
      'amount preserved to 3 dp':      () => body.amount === 100.005,
    });
  });

  // ── Happy-path: Multiple sequential deposits ──────────────
  group('DEP-03 | Multiple deposits accumulate balance correctly', () => {
    const acc2 = setupAccount(token, 'EUR');
    deposit(token, acc2.accountNumber, 100, 'EUR');
    const res2 = deposit(token, acc2.accountNumber, 200, 'EUR');
    const body2 = JSON.parse(res2.body);
    check(res2, {
      'second deposit balanceAfter is 300': () => body2.balanceAfter === 300,
    });
  });

  // ── Edge: Amount equals minimum (0.001) ───────────────────
  group('DEP-04 | Minimum valid amount 0.001 is accepted', () => {
    const res = deposit(token, accNum, 0.001, 'USD', 'Min amount deposit');
    check(res, {
      'min amount 0.001 returns 201': (r) => r.status === 201,
    });
  });

  // ── Edge: Amount is 0 ─────────────────────────────────────
  group('DEP-05 | Zero amount deposit is rejected', () => {
    const res = deposit(token, accNum, 0, 'USD');
    check(res, {
      'zero amount returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Negative amount ─────────────────────────────────
  group('DEP-06 | Negative amount deposit is rejected', () => {
    const res = deposit(token, accNum, -100, 'USD');
    check(res, {
      'negative amount returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Currency mismatch (depositing EUR into USD account)
  group('DEP-07 | Depositing wrong currency into account is rejected', () => {
    const res = deposit(token, accNum, 100, 'EUR', 'currency mismatch');
    check(res, {
      'currency mismatch returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Edge: Non-existent account number ────────────────────
  group('DEP-08 | Deposit to non-existent account returns 404', () => {
    const res = deposit(token, 'ACC-DOESNOTEXIST-9999', 100, 'USD');
    check(res, {
      'non-existent account returns 404': (r) => r.status === 404,
    });
  });

  // ── Edge: Missing accountNumber ───────────────────────────
  group('DEP-09 | Deposit with missing accountNumber returns 400', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/accounts/deposit`,
      JSON.stringify({ amount: 100, currencyCode: 'USD' }),
      { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } },
    );
    check(res, {
      'missing accountNumber returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Missing amount ──────────────────────────────────
  group('DEP-10 | Deposit with missing amount returns 400', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/accounts/deposit`,
      JSON.stringify({ accountNumber: accNum, currencyCode: 'USD' }),
      { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } },
    );
    check(res, {
      'missing amount returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Unauthenticated deposit ─────────────────────────
  group('DEP-11 | Deposit without auth token returns 401/403', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/accounts/deposit`,
      JSON.stringify({ accountNumber: accNum, amount: 100, currencyCode: 'USD' }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(res, {
      'no-token deposit returns 401 or 403': (r) => r.status === 401 || r.status === 403,
    });
  });

  // ── Edge: Deposit to another user's account ───────────────
  group('DEP-12 | Deposit to another user account returns 4xx', () => {
    const { token: otherToken } = registerAndLogin('_dep_other');
    const otherAcc = setupAccount(otherToken, 'USD');

    // Original user tries to deposit into other user's account
    const res = deposit(token, otherAcc.accountNumber, 100, 'USD', 'cross-user deposit');
    check(res, {
      'cross-user deposit returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });
}
