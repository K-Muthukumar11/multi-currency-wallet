// ============================================================
// tests/01_auth.test.js — Authentication flow tests
// ============================================================

import { check, group } from 'k6';
import http from 'k6/http';
import { register, login } from '../helpers/http.js';
import { THRESHOLDS, BASE_URL } from '../config/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: THRESHOLDS,
};

const TIMESTAMP = Date.now();

export default function () {

  // ── Happy-path: Register ──────────────────────────────────
  group('AUTH-01 | Register new user', () => {
    const res = register(
      `user_${TIMESTAMP}@wallet.test`,
      'SecurePass1!',
      'Happy User',
    );
    check(res, {
      'register returns 201': (r) => r.status === 201,
      'response has token':   (r) => JSON.parse(r.body).token !== undefined,
    });
  });

  // ── Happy-path: Login ─────────────────────────────────────
  group('AUTH-02 | Login with valid credentials', () => {
    const email = `login_${TIMESTAMP}@wallet.test`;
    register(email, 'SecurePass1!', 'Login User');

    const res = login(email, 'SecurePass1!');
    check(res, {
      'login returns 200':  (r) => r.status === 200,
      'response has token': (r) => JSON.parse(r.body).token !== undefined,
    });
  });

  // ── Edge: Duplicate registration ──────────────────────────
  group('AUTH-03 | Duplicate email registration is rejected', () => {
    const email = `dup_${TIMESTAMP}@wallet.test`;
    register(email, 'SecurePass1!', 'First User');

    const res = register(email, 'SecurePass1!', 'Second User');
    check(res, {
      'duplicate register returns 4xx': (r) => r.status >= 400 && r.status < 500,
    });
  });

  // ── Edge: Wrong password ──────────────────────────────────
  group('AUTH-04 | Login with wrong password returns 401', () => {
    const email = `wrongpass_${TIMESTAMP}@wallet.test`;
    register(email, 'SecurePass1!', 'Wrong Pass User');

    const res = login(email, 'WrongPassword!');
    check(res, {
      'wrong password returns 401': (r) => r.status === 401,
    });
  });

  // ── Edge: Non-existent user login ─────────────────────────
  group('AUTH-05 | Login with non-existent user returns 401', () => {
    const res = login('nobody_999@wallet.test', 'SomePass123!');
    check(res, {
      'non-existent user returns 401': (r) => r.status === 401,
    });
  });

  // ── Validation: Missing fields ────────────────────────────
  group('AUTH-06 | Register with missing required fields returns 400', () => {
    const res = register('', '', '');
    check(res, {
      'empty register returns 400': (r) => r.status === 400,
    });
  });

  // ── Validation: Invalid email format ─────────────────────
  group('AUTH-07 | Register with invalid email format returns 400', () => {
    const res = register('not-an-email', 'SecurePass1!', 'Bad Email User');
    check(res, {
      'invalid email returns 400': (r) => r.status === 400,
    });
  });

  // ── Validation: Short password ────────────────────────────
  group('AUTH-08 | Register with short password returns 400', () => {
    const res = register(`short_${TIMESTAMP}@wallet.test`, '123', 'Short Pass');
    check(res, {
      'short password returns 400': (r) => r.status === 400,
    });
  });

  // ── Security: No-token access to protected route ──────────
  group('AUTH-09 | Protected route without token returns 401/403', () => {
    const url = `${BASE_URL}/api/v1/accounts`;
    const res  = http.get(url);
    check(res, {
      'no-token protected route rejected': (r) => r.status === 401 || r.status === 403,
    });
  });
}
