import type { PageMeta } from '../lib/types';

interface PaginationProps {
  meta: PageMeta;
  onPageChange: (page: number) => void;
  /** Tắt tạm thời khi đang tải trang mới. */
  disabled?: boolean;
  /** Danh từ số nhiều cho dòng đếm, ví dụ "người dùng", "danh mục". */
  itemNoun?: string;
}

/**
 * Dãy số trang rút gọn: luôn giữ trang đầu, trang cuối, trang hiện tại và hai
 * trang kề, phần bị lược bỏ thay bằng dấu ba chấm.
 */
export function buildPageList(current: number, total: number): Array<number | 'gap'> {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  const pages = new Set<number>([1, total, current]);
  if (current - 1 > 1) pages.add(current - 1);
  if (current + 1 < total) pages.add(current + 1);
  if (current <= 3) {
    pages.add(2);
    pages.add(3);
  }
  if (current >= total - 2) {
    pages.add(total - 1);
    pages.add(total - 2);
  }

  const sorted = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b);

  const result: Array<number | 'gap'> = [];
  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1] > 1) result.push('gap');
    result.push(page);
  });
  return result;
}

export default function Pagination({
  meta,
  onPageChange,
  disabled = false,
  itemNoun = 'mục'
}: PaginationProps) {
  const { currentPage, limit, totalItems, totalPages, hasNext, hasPrevious } = meta;

  // Một trang thì không cần thanh điều hướng, nhưng vẫn cho biết tổng số.
  const showControls = totalPages > 1;

  const from = totalItems === 0 ? 0 : (currentPage - 1) * limit + 1;
  const to = Math.min(currentPage * limit, totalItems);

  return (
    <nav
      aria-label="Phân trang"
      className="flex flex-col gap-3 border-t border-line pt-4 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="text-sm text-ink-soft">
        {totalItems === 0
          ? `Không có ${itemNoun} nào`
          : `Hiển thị ${from}–${to} trong ${totalItems} ${itemNoun}`}
      </p>

      {showControls && (
        <ul className="flex flex-wrap items-center gap-1">
          <li>
            <button
              type="button"
              onClick={() => onPageChange(currentPage - 1)}
              disabled={disabled || !hasPrevious}
              aria-label="Trang trước"
              className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-lg border border-line bg-card px-3 text-sm transition-colors hover:bg-surface disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:bg-card"
            >
              Trước
            </button>
          </li>

          {buildPageList(currentPage, totalPages).map((page, index) =>
            page === 'gap' ? (
              <li
                key={`gap-${index}`}
                aria-hidden="true"
                className="flex min-h-[44px] items-center px-1 text-ink-soft"
              >
                …
              </li>
            ) : (
              <li key={page}>
                <button
                  type="button"
                  onClick={() => onPageChange(page)}
                  disabled={disabled}
                  aria-label={`Trang ${page}`}
                  aria-current={page === currentPage ? 'page' : undefined}
                  className={`flex min-h-[44px] min-w-[44px] items-center justify-center rounded-lg border px-3 text-sm transition-colors disabled:cursor-not-allowed ${
                    page === currentPage
                      ? 'border-brand bg-brand text-white'
                      : 'border-line bg-card hover:bg-surface'
                  }`}
                >
                  {page}
                </button>
              </li>
            )
          )}

          <li>
            <button
              type="button"
              onClick={() => onPageChange(currentPage + 1)}
              disabled={disabled || !hasNext}
              aria-label="Trang sau"
              className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-lg border border-line bg-card px-3 text-sm transition-colors hover:bg-surface disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:bg-card"
            >
              Sau
            </button>
          </li>
        </ul>
      )}
    </nav>
  );
}
