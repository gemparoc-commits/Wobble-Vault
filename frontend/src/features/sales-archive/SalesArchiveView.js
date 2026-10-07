import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import DataTable from '../../components/DataTable';
import PermissionGuard from '../../components/PermissionGuard';
import SectionIcon, { sectionIconBadgeStyle } from '../../components/SectionIcon';
import SearchField from '../../components/SearchField';
import { useNotification } from '../../context/NotificationContext';
import incomeService from '../../services/incomeService';
import { getApiErrorMessage, isAuthOrPermissionError } from '../../utils/apiErrors';

const LIQUIDATION_CATEGORY = 'LIQUIDATION';

const formatMoney = (value) => `PHP ${(Number(value) || 0).toFixed(2)}`;

const formatDate = (value) => {
  if (!value) return '-';
  const raw = String(value);
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    const [year, month, day] = raw.split('-');
    return `${month}/${day}/${year}`;
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '-';
  return parsed.toLocaleDateString();
};

const formatShop = (value) => {
  const shop = String(value || '').toLowerCase();
  if (shop === 'store') return 'Wobble Store';
  if (shop === 'online') return 'FB Page';
  return value || '-';
};

const formatPaymentMethod = (value) => {
  const method = String(value || '').toLowerCase();
  if (method === 'cash') return 'Cash';
  if (method === 'gcash') return 'Gcash';
  return value || '-';
};

const isLiquidationEntry = (entry) =>
  String(entry?.paymentCategory || '').toUpperCase() === LIQUIDATION_CATEGORY;

const matchesSearch = (entry, searchQuery) => {
  const term = String(searchQuery || '').trim().toLowerCase();
  if (!term) return true;
  return [entry.jobOrderNo, entry.referenceNumber, entry.customerName]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(term));
};

const SalesArchive = () => {
  const { error: notifyError, success: notifySuccess } = useNotification();
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const loadEntries = useCallback(async () => {
    try {
      setLoading(true);
      const response = await incomeService.getAllIncomeSources(0, 1000);
      setEntries(response.data.content || []);
    } catch (error) {
      console.error('Error loading sales archive:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      const errorMsg = getApiErrorMessage(error, 'Failed to load sales archive');
      notifyError(`Failed to load sales archive: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  }, [notifyError]);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  const filteredEntries = entries.filter((entry) => matchesSearch(entry, searchQuery));
  const receipts = filteredEntries.filter((entry) => !isLiquidationEntry(entry));
  const liquidations = filteredEntries.filter((entry) => isLiquidationEntry(entry));

  const receiptColumns = [
    { key: 'incomeDate', label: 'Date', render: formatDate },
    { key: 'shopType', label: 'Shop', render: formatShop },
    { key: 'paymentMethod', label: 'Payment', render: formatPaymentMethod },
    { key: 'jobOrderNo', label: 'Order No', render: (value) => value || '-' },
    { key: 'customerName', label: 'Customer', render: (value) => value || '-' },
    {
      key: 'amount',
      label: 'Amount',
      render: (value) => <strong>{formatMoney(value)}</strong>,
    },
    { key: 'referenceNumber', label: 'Reference', render: (value) => value || '-' },
  ];

  const liquidationColumns = [
    { key: 'incomeDate', label: 'Date', render: formatDate },
    { key: 'referenceNumber', label: 'Liquidation No', render: (value) => value || '-' },
    {
      key: 'amount',
      label: 'Amount',
      render: (value) => <strong>{formatMoney(value)}</strong>,
    },
    { key: 'remarks', label: 'Remarks', render: (value) => value || '-' },
  ];

  const handleDelete = async (id) => {
    try {
      await incomeService.deleteIncomeSource(id);
      notifySuccess('Record deleted successfully');
      await loadEntries();
    } catch (error) {
      console.error('Error deleting record:', error);
      const errorMsg = getApiErrorMessage(error, 'Failed to delete record');
      notifyError(`Failed to delete record: ${errorMsg}`);
    }
  };

  const archiveStats = [
    { label: 'Total records', value: entries.length, detail: 'All income sources' },
    { label: 'Receipts', value: receipts.length, detail: 'Sales & payments' },
    { label: 'Liquidations', value: liquidations.length, detail: 'Withdrawals' },
  ];

  return (
    <PermissionGuard permission="SALES_ARCHIVE">
      <DashboardLayout>
        <div className="page-container">
          <div className="page-header">
            <div className="page-title-block">
              <span style={sectionIconBadgeStyle} aria-hidden="true">
                <SectionIcon variant="archive" />
              </span>
              <span className="page-eyebrow">Closed records</span>
              <h1>Sales Archive</h1>
              <p className="page-subtitle">
                Review every receipt and liquidation, and permanently remove records that should no longer count.
              </p>
            </div>
          </div>

          <div className="content-surface">
            <div className="content-surface-header">
              <div>
                <h2>Record archive</h2>
                <p>Deleting a record removes it permanently from the Sales page totals.</p>
              </div>
              <div className="stats-strip">
                {archiveStats.map((stat) => (
                  <div key={stat.label} className="stat-pill">
                    <strong>{stat.value}</strong>
                    <span>{stat.label}</span>
                    <small>{stat.detail}</small>
                  </div>
                ))}
              </div>
            </div>

            <div className="search-and-filter-row">
              <SearchField
                className="finance-search-bar"
                wrapperProps={{ 'aria-label': 'Sales archive search' }}
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by order number, customer, or reference"
              />
            </div>

            <div className="archive-section">
              <div className="archive-section-heading">
                <h3>Receipts</h3>
                <p>Sales and payment entries from all shops.</p>
              </div>
              <DataTable
                columns={receiptColumns}
                data={receipts}
                onDelete={handleDelete}
                loading={loading}
              />
            </div>

            <div className="archive-section">
              <div className="archive-section-heading">
                <h3>Liquidations</h3>
                <p>Withdrawal entries (LIQ refs).</p>
              </div>
              <DataTable
                columns={liquidationColumns}
                data={liquidations}
                onDelete={handleDelete}
                loading={loading}
              />
            </div>
          </div>
        </div>
      </DashboardLayout>
    </PermissionGuard>
  );
};

export default SalesArchive;
