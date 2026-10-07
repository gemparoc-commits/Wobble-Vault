import {
  STATUS_ACTIVE,
  STATUS_ARCHIVED,
  STATUS_CANCELLED,
  createEmptyItem,
  createInitialFormData,
  getItemSubtotal,
  getTotals,
  buildRequestFromFormData,
  buildFormDataFromOrder,
} from './orderFormUtils';

describe('orderFormUtils', () => {
  const item = (unitPrice, quantity) => ({
    inventoryId: 'inv-1',
    productName: 'Air Zoom',
    size: '9',
    unitPrice,
    quantity,
  });

  describe('getTotals', () => {
    it('computes subtotal, total, and balance from items', () => {
      const totals = getTotals({
        items: [item('2500', '2'), item('1000', '1')],
        discount: '500',
        payment: '3000',
      });

      expect(totals.subtotal).toBe(6000);
      expect(totals.discount).toBe(500);
      expect(totals.total).toBe(5500);
      expect(totals.payment).toBe(3000);
      expect(totals.balance).toBe(2500);
    });

    it('never returns a negative total or balance', () => {
      const totals = getTotals({
        items: [item('100', '1')],
        discount: '500',
        payment: '1000',
      });

      expect(totals.total).toBe(0);
      expect(totals.balance).toBe(0);
    });

    it('treats missing values as zero', () => {
      const totals = getTotals({ items: [] });
      expect(totals.subtotal).toBe(0);
      expect(totals.total).toBe(0);
      expect(totals.balance).toBe(0);
    });
  });

  describe('getItemSubtotal', () => {
    it('multiplies unit price by quantity', () => {
      expect(getItemSubtotal(item('1234.50', '3'))).toBeCloseTo(3703.5);
    });

    it('returns zero for invalid values', () => {
      expect(getItemSubtotal({})).toBe(0);
    });
  });

  describe('buildRequestFromFormData', () => {
    it('sends computed price, payment, and status', () => {
      const formData = {
        ...createInitialFormData(),
        customerName: '  Juan Cruz  ',
        items: [item('2500', '2')],
        discount: '0',
        payment: '5000',
        notes: '  rush  ',
      };

      const payload = buildRequestFromFormData(formData, STATUS_ACTIVE);

      expect(payload.customerName).toBe('Juan Cruz');
      expect(payload.price).toBe(5000);
      expect(payload.payment).toBe(5000);
      expect(payload.status).toBe('ACTIVE');
      expect(payload.notes).toBe('rush');
      expect(payload.items).toHaveLength(1);
      expect(payload.items[0].inventoryId).toBe('inv-1');
      expect(payload.items[0].unitPrice).toBe(2500);
      expect(payload.items[0].quantity).toBe(2);
    });

    it('keeps status transitions as given for archive and cancel', () => {
      const formData = { ...createInitialFormData(), items: [item('100', '1')] };

      expect(buildRequestFromFormData(formData, STATUS_ARCHIVED).status).toBe('ARCHIVED');
      expect(buildRequestFromFormData(formData, STATUS_CANCELLED).status).toBe('CANCELLED');
    });

    it('maps empty customer name to null', () => {
      const formData = { ...createInitialFormData(), items: [item('100', '1')], customerName: '   ' };
      expect(buildRequestFromFormData(formData, STATUS_ACTIVE).customerName).toBeNull();
    });
  });

  describe('buildFormDataFromOrder', () => {
    it('round-trips an order back into form state', () => {
      const order = {
        customerName: 'Juan Cruz',
        items: [
          { inventoryId: 'inv-1', productName: 'Air Zoom', size: '9', unitPrice: 2500, quantity: 2 },
        ],
        discount: 500,
        payment: 5000,
        paymentMethod: 'gcash',
        shop: 'online',
        orderDate: '2026-10-07T00:00:00',
        notes: 'rush',
      };

      const formData = buildFormDataFromOrder(order);

      expect(formData.customerName).toBe('Juan Cruz');
      expect(formData.items[0].unitPrice).toBe('2500');
      expect(formData.items[0].quantity).toBe('2');
      expect(formData.discount).toBe('500');
      expect(formData.payment).toBe('5000');
      expect(formData.paymentMethod).toBe('gcash');
      expect(formData.shop).toBe('online');
      expect(formData.orderDate).toBe('2026-10-07');
      expect(formData.notes).toBe('rush');

      const payload = buildRequestFromFormData(formData, STATUS_ACTIVE);
      expect(payload.price).toBe(getTotals(formData).total);
    });
  });

  describe('createInitialFormData', () => {
    it('starts with one empty product row and safe defaults', () => {
      const formData = createInitialFormData();
      expect(formData.items).toHaveLength(1);
      expect(formData.items[0]).toEqual(createEmptyItem());
      expect(formData.shop).toBe('store');
      expect(formData.paymentMethod).toBe('cash');
      expect(formData.discount).toBe('0');
      expect(formData.payment).toBe('0');
    });
  });
});
