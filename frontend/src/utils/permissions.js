const normalizePermission = (value) => String(value || '').trim().toUpperCase();

const PERMISSION_ALIASES = {
  ORDERS: ['ORDERS'],
  SALES: ['SALES'],
  SALES_ARCHIVE: ['SALES_ARCHIVE'],
  ACCOUNTS: ['ACCOUNTS'],
  INVENTORY: ['INVENTORY'],
};

export const getPermissionAliases = (permission) => {
  const normalized = normalizePermission(permission);
  return PERMISSION_ALIASES[normalized] || [normalized];
};

export const hasPermission = (permissions, permission) => {
  const allowed = getPermissionAliases(permission);
  return (permissions || []).some((item) => allowed.includes(normalizePermission(item?.pageName)));
};

export const expandPermissions = (permissions) => {
  const normalized = new Set();

  (permissions || []).forEach((permission) => {
    const value = normalizePermission(permission);
    if (value) {
      normalized.add(value);
    }
  });

  return Array.from(normalized);
};

export const normalizePermissionName = normalizePermission;
