import { useState, useEffect } from 'react';
import { accountApi, transactionApi } from '../services/api';
import { Card, Button, Input, Select, Badge, Amount } from '../components/UI';
import { emitToast } from '../components/UI';
import './ActionPage.css';

export default function DepositPage() {
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({ accountNumber: '', amount: '', currencyCode: '', description: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    accountApi.list().then(res => {
      const accs = res.data || [];
      setAccounts(accs);
      if (accs.length > 0) {
        setForm(f => ({
          ...f,
          accountNumber: accs[0].accountNumber,
          currencyCode: accs[0].currencyCode,
        }));
      }
    });
  }, []);

  const selectedAccount = accounts.find(a => a.accountNumber === form.accountNumber);

  const update = (k) => (e) => {
    const val = e.target.value;
    setForm(f => {
      const next = { ...f, [k]: val };
      if (k === 'accountNumber') {
        const acc = accounts.find(a => a.accountNumber === val);
        if (acc) next.currencyCode = acc.currencyCode;
      }
      return next;
    });
  };

  const validate = () => {
    const e = {};
    if (!form.accountNumber) e.accountNumber = 'Select an account';
    if (!form.amount || isNaN(form.amount) || parseFloat(form.amount) <= 0)
      e.amount = 'Enter a valid amount';
    if (!form.currencyCode) e.currencyCode = 'Required';
    return e;
  };

  const submit = async () => {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({}); setLoading(true); setResult(null);
    try {
      const res = await transactionApi.deposit({
        accountNumber: form.accountNumber,
        amount: parseFloat(form.amount),
        currencyCode: form.currencyCode,
        description: form.description,
      });
      setResult(res.data);
      emitToast(`Deposit successful — ${form.amount} ${form.currencyCode}`, 'success');
      setForm(f => ({ ...f, amount: '', description: '' }));
    } catch (err) {
      const msg = err.response?.data?.message || 'Deposit failed';
      emitToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="action-page">
      <div className="action-page__header fade-up">
        <h1 className="action-page__title">Deposit Funds</h1>
        <p className="text-muted" style={{fontSize:13, marginTop:4}}>
          Credit an account with new funds
        </p>
      </div>

      <div className="action-page__layout">
        <Card className="action-page__form fade-up-1">
          <div className="action-page__form-inner">
            <Select
              label="Account"
              value={form.accountNumber}
              onChange={update('accountNumber')}
              error={errors.accountNumber}
            >
              {accounts.map(a => (
                <option key={a.id} value={a.accountNumber}>
                  {a.accountNumber} — {a.currencyCode}
                </option>
              ))}
            </Select>

            <Input
              label="Amount"
              type="number"
              min="0.001"
              step="0.001"
              placeholder="0.000"
              value={form.amount}
              onChange={update('amount')}
              error={errors.amount}
              suffix={form.currencyCode}
            />

            <Input
              label="Description (optional)"
              placeholder="e.g. Initial funding"
              value={form.description}
              onChange={update('description')}
            />

            <Button variant="primary" size="lg" loading={loading} onClick={submit}>
              ↓ Deposit Funds
            </Button>
          </div>
        </Card>

        <div className="action-page__side fade-up-2">
          {/* Account preview */}
          {selectedAccount && (
            <Card className="action-page__preview">
              <p className="action-page__preview-label">Destination Account</p>
              <p className="mono" style={{fontSize:13, color:'var(--ink-2)', marginBottom:8}}>
                {selectedAccount.accountNumber}
              </p>
              <div style={{display:'flex', alignItems:'center', gap:8}}>
                <Amount value={selectedAccount.balance} currency={selectedAccount.currencyCode} />
                <Badge variant={selectedAccount.status === 'ACTIVE' ? 'credit' : 'default'}>
                  {selectedAccount.status}
                </Badge>
              </div>
              <p className="action-page__preview-sublabel text-faint">Current Balance</p>
            </Card>
          )}

          {/* Result */}
          {result && (
            <Card className="action-page__result">
              <div className="action-page__result-icon">✓</div>
              <p className="action-page__result-label">Transaction Recorded</p>
              <div className="action-page__result-rows">
                <div className="result-row">
                  <span className="text-faint">TX ID</span>
                  <span className="mono" style={{fontSize:11}}>{result.id?.slice(0,16)}…</span>
                </div>
                <div className="result-row">
                  <span className="text-faint">Amount</span>
                  <Amount value={result.amount} currency={result.currencyCode} sign="+" />
                </div>
                <div className="result-row">
                  <span className="text-faint">Balance After</span>
                  <span className="mono text-muted" style={{fontSize:12}}>
                    {result.balanceAfter} {result.currencyCode}
                  </span>
                </div>
                <div className="result-row">
                  <span className="text-faint">Status</span>
                  <Badge variant="completed">{result.status}</Badge>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
