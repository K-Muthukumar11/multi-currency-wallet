import { useState, useEffect } from 'react';
import { transactionApi } from '../services/api';
import { Card, Button, Input, Badge, Amount, Spinner } from '../components/UI';
import { emitToast } from '../components/UI';
import './ActionPage.css';
import './ReversalPage.css';

function formatId(id) {
  return id?.slice(0, 8).toUpperCase() + '…';
}

function formatDate(iso) {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso));
}

const TYPE_ICON = {
  DEPOSIT: '↙', CREDIT: '↙', TRANSFER_CREDIT: '↙',
  DEBIT: '↗', TRANSFER_DEBIT: '↗', REVERSAL: '↺',
};

function txSign(type) {
  if (['CREDIT', 'DEPOSIT', 'TRANSFER_CREDIT'].includes(type)) return '+';
  return '-';
}

function canReverse(tx) {
  // Can reverse: completed DEPOSIT, TRANSFER_DEBIT/CREDIT (not already a REVERSAL)
  const reversibleTypes = ['DEPOSIT', 'TRANSFER_DEBIT', 'TRANSFER_CREDIT', 'CREDIT', 'DEBIT'];
  return reversibleTypes.includes(tx.type) && tx.status === 'COMPLETED' && !tx.reversalId;
}

export default function ReversalPage() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [txIdInput, setTxIdInput] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [results, setResults] = useState(null);
  const [selectedTx, setSelectedTx] = useState(null);
  const [searchError, setSearchError] = useState('');
  const [mode, setMode] = useState('browse'); // 'browse' | 'manual'

  useEffect(() => {
    transactionApi.listForUser()
      .then(res => setTransactions(res.data || []))
      .catch(() => emitToast('Failed to load transactions', 'error'))
      .finally(() => setLoading(false));
  }, []);

  const reversible = transactions.filter(canReverse);

  const handleSelectTx = (tx) => {
    setSelectedTx(tx);
    setTxIdInput(tx.id);
    setResults(null);
    setSearchError('');
  };

  const handleManualSearch = () => {
    if (!txIdInput.trim()) { setSearchError('Enter a transaction ID'); return; }
    const found = transactions.find(t => t.id === txIdInput.trim());
    if (found) {
      setSelectedTx(found);
      setSearchError('');
    } else {
      setSearchError('Transaction not found in your ledger');
      setSelectedTx(null);
    }
  };

  const isTransfer = selectedTx &&
    (selectedTx.type === 'TRANSFER_DEBIT' || selectedTx.type === 'TRANSFER_CREDIT');

  const handleReverse = async () => {
    if (!selectedTx) { emitToast('Select a transaction first', 'error'); return; }
    setSubmitting(true); setResults(null);
    try {
      let res;
      if (isTransfer) {
        // Use the atomic transfer reversal endpoint
        res = await transactionApi.reverseTransfer(selectedTx.id);
      } else {
        res = await transactionApi.reverse({
          originalTransactionId: selectedTx.id,
          reason: reason.trim() || undefined,
        });
      }
      setResults(res.data);
      emitToast('Reversal recorded successfully', 'success');
      // Refresh transaction list
      transactionApi.listForUser().then(r => setTransactions(r.data || []));
      setSelectedTx(null);
      setTxIdInput('');
      setReason('');
    } catch (err) {
      const msg = err.response?.data?.message || 'Reversal failed';
      emitToast(msg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="action-page reversal-page">
      <div className="action-page__header fade-up">
        <div className="reversal-page__header-row">
          <div>
            <h1 className="action-page__title">Reverse Transaction</h1>
            <p className="text-muted" style={{ fontSize: 13, marginTop: 4 }}>
              Create an explicit reversal entry — the original transaction remains immutable
            </p>
          </div>
          <div className="reversal-page__mode-toggle">
            <button
              className={`reversal-page__mode-btn ${mode === 'browse' ? 'reversal-page__mode-btn--active' : ''}`}
              onClick={() => setMode('browse')}
            >Browse</button>
            <button
              className={`reversal-page__mode-btn ${mode === 'manual' ? 'reversal-page__mode-btn--active' : ''}`}
              onClick={() => setMode('manual')}
            >Manual ID</button>
          </div>
        </div>
      </div>

      <div className="action-page__layout reversal-page__layout">

        {/* Left: Transaction picker */}
        <div className="reversal-page__left">

          {mode === 'manual' ? (
            <Card className="reversal-page__manual fade-up-1">
              <div className="action-page__form-inner">
                <p className="reversal-page__section-label">Transaction ID</p>
                <div className="reversal-page__search-row">
                  <Input
                    placeholder="Paste full UUID e.g. 3fa85f64-…"
                    value={txIdInput}
                    onChange={e => setTxIdInput(e.target.value)}
                    error={searchError}
                    style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}
                  />
                  <Button variant="secondary" size="md" onClick={handleManualSearch}>
                    Look up
                  </Button>
                </div>

                {selectedTx && (
                  <div className="reversal-page__found-tx">
                    <div className="reversal-page__found-row">
                      <span className="reversal-page__found-icon">{TYPE_ICON[selectedTx.type]}</span>
                      <div>
                        <p className="reversal-page__found-type">{selectedTx.type.replace('_', ' ')}</p>
                        <p className="mono text-faint" style={{ fontSize: 11 }}>
                          {selectedTx.id?.slice(0, 20)}…
                        </p>
                      </div>
                      <Amount value={selectedTx.amount} currency={selectedTx.currencyCode} sign={txSign(selectedTx.type)} />
                    </div>
                  </div>
                )}
              </div>
            </Card>
          ) : (
            <Card className="reversal-page__browse fade-up-1">
              <div className="reversal-page__browse-header">
                <p className="reversal-page__section-label">Reversible Transactions</p>
                <span className="reversal-page__count">{reversible.length} eligible</span>
              </div>

              {loading ? (
                <div className="reversal-page__loading"><Spinner size={24} /></div>
              ) : reversible.length === 0 ? (
                <div className="reversal-page__empty">
                  <p className="text-muted">No reversible transactions found.</p>
                  <p className="text-faint" style={{ fontSize: 12, marginTop: 4 }}>
                    Only completed, non-reversed transactions can be reversed.
                  </p>
                </div>
              ) : (
                <div className="reversal-page__tx-list">
                  {reversible.map(tx => (
                    <button
                      key={tx.id}
                      className={`reversal-page__tx-row ${selectedTx?.id === tx.id ? 'reversal-page__tx-row--selected' : ''}`}
                      onClick={() => handleSelectTx(tx)}
                    >
                      <span className="reversal-page__tx-icon">{TYPE_ICON[tx.type] || '•'}</span>
                      <div className="reversal-page__tx-info">
                        <span className="reversal-page__tx-type">
                          {tx.type.replace('_', ' ')}
                        </span>
                        <span className="mono text-faint" style={{ fontSize: 11 }}>
                          {formatId(tx.id)} · {formatDate(tx.createdAt)}
                        </span>
                        {tx.description && (
                          <span className="text-muted reversal-page__tx-desc">{tx.description}</span>
                        )}
                      </div>
                      <div className="reversal-page__tx-amount">
                        <Amount
                          value={tx.amount}
                          currency={tx.currencyCode}
                          sign={txSign(tx.type)}
                        />
                      </div>
                      {selectedTx?.id === tx.id && (
                        <span className="reversal-page__tx-check">✓</span>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </Card>
          )}
        </div>

        {/* Right: Confirm & submit */}
        <div className="action-page__side">

          {/* Selected TX summary */}
          {selectedTx ? (
            <Card className="reversal-page__confirm fade-up-1">
              <p className="reversal-page__section-label" style={{ marginBottom: 14 }}>
                Transaction to Reverse
              </p>

              <div className="reversal-page__confirm-rows">
                <div className="result-row">
                  <span className="text-faint">Original ID</span>
                  <span className="mono" style={{ fontSize: 11 }}>{formatId(selectedTx.id)}</span>
                </div>
                <div className="result-row">
                  <span className="text-faint">Type</span>
                  <Badge variant={selectedTx.type.includes('DEBIT') ? 'debit' : 'credit'}>
                    {selectedTx.type.replace('_', ' ')}
                  </Badge>
                </div>
                <div className="result-row">
                  <span className="text-faint">Amount</span>
                  <Amount value={selectedTx.amount} currency={selectedTx.currencyCode} sign={txSign(selectedTx.type)} />
                </div>
                <div className="result-row">
                  <span className="text-faint">Date</span>
                  <span className="mono text-muted" style={{ fontSize: 11 }}>{formatDate(selectedTx.createdAt)}</span>
                </div>
                {isTransfer && (
                  <div className="reversal-page__transfer-notice">
                    <span className="reversal-page__transfer-icon">⇄</span>
                    Both legs of this transfer will be reversed atomically
                  </div>
                )}
              </div>

              <div className="reversal-page__reason-wrap">
                <Input
                  label="Reason (optional)"
                  placeholder="e.g. Duplicate entry, customer request…"
                  value={reason}
                  onChange={e => setReason(e.target.value)}
                />
              </div>

              <Button
                variant="danger"
                size="lg"
                loading={submitting}
                onClick={handleReverse}
              >
                ↺ Confirm Reversal
              </Button>

              <p className="reversal-page__immutability-note text-faint">
                The original transaction will not be modified. A new reversal entry will be added to the ledger referencing this transaction ID.
              </p>
            </Card>
          ) : (
            <Card className="reversal-page__empty-confirm fade-up-1">
              <div className="reversal-page__empty-icon">↺</div>
              <p className="text-muted" style={{ fontSize: 13, textAlign: 'center' }}>
                Select a transaction on the left to begin reversal
              </p>
            </Card>
          )}

          {/* Rules card */}
          <Card className="action-page__info fade-up-2">
            <p className="action-page__info-title">Reversal Rules</p>
            <ul className="action-page__info-list">
              <li>Original transactions are <strong>immutable</strong> — never edited or deleted</li>
              <li>A new ledger entry is created referencing the original TX ID</li>
              <li>Each transaction may only be reversed <strong>once</strong></li>
              <li>Transfer reversals are <strong>atomic</strong> — both legs reverse together</li>
              <li>Only <strong>COMPLETED</strong> transactions are eligible</li>
            </ul>
          </Card>
        </div>
      </div>

      {/* Results section */}
      {results && results.length > 0 && (
        <div className="reversal-page__results fade-up">
          <h2 className="reversal-page__results-title">Reversal Recorded</h2>
          <div className="reversal-page__results-grid">
            {results.map(r => (
              <Card key={r.id} className="reversal-page__result-card">
                <div className="reversal-page__result-icon">↺</div>
                <div className="reversal-page__result-rows">
                  <div className="result-row">
                    <span className="text-faint">Reversal TX ID</span>
                    <span className="mono" style={{ fontSize: 11 }}>{formatId(r.id)}</span>
                  </div>
                  <div className="result-row">
                    <span className="text-faint">Type</span>
                    <Badge variant="reversal">{r.type?.replace('_', ' ')}</Badge>
                  </div>
                  <div className="result-row">
                    <span className="text-faint">Amount</span>
                    <Amount value={r.amount} currency={r.currencyCode} sign="+" />
                  </div>
                  <div className="result-row">
                    <span className="text-faint">Balance After</span>
                    <span className="mono text-muted" style={{ fontSize: 12 }}>
                      {new Intl.NumberFormat('en-US', { minimumFractionDigits: 3 }).format(r.balanceAfter)}
                      {' '}<span style={{ opacity: 0.5 }}>{r.currencyCode}</span>
                    </span>
                  </div>
                  <div className="result-row">
                    <span className="text-faint">Ref TX</span>
                    <span className="mono text-faint" style={{ fontSize: 11 }}>
                      {formatId(r.referenceTransactionId || r.originalTransactionId)}
                    </span>
                  </div>
                  <div className="result-row">
                    <span className="text-faint">Status</span>
                    <Badge variant="completed">{r.status || 'COMPLETED'}</Badge>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
