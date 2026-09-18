import { describe, it, expect } from 'vitest';
import { formatCurrency, API_BASE_URL } from '../src/api/apiClient.js';

describe('Frontend API Client & Utilities', () => {
  it('should have correct API base URL', () => {
    expect(API_BASE_URL).toBe('/api/v1');
  });

  it('should format VND currency correctly', () => {
    const formatted = formatCurrency(250000);
    expect(formatted).toContain('250.000');
  });
});
