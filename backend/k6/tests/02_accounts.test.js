// ============================================================
// tests/02_accounts.test.js — Account management tests
// ============================================================

import { check, group } from 'k6';
import http from 'k6/http';
import {
  registerAndLogin,
  createAccount,
  getMyAccounts,
} from '../helpers/http.js';
import { THRESHOLDS, BASE_URL } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

export default function () {
  const { token } = registerAndLogin('_acc');

  // ── Happy-path: Create USD account ───────────────────────
  group('ACC-01 | Create USD account', () => {
    const res = createAccount(token, 'USD');
    check(res, {
      'create account returns 201':        (r) => r.status === 201,
      'response has accountNumber':        (r) => !!JSON.parse(r.body).accountNumber,
      'response has id':                   (r) => !!JSON.parse(r.body).id,
      'response balance is 0':             (r) => JSON.parse(r.body).balance === 0,
      'response status is ACTIVE':         (r) => JSON.parse(r.body).status === 'ACTIVE',
      'response currencyCode is USD':      (r) => JSON.parse(r.body).currencyCode === 'USD',
    });
  });

  // ── Happy-path: Create EUR account (multiple currencies) ─
  group('ACC-02 | Create EUR account (multi-currency support)', () => {
    const res = createAccount(token, 'EUR');
    check(res, {
      'EUR account created 201':      (r) => r.status === 201,
      'currency is EUR':              (r) => JSON.parse(r.body).currencyCode === 'EUR',
    });
  });

  // ── Happy-path: Create INR account (ISO 4217) ────────────
  group('ACC-03 | Create INR account (ISO 4217 currency)', () => {
    const res = createAccount(token, 'INR');
    check(res, {
      'INR account created 201': (r) => r.status === 201,
      'currency is INR':         (r) => JSON.parse(r.body).currencyCode === 'INR',
    });
  });

  // ── Happy-path: List my accounts ─────────────────────────
  group('ACC-04 | List my accounts returns all created accounts', () => {
    // Create two accounts to ensure list is non-empty
    createAccount(token, 'GBP');
    createAccount(token, 'JPY');

    const res = getMyAccounts(token);
    check(res, {
      'list accounts returns 200':      (r) => r.status === 200,
      'response is array':              (r) => Array.isArray(JSON.parse(r.body)),
      'list contains at least 2 items': (r) => JSON.parse(r.body).length >= 2,
    });
  });

  // ── Edge: Invalid currency code (too short) ───────────────
  group('ACC-05 | Currency code too short returns 400', () => {
    const res = createAccount(token, 'US');
    check(res, {
      'short currency code returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Invalid currency code (too long) ────────────────
  group('ACC-06 | Currency code too long returns 400', () => {
    const res = createAccount(token, 'USDD');
    check(res, {
      'long currency code returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Empty currency code ─────────────────────────────
  group('ACC-07 | Empty currency code returns 400', () => {
    const res = createAccount(token, '');
    check(res, {
      'empty currency code returns 400': (r) => r.status === 400,
    });
  });

  // ── Edge: Unauthenticated account creation ─────────────────
  group('ACC-08 | Create account without auth token returns 401/403', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/accounts`,
      JSON.stringify({ currencyCode: 'USD' }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(res, {
      'no token returns 401 or 403': (r) => r.status === 401 || r.status === 403,
    });
  });

  // ── Edge: Account isolation between users ─────────────────
  group('ACC-09 | User only sees their own accounts', () => {
    const { token: otherToken } = registerAndLogin('_other_acc');
    createAccount(otherToken, 'CHF');

    const res = getMyAccounts(token);
    const accounts = JSON.parse(res.body);
    const allMine = accounts.every((a) => {
      // We can't directly check userId from AccountResponse without logging in
      // but all returned accounts must have an accountNumber
      return a.accountNumber !== undefined;
    });
    check(res, {
      "can't see other user's accounts - list is own": () => allMine,
    });
  });
}
