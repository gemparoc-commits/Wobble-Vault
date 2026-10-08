export const createInitialInventoryFormData = () => ({
  brand: '',
  name: '',
  gender: '',
  sizingSystem: '',
  size: '',
  quantity: '',
  price: '',
  notes: '',
});

export const validateInventoryForm = (formData) => {
  const errors = {};

  if (!String(formData.brand || '').trim()) {
    errors.brand = 'Brand is required';
  }

  if (!String(formData.name || '').trim()) {
    errors.name = 'Shoe name is required';
  }

  const parsedQty = Number.parseInt(formData.quantity, 10);
  if (!Number.isFinite(parsedQty) || parsedQty < 0) {
    errors.quantity = 'Quantity cannot be less than zero';
  }

  const parsedPrice = Number.parseFloat(formData.price);
  if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
    errors.price = 'Price must be greater than zero';
  }

  return errors;
};

export const buildInventoryPayload = (formData) => ({
  brand: String(formData.brand).trim(),
  name: String(formData.name).trim(),
  gender: String(formData.gender || '').trim() || null,
  sizingSystem: String(formData.sizingSystem || '').trim() || null,
  size: String(formData.size || '').trim() || null,
  quantity: Number.parseInt(formData.quantity, 10),
  price: Number.parseFloat(formData.price),
  notes: String(formData.notes || '').trim() || null,
});

export const buildInventoryFormData = (item) => ({
  brand: item.brand || '',
  name: item.name || '',
  gender: item.gender || '',
  sizingSystem: item.sizingSystem || '',
  size: item.size || '',
  quantity: item.quantity != null ? String(item.quantity) : '',
  price: item.price != null ? String(item.price) : '',
  notes: item.notes || '',
});
