import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Layout.css';

const NAV = [
  { to: '/dashboard',   label: 'Overview',  icon: '◈' },
  { to: '/transactions', label: 'Ledger',   icon: '≡' },
  { to: '/deposit',     label: 'Deposit',   icon: '↓' },
  { to: '/transfer',    label: 'Transfer',  icon: '⇄' },
  { to: '/reversal',    label: 'Reverse',   icon: '↺' },
  { to: '/accounts',    label: 'Accounts',  icon: '▣' },
];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.fullName
    ? user.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    : '??';

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar__top">
          <div className="sidebar__logo">
            <span className="sidebar__logo-mark">V</span>
            <span className="sidebar__logo-text">Vault</span>
          </div>

          <nav className="sidebar__nav">
            {NAV.map(({ to, label, icon }) => (
              <NavLink key={to} to={to} className={({ isActive }) =>
                `sidebar__link ${isActive ? 'sidebar__link--active' : ''}`
              }>
                <span className="sidebar__icon">{icon}</span>
                <span>{label}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="sidebar__bottom">
          <div className="sidebar__user">
            <div className="sidebar__avatar">{initials}</div>
            <div className="sidebar__user-info">
              <p className="sidebar__user-name">{user?.fullName}</p>
              <p className="sidebar__user-email">{user?.email}</p>
            </div>
          </div>
          <button className="sidebar__logout" onClick={handleLogout} title="Sign out">
            ⎋
          </button>
        </div>
      </aside>

      <main className="layout__main">
        {children}
      </main>
    </div>
  );
}
