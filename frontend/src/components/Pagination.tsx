import type { PageMeta } from '../lib/types';

interface PaginationProps {
  meta: PageMeta;
  onPageChange: (page: number) => void;
  /** Tắt tạm thời khi đang tải trang mới. */
  disabled?: boolean;
  /** Danh từ số nhiều cho dòng đếm, ví dụ "người dùng", "danh mục". */
  itemNoun?: string;
  /** Cho phép chọn số lượng mục hiển thị mỗi trang. */
  pageSize?: number;
  onPageSizeChange?: (size: number) => void;
  pageSizeOptions?: number[];
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
  itemNoun = 'mục',
  pageSize,
  onPageSizeChange,
  pageSizeOptions = [5, 10, 20]
}: PaginationProps) {
  const { currentPage, limit, totalItems, totalPages, hasNext, hasPrevious } = meta;

  // Luôn hiển thị thanh điều hướng trang khi có dữ liệu
  const showControls = totalPages >= 1;

  const from = totalItems === 0 ? 0 : (currentPage - 1) * limit + 1;
  const to = Math.min(currentPage * limit, totalItems);

  const btnBase =
    'flex min-h-[40px] min-w-[40px] items-center justify-center rounded-xl border px-3 text-sm transition-all duration-200 disabled:cursor-not-allowed';

  return (
    <nav
      aria-label="Phân trang"
      className="flex flex-col gap-3 border-t border-slate-200/80 pt-4 sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="flex flex-wrap items-center gap-3">
        <p className="text-sm text-slate-500">
          {totalItems === 0
            ? `Không có ${itemNoun} nào`
            : `Hiển thị ${from}–${to} trong ${totalItems} ${itemNoun}`}
        </p>

        {onPageSizeChange && (
          <div className="flex items-center gap-1.5 text-xs text-slate-500">
            <span>•</span>
            <label htmlFor="page-size-select" className="sr-only">Số lượng mỗi trang</label>
            <select
              id="page-size-select"
              value={pageSize ?? limit}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              disabled={disabled}
              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700 hover:border-brand focus:outline-none focus:ring-1 focus:ring-brand shadow-2xs transition-colors"
            >
              {pageSizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt} / trang
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {showControls && (
        <ul className="flex flex-wrap items-center gap-1.5">
          <li>
            <button
              type="button"
              onClick={() => onPageChange(currentPage - 1)}
              disabled={disabled || !hasPrevious}
              aria-label="Trang trước"
              className={`${btnBase} border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:hover:bg-white shadow-xs`}
            >
              ←
            </button>
          </li>

          {buildPageList(currentPage, totalPages).map((page, index) =>
            page === 'gap' ? (
              <li
                key={`gap-${index}`}
                aria-hidden="true"
                className="flex min-h-[40px] items-center px-2 text-slate-400 font-bold"
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
                  className={`${btnBase} disabled:cursor-not-allowed ${
                    page === currentPage
                      ? 'border-brand bg-brand text-white shadow-xs font-bold'
                      : 'border-slate-200 bg-white hover:bg-slate-50 text-slate-700 shadow-xs font-semibold'
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
              className={`${btnBase} border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:hover:bg-white shadow-xs`}
            >
              →
            </button>
          </li>
        </ul>
      )}
    </nav>
  );
}
