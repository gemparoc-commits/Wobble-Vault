import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../../layouts/DashboardLayout';
import PermissionGuard from '../../components/PermissionGuard';
import { useAuth } from '../../context/AuthContext';
import dashboardService from '../../services/dashboardService';
import '../../styles/Dashboard.css';

const dashboardIconProps = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: '1.9',
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
};

const formatCurrency = (value) =>
  new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency: 'PHP',
    maximumFractionDigits: 2,
  }).format(Number(value) || 0);

const formatCount = (value) =>
  new Intl.NumberFormat('en-US', {
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);

const formatCompact = (value) =>
  new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(Number(value) || 0);

const StatIcon = ({ name }) => {
  switch (name) {
    case 'orders':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true" {...dashboardIconProps}>
          <path d="M7 4.75h8l3.25 3.25V18A1.25 1.25 0 0 1 17 19.25H7A1.25 1.25 0 0 1 5.75 18V6A1.25 1.25 0 0 1 7 4.75Z" />
          <path d="M15 4.75V8h3.25" />
          <path d="M8.5 11h7" />
          <path d="M8.5 14.5h7" />
        </svg>
      );
    case 'inventory':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true" {...dashboardIconProps}>
          <path d="M20.5 7.27783L12 12.0001M12 12.0001L3.49997 7.27783M12 12.0001L12 21.5001M14 20.889L12.777 21.5684C12.4934 21.726 12.3516 21.8047 12.2015 21.8356C12.0685 21.863 11.9315 21.863 11.7986 21.8356C11.6484 21.8047 11.5066 21.726 11.223 21.5684L3.82297 17.4573C3.52346 17.2909 3.37368 17.2077 3.26463 17.0893C3.16816 16.9847 3.09515 16.8606 3.05048 16.7254C3 16.5726 3 16.4013 3 16.0586V7.94153C3 7.59889 3 7.42757 3.05048 7.27477C3.09515 7.13959 3.16816 7.01551 3.26463 6.91082C3.37368 6.79248 3.52345 6.70928 3.82297 6.54288L11.223 2.43177C11.5066 2.27421 11.6484 2.19543 11.7986 2.16454C11.9315 2.13721 12.0685 2.13721 12.2015 2.16454C12.3516 2.19543 12.4934 2.27421 12.777 2.43177L20.177 6.54288C20.4766 6.70928 20.6263 6.79248 20.7354 6.91082C20.8318 7.01551 20.9049 7.13959 20.9495 7.27477C21 7.42757 21 7.59889 21 7.94153L21 12.5001M7.5 4.50008L16.5 9.50008M19 21.0001V15.0001M16 18.0001H22" />
        </svg>
      );
    case 'sales':
      return (
        <svg viewBox="0 0 256 256" aria-hidden="true" {...dashboardIconProps}>
          <path
            fill="currentColor"
            d="M28,128a8,8,0,0,1,0-16H56a8,8,0,0,0,0-16H40a24,24,0,0,1,0-48,8,8,0,0,1,16,0h8a8,8,0,0,1,0,16H40a8,8,0,0,0,0,16H56a24,24,0,0,1,0,48,8,8,0,0,1-16,0ZM232,56V192a16,16,0,0,1-16,16H40a16,16,0,0,1-16-16V152a8,8,0,0,1,16,0v40H160V160H80a8,8,0,0,1,0-16h80v-32H104a8,8,0,0,1,0-16H216V64H96a8,8,0,0,1,0-16H224A8,8,0,0,1,232,56Zm-56,88h40V112H176Zm40,48V160H176v32Z"
          />
        </svg>
      );
    case 'accounts':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true" {...dashboardIconProps}>
          <path d="M13.5 8h-3" />
          <path d="m15 2-1 2h3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h3" />
          <path d="M16.899 22A5 5 0 0 0 7.1 22" />
          <path d="m9 2 3 6" />
          <circle cx="12" cy="15" r="3" />
        </svg>
      );
    case 'report':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true" {...dashboardIconProps}>
          <path d="M4 19V5" />
          <path d="M4 19h16" />
          <path d="M7 15.5l3.5-3.5 2.5 2.5L17.5 9" />
          <path d="M17.5 9H14" />
          <path d="M17.5 9v3.5" />
        </svg>
      );
    default:
      return null;
  }
};

const DEFAULT_STATS = {
  totalInventoryItems: 0,
  lowStockItems: 0,
  totalOrders: 0,
  activeOrders: 0,
  archivedOrders: 0,
  cancelledOrders: 0,
  monthlySalesIncome: 0,
  monthlyLiquidation: 0,
  monthlyNetIncome: 0,
};

const Dashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState(DEFAULT_STATS);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const loadStats = async () => {
      setLoading(true);
      try {
        const response = await dashboardService.getStats();
        if (!cancelled) {
          setStats({
            ...DEFAULT_STATS,
            ...(response.data || {}),
          });
        }
      } catch (error) {
        console.error('Error loading dashboard stats:', error);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadStats();

    return () => {
      cancelled = true;
    };
  }, []);

  const moduleCards = [
    {
      label: 'Inventory',
      icon: 'inventory',
      metric: stats.totalInventoryItems,
      metricLabel: 'Items',
      description: 'Brands, sizes, quantities, and prices for every shoe in stock.',
      path: '/inventory',
      permission: 'INVENTORY',
      tone: 'green',
    },
    {
      label: 'Orders',
      icon: 'orders',
      metric: stats.activeOrders,
      metricLabel: 'Active',
      description: 'Customer orders, stock deductions, and order payments.',
      path: '/orders',
      permission: 'ORDERS',
      tone: 'teal',
    },
    {
      label: 'Sales',
      icon: 'sales',
      metric: stats.monthlyNetIncome,
      metricLabel: 'Net this month',
      metricFormat: 'currency',
      description: 'Order payments, liquidations, and performance reporting.',
      path: '/sales',
      permission: 'SALES',
      tone: 'blue',
    },
    {
      label: 'Sales Archive',
      icon: 'report',
      metric: stats.archivedOrders,
      metricLabel: 'Archived',
      description: 'Receipts, liquidations, and closed orders in one place.',
      path: '/sales-archive',
      permission: 'SALES_ARCHIVE',
      tone: 'slate',
    },
    {
      label: 'Accounts',
      icon: 'accounts',
      metric: null,
      metricLabel: 'Team management',
      description: 'User registration, roles, and per-page permissions.',
      path: '/accounts',
      permission: 'ACCOUNTS',
      tone: 'violet',
    },
  ];

  const performanceItems = [
    {
      label: 'Active orders',
      value: stats.activeOrders,
      hint: 'Open orders currently being fulfilled',
    },
    {
      label: 'Archived orders',
      value: stats.archivedOrders,
      hint: 'Completed orders moved to the archive',
    },
    {
      label: 'Cancelled orders',
      value: stats.cancelledOrders,
      hint: 'Cancelled orders with stock restored',
    },
    {
      label: 'Low stock items',
      value: stats.lowStockItems,
      hint: 'Pairs below the default stock threshold of 10',
    },
  ];

  return (
    <DashboardLayout>
      <div className="dashboard" aria-busy={loading}>
        <section className="dashboard-hero">
          <div className="dashboard-hero-copy">
            <span className="page-eyebrow">Operations command center</span>
            <h1>Dashboard</h1>
            <p className="welcome">
              Welcome back, {user?.username || 'there'}. This view highlights the business areas that matter most:
              inventory stock, orders, sales, and account access.
            </p>
          </div>

          <div className="dashboard-hero-panel">
            <div className="hero-metrics">
              <div className="hero-metric">
                <span>Active orders</span>
                <strong>{loading ? '-' : formatCount(stats.activeOrders)}</strong>
                <p>Orders currently being fulfilled.</p>
              </div>
              <div className="hero-metric">
                <span>Low stock</span>
                <strong>{loading ? '-' : formatCount(stats.lowStockItems)}</strong>
                <p>Pairs that should be reviewed soon.</p>
              </div>
              <div className="hero-metric">
                <span>Net this month</span>
                <strong>{loading ? '-' : formatCurrency(stats.monthlyNetIncome)}</strong>
                <p>Sales income minus liquidations.</p>
              </div>
            </div>

            <button
              type="button"
              className="hero-action"
              onClick={() => navigate('/sales')}
            >
              Open sales report
            </button>
          </div>
        </section>

        <section className="dashboard-card-grid">
          <PermissionGuard permission="INVENTORY">
            <button type="button" className="dashboard-summary-card" onClick={() => navigate('/inventory')}>
              <span className="summary-icon summary-icon--green" aria-hidden="true">
                <StatIcon name="inventory" />
              </span>
              <span className="summary-copy">
                <span className="summary-label">Inventory Items</span>
                <strong className="summary-value">{loading ? '-' : formatCompact(stats.totalInventoryItems)}</strong>
                <span className="summary-note">Catalogued shoes and stock</span>
              </span>
            </button>
          </PermissionGuard>

          <PermissionGuard permission="ORDERS">
            <button type="button" className="dashboard-summary-card" onClick={() => navigate('/orders')}>
              <span className="summary-icon summary-icon--teal" aria-hidden="true">
                <StatIcon name="orders" />
              </span>
              <span className="summary-copy">
                <span className="summary-label">Active Orders</span>
                <strong className="summary-value">{loading ? '-' : formatCompact(stats.activeOrders)}</strong>
                <span className="summary-note">Orders in the fulfillment queue</span>
              </span>
            </button>
          </PermissionGuard>

          <PermissionGuard permission="SALES">
            <button type="button" className="dashboard-summary-card" onClick={() => navigate('/sales')}>
              <span className="summary-icon summary-icon--blue" aria-hidden="true">
                <StatIcon name="sales" />
              </span>
              <span className="summary-copy">
                <span className="summary-label">Sales This Month</span>
                <strong className="summary-value">{loading ? '-' : formatCompact(stats.monthlySalesIncome)}</strong>
                <span className="summary-note">Order payments recorded</span>
              </span>
            </button>
          </PermissionGuard>

          <PermissionGuard permission="ACCOUNTS">
            <button type="button" className="dashboard-summary-card" onClick={() => navigate('/accounts')}>
              <span className="summary-icon summary-icon--violet" aria-hidden="true">
                <StatIcon name="accounts" />
              </span>
              <span className="summary-copy">
                <span className="summary-label">Accounts</span>
                <strong className="summary-value">Open</strong>
                <span className="summary-note">Users, roles, and permissions</span>
              </span>
            </button>
          </PermissionGuard>
        </section>

        <section className="dashboard-content-grid">
          <article className="dashboard-panel dashboard-performance-panel">
            <div className="panel-header">
              <div>
                <span className="panel-kicker">Performance report</span>
                <h2>Business performance this month</h2>
                <p>
                  A compact report that mirrors the sales section and helps leadership see whether orders,
                  revenue, and stock are moving in the right direction.
                </p>
              </div>
              <span className="panel-chip">Month to date</span>
            </div>

            <div className="performance-metrics">
              <div className="performance-total">
                <span>Sales income</span>
                <strong>{loading ? '-' : formatCurrency(stats.monthlySalesIncome)}</strong>
              </div>
              <div className="performance-total">
                <span>Liquidations</span>
                <strong>{loading ? '-' : formatCurrency(stats.monthlyLiquidation)}</strong>
              </div>
              <div className="performance-total performance-total--strong">
                <span>Net result</span>
                <strong>{loading ? '-' : formatCurrency(stats.monthlyNetIncome)}</strong>
              </div>
            </div>

            <div className="performance-list">
              {performanceItems.map((item) => (
                <div key={item.label} className="performance-list-item">
                  <div>
                    <span>{item.label}</span>
                    <p>{item.hint}</p>
                  </div>
                  <strong>{loading ? '-' : formatCount(item.value)}</strong>
                </div>
              ))}
            </div>

            <div className="performance-footer">
              <button type="button" className="secondary-action" onClick={() => navigate('/sales')}>
                Open full sales workspace
              </button>
              <span className="performance-footnote">
                Detailed charts and liquidation history live in Sales.
              </span>
            </div>
          </article>

          <aside className="dashboard-panel dashboard-watchlist-panel">
            <div className="panel-header">
              <div>
                <span className="panel-kicker">Operational watchlist</span>
                <h2>Priority areas</h2>
                <p>These are the pieces of the business the team should care about every day.</p>
              </div>
            </div>

            <div className="watchlist-stack">
              <div className="watchlist-item">
                <span className="watchlist-icon watchlist-icon--green" aria-hidden="true">
                  <StatIcon name="inventory" />
                </span>
                <div>
                  <strong>Inventory control</strong>
                  <p>Keep stock balanced and watch low inventory before it becomes a problem.</p>
                </div>
              </div>
              <div className="watchlist-item">
                <span className="watchlist-icon watchlist-icon--teal" aria-hidden="true">
                  <StatIcon name="orders" />
                </span>
                <div>
                  <strong>Orders</strong>
                  <p>Track active orders, capture payments, and archive or cancel when done.</p>
                </div>
              </div>
              <div className="watchlist-item">
                <span className="watchlist-icon watchlist-icon--blue" aria-hidden="true">
                  <StatIcon name="sales" />
                </span>
                <div>
                  <strong>Sales and reporting</strong>
                  <p>Monitor income, liquidations, and the performance report in one place.</p>
                </div>
              </div>
              <div className="watchlist-item">
                <span className="watchlist-icon watchlist-icon--violet" aria-hidden="true">
                  <StatIcon name="accounts" />
                </span>
                <div>
                  <strong>Accounts</strong>
                  <p>Keep team access, roles, and page-level permissions organized.</p>
                </div>
              </div>
            </div>
          </aside>
        </section>

        <section className="dashboard-panel dashboard-modules-panel">
          <div className="panel-header">
            <div>
              <span className="panel-kicker">Core modules</span>
              <h2>What matters in this project</h2>
              <p>
                These are the main working areas that define the system and should stay easy to reach from the dashboard.
              </p>
            </div>
          </div>

          <div className="module-grid">
            {moduleCards.map((card) => (
              <PermissionGuard key={card.label} permission={card.permission}>
                <button type="button" className={`module-card module-card--${card.tone}`} onClick={() => navigate(card.path)}>
                  <span className="module-icon" aria-hidden="true">
                    <StatIcon name={card.icon} />
                  </span>
                  <span className="module-copy">
                    <span className="module-label">{card.label}</span>
                    <span className="module-description">{card.description}</span>
                  </span>
                  <span className="module-meta">
                    {card.metric !== null ? (
                      <>
                        <strong>
                          {loading
                            ? '-'
                            : card.metricFormat === 'currency'
                              ? formatCurrency(card.metric)
                              : formatCount(card.metric)}
                        </strong>
                        <span>{card.metricLabel}</span>
                      </>
                    ) : (
                      <>
                        <strong>Open</strong>
                        <span>{card.metricLabel}</span>
                      </>
                    )}
                  </span>
                  <span className="module-arrow" aria-hidden="true">
                    ->
                  </span>
                </button>
              </PermissionGuard>
            ))}
          </div>
        </section>
      </div>
    </DashboardLayout>
  );
};

export default Dashboard;
