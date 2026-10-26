import { describe, expect, it } from 'vitest';
import { API_BASE_URL, formatCurrency } from '../src/lib/api';
import { parseNumericUserId } from '../src/lib/types';

describe('API client & Utility helpers', () => {
  it('gọi đúng tiền tố đường dẫn /api/v1', () => {
    expect(API_BASE_URL).toBe('/api/v1');
  });

  it('định dạng tiền theo chuẩn Việt Nam', () => {
    expect(formatCurrency(250000)).toContain('250.000');
  });

  describe('parseNumericUserId', () => {
    it('chuyển đổi chuỗi dạng "usr_2" thành số nguyên 2', () => {
      expect(parseNumericUserId('usr_2')).toBe(2);
      expect(parseNumericUserId('usr_12345')).toBe(12345);
    });

    it('giữ nguyên giá trị số nguyên', () => {
      expect(parseNumericUserId(2)).toBe(2);
      expect(parseNumericUserId(99)).toBe(99);
    });

    it('trả về null nếu null hoặc undefined', () => {
      expect(parseNumericUserId(null)).toBeNull();
      expect(parseNumericUserId(undefined)).toBeNull();
    });
  });
});

