export const STATUS_ACTIVE = 'ACTIVE';
export const STATUS_ARCHIVED = 'ARCHIVED';
export const STATUS_CANCELLED = 'CANCELLED';

export const ORDER_TABS = [
  { key: STATUS_ACTIVE, label: 'All' },
  { key: STATUS_ARCHIVED, label: 'Archived' },
  { key: STATUS_CANCELLED, label: 'Cancelled' },
];

export const PAYMENT_OPTIONS = [
  { value: 'cash', label: 'Cash' },
  { value: 'gcash', label: 'Gcash' },
];

export const SHOP_OPTIONS = [
  { value: 'store', label: 'Wobble Store' },
  { value: 'online', label: 'FB Page' },
];

export const createEmptyItem = () => ({
  inventoryId: '',
  productName: '',
  size: '',
  unitPrice: '',
  quantity: '1',
});

export const createInitialFormData = () => ({
  customerName: '',
  items: [createEmptyItem()],
  discount: '0',
  payment: '0',
  paymentMethod: 'cash',
  shop: 'store',
  orderDate: new Date().toISOString().split('T')[0],
  notes: '',
});

export const getItemSubtotal = (item) =>
  (Number(item.unitPrice) || 0) * (Number(item.quantity) || 0);

export const getTotals = (formData) => {
  const subtotal = (formData.items || []).reduce((sum, item) => sum + getItemSubtotal(item), 0);
  const discount = Number(formData.discount) || 0;
  const total = Math.max(subtotal - discount, 0);
  const payment = Number(formData.payment) || 0;
  const balance = Math.max(total - payment, 0);
  return { subtotal, discount, total, payment, balance };
};

export const buildRequestFromFormData = (formData, status) => ({
  customerName: formData.customerName.trim() || null,
  items: formData.items.map((item) => ({
    inventoryId: item.inventoryId,
    productName: item.productName,
    size: item.size || null,
    unitPrice: Number(item.unitPrice),
    quantity: Number(item.quantity),
  })),
  discount: Number(formData.discount) || 0,
  price: getTotals(formData).total,
  payment: Number(formData.payment) || 0,
  paymentMethod: formData.paymentMethod,
  shop: formData.shop,
  orderDate: formData.orderDate,
  notes: formData.notes.trim() || null,
  status,
});

export const buildFormDataFromOrder = (order) => ({
  customerName: order.customerName || '',
  items: (order.items || []).map((item) => ({
    inventoryId: item.inventoryId || '',
    productName: item.productName || '',
    size: item.size || '',
    unitPrice: item.unitPrice != null ? String(item.unitPrice) : '',
    quantity: item.quantity != null ? String(item.quantity) : '1',
  })),
  discount: order.discount != null ? String(order.discount) : '0',
  payment: order.payment != null ? String(order.payment) : '0',
  paymentMethod: order.paymentMethod || 'cash',
  shop: order.shop || 'store',
  orderDate: order.orderDate ? String(order.orderDate).slice(0, 10) : new Date().toISOString().split('T')[0],
  notes: order.notes || '',
});
