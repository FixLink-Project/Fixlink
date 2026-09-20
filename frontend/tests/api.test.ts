import { describe, expect, it } from 'vitest';
import { API_BASE_URL, formatCurrency } from '../src/lib/api';

describe('API client', () => {
  it('gọi đúng tiền tố đường dẫn /api/v1', () => {
    expect(API_BASE_URL).toBe('/api/v1');
  });

  it('định dạng tiền theo chuẩn Việt Nam', () => {
    expect(formatCurrency(250000)).toContain('250.000');
  });
});
