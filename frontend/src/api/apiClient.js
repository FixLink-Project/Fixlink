/**
 * FixLink API Client
 */
export const API_BASE_URL = '/api/v1';

export async function fetchCategories() {
  try {
    const response = await fetch(`${API_BASE_URL}/categories`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    return data.data || [];
  } catch (error) {
    console.warn('Backend unavailable, using fallback categories', error);
    return [
      { id: 'cat_1', name: 'Điện dân dụng' },
      { id: 'cat_2', name: 'Hệ thống nước' },
      { id: 'cat_3', name: 'Điện lạnh' },
      { id: 'cat_4', name: 'Sơn & Chống thấm' }
    ];
  }
}

export function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}
