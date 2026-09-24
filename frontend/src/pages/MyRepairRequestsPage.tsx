import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import { api, formatCurrency } from '../lib/api';
import type { PageMeta, RepairRequest, RequestTabCode } from '../lib/types';

const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Tổng quan' },
  { to: '/yeu-cau-cua-toi', label: 'Yêu cầu của tôi' },
  { to: '/dang-yeu-cau', label: 'Đăng yêu cầu' }
];

const TAB_FILTERS: Array<{ code: RequestTabCode; label: string }> = [
  { code: 'ALL', label: 'Tất cả' },
  { code: 'AWAITING_QUOTE', label: 'Chờ báo giá' },
  { code: 'IN_PROGRESS', label: 'Đang thực hiện' },
  { code: 'COMPLETED', label: 'Hoàn thành' },
  { code: 'CANCELLED', label: 'Đã hủy' }
];

export default function MyRepairRequestsPage() {
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [page, setPage] = useState(1);
  const [tab, setTab] = useState<RequestTabCode>('ALL');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearch(search.trim());
      setPage(1);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    const params = new URLSearchParams({ page: String(page), limit: '10', tab });
    if (debouncedSearch) params.set('search', debouncedSearch);

    api.get<RepairRequest[]>(`/repair-requests?${params.toString()}`)
      .then((res) => {
        if (!active) return;
        setRequests(res.data ?? []);
        setMeta(res.meta ?? null);
      })
      .catch(() => {
        if (!active) return;
        setRequests([]);
        setMeta(null);
        setError('Không thể tải danh sách yêu cầu sửa chữa. Vui lòng thử lại.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, [page, tab, debouncedSearch, retry]);

  const visibleTabLabel = TAB_FILTERS.find((filter) => filter.code === tab)?.label.toLowerCase();

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title="Yêu cầu sửa chữa của tôi"
      description="Theo dõi trạng thái, báo giá và tiến độ các yêu cầu sửa chữa thiết bị của bạn."
      actions={(
        <Link to="/dang-yeu-cau" className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-xs transition-colors hover:bg-brand-strong">
          <span aria-hidden="true">＋</span> Đăng yêu cầu mới
        </Link>
      )}
    >
      <section aria-label="Tìm kiếm yêu cầu" className="mb-6">
        <label htmlFor="request-search" className="sr-only">Tìm yêu cầu sửa chữa</label>
        <div className="relative max-w-md">
          <span aria-hidden="true" className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">⌕</span>
          <input
            id="request-search"
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Tìm theo tiêu đề, mã yêu cầu hoặc địa chỉ"
            className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-4 text-sm shadow-xs transition-all placeholder:text-slate-400 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
          />
        </div>
      </section>

      <div className="mb-6 border-b border-slate-200">
        <div role="tablist" aria-label="Lọc yêu cầu theo trạng thái" className="flex gap-2 overflow-x-auto pb-px">
          {TAB_FILTERS.map((filter) => (
            <button
              key={filter.code}
              id={`request-tab-${filter.code}`}
              type="button"
              role="tab"
              aria-selected={tab === filter.code}
              onClick={() => { setTab(filter.code); setPage(1); }}
              className={`min-h-10 whitespace-nowrap border-b-2 px-4 text-sm font-semibold transition-colors ${tab === filter.code ? 'border-brand bg-blue-50/50 text-brand' : 'border-transparent text-slate-600 hover:border-slate-300 hover:text-slate-900'}`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="space-y-3" role="alert">
          <Alert tone="error">{error}</Alert>
          <button type="button" onClick={() => setRetry((current) => current + 1)} className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            Thử tải lại
          </button>
        </div>
      )}

      {loading && (
        <div className="space-y-3" aria-label="Đang tải yêu cầu" aria-busy="true">
          {[1, 2, 3].map((item) => <div key={item} className="h-28 animate-pulse rounded-2xl border border-slate-200 bg-white p-4" />)}
        </div>
      )}

      {!loading && !error && requests.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center shadow-xs sm:p-12">
          <div aria-hidden="true" className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-2xl text-brand">⌕</div>
          <h2 className="mt-4 font-display text-base font-bold text-slate-900">
            {debouncedSearch ? 'Không tìm thấy yêu cầu phù hợp' : `Chưa có yêu cầu nào trong mục “${visibleTabLabel}”`}
          </h2>
          <p className="mx-auto mt-1 max-w-md text-sm text-slate-500">
            {debouncedSearch ? 'Thử từ khóa khác hoặc xóa nội dung tìm kiếm.' : 'Các yêu cầu sửa chữa của bạn sẽ hiển thị tại đây.'}
          </p>
          {debouncedSearch ? (
            <button type="button" onClick={() => setSearch('')} className="mt-5 rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">Xóa tìm kiếm</button>
          ) : (
            <Link to="/dang-yeu-cau" className="mt-5 inline-flex min-h-11 items-center rounded-xl bg-brand px-5 py-2.5 text-sm font-semibold text-white shadow-xs transition-colors hover:bg-brand-strong">Đăng yêu cầu sửa chữa</Link>
          )}
        </div>
      )}

      {!loading && !error && requests.length > 0 && (
        <div className="space-y-3">
          {requests.map((request) => {
            const mediaCount = request.media?.length || request.mediaUrls?.length || 0;
            const price = request.agreedPrice > 0 ? request.agreedPrice : request.budgetRef;

            return (
              <Link
                key={request.id}
                to={`/yeu-cau-cua-toi/${request.id}`}
                aria-label={`${request.requestCode}: ${request.title}, ${request.statusLabel ?? request.status}`}
                className="group block rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition-all hover:border-brand/40 hover:shadow-card focus:outline-none focus:ring-2 focus:ring-brand/40"
              >
                <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                  <div className="flex min-w-0 items-start gap-4">
                    <div aria-hidden="true" className="hidden h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-xl text-brand transition-colors group-hover:bg-brand group-hover:text-white xs:flex">⌂</div>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2.5">
                        <span className="rounded border border-slate-200 bg-slate-100 px-2 py-0.5 font-mono text-xs font-semibold text-slate-600">{request.requestCode}</span>
                        <h2 className="text-base font-bold text-slate-900 transition-colors group-hover:text-brand">{request.title}</h2>
                        <StatusChip status={request.status} label={request.statusLabel} />
                      </div>
                      <p className="mt-1 line-clamp-1 max-w-xl text-sm text-slate-500">{request.description}</p>
                      <div className="mt-2.5 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                        {request.categoryName && <span className="rounded-md bg-slate-100 px-2 py-0.5 font-medium text-slate-700">{request.categoryName}</span>}
                        {(request.address || request.addressLine) && <span className="max-w-xs truncate">⌖ {request.address || request.addressLine}</span>}
                        <span>{new Date(request.createdAt).toLocaleDateString('vi-VN')}</span>
                        {mediaCount > 0 && <span className="rounded-md border border-indigo-100 bg-indigo-50 px-2 py-0.5 font-medium text-indigo-700">{mediaCount} ảnh</span>}
                        {request.quotationCount > 0 ? (
                          <span className="rounded-full border border-blue-200 bg-blue-50 px-2.5 py-0.5 font-bold text-brand">{request.quotationCount} báo giá</span>
                        ) : <span className="text-slate-400">Chưa có báo giá</span>}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center justify-between border-t border-slate-100 pt-3 sm:flex-col sm:items-end sm:justify-center sm:border-0 sm:pt-0">
                    <div className="text-left sm:text-right">
                      <div className="text-xs text-slate-400">{request.agreedPrice > 0 ? 'Giá đã thống nhất' : request.budgetRef > 0 ? 'Ngân sách dự kiến' : 'Chi phí'}</div>
                      <div className="text-base font-bold text-slate-900">{price > 0 ? formatCurrency(price) : 'Thỏa thuận'}</div>
                    </div>
                    <span aria-hidden="true" className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-brand transition-transform group-hover:translate-x-1">Chi tiết →</span>
                  </div>
                </div>
              </Link>
            );
          })}

          {meta && meta.totalPages > 1 && (
            <div className="pt-4">
              <Pagination meta={meta} onPageChange={setPage} disabled={loading} itemNoun="yêu cầu" />
            </div>
          )}
        </div>
      )}
    </DashboardLayout>
  );
}
