// ============================================================
// helpers/http.js — thin wrappers around k6 http
// ============================================================

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/config.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function authHeaders(token) {
  return { ...JSON_HEADERS, Authorization: `Bearer ${token}` };
}

// ── Auth ────────────────────────────────────────────────────

export function register(email, password, fullName) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ email, password, fullName }),
    { headers: JSON_HEADERS, tags: { name: 'auth_register' } },
  );
  return res;
}

export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: JSON_HEADERS, tags: { name: 'auth_login' } },
  );
  return res;
}

// ── Accounts ────────────────────────────────────────────────

export function createAccount(token, currencyCode) {
  const res = http.post(
    `${BASE_URL}/api/v1/accounts`,
    JSON.stringify({ currencyCode }),
    { headers: authHeaders(token), tags: { name: 'account_create' } },
  );
  return res;
}

export function getMyAccounts(token) {
  const res = http.get(
    `${BASE_URL}/api/v1/accounts`,
    { headers: authHeaders(token), tags: { name: 'account_list' } },
  );
  return res;
}

// ── Transactions ─────────────────────────────────────────────

export function deposit(token, accountNumber, amount, currencyCode, description = '') {
  const res = http.post(
    `${BASE_URL}/api/v1/accounts/deposit`,
    JSON.stringify({ accountNumber, amount, currencyCode, description }),
    { headers: authHeaders(token), tags: { name: 'deposit' } },
  );
  return res;
}

export function transfer(token, sourceAccountNumber, destinationAccountNumber, amount, currencyCode, description = '') {
  const res = http.post(
    `${BASE_URL}/api/v1/transfers`,
    JSON.stringify({ sourceAccountNumber, destinationAccountNumber, amount, currencyCode, description }),
    { headers: authHeaders(token), tags: { name: 'transfer' } },
  );
  return res;
}

export function getAccountTransactions(token, accountNumber) {
  const res = http.get(
    `${BASE_URL}/api/v1/accounts/${accountNumber}/transactions`,
    { headers: authHeaders(token), tags: { name: 'txn_by_account' } },
  );
  return res;
}

export function getMyTransactions(token) {
  const res = http.get(
    `${BASE_URL}/api/v1/transactions`,
    { headers: authHeaders(token), tags: { name: 'txn_my' } },
  );
  return res;
}

export function reverseTransaction(token, originalTransactionId, reason = '') {
  const res = http.post(
    `${BASE_URL}/api/v1/transactions/reverse`,
    JSON.stringify({ originalTransactionId, reason }),
    { headers: authHeaders(token), tags: { name: 'txn_reverse' } },
  );
  return res;
}

export function reverseTransfer(token, transactionId) {
  const res = http.post(
    `${BASE_URL}/api/v1/transactions/reverse-transfer/${transactionId}`,
    null,
    { headers: authHeaders(token), tags: { name: 'txn_reverse_transfer' } },
  );
  return res;
}

// ── Convenience helpers ──────────────────────────────────────

/**
 * Register → login → return token.
 * Uses a unique email each time to avoid collisions across VUs.
 */
export function registerAndLogin(suffix = '') {
  const ts    = Date.now();
  const email = `testuser_${ts}${suffix}@wallet.test`;
  const pass  = 'Password123!';
  const name  = `Test User ${ts}`;

  const regRes = register(email, pass, name);
  check(regRes, { 'setup: register 201': (r) => r.status === 201 });

  const loginRes = login(email, pass);
  check(loginRes, { 'setup: login 200': (r) => r.status === 200 });

  const body = JSON.parse(loginRes.body);
  return { token: body.token, email, password: pass };
}

/**
 * Create account and return full AccountResponse JSON.
 */
export function setupAccount(token, currencyCode) {
  const res = createAccount(token, currencyCode);
  check(res, { 'setup: account created 201': (r) => r.status === 201 });
  return JSON.parse(res.body);
}

/**
 * Deposit and return TransactionResponse JSON.
 */
export function setupDeposit(token, accountNumber, amount, currencyCode) {
  const res = deposit(token, accountNumber, amount, currencyCode, 'setup deposit');
  check(res, { 'setup: deposit 201': (r) => r.status === 201 });
  return JSON.parse(res.body);
}
