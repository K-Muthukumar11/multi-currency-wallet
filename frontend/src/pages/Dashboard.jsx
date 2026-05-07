import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { accountApi, transactionApi } from '../services/api';
import { Card, Badge, Amount, Spinner, Button } from '../components/UI';
import './Dashboard.css';

const TYPE_BADGE = {
  DEPOSIT: 'deposit',
  CREDIT:  'credit',
  DEBIT:   'debit',
  TRANSFER_DEBIT:  'debit',
  TRANSFER_CREDIT: 'credit',
  REVERSAL: 'reversal',
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

export default function Dashboard() {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      accountApi.list(),
      transactionApi.listForUser(),
    ])
    .then(([accRes, txRes]) => {
      setAccounts(accRes.data);
      setTransactions(txRes.data?.slice(0, 5) || []);
    })
    .finally(() => setLoading(false));
  }, []);

  const totalByCurrency = accounts.reduce((acc, a) => {
    acc[a.currencyCode] = (acc[a.currencyCode] || 0) + parseFloat(a.balance);
    return acc;
  }, {});

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  const firstName = user?.fullName?.split(' ')[0] || 'there';

  if (loading) return (
    <div className="dashboard-loading">
      <Spinner size={32} />
    </div>
  );

  return (
    <div className="dashboard">
      <div className="dashboard__header fade-up">
        <div>
          <h1 className="dashboard__greeting">{greeting}, {firstName}</h1>
          <p className="dashboard__subline text-muted">Here's your portfolio overview</p>
        </div>
        <div className="dashboard__header-actions">
          <Link to="/deposit"><Button variant="secondary" size="sm">↓ Deposit</Button></Link>
          <Link to="/transfer"><Button variant="primary" size="sm">⇄ Transfer</Button></Link>
        </div>
      </div>

      {/* Balance cards */}
      <section className="dashboard__balances fade-up-1">
        {Object.entries(totalByCurrency).length === 0 ? (
          <Card className="dashboard__empty-balance">
            <p className="text-muted">No accounts yet.</p>
            <Link to="/accounts"><Button variant="primary" size="sm" style={{marginTop: 12}}>Open Account</Button></Link>
          </Card>
        ) : (
          Object.entries(totalByCurrency).map(([currency, total]) => (
            <Card key={currency} className="balance-card">
              <div className="balance-card__currency">{currency}</div>
              <div className="balance-card__amount mono">
                {new Intl.NumberFormat('en-US', { minimumFractionDigits: 3 }).format(total)}
              </div>
              <div className="balance-card__label text-faint">Total Balance</div>
            </Card>
          ))
        )}
      </section>

      {/* Accounts row */}
      <section className="dashboard__section fade-up-2">
        <div className="dashboard__section-header">
          <h2 className="dashboard__section-title">Accounts</h2>
          <Link to="/accounts" className="dashboard__see-all text-gold">View all →</Link>
        </div>
        <div className="accounts-grid">
          {accounts.map(acc => (
            <Card key={acc.id} className="account-pill">
              <div className="account-pill__top">
                <span className="account-pill__number mono text-faint">{acc.accountNumber}</span>
                <Badge variant={acc.status === 'ACTIVE' ? 'credit' : 'default'}>
                  {acc.status}
                </Badge>
              </div>
              <div className="account-pill__balance">
                <Amount value={acc.balance} currency={acc.currencyCode} />
              </div>
            </Card>
          ))}
          {accounts.length === 0 && (
            <p className="text-faint" style={{fontSize:13}}>No accounts found.</p>
          )}
        </div>
      </section>

      {/* Recent transactions */}
      <section className="dashboard__section fade-up-3">
        <div className="dashboard__section-header">
          <h2 className="dashboard__section-title">Recent Transactions</h2>
          <Link to="/transactions" className="dashboard__see-all text-gold">View ledger →</Link>
        </div>

        {transactions.length === 0 ? (
          <Card className="dashboard__empty">
            <p className="text-muted" style={{fontSize:13}}>No transactions yet.</p>
          </Card>
        ) : (
          <Card className="tx-list">
            {transactions.map((tx, i) => (
              <div key={tx.id} className="tx-row" style={{animationDelay:`${i*0.04}s`}}>
                <div className="tx-row__type-icon">
                  <span>{['DEPOSIT','CREDIT','TRANSFER_CREDIT'].includes(tx.type) ? '↙' : '↗'}</span>
                </div>
                <div className="tx-row__info">
                  <p className="tx-row__desc">{tx.description || tx.type}</p>
                  <p className="tx-row__date text-faint mono">{formatDate(tx.createdAt)}</p>
                </div>
                <div className="tx-row__right">
                  <Amount value={tx.amount} currency={tx.currencyCode} sign={txSign(tx.type)} />
                  <Badge variant={TYPE_BADGE[tx.type] || 'default'}>{tx.type}</Badge>
                </div>
              </div>
            ))}
          </Card>
        )}
      </section>
    </div>
  );
}
