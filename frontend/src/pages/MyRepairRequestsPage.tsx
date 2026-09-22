import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import { api, formatCurrency } from '../lib/api';
import type { PageMeta, RepairRequest } from '../lib/types';

const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Bảng điều khiển' },
  { to: '/yeu-cau-cua-toi', label: 'Yêu cầu của tôi' },
  { to: '/dang-yeu-cau', label: 'Đăng yêu cầu' }
];

const STATUS_FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'BIDDING_OPEN', label: 'Đang nhận giá' },
  { value: 'MATCHED_AWAITING_DEPOSIT', label: 'Chờ đặt cọc' },
  { value: 'IN_PROGRESS', label: 'Đang sửa' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'CANCELLED', label: 'Đã hủy' }
];

export default function MyRepairRequestsPage() {
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams({ page: String(page), limit: '10' });
    if (status) params.set('status', status);

    api.get<RepairRequest[]>(`/repair-requests/my?${params}`)
      .then((res) => {
        setRequests(res.data);
        if (res.meta) setMeta(res.meta);
        setError(null);
      })
      .catch(() => setError('Không tải được danh sách yêu cầu.'))
      .finally(() => setLoading(false));
  }, [page, status]);

  function handleFilterChange(newStatus: string) {
    setStatus(newStatus);
    setPage(1);
  }

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title="Yêu cầu của tôi"
      description="Các đơn sửa chữa bạn đã đăng."
    >
      {/* Filter tabs */}
      <div className="mb-6 flex flex-wrap gap-2">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => handleFilterChange(f.value)}
            className={`min-h-[36px] rounded-full border px-3 text-sm transition-colors ${
              status === f.value
                ? 'border-brand bg-brand text-white'
                : 'border-line bg-card hover:bg-surface'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {loading && <div className="h-40 animate-pulse rounded-xl border border-line bg-card" />}

      {!loading && requests.length === 0 && (
        <div className="rounded-xl border border-line bg-card p-8 text-center">
          <p className="text-ink-soft">Chưa có yêu cầu nào.</p>
          <Link to="/dang-yeu-cau" className="mt-2 inline-block text-sm font-medium text-brand-ink hover:underline">
            Đăng yêu cầu sửa chữa
          </Link>
        </div>
      )}

      {!loading && requests.length > 0 && (
        <div className="space-y-3">
          {requests.map((req) => (
            <Link
              key={req.id}
              to={`/yeu-cau-cua-toi/${req.id}`}
              className="block rounded-xl border border-line bg-card p-4 transition-colors hover:border-brand/40 hover:bg-brand/5"
            >
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium">{req.title}</h3>
                    <StatusChip status={req.status} />
                  </div>
                  <p className="mt-1 text-sm text-ink-soft line-clamp-1">{req.description}</p>
                  <div className="mt-2 flex flex-wrap gap-3 text-xs text-ink-soft">
                    {req.categoryName && <span>{req.categoryName}</span>}
                    {req.areaName && <span>• {req.areaName}</span>}
                    <span>• {new Date(req.createdAt).toLocaleDateString('vi-VN')}</span>
                    {req.quotationCount > 0 && <span>• {req.quotationCount} báo giá</span>}
                  </div>
                </div>
                {req.budgetRef > 0 && (
                  <span className="shrink-0 text-sm font-medium text-brand-ink">
                    {formatCurrency(req.budgetRef)}
                  </span>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}

      {meta && meta.totalPages > 1 && (
        <div className="mt-6">
          <Pagination meta={meta} onPageChange={setPage} disabled={loading} itemNoun="yêu cầu" />
        </div>
      )}
    </DashboardLayout>
  );
}
