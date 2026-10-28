import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import TextField from '../components/TextField';
import { ApiError, api, formatCurrency } from '../lib/api';
import type { PageMeta, RepairRequest } from '../lib/types';

export const TECHNICIAN_NAV = [
  { to: '/tho', label: 'Bảng điều khiển' },
  { to: '/tho/lich-su', label: 'Việc đã hoàn thành' },
  { to: '/tho/ho-so', label: 'Hồ sơ của tôi' }
];

export default function TechnicianCompletedJobsPage() {
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchInput, setSearchInput] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');

  const loadCompletedRequests = useCallback(async (targetPage: number, search?: string) => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.append('page', String(targetPage));
      params.append('limit', '10');
      params.append('tab', 'COMPLETED');
      if (search && search.trim()) {
        params.append('search', search.trim());
      }
      const res = await api.get<RepairRequest[]>(`/repair-requests?${params.toString()}`);
      setRequests(res.data || []);
      if (res.meta) {
        setMeta(res.meta);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Không thể tải danh sách công việc đã hoàn thành.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCompletedRequests(page, appliedSearch);
  }, [loadCompletedRequests, page, appliedSearch]);

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setPage(1);
    setAppliedSearch(searchInput.trim());
  }

  function handleClearSearch() {
    setSearchInput('');
    setAppliedSearch('');
    setPage(1);
  }

  // Tổng doanh thu từ các đơn đã hiển thị
  const totalRevenue = requests.reduce((sum, r) => sum + (r.agreedPrice || r.budgetRef || 0), 0);

  return (
    <DashboardLayout
      nav={TECHNICIAN_NAV}
      title="Lịch sử việc đã hoàn thành"
      description="Quản lý danh sách các yêu cầu sửa chữa bạn đã hoàn tất xuất sắc và khách hàng đã nghiệm thu."
      actions={
        <Link to="/tho">
          <Button variant="secondary">← Về Bảng điều khiển</Button>
        </Link>
      }
    >
      <div className="space-y-6">
        {/* Thống kê nhanh */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Tổng số việc hoàn thành
            </span>
            <p className="font-display font-bold text-2xl text-emerald-600 mt-2">
              {meta ? meta.totalItems : requests.length} việc
            </p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Thực nhận về ví (90%)
            </span>
            <p className="font-display font-bold text-2xl text-emerald-600 mt-2">
              {formatCurrency(Math.round(totalRevenue * 0.9))}
            </p>
            <span className="text-[11px] text-slate-400 mt-0.5 block">Tổng giá trị: {formatCurrency(totalRevenue)} (đã trừ 10% phí sàn)</span>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Tỷ lệ hài lòng & bảo hành
            </span>
            <p className="font-display font-bold text-2xl text-slate-800 mt-2">
              100% đạt chuẩn
            </p>
          </div>
        </div>

        {/* Thanh tìm kiếm */}
        <Card>
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
            <div className="flex-1">
              <TextField
                label="Tìm kiếm theo mã đơn hoặc từ khóa"
                placeholder="Ví dụ: REQ-20261023... hoặc sửa máy lạnh, tivi..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
              />
            </div>
            <div className="flex gap-2">
              <Button variant="primary" type="submit">
                🔍 Tìm kiếm
              </Button>
              {appliedSearch && (
                <Button variant="secondary" type="button" onClick={handleClearSearch}>
                  Xóa lọc
                </Button>
              )}
            </div>
          </form>
        </Card>

        {/* Danh sách công việc */}
        <Card
          title={`Danh sách công việc đã hoàn thành (${meta ? meta.totalItems : requests.length})`}
          description={
            appliedSearch
              ? `Kết quả tìm kiếm cho từ khóa: "${appliedSearch}"`
              : 'Tất cả các đơn đã hoàn tất sửa chữa, bàn giao và nghiệm thu thành công.'
          }
        >
          {loading ? (
            <div className="py-12 text-center text-sm text-slate-500 animate-pulse">
              Đang tải danh sách công việc đã hoàn thành...
            </div>
          ) : error ? (
            <div className="py-8 text-center text-sm text-rose-600">
              {error}
            </div>
          ) : requests.length === 0 ? (
            <div className="py-12 text-center">
              <p className="text-4xl">🎉</p>
              <p className="mt-3 font-display font-semibold text-slate-900 text-lg">
                {appliedSearch ? 'Không tìm thấy công việc nào phù hợp' : 'Chưa có công việc nào hoàn thành'}
              </p>
              <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
                {appliedSearch
                  ? 'Hãy thử tìm kiếm với từ khóa khác hoặc xóa bộ lọc.'
                  : 'Hãy nhận và xử lý các yêu cầu sửa chữa trên Bảng điều khiển để hoàn tất công việc đầu tiên của bạn!'}
              </p>
              <div className="mt-4">
                <Link to="/tho">
                  <Button variant="primary">← Về Bảng điều khiển nhận việc</Button>
                </Link>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <ul className="divide-y divide-slate-100">
                {requests.map((request) => (
                  <li
                    key={request.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 py-4.5 first:pt-0 last:pb-0"
                  >
                    <div className="space-y-1.5 min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                          {request.requestCode || `#${request.id}`}
                        </span>
                        <StatusChip status={request.status} label={request.statusLabel || 'Hoàn thành'} />
                        {request.categoryName && (
                          <span className="text-xs text-slate-500 font-medium">
                            • {request.categoryName}
                          </span>
                        )}
                        <span className="text-xs text-slate-400">
                          {new Date(request.createdAt).toLocaleDateString('vi-VN')}
                        </span>
                      </div>
                      <Link
                        to={`/tho/yeu-cau/${request.id}`}
                        className="block font-bold text-slate-900 hover:text-brand transition-colors text-base"
                      >
                        {request.title}
                      </Link>
                      {request.description && (
                        <p className="text-sm text-slate-600 line-clamp-2">
                          {request.description}
                        </p>
                      )}
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 pt-1">
                        {request.customerName && (
                          <span>
                            Khách hàng: <strong className="text-slate-700">{request.customerName}</strong>
                          </span>
                        )}
                        <span>
                          Địa chỉ: <strong className="text-slate-700">{request.addressLine || request.address || '—'}</strong>
                        </span>
                        <span>
                          Giá đã thanh toán: <strong className="text-emerald-600 font-bold text-sm">{formatCurrency(request.agreedPrice || request.budgetRef)}</strong>
                        </span>
                      </div>
                    </div>
                    <div className="shrink-0 flex items-center gap-2 pt-2 sm:pt-0">
                      <Link to={`/tho/yeu-cau/${request.id}`}>
                        <Button variant="secondary" className="text-xs py-2 px-3.5">
                          Xem chi tiết việc →
                        </Button>
                      </Link>
                    </div>
                  </li>
                ))}
              </ul>

              {meta && meta.totalPages > 1 && (
                <div className="pt-4 border-t border-slate-100">
                  <Pagination
                    meta={meta}
                    onPageChange={(newPage) => setPage(newPage)}
                    itemNoun="yêu cầu"
                  />
                </div>
              )}
            </div>
          )}
        </Card>
      </div>
    </DashboardLayout>
  );
}
