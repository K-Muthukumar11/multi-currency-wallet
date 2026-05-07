import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('vault_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Handle 401 — only redirect on non-auth endpoints (session expired)
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const isAuthEndpoint = err.config?.url?.includes('/auth/');
      if (!isAuthEndpoint) {
        localStorage.removeItem('vault_token');
        localStorage.removeItem('vault_user');
        window.location.href = '/login?reason=session_expired';
      }
    }
    return Promise.reject(err);
  }
);

// Auth
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
};

// Accounts
export const accountApi = {
  list: () => api.get('/accounts'),
  create: (data) => api.post('/accounts', data),
};

// Transactions
export const transactionApi = {
  listForUser: () => api.get('/transactions'),
  listForAccount: (accountId, page = 0, size = 20) =>
    api.get(`/accounts/${accountId}/transactions?page=${page}&size=${size}`),
  deposit: (data) => api.post('/accounts/deposit', data),
  transfer: (data) => api.post('/transfers', data),

  /**
   * Reverse a single transaction (DEPOSIT, CREDIT, DEBIT).
   * POST /api/v1/transactions/reverse
   * Body: { originalTransactionId: UUID, reason?: string }
   * Returns: TransactionResponse[]
   */
  reverse: (data) => api.post('/transactions/reverse', data),

  /**
   * Atomically reverse both legs of a transfer.
   * POST /api/v1/transactions/reverse-transfer/{transactionId}
   * Returns: TransactionResponse[]
   */
  reverseTransfer: (transactionId) =>
    api.post(`/transactions/reverse-transfer/${transactionId}`),
};

export default api;
