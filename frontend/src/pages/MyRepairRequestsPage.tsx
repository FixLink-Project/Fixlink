import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import { api, formatCurrency } from '../lib/api';
import type { PageMeta, RepairRequest, RequestTabCode } from '../lib/types';

const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Bảng điều khiển' },
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

  // Debounce search input by 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search.trim());
      setPage(1);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams({
      page: String(page),
      limit: '10',
      tab: tab
    });
    if (debouncedSearch) {
      params.set('search', debouncedSearch);
    }

    api.get<RepairRequest[]>(`/repair-requests?${params}`)
      .then((res) => {
        setRequests(res.data || []);
        if (res.meta) setMeta(res.meta);
        setError(null);
      })
      .catch(() => setError('Không tải được danh sách yêu cầu sửa chữa.'))
      .finally(() => setLoading(false));
  }, [page, tab, debouncedSearch]);

  function handleTabChange(newTab: RequestTabCode) {
    setTab(newTab);
    setPage(1);
  }

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title="Yêu cầu của tôi"
      description="Quản lý và theo dõi tiến độ các yêu cầu sửa chữa thiết bị điện tử."
    >
      {/* Top Bar: Search & Action */}
      <div className="mb-6 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        {/* Search Input */}
        <div className="relative flex-1 max-w-md">
          <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400 text-sm">
            🔍
          </span>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm theo tiêu đề, mã đơn hoặc địa chỉ..."
            className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand/30 focus:border-brand shadow-xs transition-all"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch('')}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 text-xs font-semibold"
            >
              ✕
            </button>
          )}
        </div>

        <Link
          to="/dang-yeu-cau"
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-brand hover:bg-brand-strong text-white px-5 py-2.5 text-sm font-semibold shadow-xs transition-all whitespace-nowrap"
        >
          <span>＋ Đăng yêu cầu mới</span>
        </Link>
      </div>

      {/* 5-Tab Bar */}
      <div className="mb-6 border-b border-slate-200">
        <div className="flex gap-2 overflow-x-auto pb-px scrollbar-none">
          {TAB_FILTERS.map((f) => {
            const isActive = tab === f.code;
            return (
              <button
                key={f.code}
                type="button"
                onClick={() => handleTabChange(f.code)}
                className={`min-h-[40px] px-4 text-xs sm:text-sm font-semibold transition-all border-b-2 whitespace-nowrap ${
                  isActive
                    ? 'border-brand text-brand font-bold bg-blue-50/50 rounded-t-lg'
                    : 'border-transparent text-slate-600 hover:text-slate-900 hover:border-slate-300'
                }`}
              >
                {f.label}
              </button>
            );
          })}
        </div>
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {loading && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-28 animate-pulse rounded-2xl border border-slate-200 bg-white p-4" />
          ))}
        </div>
      )}

      {!loading && requests.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center shadow-xs">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-2xl text-brand">
            📦
          </div>
          <h3 className="mt-4 font-display text-base font-bold text-slate-900">
            {debouncedSearch ? 'Không tìm thấy yêu cầu nào phù hợp' : 'Không có yêu cầu nào trong tab này'}
          </h3>
          <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
            {debouncedSearch
              ? `Thử tìm kiếm với từ khóa khác hoặc xóa bộ lọc tìm kiếm.`
              : `Các yêu cầu sửa chữa thuộc trạng thái này sẽ xuất hiện tại đây khi có phát sinh.`}
          </p>
          {!debouncedSearch && (
            <Link
              to="/dang-yeu-cau"
              className="mt-5 inline-flex items-center gap-2 rounded-xl bg-brand hover:bg-brand-strong text-white px-5 py-2.5 text-sm font-semibold shadow-xs transition-all"
            >
              <span>Đăng yêu cầu ngay</span>
              <span>→</span>
            </Link>
          )}
        </div>
      )}

      {!loading && requests.length > 0 && (
        <div className="space-y-3">
          {requests.map((req) => {
            const mediaCount = (req.media && req.media.length > 0)
              ? req.media.length
              : (req.mediaUrls && req.mediaUrls.length > 0 ? req.mediaUrls.length : 0);

            return (
              <Link
                key={req.id}
                to={`/yeu-cau-cua-toi/${req.id}`}
                className="group block rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition-all duration-200 hover:border-brand/40 hover:shadow-card"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className="hidden xs:flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-50 border border-blue-100 text-brand text-xl group-hover:bg-brand group-hover:text-white transition-colors">
                      ⚡
                    </div>
                    <div>
                      <div className="flex flex-wrap items-center gap-2.5">
                        <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">
                          {req.requestCode}
                        </span>
                        <h3 className="font-bold text-slate-900 group-hover:text-brand transition-colors text-base">
                          {req.title}
                        </h3>
                        <StatusChip status={req.status} label={req.statusLabel} />
                      </div>

                      <p className="mt-1 text-sm text-slate-500 line-clamp-1 max-w-xl">
                        {req.description}
                      </p>

                      <div className="mt-2.5 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                        {req.categoryName && (
                          <span className="rounded-md bg-slate-100 px-2 py-0.5 font-medium text-slate-700">
                            {req.categoryName}
                          </span>
                        )}
                        {(req.address || req.addressLine) && (
                          <span className="flex items-center gap-1 text-slate-600 max-w-xs truncate">
                            <span>📍</span> {req.address || req.addressLine}
                          </span>
                        )}
                        <span>• {new Date(req.createdAt).toLocaleDateString('vi-VN')}</span>

                        {mediaCount > 0 && (
                          <span className="rounded-md bg-indigo-50 px-2 py-0.5 font-medium text-indigo-700 border border-indigo-100 flex items-center gap-1">
                            <span>📷</span> {mediaCount} ảnh
                          </span>
                        )}

                        {req.quotationCount > 0 ? (
                          <span className="rounded-full bg-blue-50 px-2.5 py-0.5 font-bold text-brand border border-blue-200">
                            {req.quotationCount} báo giá từ thợ
                          </span>
                        ) : (
                          <span className="text-slate-400">• Đang chờ thợ báo giá</span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
                    <div className="text-left sm:text-right">
                      <div className="text-xs text-slate-400">
                        {req.agreedPrice > 0 ? 'Giá chốt:' : req.budgetRef > 0 ? 'Ngân sách dự kiến:' : 'Chi phí:'}
                      </div>
                      <div className="text-base font-bold text-slate-900">
                        {req.agreedPrice > 0
                          ? formatCurrency(req.agreedPrice)
                          : req.budgetRef > 0
                          ? formatCurrency(req.budgetRef)
                          : 'Thương lượng'}
                      </div>
                    </div>
                    <span className="mt-1 text-xs font-semibold text-brand group-hover:translate-x-1 transition-transform inline-flex items-center gap-1">
                      Chi tiết <span>→</span>
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}

          {/* Numbered Pagination */}
          {meta && meta.totalPages > 1 && (
            <div className="pt-4">
              <Pagination
                meta={meta}
                onPageChange={(newPage) => setPage(newPage)}
                disabled={loading}
                itemNoun="yêu cầu"
              />
            </div>
          )}
        </div>
      )}
    </DashboardLayout>
  );
}
