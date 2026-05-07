import { useState, useEffect } from 'react';
import { accountApi } from '../services/api';
import { Card, Button, Select, Badge, Amount, Modal, Spinner } from '../components/UI';
import { emitToast } from '../components/UI';
import './AccountsPage.css';

const CURRENCIES = [
  'USD','EUR','GBP','INR','JPY','AUD','CAD','CHF','CNY','SGD',
  'HKD','NZD','SEK','NOK','DKK','AED','SAR','MXN','BRL','ZAR',
];

export default function AccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [currency, setCurrency] = useState('USD');
  const [creating, setCreating] = useState(false);

  const load = () => {
    setLoading(true);
    accountApi.list()
      .then(res => setAccounts(res.data || []))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const createAccount = async () => {
    setCreating(true);
    try {
      await accountApi.create({ currencyCode: currency });
      emitToast(`${currency} account opened`, 'success');
      setShowModal(false);
      load();
    } catch (err) {
      emitToast(err.response?.data?.message || 'Failed to create account', 'error');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="accounts-page">
      <div className="accounts-page__header fade-up">
        <div>
          <h1 className="accounts-page__title">Accounts</h1>
          <p className="text-muted" style={{fontSize:13, marginTop:4}}>
            Manage your multi-currency accounts
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setShowModal(true)}>
          + Open Account
        </Button>
      </div>

      {loading ? (
        <div style={{display:'flex', justifyContent:'center', padding:'48px'}}>
          <Spinner size={28} />
        </div>
      ) : accounts.length === 0 ? (
        <Card className="accounts-page__empty fade-up-1">
          <p className="text-muted" style={{marginBottom:16}}>No accounts yet. Open one to get started.</p>
          <Button variant="primary" size="sm" onClick={() => setShowModal(true)}>
            Open First Account
          </Button>
        </Card>
      ) : (
        <div className="accounts-page__grid fade-up-1">
          {accounts.map((acc, i) => (
            <Card key={acc.id} className="acc-card" style={{animationDelay:`${i*0.05}s`}}>
              <div className="acc-card__top">
                <div className="acc-card__currency-badge">{acc.currencyCode}</div>
                <Badge variant={acc.status === 'ACTIVE' ? 'credit' : 'default'}>
                  {acc.status}
                </Badge>
              </div>

              <div className="acc-card__balance">
                <Amount value={acc.balance} currency={acc.currencyCode} />
              </div>

              <div className="acc-card__meta">
                <div className="acc-card__row">
                  <span className="text-faint" style={{fontSize:11}}>Account No.</span>
                  <span className="mono" style={{fontSize:11, color:'var(--ink-2)'}}>
                    {acc.accountNumber}
                  </span>
                </div>
                <div className="acc-card__row">
                  <span className="text-faint" style={{fontSize:11}}>Opened</span>
                  <span className="mono text-faint" style={{fontSize:11}}>
                    {new Date(acc.createdAt).toLocaleDateString('en-GB', {
                      day:'2-digit', month:'short', year:'numeric'
                    })}
                  </span>
                </div>
                <div className="acc-card__row">
                  <span className="text-faint" style={{fontSize:11}}>ID</span>
                  <span className="mono text-faint" style={{fontSize:10}}>
                    {acc.id?.slice(0,8)}…
                  </span>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Open New Account">
        <div style={{display:'flex', flexDirection:'column', gap:20}}>
          <Select
            label="Currency"
            value={currency}
            onChange={e => setCurrency(e.target.value)}
          >
            {CURRENCIES.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </Select>
          <p style={{fontSize:12, color:'var(--ink-2)', lineHeight:1.6}}>
            A new wallet account will be opened in <strong style={{color:'var(--gold)'}}>{currency}</strong>.
            Transfers are only permitted between same-currency accounts.
          </p>
          <Button variant="primary" size="lg" loading={creating} onClick={createAccount}>
            Open {currency} Account
          </Button>
        </div>
      </Modal>
    </div>
  );
}
