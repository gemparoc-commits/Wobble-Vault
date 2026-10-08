import React, { useEffect, useState } from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PermissionGuard from '../../components/PermissionGuard';
import SectionIcon, { sectionIconBadgeStyle } from '../../components/SectionIcon';
import SearchField from '../../components/SearchField';
import { useNotification } from '../../context/NotificationContext';
import inventoryService from '../../services/inventoryService';
import { getApiErrorMessage, isAuthOrPermissionError } from '../../utils/apiErrors';
import SIZE_OPTIONS from '../../utils/sizes';
import {
  createInitialInventoryFormData,
  validateInventoryForm,
  buildInventoryPayload,
  buildInventoryFormData,
} from './inventoryFormUtils';

const INITIAL_PAGE_SIZE = 100;

const formatDateCreated = (value) => {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString();
};

const normalizeText = (value) => String(value ?? '').toLowerCase().trim();

const Inventory = () => {
  const { error: notifyError, success: notifySuccess, info: notifyInfo } = useNotification();
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [detailsItem, setDetailsItem] = useState(null);
  const [formData, setFormData] = useState(createInitialInventoryFormData());
  const [searchTerm, setSearchTerm] = useState('');
  const [sizeFilter, setSizeFilter] = useState('');

  const loadInventory = React.useCallback(async () => {
    try {
      setLoading(true);
      const response = await inventoryService.getAllInventory(0, INITIAL_PAGE_SIZE);
      setInventory(response.data.content || []);
    } catch (error) {
      console.error('Error loading inventory:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      const errorMsg = getApiErrorMessage(error, 'Failed to load inventory');
      notifyError(`Failed to load inventory: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  }, [notifyError]);

  useEffect(() => {
    loadInventory();
  }, [loadInventory]);

  const filteredInventory = inventory
    .slice()
    .sort((a, b) => normalizeText(a.brand).localeCompare(normalizeText(b.brand))
      || normalizeText(a.name).localeCompare(normalizeText(b.name)))
    .filter((item) => {
      const term = normalizeText(searchTerm);
      const matchesSearch = !term
        || normalizeText(item.name).includes(term)
        || normalizeText(item.brand).includes(term);
      const matchesSize = !sizeFilter || normalizeText(item.size) === normalizeText(sizeFilter);
      return matchesSearch && matchesSize;
    });

  const totalPages = Math.max(1, Math.ceil(filteredInventory.length / 10));
  const paginatedInventory = filteredInventory.slice((currentPage - 1) * 10, currentPage * 10);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, sizeFilter]);

  const handleEdit = (item) => {
    setEditingItem(item);
    setFormData(buildInventoryFormData(item));
    setModalOpen(true);
  };

  const handleViewDetails = (item) => {
    setDetailsItem(item);
  };

  const handleDelete = async (id) => {
    try {
      await inventoryService.deleteInventory(id);
      notifySuccess('Item deleted successfully');
      loadInventory();
    } catch (error) {
      console.error('Error deleting item:', error);
      notifyError('Failed to delete item');
    }
  };

  const handleSubmit = async () => {
    try {
      const errors = validateInventoryForm(formData);
      const firstError = Object.values(errors)[0];
      if (firstError) {
        notifyInfo(firstError);
        return;
      }

      const payload = buildInventoryPayload(formData);

      if (editingItem) {
        await inventoryService.updateInventory(editingItem.id, payload);
        notifySuccess('Item updated successfully');
      } else {
        await inventoryService.createInventory(payload);
        notifySuccess('Item created successfully');
      }

      setModalOpen(false);
      setEditingItem(null);
      setFormData(createInitialInventoryFormData());
      loadInventory();
    } catch (error) {
      console.error('Error saving item:', error);
      const errorMessage = error.response?.data?.message || error.message || 'Failed to save item';
      notifyError(`Failed to save item: ${errorMessage}`);
    }
  };

  const columns = [
    { key: 'brand', label: 'Brand' },
    { key: 'name', label: 'Shoe Name' },
    { key: 'size', label: 'Size', render: (value) => value || '-' },
    { key: 'quantity', label: 'Qty', render: (value) => value || '0' },
  ];

  const openNewItemModal = () => {
    setEditingItem(null);
    setFormData(createInitialInventoryFormData());
    setModalOpen(true);
  };

  const inventoryStats = [
    { label: 'Tracked items', value: inventory.length, detail: 'Loaded in current roster' },
    { label: 'Visible now', value: paginatedInventory.length, detail: 'On this page' },
    { label: 'Filtered results', value: filteredInventory.length, detail: 'Matching your search' },
  ];

  return (
    <PermissionGuard permission="INVENTORY">
      <DashboardLayout>
        <div className="page-container">
          <div className="page-header">
            <div className="page-title-block">
              <span style={sectionIconBadgeStyle} aria-hidden="true">
                <SectionIcon variant="inventory" />
              </span>
              <span className="page-eyebrow">Stock control</span>
              <h1>Inventory</h1>
              <p className="page-subtitle">
                Keep brands, sizes, quantities, and prices up to date for every shoe you carry.
              </p>
            </div>
            <div className="page-actions">
              <button className="btn-primary" onClick={openNewItemModal} type="button">
                Add Shoe
              </button>
            </div>
          </div>

          <div className="content-surface">
            <div className="content-surface-header">
              <div>
                <h2>Inventory overview</h2>
                <p>Browse the full catalog and jump to the exact pair you need.</p>
              </div>
              <div className="stats-strip">
                {inventoryStats.map((stat) => (
                  <div key={stat.label} className="stat-pill">
                    <strong>{stat.value}</strong>
                    <span>{stat.label}</span>
                    <small>{stat.detail}</small>
                  </div>
                ))}
              </div>
            </div>

            <div className="search-and-filter-row">
              <div className="inventory-search-bar" aria-label="Inventory search filters">
                <SearchField
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search by brand or name"
                />
                <select
                  className="inventory-size-filter"
                  value={sizeFilter}
                  onChange={(e) => setSizeFilter(e.target.value)}
                  aria-label="Filter by size"
                >
                  <option value="">All sizes</option>
                  {SIZE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {filteredInventory.length === 0 ? (
              <div className="empty-state">
                <span style={{ ...sectionIconBadgeStyle, marginBottom: '12px' }} aria-hidden="true">
                  <SectionIcon variant="inventory" />
                </span>
                <h3>No inventory items match your current filters</h3>
                <p>Try a broader term or add a new item to refresh the catalog.</p>
              </div>
            ) : (
              <DataTable
                columns={columns}
                data={paginatedInventory}
                onView={handleViewDetails}
                onEdit={handleEdit}
                onDelete={handleDelete}
                loading={loading}
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
              />
            )}

            <Modal
              isOpen={modalOpen}
              title={editingItem ? 'Edit Shoe' : 'New Shoe'}
              onClose={() => setModalOpen(false)}
              onSubmit={handleSubmit}
              submitText={editingItem ? 'Update' : 'Add'}
              size="medium"
            >
              <form className="inventory-modal-form">
                <div className="inventory-modal-grid inventory-modal-grid-row-2">
                  <div className="form-group">
                    <label>Brand</label>
                    <input
                      type="text"
                      value={formData.brand}
                      onChange={(e) => setFormData({ ...formData, brand: e.target.value })}
                      placeholder="Enter brand"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Shoe Name</label>
                    <input
                      type="text"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="Enter shoe name"
                      required
                    />
                  </div>
                </div>

                <div className="inventory-modal-grid inventory-modal-grid-row-2">
                  <div className="form-group">
                    <label>Gender</label>
                    <select
                      value={formData.gender}
                      onChange={(e) => setFormData({ ...formData, gender: e.target.value })}
                    >
                      <option value="">Select gender</option>
                      <option value="Men">Men</option>
                      <option value="Women">Women</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Sizing System</label>
                    <select
                      value={formData.sizingSystem}
                      onChange={(e) => setFormData({ ...formData, sizingSystem: e.target.value })}
                    >
                      <option value="">Select sizing system</option>
                      <option value="US">US</option>
                      <option value="UK">UK</option>
                      <option value="EU">EU</option>
                    </select>
                  </div>
                </div>

                <div className="inventory-modal-grid inventory-modal-grid-row-3">
                  <div className="form-group">
                    <label>Size</label>
                    <input
                      type="text"
                      value={formData.size}
                      onChange={(e) => setFormData({ ...formData, size: e.target.value })}
                      placeholder="Enter size"
                    />
                  </div>

                  <div className="form-group">
                    <label>Quantity</label>
                    <input
                      type="text"
                      inputMode="numeric"
                      value={formData.quantity}
                      onChange={(e) => {
                        const v = e.target.value;
                        if (v === '') {
                          setFormData({ ...formData, quantity: '' });
                          return;
                        }
                        const n = Number.parseInt(v, 10);
                        if (!Number.isFinite(n)) return;
                        setFormData({ ...formData, quantity: String(Math.max(0, n)) });
                      }}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Price</label>
                    <input
                      type="text"
                      inputMode="decimal"
                      value={formData.price}
                      onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                      required
                    />
                  </div>
                </div>

                <div className="form-group inventory-notes-group">
                  <label>Notes</label>
                  <textarea
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    placeholder="Add any notes"
                    rows={4}
                  />
                </div>
              </form>
            </Modal>

            <Modal
              isOpen={Boolean(detailsItem)}
              title="Item Details"
              onClose={() => setDetailsItem(null)}
              size="large"
            >
              {detailsItem && (
                <div className="inventory-details">
                  <div className="inventory-details-hero">
                    <div>
                      <p className="inventory-details-eyebrow">Inventory Item</p>
                      <h3>{detailsItem.name || '-'}</h3>
                    </div>
                    <div className="inventory-details-hero-badge">
                      {detailsItem.brand || 'Brand'}
                    </div>
                  </div>

                  <div className="inventory-details-grid">
                    <div className="inventory-details-item">
                      <span>Brand</span>
                      <strong>{detailsItem.brand || '-'}</strong>
                    </div>
                    <div className="inventory-details-item">
                      <span>Size</span>
                      <strong>{detailsItem.size || '-'}</strong>
                    </div>
                    <div className="inventory-details-item">
                      <span>Gender</span>
                      <strong>{detailsItem.gender || '-'}</strong>
                    </div>
                    <div className="inventory-details-item">
                      <span>Sizing System</span>
                      <strong>{detailsItem.sizingSystem || '-'}</strong>
                    </div>
                    <div className="inventory-details-item">
                      <span>Quantity</span>
                      <strong>{detailsItem.quantity != null ? detailsItem.quantity : '-'}</strong>
                    </div>
                    <div className="inventory-details-item">
                      <span>Price</span>
                      <strong>
                        {detailsItem.price != null ? Number(detailsItem.price).toFixed(2) : '-'}
                      </strong>
                    </div>
                    <div className="inventory-details-item inventory-details-item-full">
                      <span>Notes</span>
                      <strong>{detailsItem.notes || '-'}</strong>
                    </div>
                    <div className="inventory-details-item">
                      <span>Date Created</span>
                      <strong>{formatDateCreated(detailsItem.createdAt)}</strong>
                    </div>
                  </div>
                </div>
              )}
            </Modal>
          </div>
        </div>
      </DashboardLayout>
    </PermissionGuard>
  );
};

export default Inventory;
