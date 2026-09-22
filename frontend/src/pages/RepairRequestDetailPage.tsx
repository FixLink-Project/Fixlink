import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import StatusChip from '../components/StatusChip';
import { ApiError, api, formatCurrency } from '../lib/api';
import type { AcceptQuotationResult, Quotation, RepairRequestDetail } from '../lib/types';

const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Bảng điều khiển' },
  { to: '/yeu-cau-cua-toi', label: 'Yêu cầu của tôi' },
  { to: '/dang-yeu-cau', label: 'Đăng yêu cầu' }
];

export default function RepairRequestDetailPage() {
  const { id } = useParams<{ id: string }>();

  const [detail, setDetail] = useState<RepairRequestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [accepting, setAccepting] = useState<number | null>(null);
  const [acceptError, setAcceptError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [showCancelForm, setShowCancelForm] = useState(false);

  useEffect(() => {
    api.get<RepairRequestDetail>(`/repair-requests/${id}`)
      .then((res) => {
        setDetail(res.data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Không tải được chi tiết yêu cầu.');
      })
      .finally(() => setLoading(false));
  }, [id]);

  async function handleAccept(quotationId: number, techName: string | null, totalPrice: number) {
    const deposit = Math.ceil(totalPrice * 0.3);
    const confirmed = window.confirm(
      `Chọn ${techName ?? 'thợ này'}?\n\nGiá chốt: ${formatCurrency(totalPrice)}\nTiền cọc 30%: ${formatCurrency(deposit)}\n\nSau khi chọn, các báo giá khác sẽ tự động bị từ chối.`
    );
    if (!confirmed) return;

    setAccepting(quotationId);
    setAcceptError(null);
    try {
      await api.post<AcceptQuotationResult>(`/repair-requests/${id}/quotations/${quotationId}/accept`);
      const res = await api.get<RepairRequestDetail>(`/repair-requests/${id}`);
      setDetail(res.data);
    } catch (err) {
      setAcceptError(err instanceof ApiError ? err.message : 'Lỗi khi chọn thợ.');
    } finally {
      setAccepting(null);
    }
  }

  async function handleCancel() {
    if (!cancelReason.trim()) return;
    setCancelling(true);
    try {
      await api.post(`/repair-requests/${id}/cancel`, { reason: cancelReason.trim() });
      const res = await api.get<RepairRequestDetail>(`/repair-requests/${id}`);
      setDetail(res.data);
      setShowCancelForm(false);
    } catch (err) {
      setAcceptError(err instanceof ApiError ? err.message : 'Lỗi khi hủy đơn.');
    } finally {
      setCancelling(false);
    }
  }

  if (loading) {
    return (
      <DashboardLayout nav={CUSTOMER_NAV} title="Chi tiết yêu cầu">
        <div className="h-80 animate-pulse rounded-xl border border-line bg-card" />
      </DashboardLayout>
    );
  }

  if (error || !detail) {
    return (
      <DashboardLayout nav={CUSTOMER_NAV} title="Chi tiết yêu cầu">
        <Alert tone="error">{error ?? 'Không tìm thấy yêu cầu.'}</Alert>
      </DashboardLayout>
    );
  }

  const req = detail.request;
  const canCancel = !['IN_PROGRESS', 'AWAITING_ACCEPTANCE', 'COMPLETED', 'CANCELLED'].includes(req.status);

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title={req.title}
      description={<span className="font-mono text-xs text-ink-soft">{req.requestCode}</span>}
      actions={<StatusChip status={req.status} />}
    >
      {acceptError && (
        <div className="mb-5">
          <Alert tone="error">{acceptError}</Alert>
        </div>
      )}

      <div className="space-y-6">
        {/* Thông tin đơn */}
        <Card title="Thông tin yêu cầu">
          <dl className="divide-y divide-line text-sm">
            <Row label="Danh mục" value={req.categoryName ?? '—'} />
            <Row label="Khu vực" value={req.areaName ?? '—'} />
            <Row label="Địa chỉ" value={req.addressLine} />
            <Row label="Mô tả" value={req.description} />
            {req.budgetRef > 0 && <Row label="Ngân sách" value={formatCurrency(req.budgetRef)} />}
            {req.biddingDeadline && (
              <Row label="Hạn nhận giá" value={new Date(req.biddingDeadline).toLocaleString('vi-VN')} />
            )}
            {req.agreedPrice > 0 && <Row label="Giá chốt" value={formatCurrency(req.agreedPrice)} />}
            {req.depositAmount > 0 && <Row label="Tiền cọc" value={formatCurrency(req.depositAmount)} />}
            {req.cancelReason && <Row label="Lý do hủy" value={req.cancelReason} />}
          </dl>
          {req.mediaUrls.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-2">
              {req.mediaUrls.map((url, i) => (
                <img key={i} src={url} alt={`Ảnh ${i + 1}`} className="h-24 w-24 rounded-lg border border-line object-cover" />
              ))}
            </div>
          )}
        </Card>

        {/* Báo giá */}
        {detail.quotations.length > 0 && (
          <Card title={`Báo giá (${detail.quotations.length})`}>
            <div className="grid gap-4 sm:grid-cols-2">
              {detail.quotations.map((q) => (
                <QuotationCard
                  key={q.id}
                  quotation={q}
                  canAccept={req.status === 'BIDDING_OPEN' && q.status === 'PENDING'}
                  accepting={accepting === q.id}
                  onAccept={() => handleAccept(q.id, q.technicianName, q.totalPrice)}
                />
              ))}
            </div>
          </Card>
        )}

        {detail.quotations.length === 0 && req.status === 'BIDDING_OPEN' && (
          <Card title="Báo giá">
            <p className="text-ink-soft">Chưa có thợ nào gửi báo giá. Hãy chờ trong vài giờ.</p>
          </Card>
        )}

        {/* Timeline */}
        {detail.workProgress.length > 0 && (
          <Card title="Tiến trình">
            <ol className="relative border-l-2 border-line pl-6">
              {detail.workProgress.map((wp) => (
                <li key={wp.id} className="mb-4 last:mb-0">
                  <div className="absolute -left-[9px] h-4 w-4 rounded-full border-2 border-brand bg-card" />
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusChip status={wp.toStatus} />
                    <span className="text-xs text-ink-soft">
                      {new Date(wp.createdAt).toLocaleString('vi-VN')}
                    </span>
                  </div>
                  {wp.note && <p className="mt-1 text-sm text-ink-soft">{wp.note}</p>}
                </li>
              ))}
            </ol>
          </Card>
        )}

        {/* Hủy đơn */}
        {canCancel && !showCancelForm && (
          <div className="flex justify-end">
            <Button variant="secondary" onClick={() => setShowCancelForm(true)}>
              Hủy yêu cầu
            </Button>
          </div>
        )}
        {showCancelForm && (
          <Card title="Hủy yêu cầu">
            <div className="max-w-md space-y-3">
              <textarea
                placeholder="Lý do hủy..."
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                rows={3}
                className="w-full rounded-lg border border-line bg-card px-3 py-2 text-sm"
              />
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setShowCancelForm(false)}>
                  Thôi
                </Button>
                <Button
                  onClick={handleCancel}
                  loading={cancelling}
                  disabled={!cancelReason.trim()}
                >
                  Xác nhận hủy
                </Button>
              </div>
            </div>
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 py-3 sm:flex-row sm:gap-4">
      <dt className="w-36 shrink-0 font-medium text-ink-soft">{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function QuotationCard({
  quotation: q,
  canAccept,
  accepting,
  onAccept
}: {
  quotation: Quotation;
  canAccept: boolean;
  accepting: boolean;
  onAccept: () => void;
}) {
  return (
    <div
      className={`rounded-xl border p-4 ${
        q.status === 'ACCEPTED' ? 'border-brand bg-brand/5' : q.status === 'REJECTED' ? 'border-line opacity-60' : 'border-line'
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-medium">{q.technicianName ?? 'Thợ ẩn danh'}</p>
          <div className="mt-1 flex flex-wrap gap-2 text-xs text-ink-soft">
            {q.avgRating != null && <span>★ {q.avgRating.toFixed(1)}</span>}
            {q.completedJobs != null && <span>{q.completedJobs} việc</span>}
            {q.yearsExperience != null && <span>{q.yearsExperience} năm</span>}
          </div>
        </div>
        <StatusChip status={q.status} />
      </div>

      <p className="mt-3 text-sm">{q.solution}</p>

      <div className="mt-3 space-y-1 text-sm">
        <div className="flex justify-between">
          <span className="text-ink-soft">Công</span>
          <span>{formatCurrency(q.priceLaborVnd)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-ink-soft">Vật tư</span>
          <span>{formatCurrency(q.priceMaterialsVnd)}</span>
        </div>
        <div className="flex justify-between border-t border-line pt-1 font-medium">
          <span>Tổng</span>
          <span className="text-brand-ink">{formatCurrency(q.totalPrice)}</span>
        </div>
      </div>

      {q.note && <p className="mt-2 text-xs text-ink-soft">Ghi chú: {q.note}</p>}

      {canAccept && (
        <div className="mt-3">
          <Button onClick={onAccept} loading={accepting} className="w-full">
            {accepting ? 'Đang xử lý...' : 'Chọn thợ này'}
          </Button>
        </div>
      )}
    </div>
  );
}
