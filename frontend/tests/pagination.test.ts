import { describe, expect, it } from 'vitest';
import { buildPageList } from '../src/components/Pagination';

describe('Dãy số trang', () => {
  it('hiện đủ mọi trang khi tổng số trang còn ít', () => {
    expect(buildPageList(1, 5)).toEqual([1, 2, 3, 4, 5]);
    expect(buildPageList(4, 7)).toEqual([1, 2, 3, 4, 5, 6, 7]);
  });

  it('rút gọn phần cuối khi đang ở đầu danh sách', () => {
    expect(buildPageList(2, 20)).toEqual([1, 2, 3, 'gap', 20]);
  });

  it('rút gọn cả hai phía khi đang ở giữa', () => {
    expect(buildPageList(10, 20)).toEqual([1, 'gap', 9, 10, 11, 'gap', 20]);
  });

  it('rút gọn phần đầu khi đang ở cuối danh sách', () => {
    expect(buildPageList(19, 20)).toEqual([1, 'gap', 18, 19, 20]);
  });

  it('không sinh ra số trang nằm ngoài khoảng hợp lệ', () => {
    const pages = buildPageList(1, 8).filter((p): p is number => p !== 'gap');
    expect(Math.min(...pages)).toBe(1);
    expect(Math.max(...pages)).toBe(8);
  });
});
