import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import ProtectedRoute from './components/ProtectedRoute';
import PermissionGuard from './components/PermissionGuard';
import Login from './features/auth/LoginView';
import Dashboard from './features/dashboard/DashboardView';
import Inventory from './features/inventory/InventoryView';
import Orders from './features/orders/OrdersView';
import Sales from './features/sales/SalesView';
import SalesArchive from './features/sales-archive/SalesArchiveView';
import Accounts from './features/accounts/AccountsView';
import './App.css';
import './styles/BrandTheme.css';

const PING_INTERVAL_MS = 10 * 60 * 1000;

function App() {
  React.useEffect(() => {
    const pingBackend = () => {
      const apiUrl = process.env.REACT_APP_API_URL || '';
      if (!apiUrl) {
        return;
      }

      fetch(`${apiUrl.replace(/\/$/, '')}/ping`, {
        method: 'GET',
        headers: { 'Cache-Control': 'no-store' },
      }).catch(() => {});
    };

    const apiUrl = process.env.REACT_APP_API_URL || '';
    if (!apiUrl) {
      return undefined;
    }

    pingBackend();
    const intervalId = window.setInterval(pingBackend, PING_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, []);

  return (
    <Router>
      <AuthProvider>
        <NotificationProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inventory"
            element={
              <ProtectedRoute>
                <PermissionGuard permission="INVENTORY">
                  <Inventory />
                </PermissionGuard>
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <PermissionGuard permission="ORDERS">
                  <Orders />
                </PermissionGuard>
              </ProtectedRoute>
            }
          />
          <Route
            path="/sales"
            element={
              <ProtectedRoute>
                <PermissionGuard permission="SALES">
                  <Sales />
                </PermissionGuard>
              </ProtectedRoute>
            }
          />
          <Route
            path="/sales-archive"
            element={
              <ProtectedRoute>
                <PermissionGuard permission="SALES_ARCHIVE">
                  <SalesArchive />
                </PermissionGuard>
              </ProtectedRoute>
            }
          />
          <Route
            path="/accounts"
            element={
              <ProtectedRoute>
                <PermissionGuard permission="ACCOUNTS">
                  <Accounts />
                </PermissionGuard>
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        </NotificationProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
