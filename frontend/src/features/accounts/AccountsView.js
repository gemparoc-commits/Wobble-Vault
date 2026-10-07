import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../../layouts/DashboardLayout';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PermissionGuard from '../../components/PermissionGuard';
import SectionIcon, { sectionIconBadgeStyle } from '../../components/SectionIcon';
import SearchField from '../../components/SearchField';
import { useAuth } from '../../context/AuthContext';
import { useNotification } from '../../context/NotificationContext';
import userService from '../../services/userService';
import { expandPermissions } from '../../utils/permissions';
import { getApiErrorMessage, isAuthOrPermissionError } from '../../utils/apiErrors';

const extractUsers = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (payload?.content && Array.isArray(payload.content)) return payload.content;
  return [];
};

const createInitialFormData = () => ({
  username: '',
  email: '',
  password: '',
  role: '',
});

const Accounts = () => {
  const { user } = useAuth();
  const { success: notifySuccess, error: notifyError, info: notifyInfo } = useNotification();
  const isAdmin = user?.role === 'ADMIN';
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [registerModalOpen, setRegisterModalOpen] = useState(false);
  const [permissionsModalOpen, setPermissionsModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [permissionSaving, setPermissionSaving] = useState(false);
  const [formData, setFormData] = useState(createInitialFormData());
  const [selectedPermissions, setSelectedPermissions] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');

  const pagePermissions = [
    { key: 'INVENTORY', label: 'Inventory' },
    { key: 'ORDERS', label: 'Orders' },
    { key: 'SALES', label: 'Sales' },
    { key: 'SALES_ARCHIVE', label: 'Sales Archive' },
    { key: 'ACCOUNTS', label: 'Accounts' },
  ];

  const loadUsers = useCallback(async () => {
    if (!isAdmin) {
      setUsers([]);
      setTotalPages(1);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const response = await userService.getAllUsers(currentPage - 1, 100);
      const nextUsers = extractUsers(response.data);
      setUsers(nextUsers);

      const totalElements =
        typeof response.data?.totalElements === 'number' ? response.data.totalElements : nextUsers.length;
      setTotalPages(Math.max(1, Math.ceil(totalElements / 100)));
    } catch (error) {
      console.error('Error loading accounts:', error);
      if (isAuthOrPermissionError(error)) {
        return;
      }
      notifyError(`Failed to load accounts: ${getApiErrorMessage(error, 'Failed to load accounts')}`);
    } finally {
      setLoading(false);
    }
  }, [currentPage, isAdmin, notifyError]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const openRegisterModal = () => {
    const initialFormData = createInitialFormData();
    initialFormData.role = 'ADMIN';
    setFormData(initialFormData);
    setSelectedPermissions([]);
    setRegisterModalOpen(true);
  };

  const closeRegisterModal = () => {
    setRegisterModalOpen(false);
    setFormData(createInitialFormData());
    setSelectedPermissions([]);
  };

  const openPermissionsModal = (account) => {
    setSelectedUser(account);
    setSelectedPermissions(expandPermissions((account.permissions || []).map((permission) => permission.pageName)));
    setPermissionsModalOpen(true);
  };

  const closePermissionsModal = () => {
    setPermissionsModalOpen(false);
    setSelectedUser(null);
    setSelectedPermissions([]);
  };

  const handlePermissionToggle = (permissionKey) => {
    setSelectedPermissions((current) =>
      current.includes(permissionKey) ? current.filter((item) => item !== permissionKey) : [...current, permissionKey]
    );
  };

  const syncPermissions = async (userId, nextPermissions, currentPermissions = []) => {
    const currentSet = new Set(currentPermissions);
    const nextSet = new Set(nextPermissions);

    const permissionsToGrant = nextPermissions.filter((permission) => !currentSet.has(permission));
    const permissionsToRevoke = currentPermissions.filter((permission) => !nextSet.has(permission));

    for (const permission of permissionsToGrant) {
      await userService.grantPermission(userId, permission);
    }
    for (const permission of permissionsToRevoke) {
      await userService.revokePermission(userId, permission);
    }
  };

  const handleRegisterAccount = async () => {
    try {
      const trimmedUsername = String(formData.username || '').trim();
      const selectedRole = String(formData.role || '').trim();

      if (!trimmedUsername) {
        notifyInfo('Please enter the account name before saving.');
        return;
      }

      if (!selectedRole) {
        notifyInfo('Please select a role before saving.');
        return;
      }

      const email = String(formData.email || '').trim();
      const password = String(formData.password || '').trim();

      if (!email) {
        notifyInfo('Please enter the email address before saving.');
        return;
      }

      if (!password) {
        notifyInfo('Please enter the password before saving.');
        return;
      }

      const payload = {
        username: trimmedUsername,
        email: email || null,
        password: password || null,
        role: selectedRole.toUpperCase(),
      };

      setFormLoading(true);
      const response = await userService.createUser(payload);
      const createdUser = response.data;
      let permissionSyncFailed = false;

      if (selectedPermissions.length > 0 && createdUser?.id) {
        try {
          await syncPermissions(createdUser.id, selectedPermissions);
        } catch (permissionError) {
          console.error('Error applying permissions:', permissionError);
          permissionSyncFailed = true;
        }
      }

      if (permissionSyncFailed) {
        notifyError('Account registered, but some permissions could not be saved.');
      } else {
        notifySuccess('Account registered successfully');
      }
      closeRegisterModal();
      loadUsers();
    } catch (error) {
      console.error('Error registering account:', error);
      notifyError(getApiErrorMessage(error, 'Failed to register account'));
    } finally {
      setFormLoading(false);
    }
  };

  const handleSavePermissions = async () => {
    if (!selectedUser?.id) return;

    try {
      setPermissionSaving(true);
      const currentPermissions = expandPermissions((selectedUser.permissions || []).map((permission) => permission.pageName));
      await syncPermissions(selectedUser.id, selectedPermissions, currentPermissions);
      notifySuccess('Permissions updated successfully');
      closePermissionsModal();
      loadUsers();
    } catch (error) {
      console.error('Error updating permissions:', error);
      notifyError(error.response?.data?.message || 'Failed to update permissions');
    } finally {
      setPermissionSaving(false);
    }
  };

  const handleDeleteUser = async (id) => {
    try {
      await userService.deleteUser(id);
      notifySuccess('Account deleted successfully');
      loadUsers();
    } catch (error) {
      console.error('Error deleting account:', error);
      notifyError('Failed to delete account');
    }
  };

  const columns = [
    { key: 'username', label: 'Account Name' },
    { key: 'email', label: 'Email', render: (value) => value || '-' },
    { key: 'role', label: 'Role' },
    {
      key: 'permissions',
      label: 'Page Access',
      render: (_, row) =>
        row.permissions && row.permissions.length > 0
          ? row.permissions.map((permission) => permission.pageName).join(', ')
          : 'No page access',
    },
  ];

  const filteredUsers = users.filter((account) => {
    const haystack = `${account.username || ''} ${account.email || ''} ${account.role || ''}`.toLowerCase();
    return haystack.includes(searchQuery.trim().toLowerCase());
  });

  const accountStats = [
    { label: 'Registered accounts', value: users.length, detail: 'People in the roster' },
    { label: 'Admins', value: users.filter((account) => (account.role || '').toUpperCase() === 'ADMIN').length, detail: 'Permission leaders' },
    { label: 'Visible now', value: filteredUsers.length, detail: 'Matching your filters' },
  ];

  return (
    <PermissionGuard permission="ACCOUNTS">
      <DashboardLayout>
        <div className="page-container">
          <div className="page-header">
            <div className="page-title-block">
              <span style={sectionIconBadgeStyle} aria-hidden="true">
                <SectionIcon variant="employees" />
              </span>
              <span className="page-eyebrow">People & access</span>
              <h1>Accounts</h1>
              <p className="page-subtitle">
                Keep your team accounts, roles, and page permissions clear and easy to manage.
              </p>
            </div>
            {isAdmin && (
              <div className="page-actions">
                <button className="btn-primary" onClick={() => openRegisterModal()} type="button">
                  Register Admin
                </button>
              </div>
            )}
          </div>

          {isAdmin ? (
            <div className="content-surface">
              <div className="content-surface-header">
                <div>
                  <h2>Accounts overview</h2>
                  <p>Quickly scan your roster, refine the list, and keep permissions aligned.</p>
                </div>
                <div className="stats-strip">
                  {accountStats.map((stat) => (
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
                  className="employee-search-bar"
                  wrapperProps={{ 'aria-label': 'Account search' }}
                  type="text"
                  value={searchQuery}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    setCurrentPage(1);
                  }}
                  placeholder="Search accounts by name or email"
                />
              </div>

              {filteredUsers.length === 0 ? (
                <div className="empty-state">
                  <span style={{ ...sectionIconBadgeStyle, marginBottom: '12px' }} aria-hidden="true">
                    <SectionIcon variant="employees" />
                  </span>
                  <h3>No accounts match this view yet</h3>
                  <p>Try adjusting your search or switching filters to see more people.</p>
                </div>
              ) : (
                <DataTable
                  columns={columns}
                  data={filteredUsers}
                  onEdit={openPermissionsModal}
                  onDelete={handleDeleteUser}
                  loading={loading}
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={setCurrentPage}
                />
              )}

              <Modal
                isOpen={registerModalOpen}
                title="Register New Admin"
                onClose={closeRegisterModal}
                onSubmit={handleRegisterAccount}
                submitText="Register Admin"
                loading={formLoading}
                size="large"
              >
                <div className="employee-modal-grid">
                  <div className="form-group">
                    <label>Account Name</label>
                    <input
                      type="text"
                      value={formData.username}
                      onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                      placeholder="Enter account name"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Email</label>
                    <input
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      placeholder="Enter email address"
                    />
                  </div>

                  <div className="form-group">
                    <label>Password</label>
                    <input
                      type="password"
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      placeholder="Create a password"
                    />
                  </div>

                  <div className="form-group">
                    <label>Role</label>
                    <select
                      value={formData.role}
                      onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                      required
                    >
                      <option value="ADMIN">Admin</option>
                    </select>
                  </div>
                </div>

                <div className="permission-section">
                  <div className="permission-section-header">
                    <h3>Page Viewing Permissions</h3>
                    <p>Dashboard is always available. Select from the current sidebar pages below.</p>
                  </div>

                  <div className="permission-checkbox-grid">
                    {pagePermissions.map((permission) => (
                      <label key={permission.key} className="permission-checkbox">
                        <input
                          type="checkbox"
                          checked={selectedPermissions.includes(permission.key)}
                          onChange={() => handlePermissionToggle(permission.key)}
                        />
                        <span>{permission.label}</span>
                      </label>
                    ))}
                  </div>
                </div>
              </Modal>

              <Modal
                isOpen={permissionsModalOpen}
                title={selectedUser ? `Manage Permissions: ${selectedUser.username}` : 'Manage Permissions'}
                onClose={closePermissionsModal}
                onSubmit={handleSavePermissions}
                submitText="Save Permissions"
                loading={permissionSaving}
                size="large"
              >
                <div className="permission-section">
                  <div className="permission-section-header">
                    <h3>Page Viewing Permissions</h3>
                    <p>Dashboard is always available. Update access for the current sidebar pages below.</p>
                  </div>

                  <div className="permission-checkbox-grid">
                    {pagePermissions.map((permission) => (
                      <label key={permission.key} className="permission-checkbox">
                        <input
                          type="checkbox"
                          checked={selectedPermissions.includes(permission.key)}
                          onChange={() => handlePermissionToggle(permission.key)}
                        />
                        <span>{permission.label}</span>
                      </label>
                    ))}
                  </div>
                </div>
              </Modal>
            </div>
          ) : (
            <div className="permission-section" style={{ marginTop: '8px' }}>
              <div className="permission-section-header">
                <h3>Accounts Access</h3>
                <p>You have permission to open this page, but account management controls are reserved for administrators.</p>
              </div>
            </div>
          )}
        </div>
      </DashboardLayout>
    </PermissionGuard>
  );
};

export default Accounts;
