# Vault — Multi-Currency Digital Wallet Frontend

React SPA frontend for the Mini-Wallet Banking System backend.

## Tech Stack

- **React 18** + **Vite 5** — fast SPA with HMR
- **React Router v6** — client-side routing with protected routes
- **Axios** — HTTP client with JWT interceptors
- **Custom CSS** — no UI framework dependency (DM Serif Display + DM Mono fonts)

## Pages & Features

| Route | Feature |
|---|---|
| `/login` | Register / Login with JWT auth |
| `/dashboard` | Portfolio overview, balance by currency, recent transactions |
| `/transactions` | Full immutable ledger — filterable by account & type, paginated |
| `/deposit` | Deposit funds to an account |
| `/transfer` | Atomic fund transfer between same-currency accounts |
| `/accounts` | View & create multi-currency accounts |

## Setup

### Prerequisites
- Node.js 18+
- Backend running at `http://localhost:8080`

### Install & Run

```bash
cd wallet-frontend
npm install
npm run dev        # starts at http://localhost:3000
```

The Vite dev server proxies `/api/*` to `http://localhost:8080` automatically.

### Production Build

```bash
npm run build      # outputs to dist/
npm run preview    # preview the build locally
```

## Backend API Contract

The frontend calls these backend endpoints:

```
POST   /api/auth/register          { email, password, fullName }
POST   /api/auth/login             { email, password }

GET    /api/accounts               List accounts for authenticated user
POST   /api/accounts               { currencyCode }

GET    /api/transactions           All transactions for authenticated user
GET    /api/accounts/:accountNumber/transactions?page=&size=   Paginated by account

POST   /api/transactions/deposit   { accountNumber, amount, currencyCode, description }
POST   /api/transactions/transfer  { sourceAccountNumber, destinationAccountNumber, amount, currencyCode, description }
```

All authenticated endpoints require `Authorization: Bearer <jwt>` header (handled automatically).

## Architecture

```
src/
├── context/        AuthContext — user state + JWT management
├── services/       api.js — axios instance + all API calls
├── components/     UI.jsx/css — shared components (Button, Input, Modal, Toast…)
│                   Layout.jsx/css — sidebar navigation shell
│                   ProtectedRoute.jsx — auth guard
└── pages/          AuthPage, Dashboard, Transactions, DepositPage,
                    TransferPage, AccountsPage
```
