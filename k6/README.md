# Multi-Currency Wallet — k6 Test Suite
End-to-end + load tests for the **Multi-Currency Digital Wallet** API,
covering the full banking domain: accounts, deposits, transfers and reversals.

---

## Folder Structure

```
k6/
├── config/
│   └── config.js               # Base URL, shared thresholds, VU/stage options
├── helpers/
│   └── http.js                 # Typed HTTP wrappers + convenience setup helpers
├── tests/
│   ├── 00_smoke.test.js        # Full E2E happy-path smoke run (single VU)
│   ├── 01_auth.test.js         # Register, login, validation, security
│   ├── 02_accounts.test.js     # Account creation, listing, isolation
│   ├── 03_deposit.test.js      # Deposit happy-paths + edge cases
│   ├── 04_transfer.test.js     # Transfer atomicity, currency rules, edge cases
│   ├── 05_transactions.test.js # Transaction listing, pagination, immutability
│   ├── 06_reversals.test.js    # Reversal auditability, double-reversal guard
│   └── 07_load.test.js         # Concurrent load — no negative balances, no 500s
├── scripts/
│   ├── run-all.sh              # Run every suite → generate combined HTML report
│   ├── run-one.sh              # Run a single suite by short name
│   └── generate-report.js      # Node.js HTML report generator (no dependencies)
└── reports/                    # Auto-created — JSON summaries + HTML output
```

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| k6   | ≥ 0.50  | https://k6.io/docs/get-started/installation/ |
| Node | ≥ 14    | https://nodejs.org |

The report generator uses **only Node built-ins** (`fs`, `path`) — no `npm install` needed.

---

## Quick Start

### 1. Start the API

```bash
cd multi-currency-wallet
$env:DB_PASSWORD="yourpassword"
$env:JWT_SECRET="your-secret-key-at-least-32-characters"
./mvnw spring-boot:run
# API will be available at http://localhost:8080
```

### 2. Run all tests + generate HTML report

```bash
chmod +x scripts/run-all.sh scripts/run-one.sh
./scripts/run-all.sh
# Open: reports/wallet-test-report.html
```

### 3. Run a single suite

```bash
# Short-name aliases: smoke | auth | accounts | deposit | transfer | transactions | reversals | load
./scripts/run-one.sh smoke
./scripts/run-one.sh transfer
```

### 4. Override the base URL

```bash
BASE_URL=http://staging-server:8080 ./scripts/run-all.sh
```

### 5. Skip the load test

```bash
NO_LOAD=1 ./scripts/run-all.sh
```

### 6. Run a single file directly with k6

```bash
k6 run --env BASE_URL=http://localhost:8080 tests/04_transfer.test.js
```

### 7. Generate the HTML report from existing JSON files

```bash
node scripts/generate-report.js reports/smoke.json reports/transfer.json
# Or use a glob (bash):
node scripts/generate-report.js reports/*.json --out reports/full-report.html
```

---

## Test Coverage

### Functional Tests

| Suite | Test IDs | What is covered |
|-------|----------|-----------------|
| **00_smoke** | SMOKE-01…10 | Full happy-path flow: register→login→account→deposit→transfer→list→reverse→double-reversal guard |
| **01_auth** | AUTH-01…09 | Registration, login, duplicate email, wrong password, field validation, JWT protection |
| **02_accounts** | ACC-01…09 | USD/EUR/INR/GBP creation, listing, invalid currency codes, unauthenticated access, cross-user isolation |
| **03_deposit** | DEP-01…12 | Standard deposit, 3-decimal precision, accumulation, min amount (0.001), zero/negative rejection, currency mismatch, non-existent account, missing fields, cross-user deposit |
| **04_transfer** | TRF-01…15 | Basic transfer, balance decrement, exact balance drain, 3-decimal precision, insufficient funds (422), cross-currency rejection, currency mismatch, self-transfer, non-existent accounts, zero/negative amount, missing fields, unauthenticated, atomicity verification in ledger |
| **05_transactions** | TXN-01…09 | List own transactions, paginated by account, field completeness, both-party ledger entries, pagination parameters, sort order DESC, unauthenticated rejection, fake account ID 404, no DELETE endpoint (immutability) |
| **06_reversals** | REV-01…10 | Reverse deposit, original immutability, reverse both legs of transfer, double-reversal rejection, non-existent txn 404, missing ID 400, unauthenticated, reversing a reversal, referenceTransactionId audit, both entries in ledger |

### Load Tests

| Suite | Scenario | What is covered |
|-------|----------|-----------------|
| **07_load** | concurrent_deposits (5 VUs × 4 iter) | No balance corruption under parallel deposits |
| **07_load** | concurrent_transfers (3 VUs × 3 iter) | No negative balances under concurrent transfers, no 500s |

---

## Business Rules Verified

| Rule | Where tested |
|------|-------------|
| **Atomic Transfers** — debit+credit succeed or fail together | TRF-15, LOAD-02 |
| **Auditability** — transactions immutable once created | TXN-09, REV-02, REV-10 |
| **No edit / soft-delete** | TXN-09 (DELETE returns 404/405) |
| **Corrections via explicit reversals** | REV-01…10 |
| **3 decimal place precision** | DEP-02, TRF-04 |
| **ISO 4217 currency support** | ACC-02, ACC-03, TRF-06 |
| **Same-currency transfers only** | TRF-06, TRF-07 |
| **Reversal references original ID** | REV-01, REV-09 |
| **Double reversal forbidden** | REV-04, SMOKE-10 |

---

## Thresholds

| Metric | Threshold |
|--------|-----------|
| `http_req_failed` rate | < 5% |
| `http_req_duration` p95 | < 2000 ms |

Load test uses relaxed thresholds (< 10% failure, p95 < 3 s) to account for setup overhead.

---

## HTML Report

After any run you will find:

```
reports/
├── smoke.json            # k6 --summary-export raw JSON
├── auth.json
├── ...
└── wallet-test-report.html   # Self-contained, no external dependencies
```

The HTML dashboard shows:
- Overall PASS / FAIL badge
- Per-suite summary cards with pass rate
- HTTP metrics: total requests, failure rate, avg/p95/p99 latency
- Expandable per-check result table
