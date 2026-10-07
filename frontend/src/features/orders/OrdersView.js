import React, { useState, useEffect, useCallback } from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import ConfirmModal from '../../components/ConfirmModal';
import PermissionGuard from '../../components/PermissionGuard';
import SectionIcon, { sectionIconBadgeStyle } from '../../components/SectionIcon';
import SearchField from '../../components/SearchField';
import inventoryService from '../../services/inventoryService';
import orderService from '../../services/orderService';
import { getApiErrorMessage, isAuthOrPermissionError } from '../../utils/apiErrors';
import { useNotification } from '../../context/NotificationContext';
import { useAuth } from '../../context/AuthContext';
import {
  STATUS_ACTIVE,
  STATUS_ARCHIVED,
  STATUS_CANCELLED,
  ORDER_TABS,
  PAYMENT_OPTIONS,
  SHOP_OPTIONS,
  getItemSubtotal,
  getTotals,
  buildRequestFromFormData,
  buildFormDataFromOrder,
  createInitialFormData,
  createEmptyItem,
} from './orderFormUtils';

const INITIAL_PAGE_SIZE = 100;

const formatMoney = (value) => `PHP ${(Number(value) || 0).toFixed(2)}`;

const formatDate = (value) => {
  if (!value) return '-';
  return String(value).slice(0, 10);
};

const normalizeText = (value) => String(value ?? '').toLowerCase().trim();

const Orders = () => {
  const [orders, setOrders] = useState([]);
  const [inventoryItems, setInventoryItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [statusTab, setStatusTab] = useState(STATUS_ACTIVE);
  const [searchQuery, setSearchQuery] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOrder, setEditingOrder] = useState(null);
  const [formData, setFormData] = useState(createInitialFormData());
  const [fieldErrors, setFieldErrors] = useState({});
  const [detailsOrder, setDetailsOrder] = useState(null);
  const [confirmAction, setConfirmAction] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const { success: notifySuccess, error: notifyError, info: notifyInfo } = useNotification();
  const { user } = useAuth();
  const isAdmin = String(user?.role || '').toUpperCase() === 'ADMIN';

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      const response = await orderService.getAllOrders(0, INITIAL_PAGE_SIZE, statusTab);
      setOrders(response.data.content || []);
    } catch (error) {
      console.error('Error loading orders:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      notifyError(`Failed to load orders: ${getApiErrorMessage(error)}`);
    } finally {
      setLoading(false);
    }
  }, [statusTab, notifyError]);

  const loadInventory = useCallback(async () => {
    try {
      const response = await inventoryService.getAllInventory(0, 1000);
      setInventoryItems(response.data.content || []);
    } catch (error) {
      console.error('Error loading inventory:', error);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  useEffect(() => {
    loadInventory();
  }, [loadInventory]);

  useEffect(() => {
    setCurrentPage(1);
  }, [statusTab, searchQuery]);

  const filteredOrders = orders
    .filter((order) => {
      const term = normalizeText(searchQuery);
      if (!term) return true;
      return normalizeText(order.jobOrderNo).includes(term)
        || normalizeText(order.customerName).includes(term);
    });

  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / 10));
  const paginatedOrders = filteredOrders.slice((currentPage - 1) * 10, currentPage * 10);

  const openNewItemModal = () => {
    setEditingOrder(null);
    setFormData(createInitialFormData());
    setFieldErrors({});
    setModalOpen(true);
  };

  const handleEdit = (order) => {
    setEditingOrder(order);
    setFormData(buildFormDataFromOrder(order));
    setFieldErrors({});
    setModalOpen(true);
  };

  const handleItemChange = (index, key, value) => {
    setFormData((prev) => {
      const items = prev.items.map((item, i) => {
        if (i !== index) return item;
        const next = { ...item, [key]: value };
        if (key === 'inventoryId') {
          const inventory = inventoryItems.find((entry) => entry.id === value);
          if (inventory) {
            next.productName = inventory.name;
            next.size = inventory.size || '';
            next.unitPrice = inventory.price != null ? String(inventory.price) : '';
          }
        }
        return next;
      });
      return { ...prev, items };
    });
  };

  const handleAddItem = () => {
    setFormData((prev) => ({ ...prev, items: [...prev.items, createEmptyItem()] }));
  };

  const handleRemoveItem = (index) => {
    setFormData((prev) => {
      if (prev.items.length === 1) return prev;
      return { ...prev, items: prev.items.filter((_, i) => i !== index) };
    });
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.customerName.trim()) {
      errors.customerName = 'Customer name is required';
    }
    if (!formData.orderDate) {
      errors.orderDate = 'Order date is required';
    }
    if (!formData.items.every((item) => item.inventoryId && Number(item.quantity) >= 1 && Number(item.unitPrice) > 0)) {
      errors.items = 'Each product row needs an inventory item, quantity, and price';
    }
    const totals = getTotals(formData);
    if (totals.total <= 0) {
      errors.discount = 'Order total must be greater than zero';
    }
    if (totals.payment > totals.total) {
      errors.payment = 'Payment cannot exceed the order total';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      notifyInfo('Please fix the highlighted fields');
      return;
    }

    try {
      setSubmitting(true);
      const status = editingOrder ? editingOrder.status : STATUS_ACTIVE;
      const payload = buildRequestFromFormData(formData, status);
      if (editingOrder) {
        await orderService.updateOrder(editingOrder.id, payload);
        notifySuccess('Order updated successfully');
      } else {
        await orderService.createOrder(payload);
        notifySuccess('Order created successfully');
      }
      setModalOpen(false);
      setEditingOrder(null);
      setFormData(createInitialFormData());
      loadOrders();
      loadInventory();
    } catch (error) {
      notifyError(`Failed to save order: ${getApiErrorMessage(error)}`);
    } finally {
      setSubmitting(false);
    }
  };

  const buildStatusPayload = (order, nextStatus) =>
    buildRequestFromFormData(buildFormDataFromOrder(order), nextStatus);

  const applyStatusChange = async (order, nextStatus, message) => {
    try {
      setSubmitting(true);
      await orderService.updateOrder(order.id, buildStatusPayload(order, nextStatus));
      notifySuccess(message);
      setDetailsOrder(null);
      setConfirmAction(null);
      loadOrders();
      loadInventory();
    } catch (error) {
      notifyError(`Failed to update order: ${getApiErrorMessage(error)}`);
      setConfirmAction(null);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await orderService.deleteOrder(id);
      notifySuccess('Order deleted successfully');
      loadOrders();
      loadInventory();
    } catch (error) {
      notifyError(`Failed to delete order: ${getApiErrorMessage(error)}`);
    }
  };

  const columns = [
    { key: 'jobOrderNo', label: 'Order No' },
    { key: 'customerName', label: 'Customer', render: (value) => value || 'Walk-in' },
    { key: 'orderDate', label: 'Date', render: (value) => formatDate(value) },
    {
      key: 'price',
      label: 'Total',
      render: (value) => formatMoney(value),
    },
    {
      key: 'status',
      label: 'Status',
      render: (value) => <span className="order-status-badge">{value}</span>,
    },
  ];

  const totals = getTotals(formData);

  return (
    <PermissionGuard permission="ORDERS">
      <DashboardLayout>
        <div className="page-container">
          <div className="page-header">
            <div className="page-title-block">
              <span style={sectionIconBadgeStyle} aria-hidden="true">
                <SectionIcon variant="orders" />
              </span>
              <span className="page-eyebrow">Fulfillment</span>
              <h1>Orders</h1>
              <p className="page-subtitle">
                Create orders from stock, capture payment once, and archive or cancel when finished.
              </p>
            </div>
            <div className="page-actions">
              <button className="btn-primary" onClick={openNewItemModal} type="button">
                New Order
              </button>
            </div>
          </div>

          <div className="content-surface">
            <div className="content-surface-header">
              <div>
                <h2>Order overview</h2>
                <p>Track active orders, review archived history, and manage cancellations.</p>
              </div>
            </div>

            <div className="orders-filter-bar" role="tablist" aria-label="Order status">
              {ORDER_TABS.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  role="tab"
                  aria-selected={statusTab === tab.key}
                  className={`order-filter-btn ${statusTab === tab.key ? 'active' : ''}`}
                  onClick={() => setStatusTab(tab.key)}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <div className="search-and-filter-row">
              <div className="order-search-bar">
                <SearchField
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search by order no or customer"
                />
              </div>
            </div>

            {filteredOrders.length === 0 ? (
              <div className="empty-state">
                <span style={{ ...sectionIconBadgeStyle, marginBottom: '12px' }} aria-hidden="true">
                  <SectionIcon variant="orders" />
                </span>
                <h3>No orders in this view</h3>
                <p>Create a new order or switch tabs to see other statuses.</p>
              </div>
            ) : (
              <DataTable
                columns={columns}
                data={paginatedOrders}
                onView={setDetailsOrder}
                onEdit={statusTab === STATUS_CANCELLED ? undefined : handleEdit}
                onDelete={isAdmin ? handleDelete : undefined}
                canEdit={(row) => row.status !== STATUS_CANCELLED}
                loading={loading}
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
              />
            )}
          </div>

          <Modal
            isOpen={modalOpen}
            title={editingOrder ? `Edit Order ${editingOrder.jobOrderNo}` : 'New Order'}
            onClose={() => setModalOpen(false)}
            onSubmit={handleSubmit}
            submitText={editingOrder ? 'Update' : 'Create Order'}
            loading={submitting}
            size="large"
          >
            <form className="order-form-grid">
              <div className="order-modal-row">
                <div className="form-group">
                  <label>Customer Name</label>
                  <input
                    type="text"
                    value={formData.customerName}
                    onChange={(e) => setFormData({ ...formData, customerName: e.target.value })}
                    placeholder="Enter customer name"
                    required
                  />
                  {fieldErrors.customerName && <small className="form-help-text">{fieldErrors.customerName}</small>}
                </div>
                <div className="form-group">
                  <label>Order Date</label>
                  <input
                    type="date"
                    value={formData.orderDate}
                    onChange={(e) => setFormData({ ...formData, orderDate: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="order-status-prompt">
                <span>Products</span>
                <button className="btn-primary" type="button" onClick={handleAddItem}>
                  + Add another product
                </button>
              </div>

              {fieldErrors.items && <small className="form-help-text">{fieldErrors.items}</small>}

              {formData.items.map((item, index) => (
                <div className="order-retail-item" key={`item-${index}`}>
                  <div className="form-group">
                    <label>Product</label>
                    <select
                      value={item.inventoryId}
                      onChange={(e) => handleItemChange(index, 'inventoryId', e.target.value)}
                      required
                    >
                      <option value="">Select from inventory</option>
                      {inventoryItems.map((entry) => (
                        <option key={entry.id} value={entry.id}>
                          {entry.brand} - {entry.name} {entry.size ? `(${entry.size})` : ''} — stock {entry.quantity}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Size</label>
                    <input type="text" value={item.size} readOnly placeholder="-" />
                  </div>
                  <div className="form-group">
                    <label>Unit Price</label>
                    <input
                      type="text"
                      inputMode="decimal"
                      value={item.unitPrice}
                      onChange={(e) => handleItemChange(index, 'unitPrice', e.target.value)}
                      placeholder="0.00"
                    />
                  </div>
                  <div className="form-group">
                    <label>Qty</label>
                    <input
                      type="text"
                      inputMode="numeric"
                      value={item.quantity}
                      onChange={(e) => handleItemChange(index, 'quantity', e.target.value)}
                      placeholder="1"
                    />
                  </div>
                  <div className="form-group">
                    <label>Subtotal</label>
                    <input type="text" value={formatMoney(getItemSubtotal(item))} readOnly />
                  </div>
                  {formData.items.length > 1 && (
                    <button
                      type="button"
                      className="btn-cancel"
                      onClick={() => handleRemoveItem(index)}
                      aria-label="Remove product"
                    >
                      Remove
                    </button>
                  )}
                </div>
              ))}

              <div className="order-modal-row">
                <div className="form-group">
                  <label>Discount</label>
                  <input
                    type="text"
                    inputMode="decimal"
                    value={formData.discount}
                    onChange={(e) => setFormData({ ...formData, discount: e.target.value })}
                  />
                  {fieldErrors.discount && <small className="form-help-text">{fieldErrors.discount}</small>}
                </div>
                <div className="form-group">
                  <label>Payment</label>
                  <input
                    type="text"
                    inputMode="decimal"
                    value={formData.payment}
                    onChange={(e) => setFormData({ ...formData, payment: e.target.value })}
                  />
                  {fieldErrors.payment && <small className="form-help-text">{fieldErrors.payment}</small>}
                </div>
                <div className="form-group">
                  <label>Payment Method</label>
                  <select
                    value={formData.paymentMethod}
                    onChange={(e) => setFormData({ ...formData, paymentMethod: e.target.value })}
                  >
                    {PAYMENT_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="order-modal-row">
                <div className="form-group">
                  <label>Shop</label>
                  <select value={formData.shop} onChange={(e) => setFormData({ ...formData, shop: e.target.value })}>
                    {SHOP_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Notes</label>
                  <input
                    type="text"
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    placeholder="Optional notes"
                  />
                </div>
              </div>

              <div className="price-calculation">
                <div className="details-row">
                  <span className="details-label">Subtotal</span>
                  <span className="details-value">{formatMoney(totals.subtotal)}</span>
                </div>
                <div className="details-row">
                  <span className="details-label">Discount</span>
                  <span className="details-value">-{formatMoney(totals.discount)}</span>
                </div>
                <div className="details-row">
                  <span className="details-label">Total</span>
                  <span className="details-value">{formatMoney(totals.total)}</span>
                </div>
              </div>

              <div className="payment-calculation">
                <div className="details-row">
                  <span className="details-label">Payment</span>
                  <span className="details-value">{formatMoney(totals.payment)}</span>
                </div>
                <div className="details-row">
                  <span className="details-label">Balance</span>
                  <span className="details-value">{formatMoney(totals.balance)}</span>
                </div>
              </div>
            </form>
          </Modal>

          <Modal
            isOpen={Boolean(detailsOrder)}
            title={detailsOrder ? `Order ${detailsOrder.jobOrderNo}` : 'Order Details'}
            onClose={() => setDetailsOrder(null)}
            size="large"
          >
            {detailsOrder && (
              <div className="order-details-panel">
                <div className="order-details-topbar">
                  <div className="order-details-topbar-title">
                    <p className="page-eyebrow">{detailsOrder.status}</p>
                    <h3>{detailsOrder.customerName || 'Walk-in'}</h3>
                  </div>
                  <span className="order-details-topbar-date">{formatDate(detailsOrder.orderDate)}</span>
                </div>

                <div className="details-table">
                  {(detailsOrder.items || []).map((item) => (
                    <div className="details-row" key={item.id}>
                      <span className="details-label">
                        {item.productName} {item.size ? `(${item.size})` : ''} x {item.quantity}
                      </span>
                      <span className="details-value">{formatMoney(item.unitPrice * item.quantity)}</span>
                    </div>
                  ))}
                  <div className="details-row">
                    <span className="details-label">Discount</span>
                    <span className="details-value">-{formatMoney(detailsOrder.discount)}</span>
                  </div>
                  <div className="details-row">
                    <span className="details-label">Total</span>
                    <span className="details-value">{formatMoney(detailsOrder.price)}</span>
                  </div>
                  <div className="details-row">
                    <span className="details-label">Payment ({detailsOrder.paymentMethod})</span>
                    <span className="details-value">{formatMoney(detailsOrder.payment)}</span>
                  </div>
                  <div className="details-row">
                    <span className="details-label">Balance</span>
                    <span className="details-value">{formatMoney(detailsOrder.balance)}</span>
                  </div>
                  <div className="details-row">
                    <span className="details-label">Shop</span>
                    <span className="details-value">
                      {SHOP_OPTIONS.find((option) => option.value === detailsOrder.shop)?.label || detailsOrder.shop}
                    </span>
                  </div>
                  {detailsOrder.notes && (
                    <div className="details-row">
                      <span className="details-label">Notes</span>
                      <span className="details-value">{detailsOrder.notes}</span>
                    </div>
                  )}
                </div>

                <div className="order-status-buttons">
                  {detailsOrder.status === STATUS_ACTIVE && (
                    <>
                      <button
                        type="button"
                        className="status-btn status-btn-secondary"
                        onClick={() => setConfirmAction({ order: detailsOrder, nextStatus: STATUS_ARCHIVED, message: 'Order archived' })}
                      >
                        Hide/Archive order
                      </button>
                      <button
                        type="button"
                        className="status-btn status-btn-primary"
                        onClick={() => setConfirmAction({ order: detailsOrder, nextStatus: STATUS_CANCELLED, message: 'Order is cancelled' })}
                      >
                        Order is cancelled
                      </button>
                    </>
                  )}
                  {detailsOrder.status === STATUS_ARCHIVED && (
                    <>
                      <button
                        type="button"
                        className="status-btn status-btn-secondary"
                        onClick={() => setConfirmAction({ order: detailsOrder, nextStatus: STATUS_ACTIVE, message: 'Order restored' })}
                      >
                        Restore order
                      </button>
                      <button
                        type="button"
                        className="status-btn status-btn-primary"
                        onClick={() => setConfirmAction({ order: detailsOrder, nextStatus: STATUS_CANCELLED, message: 'Order is cancelled' })}
                      >
                        Order is cancelled
                      </button>
                    </>
                  )}
                  {detailsOrder.status === STATUS_CANCELLED && (
                    <button
                      type="button"
                      className="status-btn status-btn-secondary"
                      onClick={() => setConfirmAction({ order: detailsOrder, nextStatus: STATUS_ACTIVE, message: 'Order restored' })}
                    >
                      Restore order
                    </button>
                  )}
                </div>
              </div>
            )}
          </Modal>

          <ConfirmModal
            isOpen={Boolean(confirmAction)}
            title="Confirm status change"
            message={
              confirmAction
                ? `Change order ${confirmAction.order.jobOrderNo} to ${confirmAction.nextStatus}?`
                : ''
            }
            onConfirm={() =>
              confirmAction &&
              applyStatusChange(confirmAction.order, confirmAction.nextStatus, confirmAction.message)
            }
            onCancel={() => setConfirmAction(null)}
          />
        </div>
      </DashboardLayout>
    </PermissionGuard>
  );
};

export default Orders;
