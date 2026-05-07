# Multi-Currency Digital Wallet

A production-grade **Multi-Currency Digital Wallet** built with **Spring Boot 3**, **React**, and **PostgreSQL**, following **Clean Architecture** principles and developed entirely using **Test-Driven Development (TDD)**.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [PostgreSQL Setup — Production](#postgresql-setup--production)
- [PostgreSQL Setup — Integration Tests](#postgresql-setup--integration-tests)
- [Backend Setup & Run](#backend-setup--run)
- [Frontend Setup & Run](#frontend-setup--run)
- [Running Tests](#running-tests)
- [k6 Load & Functional Tests](#k6-load--functional-tests)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Environment Variables](#environment-variables)
- [Business Rules](#business-rules)
- [TDD Commit Journey](#tdd-commit-journey)

---

## Overview

A fully functional digital wallet system supporting:

- User registration and JWT-based authentication
- Multi-currency account management (ISO 4217 currencies — USD, EUR, INR, GBP, etc.)
- Deposits with 3-decimal-place precision
- Atomic peer-to-peer transfers (same currency only)
- Immutable transaction ledger (enforced at both application and database level)
- Reversal system — corrections via new transactions, never by modifying originals
- Double-reversal protection
- Paginated transaction history
- React frontend with full wallet UI

---

## Project Structure

multi-currency-wallet/
│
├── backend/                                  # Spring Boot backend
│   ├── src/
│   │   ├── main/java/com/multi/currency/wallet/
│   │   │   ├── domain/                       # Enterprise Business Rules (innermost ring)
│   │   │   │   ├── model/                    # Entities: Account, Transaction, User, Money
│   │   │   │   ├── repository/               # Repository interfaces (owned by domain)
│   │   │   │   ├── service/                  # TransferDomainService
│   │   │   │   └── exception/                # Domain exceptions
│   │   │   ├── application/                  # Application Business Rules (use case ring)
│   │   │   │   ├── usecase/                  # Interactors: AuthUseCase, DepositUseCase, etc.
│   │   │   │   └── dto/                      # Request and response data structures
│   │   │   └── infrastructure/               # Frameworks & Drivers (outermost ring)
│   │   │       ├── persistence/              # JPA entities, repositories, adapters
│   │   │       ├── security/                 # JwtService, JwtAuthenticationFilter
│   │   │       ├── web/                      # Controllers, GlobalExceptionHandler
│   │   │       └── config/                   # SecurityConfig, DomainServiceConfig
│   │   ├── main/resources/
│   │   │   ├── application.yml               # Production config (env-var driven)
│   │   │   └── db/migration/                 # Flyway SQL migrations V1, V2, V3
│   │   └── test/
│   │       ├── java/com/multi/currency/wallet/
│   │       │   ├── domain/                   # Pure entity unit tests
│   │       │   ├── application/usecase/      # Use case interactor tests (Mockito)
│   │       │   └── integration/              # WalletIntegrationTest (full stack)
│   │       └── resources/
│   │           └── application-test.yml      # Test datasource config
│   ├── k6/                                   # Load and functional tests
│   │   ├── config/config.js
│   │   ├── helpers/http.js
│   │   ├── tests/
│   │   │   ├── 00_smoke.test.js
│   │   │   ├── 01_auth.test.js
│   │   │   ├── 02_accounts.test.js
│   │   │   ├── 03_deposit.test.js
│   │   │   ├── 04_transfer.test.js
│   │   │   ├── 05_transactions.test.js
│   │   │   ├── 06_reversals.test.js
│   │   │   └── 07_load.test.js
│   │   ├── scripts/
│   │   │   ├── run-all.sh
│   │   │   ├── run-one.sh
│   │   │   └── generate-report.js
│   │   └── reports/                          # Generated JSON + HTML (gitignored)
│   ├── pom.xml
│   └── mvnw / mvnw.cmd
│
├── frontend/                                 # React frontend
│   ├── src/
│   │   ├── components/                       # Layout, ProtectedRoute, UI primitives
│   │   ├── context/                          # AuthContext (JWT + localStorage)
│   │   ├── pages/                            # AuthPage, Dashboard, Accounts, Deposit,
│   │   │                                     # Transfer, Transactions, Reversal
│   │   ├── services/                         # api.js (Axios instance + interceptors)
│   │   ├── App.jsx                           # Router + route definitions
│   │   └── main.jsx
│   ├── index.html
│   ├── vite.config.js                        # Proxy /api → localhost:8080
│   └── package.json
│
└── README.md                                 ← repo root


---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (JJWT) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Validation | Jakarta Validation |
| Boilerplate | Lombok |
| Unit Testing | JUnit 5 + Mockito |
| Integration Testing | Spring Boot Test (`@SpringBootTest`) |
| Load Testing | k6 ≥ 0.50 |
| Frontend | React 18 + React Router 6 + Axios |
| Frontend Build | Vite 5 |
| Build Tool | Maven (Maven Wrapper included) |

---

## Architecture

This project follows **Clean Architecture** as defined by Robert C. Martin. The codebase is organized as concentric rings where **source code dependencies point inward only** — outer rings depend on inner rings, never the reverse.

```
                    ┌─────────────────────────────────┐
                    │   Frameworks & Drivers           │
                    │  (infrastructure/)               │
                    │  Spring MVC Controllers          │
                    │  JPA Repositories & Adapters     │
                    │  Spring Security / JWT           │
                    │  ┌───────────────────────────┐  │
                    │  │  Interface Adapters        │  │
                    │  │  (web/controllers,         │  │
                    │  │   persistence/adapters)    │  │
                    │  │  ┌─────────────────────┐  │  │
                    │  │  │  Application        │  │  │
                    │  │  │  Business Rules     │  │  │
                    │  │  │  (application/)     │  │  │
                    │  │  │  Use Case           │  │  │
                    │  │  │  Interactors        │  │  │
                    │  │  │  ┌───────────────┐  │  │  │
                    │  │  │  │   Enterprise  │  │  │  │
                    │  │  │  │   Business    │  │  │  │
                    │  │  │  │   Rules       │  │  │  │
                    │  │  │  │  (domain/)    │  │  │  │
                    │  │  │  │  Entities     │  │  │  │
                    │  │  │  │  Domain Svc   │  │  │  │
                    │  │  │  └───────────────┘  │  │  │
                    │  │  └─────────────────────┘  │  │
                    │  └───────────────────────────┘  │
                    └─────────────────────────────────┘
```

### The Dependency Rule

No inner ring imports anything from an outer ring:

- `domain/` has **zero imports** from `application/`, `infrastructure/`, or any framework
- `application/` imports from `domain/` only — never from `infrastructure/`
- `infrastructure/` imports from both `domain/` and `application/` to wire everything together

### How the Dependency Inversion is Applied

The `domain/repository/` interfaces (`UserRepository`, `AccountRepository`, `TransactionRepository`) are defined in the **domain ring** and implemented in the **infrastructure ring** (JPA adapters). This means the domain dictates the contract — the database is a plugin, not a dependency.

```
domain/repository/UserRepository (interface)       ← owned by domain
        ↑ implemented by
infrastructure/persistence/adapter/UserRepositoryAdapter (JPA concrete class)
```

Use cases (`application/usecase/`) depend only on the repository interfaces, never on JPA directly. This is why use cases are fully testable with Mockito — the JPA implementation is swapped out in tests with no framework involvement.

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java | 21+ | Use `./mvnw` — no local Maven install needed |
| Docker | 20.10+ | Required for integration tests (Testcontainers spins up PostgreSQL 15 automatically) |
| PostgreSQL | 15+ | Required for **production** only — integration tests use Docker via Testcontainers |
| Node.js | 18+ | For frontend and k6 report generation |
| npm | 9+ | Comes with Node.js |
| k6 | ≥ 0.50 | For load and functional API tests |

---

## PostgreSQL Setup — Production

### Step 1 — Install PostgreSQL 15

Download from [postgresql.org/download](https://www.postgresql.org/download/) or use your OS package manager.

### Step 2 — Create the production database

Connect using `psql` or pgAdmin and run:

```sql
CREATE DATABASE wallet_db;
```

To use a dedicated user instead of the default `postgres` superuser:

```sql
CREATE USER wallet_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE wallet_db TO wallet_user;
```

### Step 3 — Set environment variables

The `application.yml` reads all sensitive values from environment variables. Set these before starting the backend:

**Windows (Command Prompt):**
```cmd
set DB_URL=jdbc:postgresql://localhost:5432/wallet_db
set DB_USERNAME=postgres
set DB_PASSWORD=your_password
set JWT_SECRET=your-base64-encoded-secret-min-32-chars
```

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/wallet_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your-base64-encoded-secret-min-32-chars"
```

**Linux / macOS:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/wallet_db
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your-base64-encoded-secret-min-32-chars
```

> **JWT Secret:** Must be a Base64-encoded string of at least 32 characters. Generate one with:
> ```bash
> openssl rand -base64 48
> ```

### Step 4 — Flyway runs automatically on startup

On first startup, Flyway automatically runs the migration scripts in `src/main/resources/db/migration/` and creates all three tables. No manual SQL is needed beyond creating the database itself.

---

## PostgreSQL Setup — Integration Tests

> **No manual database setup required.** The integration test uses **Testcontainers** to spin up a real PostgreSQL 15 Docker container automatically at test startup and tear it down when finished. The only prerequisite is a running Docker daemon.

### How it works

The `application-test.yml` datasource URL uses the special Testcontainers JDBC scheme:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///multi_currency_wallet_test?TimeZone=UTC
    username: postgres
    password: test
```

The `jdbc:tc:` prefix is intercepted by the Testcontainers JDBC driver, which:

1. Pulls the `postgres:15` Docker image on first run (cached for subsequent runs)
2. Starts a temporary container bound to a random free port
3. Hands the resolved JDBC URL to Spring's connection pool
4. Stops and removes the container automatically when the JVM exits

Flyway then runs all migrations inside that container, and `WalletIntegrationTest`'s `@BeforeAll` calls `flyway.clean()` + `flyway.migrate()` to guarantee a pristine schema before every test run.

**The test database is completely isolated from production and requires no credentials, no port configuration, and no manual SQL.**

### Step 1 — Ensure Docker is running

```bash
docker info   # must succeed before running tests
```

### Step 2 — Run the integration test

```bash
cd backend
./mvnw test -Dtest="WalletIntegrationTest" -P test
```

On the very first run, Docker pulls the `postgres:15` image (~130 MB). All subsequent runs reuse the cached image and start in seconds.

> **`clean-disabled: false`** in `application-test.yml` is required — it allows `WalletIntegrationTest` to call `flyway.clean()` in `@BeforeAll`, resetting the container schema to a clean state before every test run. This setting is safe here because the container is ephemeral and completely isolated from production.

---

## Backend Setup & Run

```bash
# 1. Clone the repository
git clone <repo-url>
cd multi-currency-wallet

# 2. Set environment variables (see PostgreSQL Setup — Production above)

# 3. Start the backend
cd backend
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The API starts at **`http://localhost:8080`**.

Verify it's running:
```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## Frontend Setup & Run

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start the development server
npm run dev
```

The frontend starts at **`http://localhost:5173`** and proxies all `/api` requests to the backend at `http://localhost:8080`. Start the backend first, then the frontend.

---

## Running Tests

### All unit tests (no database required)

```bash
./mvnw test
```

Runs the full unit test suite — entity tests and use case interactor tests. Expected run time: under 10 seconds.

### Integration test only (requires Docker)

```bash
./mvnw test -Dtest="WalletIntegrationTest"
```

Testcontainers starts a fresh PostgreSQL 15 Docker container, Flyway migrates it, and the full test suite runs against it. No local PostgreSQL installation needed — just Docker.

Covers 25 scenarios across three sections:

- **Happy path journey** (13 ordered steps) — register → login → create accounts → deposit × 2 → transfer → get history → reverse deposit → reverse full transfer → verify DELETE returns 405
- **Security boundaries** (5 tests) — no token, malformed JWT, wrong password, deposit to another user's account, transfer from another user's account
- **Business rule errors** (7 tests) — duplicate email, zero amount deposit, insufficient funds, non-existent account, already reversed, reversing a reversal, invalid currency code

### Test Pyramid

```
              [ WalletIntegrationTest ]
              Full HTTP stack · Real PostgreSQL
              25 tests (13 journey + 5 security + 7 business rules)
            ──────────────────────────────────────────────────────────
          [ Use Case / Interactor Tests ]
          Mockito · Repository interfaces mocked · No Spring context
          6 test classes — Auth, CreateAccount, Deposit,
          Transfer, GetTransaction, Reversal
        ──────────────────────────────────────────────────────────────
      [ Entity / Domain Tests ]
      Zero dependencies · Pure Java · No mocks · No framework
      5 test classes — Money, Account, Transaction, User,
      TransferDomainService
```

The test pyramid directly reflects Clean Architecture's rings — the innermost ring (entities) needs no mocks, the middle ring (use cases) mocks only the repository interface, and only the outermost test touches the real database.

---

## k6 Load & Functional Tests

### Install k6

**Windows (via Chocolatey):**
```bash
choco install k6
```

**macOS:**
```bash
brew install k6
```

**Linux (Debian/Ubuntu):**
```bash
sudo gpg --no-default-keyring \
  --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69

echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] \
  https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list

sudo apt-get update && sudo apt-get install k6
```

Verify:
```bash
k6 version
# → k6 v0.50.x (...)
```

Node.js (≥ 18) is required for the HTML report generator. No `npm install` needed — it uses only Node built-ins.

### Start the Backend First

```bash
./mvnw spring-boot:run
```

### Run All Suites and Generate HTML Report

```bash
cd backend/k6

# Linux / macOS — make scripts executable first
chmod +x scripts/run-all.sh scripts/run-one.sh

./scripts/run-all.sh

# Open the report
open reports/wallet-test-report.html        # macOS
start reports/wallet-test-report.html       # Windows
xdg-open reports/wallet-test-report.html   # Linux
```

### Run a Single Suite

```bash
./scripts/run-one.sh smoke        # Full E2E happy path — fastest sanity check
./scripts/run-one.sh auth         # Registration, login, JWT protection
./scripts/run-one.sh accounts     # Account creation, listing, isolation
./scripts/run-one.sh deposit      # Deposit precision and edge cases
./scripts/run-one.sh transfer     # Transfer atomicity and currency rules
./scripts/run-one.sh transactions # History, pagination, immutability
./scripts/run-one.sh reversals    # Reversal auditability, double-reversal guard
./scripts/run-one.sh load         # Concurrent load — no balance corruption
```

### Additional Options

```bash
# Run against a different server
BASE_URL=http://staging-server:8080 ./scripts/run-all.sh

# Skip the load test
NO_LOAD=1 ./scripts/run-all.sh

# Run a single file directly with k6
k6 run --env BASE_URL=http://localhost:8080 tests/04_transfer.test.js
```

### Test Coverage

| Suite | Test IDs | What is verified |
|---|---|---|
| `00_smoke` | SMOKE-01…10 | Full E2E: register → login → account → deposit → transfer → list → reverse |
| `01_auth` | AUTH-01…09 | Register, login, duplicate email, wrong password, field validation, JWT |
| `02_accounts` | ACC-01…09 | USD/EUR/INR/GBP creation, listing, invalid currency, cross-user isolation |
| `03_deposit` | DEP-01…12 | Standard deposit, 3dp precision, min amount (0.001), zero/negative rejection |
| `04_transfer` | TRF-01…15 | Atomicity, exact balance drain, 3dp, insufficient funds, self-transfer |
| `05_transactions` | TXN-01…09 | List, paginated by account, field completeness, sort order DESC, immutability |
| `06_reversals` | REV-01…10 | Reverse deposit, reverse transfer (both legs), double-reversal guard, audit trail |
| `07_load` | LOAD-01…02 | 5 VUs × concurrent deposits, 3 VUs × concurrent transfers — no 500s, no corruption |

### Performance Thresholds

| Metric | Functional Tests | Load Test |
|---|---|---|
| `http_req_failed` rate | < 5% | < 10% |
| `http_req_duration` p95 | < 2000 ms | < 3000 ms |

### HTML Report

After each run, reports are written to `k6/reports/` (gitignored):

```
k6/reports/
├── smoke.json
├── auth.json
├── accounts.json
├── deposit.json
├── transfer.json
├── transactions.json
├── reversals.json
├── load.json
└── wallet-test-report.html    ← self-contained dashboard, no external dependencies
```

---

## API Reference

All endpoints are prefixed with `/api/v1`. Protected endpoints require `Authorization: Bearer <token>`.

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | ✗ | Register a new user, returns JWT |
| `POST` | `/auth/login` | ✗ | Login with email + password, returns JWT |

### Accounts

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/accounts` | ✓ | Create a new account |
| `GET` | `/accounts` | ✓ | List all accounts for the authenticated user |

### Transactions

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/accounts/deposit` | ✓ | Deposit funds into an account |
| `POST` | `/transfers` | ✓ | Transfer funds between two accounts |
| `GET` | `/transactions` | ✓ | List all transactions for the authenticated user |
| `GET` | `/accounts/{accountNumber}/transactions` | ✓ | Paginated transactions for a specific account |
| `POST` | `/transactions/reverse` | ✓ | Reverse a single transaction |
| `POST` | `/transactions/reverse-transfer/{transactionId}` | ✓ | Atomically reverse both legs of a transfer |
| `DELETE` | `/transactions/{id}` | ✓ | Always returns `405` — transactions are immutable |

### Error Response Format (RFC 9457 Problem Detail)

```json
{
  "type": "about:blank",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Insufficient funds in account 123456789012. Available: 50.000 USD, Requested: 500.000 USD",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

| Scenario | HTTP Status |
|---|---|
| Account / transaction not found | `404` |
| Insufficient funds | `422` |
| Already reversed | `409` |
| Invalid operation / currency mismatch | `400` |
| Validation failure | `400` |
| Bad credentials | `401` |
| Unauthenticated request | `401` |
| Accessing another user's resource | `403` |
| Delete transaction (immutability guard) | `405` |

---

## Database Schema

Three tables managed by Flyway migrations in `src/main/resources/db/migration/`:

```
users
├── id              UUID PRIMARY KEY
├── email           VARCHAR(255) UNIQUE NOT NULL
├── password_hash   VARCHAR(255) NOT NULL
├── full_name       VARCHAR(255) NOT NULL
├── role            VARCHAR(20) NOT NULL DEFAULT 'USER'
└── created_at      TIMESTAMPTZ NOT NULL

accounts
├── id              UUID PRIMARY KEY
├── account_number  VARCHAR(20) UNIQUE NOT NULL
├── user_id         UUID NOT NULL → users(id)
├── balance         NUMERIC(20,3) NOT NULL DEFAULT 0.000
├── currency_code   VARCHAR(3) NOT NULL
├── status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
├── created_at      TIMESTAMPTZ NOT NULL
└── updated_at      TIMESTAMPTZ NOT NULL

transactions  (immutable — PostgreSQL trigger blocks UPDATE and DELETE)
├── id                       UUID PRIMARY KEY
├── account_id               UUID NOT NULL → accounts(id)
├── type                     VARCHAR(30) NOT NULL
├── amount                   NUMERIC(20,3) NOT NULL
├── currency_code            VARCHAR(3) NOT NULL
├── balance_after            NUMERIC(20,3) NOT NULL
├── balance_after_currency   VARCHAR(3) NOT NULL
├── description              VARCHAR(500)
├── reference_transaction_id UUID → transactions(id)
├── related_account_id       UUID → accounts(id)
├── status                   VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
└── created_at               TIMESTAMPTZ NOT NULL
```

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/wallet_db` | JDBC connection URL |
| `DB_USERNAME` | No | `postgres` | Database username |
| `DB_PASSWORD` | **Yes** | — | Database password (no default — must be set) |
| `JWT_SECRET` | **Yes** | — | Base64 JWT signing secret, minimum 32 characters |
| `JWT_EXPIRATION_MS` | No | `86400000` (24 h) | Token expiry in milliseconds |
| `DB_POOL_MAX` | No | `10` | HikariCP maximum pool size |
| `DB_POOL_MIN_IDLE` | No | `2` | HikariCP minimum idle connections |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://localhost:3000` | Comma-separated allowed CORS origins |

---

## Business Rules

| Rule | Implementation |
|---|---|
| **Atomic transfers** | `@Transactional` on `TransferUseCase` — both account saves and both ledger inserts succeed or roll back together |
| **No negative balance** | `Account.debit()` (entity method) throws `InsufficientFundsException` before any state mutation |
| **Same-currency transfers only** | `TransferDomainService` enforces currency match between source account, destination account, and transfer amount |
| **3 decimal place precision** | `Money.SCALE = 3`, `RoundingMode.HALF_UP`, `NUMERIC(20,3)` in DB |
| **Immutable ledger** | `Transaction` entity has no setters; PostgreSQL trigger blocks `UPDATE`/`DELETE` on the `transactions` table |
| **Corrections via reversals** | `ReversalUseCase` (interactor) creates new `REVERSAL_DEBIT`/`REVERSAL_CREDIT` entries — original transactions are never modified |
| **Double-reversal protection** | `TransactionRepository.existsByReferenceTransactionId()` checked before every reversal |
| **Reversing a reversal is blocked** | `ReversalUseCase` rejects any transaction of type `REVERSAL_*` as an original |
| **Ownership enforcement** | `DepositUseCase` and `TransferUseCase` verify `account.getUserId().equals(authenticatedUserId)` — returns `403` on mismatch |
| **ISO 4217 currency validation** | `Money` entity uses Java's `Currency.getInstance()` — rejects invalid codes at the enterprise business rules layer |

---

## TDD Commit Journey

This project was built commit-by-commit in strict RED → GREEN order, working from the innermost Clean Architecture ring outward. Each feature follows the cycle: failing test committed first, then the minimal implementation to make it pass.

```
Enterprise Business Rules — Domain Entities (innermost ring)
  ├── Money value object (construction, arithmetic, equality, ISO 4217 validation)
  ├── Account entity (open factory, credit, debit, balance guard)
  ├── Transaction entity (immutable factory methods, Builder pattern)
  ├── User entity + UserPrincipal (Spring Security adapter)
  └── TransferDomainService (same-currency, self-transfer, ownership validation)

Application Business Rules — Use Case Interactors
  ├── AuthUseCase (register with BCrypt, login with JWT)
  ├── CreateAccountUseCase (account creation, user account listing)
  ├── DepositUseCase (deposit with ownership check)
  ├── TransferUseCase (atomic two-leg transfer via TransferDomainService)
  ├── GetTransactionUseCase (user transaction list, account paginated list)
  └── ReversalUseCase (single reversal, transfer reversal, double-reversal guard)

Interface Adapters & Frameworks — Infrastructure (outermost ring)
  ├── Flyway migrations V1 (users), V2 (accounts), V3 (transactions + immutability trigger)
  ├── JPA entities + Spring Data repositories + adapter implementations
  ├── JwtService (generate, validate, extract claims)
  ├── SecurityConfig + JwtAuthenticationFilter (stateless JWT)
  ├── GlobalExceptionHandler (RFC 9457 ProblemDetail, all domain exceptions mapped)
  ├── AuthController, AccountController, TransactionController
  └── WalletIntegrationTest (25 tests, full HTTP → Security → Interactor → DB vertical)

Frontend (React)
  ├── Vite + React scaffold, Axios API service, JWT interceptors
  ├── AuthContext, ProtectedRoute, Layout, shared UI components
  ├── AuthPage (login + register forms)
  ├── Dashboard (account summary + quick actions)
  ├── AccountsPage (create + list accounts)
  ├── DepositPage + TransferPage (write operations)
  └── TransactionsPage + ReversalPage (history + corrections)

k6 Test Suite
  ├── Smoke test — full E2E happy path (1 VU)
  ├── Functional suites — auth, accounts, deposit, transfer, transactions, reversals
  └── Load test — concurrent deposits (5 VUs) and transfers (3 VUs), no corruption
```