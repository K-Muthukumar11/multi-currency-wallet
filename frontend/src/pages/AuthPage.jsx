import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, Input } from '../components/UI';
import './Auth.css';

export default function AuthPage() {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [form, setForm] = useState({ email: '', password: '', fullName: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState('');
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const sessionExpired = searchParams.get('reason') === 'session_expired';

  const update = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }));

  const validate = () => {
    const e = {};
    if (!form.email) e.email = 'Required';
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Invalid email';
    if (!form.password) e.password = 'Required';
    else if (form.password.length < 8) e.password = 'At least 8 characters';
    if (mode === 'register' && !form.fullName) e.fullName = 'Required';
    return e;
  };

  const submit = async () => {
    const e = validate();
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({}); setServerError(''); setLoading(true);
    try {
      if (mode === 'login') {
        await login({ email: form.email, password: form.password });
      } else {
        await register({ email: form.email, password: form.password, fullName: form.fullName });
      }
      navigate('/dashboard');
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Something went wrong';
      setServerError(msg);
    } finally {
      setLoading(false);
    }
  };

  const toggleMode = () => {
    setMode(m => m === 'login' ? 'register' : 'login');
    setErrors({}); setServerError('');
  };

  return (
    <div className="auth">
      <div className="auth__bg">
        <div className="auth__grid" />
        <div className="auth__orb auth__orb-1" />
        <div className="auth__orb auth__orb-2" />
      </div>

      <div className="auth__card fade-up">
        <div className="auth__logo">
          <span className="auth__logo-mark">V</span>
          <span className="auth__logo-text">Vault</span>
        </div>

        <div className="auth__header">
          <h1 className="auth__title">
            {mode === 'login' ? 'Welcome back' : 'Create account'}
          </h1>
          <p className="auth__subtitle">
            {mode === 'login'
              ? 'Sign in to your Multi-Currency Digital Wallet'
              : 'Start managing your digital assets'}
          </p>
        </div>

        <div className="auth__form">
          {sessionExpired && (
            <div className="auth__session-expired">⏱ Your session expired. Please sign in again.</div>
          )}
          {mode === 'register' && (
            <Input
              label="Full Name"
              placeholder="Jane Smith"
              value={form.fullName}
              onChange={update('fullName')}
              error={errors.fullName}
            />
          )}
          <Input
            label="Email"
            type="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={update('email')}
            error={errors.email}
          />
          <Input
            label="Password"
            type="password"
            placeholder="••••••••"
            value={form.password}
            onChange={update('password')}
            error={errors.password}
            onKeyDown={(e) => e.key === 'Enter' && submit()}
          />

          {serverError && (
            <div className="auth__server-error">{serverError}</div>
          )}

          <Button variant="primary" size="lg" loading={loading} onClick={submit}>
            {mode === 'login' ? 'Sign in' : 'Create account'}
          </Button>
        </div>

        <div className="auth__toggle">
          {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}
          <button className="auth__toggle-btn" onClick={toggleMode}>
            {mode === 'login' ? 'Register' : 'Sign in'}
          </button>
        </div>
      </div>
    </div>
  );
}
