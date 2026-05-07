import { useState, useEffect } from 'react';
import { accountApi, transactionApi } from '../services/api';
import { Card, Button, Input, Select, Badge, Amount } from '../components/UI';
import { emitToast } from '../components/UI';
import './ActionPage.css';

export default function TransferPage() {
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({
    sourceAccountNumber: '',
    destinationAccountNumber: '',
    amount: '',
    currencyCode: '',
    description: '',
  });
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
          sourceAccountNumber: accs[0].accountNumber,
          currencyCode: accs[0].currencyCode,
        }));
      }
    });
  }, []);

  const update = (k) => (e) => {
    const val = e.target.value;
    setForm(f => {
      const next = { ...f, [k]: val };
      if (k === 'sourceAccountNumber') {
        const acc = accounts.find(a => a.accountNumber === val);
        if (acc) next.currencyCode = acc.currencyCode;
      }
      return next;
    });
  };

  const sourceAccount = accounts.find(a => a.accountNumber === form.sourceAccountNumber);

  const validate = () => {
    const e = {};
    if (!form.sourceAccountNumber) e.sourceAccountNumber = 'Required';
    if (!form.destinationAccountNumber) e.destinationAccountNumber = 'Required';
    if (form.sourceAccountNumber === form.destinationAccountNumber)
      e.destinationAccountNumber = 'Cannot transfer to same account';
    if (!form.amount || isNaN(form.amount) || parseFloat(form.amount) <= 0)
      e.amount = 'Enter a valid amount';
    if (!form.currencyCode) e.currencyCode = 'Required';
    if (sourceAccount && parseFloat(form.amount) > parseFloat(sourceAccount.balance))
      e.amount = 'Insufficient funds';
    return e;
  };

  const submit = async () => {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({}); setLoading(true); setResult(null);
    try {
      const res = await transactionApi.transfer({
        sourceAccountNumber: form.sourceAccountNumber,
        destinationAccountNumber: form.destinationAccountNumber,
        amount: parseFloat(form.amount),
        currencyCode: form.currencyCode,
        description: form.description,
      });
      setResult(res.data);
      emitToast(`Transfer of ${form.amount} ${form.currencyCode} successful`, 'success');
      setForm(f => ({ ...f, amount: '', description: '', destinationAccountNumber: '' }));
      // Refresh balances
      accountApi.list().then(r => setAccounts(r.data || []));
    } catch (err) {
      const msg = err.response?.data?.message || 'Transfer failed';
      emitToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="action-page">
      <div className="action-page__header fade-up">
        <h1 className="action-page__title">Transfer Funds</h1>
        <p className="text-muted" style={{fontSize:13, marginTop:4}}>
          Atomic debit-credit transfer between same-currency accounts
        </p>
      </div>

      <div className="action-page__layout">
        <Card className="action-page__form fade-up-1">
          <div className="action-page__form-inner">

            <Select
              label="From Account"
              value={form.sourceAccountNumber}
              onChange={update('sourceAccountNumber')}
              error={errors.sourceAccountNumber}
            >
              {accounts.map(a => (
                <option key={a.id} value={a.accountNumber}>
                  {a.accountNumber} — {a.currencyCode} ({parseFloat(a.balance).toFixed(3)})
                </option>
              ))}
            </Select>

            <Input
              label="To Account Number"
              placeholder="e.g. ACC-000123"
              value={form.destinationAccountNumber}
              onChange={update('destinationAccountNumber')}
              error={errors.destinationAccountNumber}
            />

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
              placeholder="e.g. Rent payment"
              value={form.description}
              onChange={update('description')}
            />

            {sourceAccount && form.amount && !errors.amount && (
              <div className="action-page__preview-inline">
                <span className="text-faint" style={{fontSize:12}}>Balance after:</span>
                <span className="mono text-muted" style={{fontSize:12}}>
                  {(parseFloat(sourceAccount.balance) - parseFloat(form.amount || 0)).toFixed(3)} {form.currencyCode}
                </span>
              </div>
            )}

            <Button variant="primary" size="lg" loading={loading} onClick={submit}>
              ⇄ Execute Transfer
            </Button>
          </div>
        </Card>

        <div className="action-page__side fade-up-2">
          {sourceAccount && (
            <Card className="action-page__preview">
              <p className="action-page__preview-label">Source Account</p>
              <p className="mono" style={{fontSize:13, color:'var(--ink-2)', marginBottom:8}}>
                {sourceAccount.accountNumber}
              </p>
              <Amount value={sourceAccount.balance} currency={sourceAccount.currencyCode} />
              <p className="action-page__preview-sublabel text-faint">Available Balance</p>
            </Card>
          )}

          <Card className="action-page__info">
            <p className="action-page__info-title">Transfer Rules</p>
            <ul className="action-page__info-list">
              <li>Both accounts must share the same currency</li>
              <li>Transfer is atomic — either both sides complete or neither does</li>
              <li>Transaction records are immutable once created</li>
              <li>Corrections require an explicit reversal transaction</li>
            </ul>
          </Card>

          {result && (
            <Card className="action-page__result">
              <div className="action-page__result-icon">✓</div>
              <p className="action-page__result-label">Transfer Complete</p>
              <div className="action-page__result-rows">
                <div className="result-row">
                  <span className="text-faint">Debit TX</span>
                  <span className="mono" style={{fontSize:11}}>{result.debitTransaction?.id?.slice(0,16)}…</span>
                </div>
                <div className="result-row">
                  <span className="text-faint">Credit TX</span>
                  <span className="mono" style={{fontSize:11}}>{result.creditTransaction?.id?.slice(0,16)}…</span>
                </div>
                <div className="result-row">
                  <span className="text-faint">Amount</span>
                  <Amount value={result.debitTransaction?.amount} currency={result.debitTransaction?.currencyCode} sign="-" />
                </div>
                <div className="result-row">
                  <span className="text-faint">To</span>
                  <span className="mono text-muted" style={{fontSize:11}}>
                    {result.destinationAccountNumber}
                  </span>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
