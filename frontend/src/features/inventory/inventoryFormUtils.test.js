import {
  createInitialInventoryFormData,
  validateInventoryForm,
  buildInventoryPayload,
  buildInventoryFormData,
} from './inventoryFormUtils';

describe('inventoryFormUtils', () => {
  it('creates an empty form with safe defaults', () => {
    const formData = createInitialInventoryFormData();
    expect(formData).toEqual({
      brand: '',
      name: '',
      gender: '',
      sizingSystem: '',
      size: '',
      quantity: '',
      price: '',
      notes: '',
    });
  });

  it('flags every missing required field when adding an item', () => {
    const errors = validateInventoryForm(createInitialInventoryFormData());

    expect(errors.brand).toBeTruthy();
    expect(errors.name).toBeTruthy();
    expect(errors.quantity).toBeTruthy();
    expect(errors.price).toBeTruthy();
  });

  it('rejects negative quantity and non-positive price', () => {
    const errors = validateInventoryForm({
      brand: 'Nike',
      name: 'Air Zoom',
      size: '9',
      quantity: '-1',
      price: '0',
      notes: '',
    });

    expect(errors.quantity).toBeTruthy();
    expect(errors.price).toBeTruthy();
  });

  it('passes a valid add-item form', () => {
    const errors = validateInventoryForm({
      brand: 'Nike',
      name: 'Air Zoom',
      size: '9',
      quantity: '12',
      price: '2500',
      notes: '',
    });

    expect(errors).toEqual({});
  });

  it('builds the API payload with trimmed values and numeric types', () => {
    const payload = buildInventoryPayload({
      brand: '  Nike  ',
      name: ' Air Zoom ',
      gender: '  Men  ',
      sizingSystem: ' UK ',
      size: ' 9 ',
      quantity: '12',
      price: '2500.50',
      notes: '  restock soon  ',
    });

    expect(payload).toEqual({
      brand: 'Nike',
      name: 'Air Zoom',
      gender: 'Men',
      sizingSystem: 'UK',
      size: '9',
      quantity: 12,
      price: 2500.5,
      notes: 'restock soon',
    });
  });

  it('maps blank size and notes to null', () => {
    const payload = buildInventoryPayload({
      brand: 'Nike',
      name: 'Air Zoom',
      size: '   ',
      quantity: '1',
      price: '100',
      notes: '',
    });

    expect(payload.size).toBeNull();
    expect(payload.gender).toBeNull();
    expect(payload.sizingSystem).toBeNull();
    expect(payload.notes).toBeNull();
  });

  it('round-trips an inventory item back into form state', () => {
    const formData = buildInventoryFormData({
      brand: 'Nike',
      name: 'Air Zoom',
      gender: 'Men',
      sizingSystem: 'US',
      size: '9',
      quantity: 8,
      price: 2500,
      notes: 'restock soon',
    });

    expect(formData).toEqual({
      brand: 'Nike',
      name: 'Air Zoom',
      gender: 'Men',
      sizingSystem: 'US',
      size: '9',
      quantity: '8',
      price: '2500',
      notes: 'restock soon',
    });

    expect(validateInventoryForm(formData)).toEqual({});
  });
});
