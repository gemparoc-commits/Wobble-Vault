import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import PermissionGuard from '../../components/PermissionGuard';
import Modal from '../../components/Modal';
import DataTable from '../../components/DataTable';
import SectionIcon, { sectionIconBadgeStyle } from '../../components/SectionIcon';
import SearchField from '../../components/SearchField';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import incomeService from '../../services/incomeService';
import { hasPermission } from '../../utils/permissions';
import { getApiErrorMessage, isAuthOrPermissionError } from '../../utils/apiErrors';
import { generateLiquidationReferenceNumber } from './liquidationUtils';

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

const getAmount = (entry) => Number.parseFloat(entry?.amount) || 0;

const isLiquidationEntry = (entry) =>
  String(entry?.paymentCategory || '').toUpperCase() === LIQUIDATION_CATEGORY;

const isSaleEntry = (entry) => !isLiquidationEntry(entry);

const matchesSearch = (entry, searchQuery) => {
  const term = String(searchQuery || '').trim().toLowerCase();
  if (!term) return true;
  return [entry.jobOrderNo, entry.customerName, entry.referenceNumber]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(term));
};

const getTodayLocalDateKey = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const escapeCsvCell = (value) => {
  const text = String(value ?? '');
  return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
};

const sumEntries = (list, predicate) =>
  list.filter(predicate).reduce((total, entry) => total + getAmount(entry), 0);

const Sales = () => {
  const { user } = useAuth();
  const { success: notifySuccess, error: notifyError, info: notifyInfo } = useNotification();
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [receiptTarget, setReceiptTarget] = useState(null);
  const [liquidationModalOpen, setLiquidationModalOpen] = useState(false);
  const [liquidationDate, setLiquidationDate] = useState(getTodayLocalDateKey);
  const [liquidationAmount, setLiquidationAmount] = useState('');
  const [liquidationReason, setLiquidationReason] = useState('');
  const [liquidationSubmitting, setLiquidationSubmitting] = useState(false);
  const [rangeStart, setRangeStart] = useState('');
  const [rangeEnd, setRangeEnd] = useState('');
  const [reportEntries, setReportEntries] = useState(null);
  const [reportLoading, setReportLoading] = useState(false);

  const canArchive = hasPermission(user?.permissions, 'SALES_ARCHIVE');

  const loadEntries = useCallback(async () => {
    try {
      setLoading(true);
      const response = await incomeService.getAllIncomeSources(0, 1000);
      setEntries(response.data.content || []);
    } catch (error) {
      console.error('Error loading sales entries:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      const errorMsg = getApiErrorMessage(error, 'Failed to load sales entries');
      notifyError(`Failed to load sales entries: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  }, [notifyError]);

  useEffect(() => {
    loadEntries();
  }, [loadEntries]);

  const filteredEntries = entries.filter((entry) => matchesSearch(entry, searchQuery));
  const receipts = filteredEntries.filter(isSaleEntry);
  const liquidations = filteredEntries.filter(isLiquidationEntry);

  const salesTotal = sumEntries(entries, isSaleEntry);
  const liquidationTotal = sumEntries(entries, isLiquidationEntry);
  const netTotal = salesTotal - liquidationTotal;

  const isShopType = (entry, shop) =>
    isSaleEntry(entry) && String(entry.shopType || '').toLowerCase() === shop;
  const isPayment = (entry, method) =>
    isSaleEntry(entry) && String(entry.paymentMethod || '').toLowerCase() === method;

  const storeTotal = sumEntries(entries, (entry) => isShopType(entry, 'store'));
  const onlineTotal = sumEntries(entries, (entry) => isShopType(entry, 'online'));
  const cashTotal = sumEntries(entries, (entry) => isPayment(entry, 'cash'));
  const gcashTotal = sumEntries(entries, (entry) => isPayment(entry, 'gcash'));

  const reportSourceEntries = reportEntries || entries;
  const reportSalesTotal = sumEntries(reportSourceEntries, isSaleEntry);
  const reportLiquidationTotal = sumEntries(reportSourceEntries, isLiquidationEntry);
  const reportNetTotal = reportSalesTotal - reportLiquidationTotal;

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
    { key: 'referenceNumber', label: 'Reference', render: (value) => value || '-' },
    {
      key: 'amount',
      label: 'Amount',
      render: (value) => <strong>{formatMoney(value)}</strong>,
    },
    { key: 'remarks', label: 'Remarks', render: (value) => value || '-' },
  ];

  const openReceipt = (entry) => {
    setReceiptTarget(entry);
  };

  const closeReceipt = () => {
    setReceiptTarget(null);
  };

  const openLiquidationModal = () => {
    setLiquidationAmount('');
    setLiquidationReason('');
    setLiquidationDate(getTodayLocalDateKey());
    setLiquidationModalOpen(true);
  };

  const closeLiquidationModal = () => {
    setLiquidationModalOpen(false);
  };

  const handleRecordLiquidation = async () => {
    const amount = Number(liquidationAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      notifyInfo('Please enter a valid liquidation amount.');
      return;
    }

    if (!liquidationReason.trim()) {
      notifyInfo('Please enter a reason for the liquidation.');
      return;
    }

    try {
      setLiquidationSubmitting(true);
      const referenceNumber = generateLiquidationReferenceNumber(entries, liquidationDate);

      await incomeService.createIncomeSource({
        shopType: 'store',
        paymentMethod: 'cash',
        incomeDate: liquidationDate,
        customerName: null,
        jobOrderNo: null,
        amount,
        referenceNumber,
        paymentCategory: LIQUIDATION_CATEGORY,
        remarks: liquidationReason.trim(),
      });

      setLiquidationAmount('');
      setLiquidationReason('');
      setLiquidationDate(getTodayLocalDateKey());
      setLiquidationModalOpen(false);
      await loadEntries();
      notifySuccess('Liquidation recorded successfully.');
    } catch (error) {
      console.error('Error recording liquidation:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      const errorMsg = getApiErrorMessage(error, 'Unknown error');
      notifyError(`Failed to record liquidation: ${errorMsg}`);
    } finally {
      setLiquidationSubmitting(false);
    }
  };

  const handleGenerateReport = async () => {
    if (!rangeStart && !rangeEnd) {
      setReportEntries(null);
      return;
    }

    if (!rangeStart || !rangeEnd) {
      notifyInfo('Please select both a start and an end date.');
      return;
    }

    if (rangeStart > rangeEnd) {
      notifyInfo('Start date must be on or before the end date.');
      return;
    }

    try {
      setReportLoading(true);
      const response = await incomeService.getIncomeSourcesByDateRange(rangeStart, rangeEnd);
      setReportEntries(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Error generating performance report:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      const errorMsg = getApiErrorMessage(error, 'Failed to generate report');
      notifyError(`Failed to generate report: ${errorMsg}`);
    } finally {
      setReportLoading(false);
    }
  };

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

  const handleExportCsv = () => {
    const header = ['Date', 'Shop', 'Payment', 'Order No', 'Customer', 'Amount', 'Reference', 'Category'];
    const rows = filteredEntries.map((entry) => [
      entry.incomeDate || '',
      formatShop(entry.shopType),
      formatPaymentMethod(entry.paymentMethod),
      entry.jobOrderNo || '',
      entry.customerName || '',
      getAmount(entry).toFixed(2),
      entry.referenceNumber || '',
      entry.paymentCategory || '',
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map(escapeCsvCell).join(','))
      .join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const downloadUrl = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = downloadUrl;
    anchor.download = `sales-export-${getTodayLocalDateKey()}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => {
      window.URL.revokeObjectURL(downloadUrl);
    }, 1000);
  };

  const renderReceiptContent = () => {
    if (!receiptTarget) return null;

    const entry = receiptTarget;
    const liquidation = isLiquidationEntry(entry);
    const customerLabel = liquidation ? 'N/A' : entry.customerName || 'Walk-in Customer';

    return (
      <div className="receipt-modal">
        <div className="receipt-paper">
          <header className="receipt-brand">
            <div>
              <span className="receipt-eyebrow">Wobble Vault</span>
              <h3>{liquidation ? 'Liquidation record' : 'Payment receipt'}</h3>
              <p className="receipt-muted">A clear record of your transaction</p>
            </div>
            <div className="receipt-brand-mark" aria-hidden="true">WV</div>
          </header>

          <section className="receipt-amount-panel">
            <div>
              <span className="receipt-label">{liquidation ? 'Amount liquidated' : 'Amount received'}</span>
              <strong>{formatMoney(entry.amount)}</strong>
            </div>
            <span className="receipt-status">{liquidation ? 'Liquidation' : 'Recorded payment'}</span>
          </section>

          <section className="receipt-meta-grid" aria-label="Receipt details">
            <div><span className="receipt-label">Reference no.</span><strong>{entry.referenceNumber || 'N/A'}</strong></div>
            <div><span className="receipt-label">Date issued</span><strong>{formatDate(entry.incomeDate)}</strong></div>
            <div><span className="receipt-label">Order no</span><strong>{entry.jobOrderNo || 'N/A'}</strong></div>
            <div><span className="receipt-label">Payment method</span><strong>{formatPaymentMethod(entry.paymentMethod)}</strong></div>
          </section>

          <section className="receipt-customer-row">
            <div><span className="receipt-label">Customer</span><strong>{customerLabel}</strong></div>
            <div><span className="receipt-label">Shop</span><strong>{formatShop(entry.shopType)}</strong></div>
          </section>

          {entry.remarks && (
            <section className="receipt-items">
              <div className="receipt-section-heading"><span>Remarks</span><span /></div>
              <p className="receipt-empty-cell">{entry.remarks}</p>
            </section>
          )}

          <section className="receipt-totals">
            <div><span>Amount</span><strong>{formatMoney(entry.amount)}</strong></div>
            <div><span>Category</span><strong>{liquidation ? 'Liquidation' : 'Payment'}</strong></div>
          </section>

          <footer className="receipt-footer-notes">
            <strong>Keep this receipt for your records.</strong>
            <p>Thank you for shopping with Wobble Vault.</p>
          </footer>
        </div>

        <div className="receipt-actions">
          <button type="button" className="income-details-btn" onClick={() => window.print()}>
            Print
          </button>
        </div>
      </div>
    );
  };

  return (
    <PermissionGuard permission="SALES">
      <DashboardLayout>
        <div className="page-container">
          <div className="page-header">
            <div className="page-title-block">
              <span style={sectionIconBadgeStyle} aria-hidden="true">
                <SectionIcon variant="finance" />
              </span>
              <span className="page-eyebrow">Revenue</span>
              <h1>Sales</h1>
              <p className="page-subtitle">
                Track receipts from the store and FB Page, review liquidations, and report performance for any date range.
              </p>
            </div>
            <div className="page-actions">
              <button className="btn-primary" onClick={openLiquidationModal} type="button">
                Record Liquidation
              </button>
              <button className="btn-cancel" onClick={handleExportCsv} type="button">
                Export CSV
              </button>
            </div>
          </div>

          <div className="content-surface">
            <div className="content-surface-header">
              <div>
                <h2>Transaction records</h2>
                <p>Receipts from order payments and cash liquidations recorded across all shops.</p>
              </div>
              <div className="stats-strip">
                <div className="stat-pill">
                  <strong>{formatMoney(salesTotal)}</strong>
                  <span>Sales total</span>
                  <small>All receipts</small>
                </div>
                <div className="stat-pill">
                  <strong>{formatMoney(liquidationTotal)}</strong>
                  <span>Liquidation total</span>
                  <small>Withdrawals</small>
                </div>
                <div className="stat-pill">
                  <strong>{formatMoney(netTotal)}</strong>
                  <span>Net</span>
                  <small>Sales minus liquidations</small>
                </div>
              </div>
            </div>

            <div className="finance-summary-grid finance-summary-grid-two">
              <div className="finance-summary-card">
                <span className="income-details-label">Source of income</span>
                <div className="finance-breakdown-row">
                  <span>Store</span>
                  <strong>{formatMoney(storeTotal)}</strong>
                </div>
                <div className="finance-breakdown-row">
                  <span>Online (FB Page)</span>
                  <strong>{formatMoney(onlineTotal)}</strong>
                </div>
              </div>
              <div className="finance-summary-card">
                <span className="income-details-label">Payment methods</span>
                <div className="finance-breakdown-row">
                  <span>Cash</span>
                  <strong>{formatMoney(cashTotal)}</strong>
                </div>
                <div className="finance-breakdown-row">
                  <span>Gcash</span>
                  <strong>{formatMoney(gcashTotal)}</strong>
                </div>
              </div>
            </div>

            <div className="search-and-filter-row">
              <SearchField
                className="finance-search-bar"
                wrapperProps={{ 'aria-label': 'Sales search' }}
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by order number, customer, or reference"
              />
            </div>

            <div>
              <h3 className="transaction-histories-title" style={{ margin: '4px 0 10px' }}>
                Receipts
              </h3>
              <div className="finance-entry-subtext" style={{ marginBottom: '10px' }}>
                Payment entries from order checkouts.
              </div>
              <DataTable
                columns={receiptColumns}
                data={receipts}
                onView={openReceipt}
                onDelete={canArchive ? handleDelete : undefined}
                loading={loading}
              />
            </div>

            <div style={{ marginTop: '24px' }}>
              <h3 className="transaction-histories-title" style={{ margin: '4px 0 10px' }}>
                Liquidations
              </h3>
              <div className="finance-entry-subtext" style={{ marginBottom: '10px' }}>
                Cash withdrawals recorded with a liquidation reference.
              </div>
              <DataTable
                columns={liquidationColumns}
                data={liquidations}
                onView={openReceipt}
                onDelete={canArchive ? handleDelete : undefined}
                loading={loading}
              />
            </div>
          </div>

          <div className="content-surface" style={{ marginTop: '24px' }}>
            <div className="content-surface-header">
              <div>
                <h2>Performance report</h2>
                <p>Compare sales against liquidations for a selected date range.</p>
              </div>
            </div>

            <div className="finance-toolbar">
              <div className="finance-form-field" style={{ maxWidth: '220px' }}>
                <label className="finance-form-label" htmlFor="report-start-date">Start date</label>
                <input
                  id="report-start-date"
                  type="date"
                  value={rangeStart}
                  onChange={(e) => setRangeStart(e.target.value)}
                  className="finance-form-input"
                />
              </div>
              <div className="finance-form-field" style={{ maxWidth: '220px' }}>
                <label className="finance-form-label" htmlFor="report-end-date">End date</label>
                <input
                  id="report-end-date"
                  type="date"
                  value={rangeEnd}
                  onChange={(e) => setRangeEnd(e.target.value)}
                  className="finance-form-input"
                />
              </div>
              <div className="finance-action-row">
                <button
                  type="button"
                  className="btn-primary"
                  onClick={handleGenerateReport}
                  disabled={reportLoading}
                >
                  {reportLoading ? 'Generating...' : 'Generate'}
                </button>
              </div>
            </div>

            <div className="finance-entry-subtext">
              {reportEntries
                ? `Showing entries from ${formatDate(rangeStart)} to ${formatDate(rangeEnd)}.`
                : 'Showing all recorded entries.'}
            </div>

            <div className="finance-summary-grid finance-summary-grid-three">
              <div className="finance-summary-card">
                <span className="income-details-label">Sales total</span>
                <strong>{formatMoney(reportSalesTotal)}</strong>
              </div>
              <div className="finance-summary-card">
                <span className="income-details-label">Liquidation total</span>
                <strong>{formatMoney(reportLiquidationTotal)}</strong>
              </div>
              <div className="finance-summary-card">
                <span className="income-details-label">Net</span>
                <strong className={reportNetTotal < 0 ? 'finance-value-negative' : 'finance-value-net'}>
                  {formatMoney(reportNetTotal)}
                </strong>
              </div>
            </div>
          </div>

          <Modal
            isOpen={liquidationModalOpen}
            onClose={closeLiquidationModal}
            onSubmit={handleRecordLiquidation}
            title="Record Liquidation"
            submitText="Save Liquidation"
            loading={liquidationSubmitting}
            size="finance"
          >
            <div className="finance-form-stack">
              <div className="finance-form-field">
                <label className="finance-form-label" htmlFor="liquidation-date">Date</label>
                <input
                  id="liquidation-date"
                  type="date"
                  value={liquidationDate}
                  onChange={(e) => setLiquidationDate(e.target.value)}
                  className="finance-form-input"
                />
              </div>
              <div className="finance-form-field">
                <label className="finance-form-label" htmlFor="liquidation-amount">Amount</label>
                <input
                  id="liquidation-amount"
                  type="number"
                  min="0"
                  step="0.01"
                  value={liquidationAmount}
                  onChange={(e) => setLiquidationAmount(e.target.value)}
                  placeholder="Enter amount"
                  className="finance-form-input"
                />
              </div>
              <div className="finance-form-field">
                <label className="finance-form-label" htmlFor="liquidation-reason">Reason</label>
                <textarea
                  id="liquidation-reason"
                  value={liquidationReason}
                  onChange={(e) => setLiquidationReason(e.target.value)}
                  placeholder="Enter reason for the liquidation"
                  className="finance-form-textarea"
                />
              </div>
              <div className="finance-form-field">
                <label className="finance-form-label" htmlFor="liquidation-reference">Reference number</label>
                <input
                  id="liquidation-reference"
                  type="text"
                  value={generateLiquidationReferenceNumber(entries, liquidationDate)}
                  readOnly
                  className="finance-form-input"
                />
              </div>
            </div>
          </Modal>

          <Modal
            isOpen={!!receiptTarget}
            onClose={closeReceipt}
            title="Receipt"
            size="finance"
          >
            {renderReceiptContent()}
          </Modal>
        </div>
      </DashboardLayout>
    </PermissionGuard>
  );
};

export default Sales;
