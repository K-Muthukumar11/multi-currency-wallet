import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import { ToastContainer } from './components/UI';

import AuthPage from './pages/AuthPage';
import Dashboard from './pages/Dashboard';
import Transactions from './pages/Transactions';
import DepositPage from './pages/DepositPage';
import TransferPage from './pages/TransferPage';
import AccountsPage from './pages/AccountsPage';
import ReversalPage from './pages/ReversalPage';

import './index.css';
import './components/UI.css';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<AuthPage />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          <Route path="/dashboard" element={
            <ProtectedRoute><Dashboard /></ProtectedRoute>
          } />
          <Route path="/transactions" element={
            <ProtectedRoute><Transactions /></ProtectedRoute>
          } />
          <Route path="/deposit" element={
            <ProtectedRoute><DepositPage /></ProtectedRoute>
          } />
          <Route path="/transfer" element={
            <ProtectedRoute><TransferPage /></ProtectedRoute>
          } />
          <Route path="/accounts" element={
            <ProtectedRoute><AccountsPage /></ProtectedRoute>
          } />
          <Route path="/reversal" element={
            <ProtectedRoute><ReversalPage /></ProtectedRoute>
          } />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <ToastContainer />
      </BrowserRouter>
    </AuthProvider>
  );
}
