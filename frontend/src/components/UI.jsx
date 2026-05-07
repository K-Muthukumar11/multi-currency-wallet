import { useState } from 'react';

/* ── Button ── */
export function Button({ children, variant = 'primary', size = 'md', loading, disabled, className = '', ...props }) {
  const base = `btn btn--${variant} btn--${size} ${className}`;
  return (
    <button className={base} disabled={disabled || loading} {...props}>
      {loading ? <span className="btn__spinner" /> : children}
    </button>
  );
}

/* ── Input ── */
export function Input({ label, error, prefix, suffix, className = '', ...props }) {
  return (
    <div className={`field ${className}`}>
      {label && <label className="field__label">{label}</label>}
      <div className={`field__wrap ${error ? 'field__wrap--error' : ''}`}>
        {prefix && <span className="field__prefix">{prefix}</span>}
        <input className="field__input" {...props} />
        {suffix && <span className="field__suffix">{suffix}</span>}
      </div>
      {error && <p className="field__error">{error}</p>}
    </div>
  );
}

/* ── Select ── */
export function Select({ label, error, children, className = '', ...props }) {
  return (
    <div className={`field ${className}`}>
      {label && <label className="field__label">{label}</label>}
      <div className={`field__wrap ${error ? 'field__wrap--error' : ''}`}>
        <select className="field__input field__select" {...props}>{children}</select>
        <span className="field__caret">▾</span>
      </div>
      {error && <p className="field__error">{error}</p>}
    </div>
  );
}

/* ── Card ── */
export function Card({ children, className = '', ...props }) {
  return <div className={`card ${className}`} {...props}>{children}</div>;
}

/* ── Badge ── */
export function Badge({ children, variant = 'default' }) {
  return <span className={`badge badge--${variant}`}>{children}</span>;
}

/* ── Modal ── */
export function Modal({ open, onClose, title, children }) {
  if (!open) return null;
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal__header">
          <h2 className="modal__title">{title}</h2>
          <button className="modal__close" onClick={onClose}>✕</button>
        </div>
        <div className="modal__body">{children}</div>
      </div>
    </div>
  );
}

/* ── Toast ── */
let toastListeners = [];
export function emitToast(msg, type = 'info') {
  toastListeners.forEach(fn => fn({ msg, type, id: Date.now() }));
}

export function ToastContainer() {
  const [toasts, setToasts] = useState([]);
  toastListeners = [(t) => {
    setToasts(prev => [...prev, t]);
    setTimeout(() => setToasts(prev => prev.filter(x => x.id !== t.id)), 3500);
  }];
  return (
    <div className="toast-container">
      {toasts.map(t => (
        <div key={t.id} className={`toast toast--${t.type}`}>{t.msg}</div>
      ))}
    </div>
  );
}

/* ── Spinner ── */
export function Spinner({ size = 24 }) {
  return <div className="spinner" style={{ width: size, height: size }} />;
}

/* ── Amount display ── */
export function Amount({ value, currency, sign }) {
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  }).format(Math.abs(value));

  const isPositive = sign === '+' || (!sign && value >= 0);
  const cls = sign ? (isPositive ? 'text-green' : 'text-red') : '';

  return (
    <span className={`mono ${cls}`}>
      {sign === '+' ? '+' : sign === '-' ? '−' : ''}
      {formatted} <span style={{ opacity: 0.6, fontSize: '0.85em' }}>{currency}</span>
    </span>
  );
}
