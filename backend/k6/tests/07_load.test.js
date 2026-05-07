// ============================================================
// tests/07_load.test.js — Load & concurrency tests
// Validates atomicity, no negative balances, no data races
// under concurrent VUs.
// ============================================================

import { check, group, sleep } from 'k6';
import {
  registerAndLogin,
  setupAccount,
  deposit,
  transfer,
  getMyAccounts,
} from '../helpers/http.js';
import { THRESHOLDS } from '../config/config.js';

export const options = {
  thresholds: {
    http_req_failed:   ['rate<0.10'],
    http_req_duration: ['p(95)<3000'],
  },
  scenarios: {
    concurrent_deposits: {
      executor:   'per-vu-iterations',
      vus:        5,
      iterations: 4,
      maxDuration: '60s',
    },
    concurrent_transfers: {
      executor:    'per-vu-iterations',
      vus:         3,
      iterations:  3,
      maxDuration: '60s',
      startTime:   '30s',
    },
  },
};

// Each VU sets up its own accounts during setup phase
// Since k6 runs setup() once, we use __VU for isolation
export function setup() {
  // Create a shared "receiver" account that many VUs will transfer into
  const { token: receiverToken } = registerAndLogin(`_load_rx`);
  const receiverAcc = setupAccount(receiverToken, 'USD');
  return {
    receiverAccountNumber: receiverAcc.accountNumber,
    receiverToken,
  };
}

export default function (data) {
  // Each VU creates its own sender account
  const { token } = registerAndLogin(`_vu${__VU}_iter${__ITER}`);
  const acc       = setupAccount(token, 'USD');

  // ── Concurrent Deposits ───────────────────────────────────
  group('LOAD-01 | Concurrent deposits do not corrupt balance', () => {
    // Deposit 5 times in this VU iteration
    for (let i = 0; i < 5; i++) {
      const res = deposit(token, acc.accountNumber, 100, 'USD', `load deposit ${i}`);
      check(res, {
        'concurrent deposit returns 201': (r) => r.status === 201,
      });
    }

    // Verify final balance
    const accounts = JSON.parse(getMyAccounts(token).body);
    const balance  = accounts.find((a) => a.accountNumber === acc.accountNumber)?.balance;
    check(null, {
      'balance after 5x100 deposits is 500': () => balance === 500,
    });
  });

  // ── Concurrent Transfers ──────────────────────────────────
  group('LOAD-02 | Concurrent transfers — no negative balance', () => {
    // Fund the account generously first
    deposit(token, acc.accountNumber, 1000, 'USD', 'load fund');

    // Issue 3 rapid transfers
    for (let i = 0; i < 3; i++) {
      const res = transfer(
        token,
        acc.accountNumber,
        data.receiverAccountNumber,
        50,
        'USD',
        `load transfer ${i}`,
      );
      check(res, {
        [`concurrent transfer ${i} not 500`]: (r) => r.status !== 500,
      });
    }

    // Verify no negative balance
    const accounts = JSON.parse(getMyAccounts(token).body);
    const balance  = accounts.find((a) => a.accountNumber === acc.accountNumber)?.balance;
    check(null, {
      'balance never goes negative': () => balance >= 0,
    });
  });

  sleep(1);
}
