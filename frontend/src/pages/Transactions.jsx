import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { transactionApi, accountApi } from '../services/api';
import { Card, Badge, Amount, Spinner, Select, Modal, Button, Input } from '../components/UI';
import { emitToast } from '../components/UI';
import './Transactions.css';

const TYPE_BADGE = {
  DEPOSIT: 'deposit',
  CREDIT: 'credit',
  DEBIT: 'debit',
  TRANSFER_DEBIT: 'debit',
  TRANSFER_CREDIT: 'credit',
  REVERSAL: 'reversal',
};

const TYPE_ICON = {
  DEPOSIT: '↙',
  CREDIT: '↙',
  TRANSFER_CREDIT: '↙',
  DEBIT: '↗',
  TRANSFER_DEBIT: '↗',
  REVERSAL: '↺',
};

function txSign(type) {
  if (['CREDIT', 'DEPOSIT', 'TRANSFER_CREDIT'].includes(type)) return '+';
  return '-';
}

function formatDate(iso) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso));
}

function formatId(id) {
  return id?.slice(0, 8).toUpperCase() + '…';
}

function canReverse(tx) {
  const reversibleTypes = ['DEPOSIT', 'TRANSFER_DEBIT', 'TRANSFER_CREDIT', 'CREDIT', 'DEBIT'];
  return reversibleTypes.includes(tx.type) && tx.status === 'COMPLETED' && !tx.reversalId;
}

export default function Transactions() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState('all');
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filter, setFilter] = useState('ALL');

  // Reversal modal state
  const [reversalTx, setReversalTx] = useState(null);
  const [reversalReason, setReversalReason] = useState('');
  const [reversing, setReversing] = useState(false);

  // Load accounts
  useEffect(() => {
    accountApi.list().then(res => setAccounts(res.data || []));
  }, []);

  const loadTx = useCallback(async () => {
    setLoading(true);
    try {
      if (selectedAccount === 'all') {
        const res = await transactionApi.listForUser();
        setTransactions(res.data || []);
        setTotalPages(1);
      } else {
        const res = await transactionApi.listForAccount(selectedAccount, page, 15);
        setTransactions(res.data?.content || []);
        setTotalPages(res.data?.totalPages || 1);
      }
    } finally {
      setLoading(false);
    }
  }, [selectedAccount, page]);

  useEffect(() => { loadTx(); }, [loadTx]);

  const filtered = filter === 'ALL'
    ? transactions
    : transactions.filter(tx => tx.type === filter || tx.type.includes(filter));

  const openReversalModal = (tx, e) => {
    e.stopPropagation();
    setReversalTx(tx);
    setReversalReason('');
  };

  const handleQuickReverse = async () => {
    if (!reversalTx) return;
    setReversing(true);
    try {
      const isTransfer =
        reversalTx.type === 'TRANSFER_DEBIT' || reversalTx.type === 'TRANSFER_CREDIT';
      if (isTransfer) {
        await transactionApi.reverseTransfer(reversalTx.id);
      } else {
        await transactionApi.reverse({
          originalTransactionId: reversalTx.id,
          reason: reversalReason.trim() || undefined,
        });
      }
      emitToast('Reversal recorded successfully', 'success');
      setReversalTx(null);
      loadTx();
    } catch (err) {
      const msg = err.response?.data?.message || 'Reversal failed';
      emitToast(msg, 'error');
    } finally {
      setReversing(false);
    }
  };

  return (
    <div className="ledger">
      <div className="ledger__header fade-up">
        <div>
          <h1 className="ledger__title">Transaction Ledger</h1>
          <p className="text-muted" style={{ fontSize: 13, marginTop: 4 }}>
            Immutable audit trail of all account activity
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="ledger__total text-faint mono">{filtered.length} entries</span>
          <button
            className="ledger__reverse-nav-btn"
            onClick={() => navigate('/reversal')}
            title="Go to Reversal page"
          >
            ↺ Reversal
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="ledger__filters fade-up-1">
        <Select
          value={selectedAccount}
          onChange={e => { setSelectedAccount(e.target.value); setPage(0); }}
          style={{ minWidth: 200 }}
        >
          <option value="all">All Accounts</option>
          {accounts.map(a => (
            <option key={a.id} value={a.id}>
              {a.accountNumber} ({a.currencyCode})
            </option>
          ))}
        </Select>

        <div className="ledger__type-tabs">
          {['ALL', 'DEPOSIT', 'DEBIT', 'CREDIT', 'REVERSAL'].map(t => (
            <button
              key={t}
              className={`ledger__tab ${filter === t ? 'ledger__tab--active' : ''}`}
              onClick={() => setFilter(t)}
            >
              {t}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="ledger__loading"><Spinner size={28} /></div>
      ) : filtered.length === 0 ? (
        <Card className="ledger__empty">
          <p className="text-muted">No transactions found.</p>
        </Card>
      ) : (
        <Card className="ledger__card fade-up-2">
          <div className="ledger__table-wrap">
            <table className="ledger__table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Description</th>
                  <th>Amount</th>
                  <th>Balance After</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((tx, i) => (
                  <tr
                    key={tx.id}
                    className="ledger__row"
                    style={{ animationDelay: `${i * 0.02}s` }}
                    title={`Account: ${tx.accountId}\nRef: ${tx.referenceTransactionId || '—'}`}
                  >
                    <td>
                      <span className="mono text-faint" style={{ fontSize: 11 }}>
                        {formatId(tx.id)}
                      </span>
                    </td>
                    <td>
                      <div className="ledger__type-cell">
                        <span className="ledger__type-icon">
                          {TYPE_ICON[tx.type] || '•'}
                        </span>
                        <Badge variant={TYPE_BADGE[tx.type] || 'default'}>
                          {tx.type.replace('_', ' ')}
                        </Badge>
                      </div>
                    </td>
                    <td>
                      <span className="ledger__desc">{tx.description || '—'}</span>
                      {tx.referenceTransactionId && (
                        <span className="ledger__ref mono text-faint">
                          ref: {formatId(tx.referenceTransactionId)}
                        </span>
                      )}
                    </td>
                    <td>
                      <Amount
                        value={tx.amount}
                        currency={tx.currencyCode}
                        sign={txSign(tx.type)}
                      />
                    </td>
                    <td>
                      <span className="mono text-muted" style={{ fontSize: 12 }}>
                        {new Intl.NumberFormat('en-US', { minimumFractionDigits: 3 }).format(tx.balanceAfter)}
                        {' '}
                        <span style={{ opacity: .5, fontSize: '0.85em' }}>{tx.currencyCode}</span>
                      </span>
                    </td>
                    <td>
                      <Badge variant={tx.status?.toLowerCase()}>
                        {tx.status}
                      </Badge>
                    </td>
                    <td>
                      <span className="mono text-faint" style={{ fontSize: 11 }}>
                        {formatDate(tx.createdAt)}
                      </span>
                    </td>
                    <td>
                      {canReverse(tx) && (
                        <button
                          className="ledger__reverse-btn"
                          onClick={(e) => openReversalModal(tx, e)}
                          title="Reverse this transaction"
                        >
                          ↺
                        </button>
                      )}
                      {tx.type === 'REVERSAL' && (
                        <span className="ledger__reversed-tag" title="This is a reversal entry">
                          reversal
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination - only when per-account */}
          {selectedAccount !== 'all' && totalPages > 1 && (
            <div className="ledger__pagination">
              <button
                className="ledger__page-btn"
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >← Prev</button>
              <span className="text-faint mono" style={{ fontSize: 12 }}>
                {page + 1} / {totalPages}
              </span>
              <button
                className="ledger__page-btn"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >Next →</button>
            </div>
          )}
        </Card>
      )}

      {/* Quick-Reverse Modal */}
      <Modal
        open={!!reversalTx}
        onClose={() => setReversalTx(null)}
        title="Reverse Transaction"
      >
        {reversalTx && (
          <div className="ledger__reversal-modal">
            <div className="ledger__reversal-modal-info">
              <div className="ledger__reversal-modal-row">
                <span className="text-faint">Transaction</span>
                <span className="mono" style={{ fontSize: 12 }}>{formatId(reversalTx.id)}</span>
              </div>
              <div className="ledger__reversal-modal-row">
                <span className="text-faint">Type</span>
                <Badge variant={TYPE_BADGE[reversalTx.type] || 'default'}>
                  {reversalTx.type.replace('_', ' ')}
                </Badge>
              </div>
              <div className="ledger__reversal-modal-row">
                <span className="text-faint">Amount</span>
                <Amount value={reversalTx.amount} currency={reversalTx.currencyCode} sign={txSign(reversalTx.type)} />
              </div>
              <div className="ledger__reversal-modal-row">
                <span className="text-faint">Date</span>
                <span className="mono text-muted" style={{ fontSize: 12 }}>{formatDate(reversalTx.createdAt)}</span>
              </div>
            </div>

            {(reversalTx.type === 'TRANSFER_DEBIT' || reversalTx.type === 'TRANSFER_CREDIT') && (
              <div className="ledger__reversal-transfer-notice">
                ⇄ Both legs of this transfer will be reversed atomically
              </div>
            )}

            <Input
              label="Reason (optional)"
              placeholder="e.g. Duplicate entry, customer request…"
              value={reversalReason}
              onChange={e => setReversalReason(e.target.value)}
            />

            <div className="ledger__reversal-modal-actions">
              <Button variant="ghost" size="md" onClick={() => setReversalTx(null)}>
                Cancel
              </Button>
              <Button variant="danger" size="md" loading={reversing} onClick={handleQuickReverse}>
                ↺ Confirm Reversal
              </Button>
            </div>

            <p className="ledger__reversal-note text-faint">
              The original transaction is immutable. A new reversal entry will be created in the ledger.
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}
