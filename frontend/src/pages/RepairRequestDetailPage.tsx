import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import Alert from '../components/Alert';
import AppointmentCard from '../components/AppointmentCard';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import MultiImageUploadField from '../components/MultiImageUploadField';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, attachRequestMedia, createAppointment, fetchAppointmentsForRequest, formatCurrency, updateRequestStatus } from '../lib/api';
import type { AcceptQuotationResult, Appointment, Quotation, RepairRequestDetail } from '../lib/types';

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

  // Appointments (RC-48)
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [showCreateApptModal, setShowCreateApptModal] = useState(false);
  const [newApptDate, setNewApptDate] = useState('');
  const [newApptTime, setNewApptTime] = useState('09:00');
  const [newApptNotes, setNewApptNotes] = useState('');
  const [creatingAppt, setCreatingAppt] = useState(false);
  const [createApptError, setCreateApptError] = useState<string | null>(null);

  // Modal xem ảnh phóng to
  const [activeMediaUrl, setActiveMediaUrl] = useState<string | null>(null);

  // Gắn thêm ảnh bằng chứng
  const [showAttachModal, setShowAttachModal] = useState(false);
  const [additionalMediaUrls, setAdditionalMediaUrls] = useState<string[]>([]);
  const [attaching, setAttaching] = useState(false);

  // Kích hoạt bản nháp
  const [publishing, setPublishing] = useState(false);

  useEffect(() => {
    loadDetail();
  }, [id]);

  function loadDetail() {
    setLoading(true);
    api.get<RepairRequestDetail>(`/repair-requests/${id}`)
      .then((res) => {
        setDetail(res.data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Không tải được chi tiết yêu cầu.');
      })
      .finally(() => setLoading(false));

    if (id) {
      fetchAppointmentsForRequest(Number(id))
        .then((res) => setAppointments(res.data || []))
        .catch(() => setAppointments([]));
    }
  }

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
      loadDetail();
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
      loadDetail();
      setShowCancelForm(false);
    } catch (err) {
      setAcceptError(err instanceof ApiError ? err.message : 'Lỗi khi hủy đơn.');
    } finally {
      setCancelling(false);
    }
  }

  async function handlePublishDraft() {
    if (!detail) return;
    setPublishing(true);
    try {
      await updateRequestStatus(detail.request.id, 'PENDING', 'Khách hàng đăng bản nháp');
      loadDetail();
    } catch (err) {
      setAcceptError(err instanceof ApiError ? err.message : 'Lỗi khi phát sóng bản nháp.');
    } finally {
      setPublishing(false);
    }
  }

  async function handleAttachMedia() {
    if (!detail || additionalMediaUrls.length === 0) return;
    setAttaching(true);
    try {
      await attachRequestMedia(detail.request.id, additionalMediaUrls);
      setAdditionalMediaUrls([]);
      setShowAttachModal(false);
      loadDetail();
    } catch (err) {
      setAcceptError(err instanceof ApiError ? err.message : 'Lỗi khi gắn ảnh bằng chứng.');
    } finally {
      setAttaching(false);
    }
  }

  async function handleCreateAppointment(e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    setCreatingAppt(true);
    setCreateApptError(null);
    try {
      await createAppointment({
        repairRequestId: detail.request.id,
        technicianId: detail.request.technicianId || undefined,
        scheduledDate: newApptDate,
        scheduledTime: newApptTime.length === 5 ? `${newApptTime}:00` : newApptTime,
        notes: newApptNotes.trim() || undefined
      });
      setShowCreateApptModal(false);
      setNewApptNotes('');
      loadDetail();
    } catch (err) {
      setCreateApptError(err instanceof ApiError ? err.message : 'Không tạo được lịch hẹn.');
    } finally {
      setCreatingAppt(false);
    }
  }

  if (loading) {
    return (
      <DashboardLayout nav={CUSTOMER_NAV} title="Chi tiết yêu cầu">
        <div className="h-80 animate-pulse rounded-2xl border border-slate-200 bg-white" />
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
  const isDraft = req.status === 'DRAFT';

  // Lấy danh sách ảnh tổng hợp (từ media hoặc mediaUrls)
  const displayPhotos: string[] = req.media && req.media.length > 0
    ? req.media.map(m => m.url)
    : req.mediaUrls || [];

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title={req.title}
      description={<span className="font-mono text-xs text-slate-500 font-semibold">{req.requestCode}</span>}
      actions={<StatusChip status={req.status} label={req.statusLabel} />}
    >
      {acceptError && (
        <div className="mb-5">
          <Alert tone="error">{acceptError}</Alert>
        </div>
      )}

      {/* Draft Notification Banner */}
      {isDraft && (
        <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50/80 p-5 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h4 className="font-bold text-amber-900 text-sm flex items-center gap-1.5">
              <span>📝</span> Yêu cầu này đang ở trạng thái Bản nháp
            </h4>
            <p className="text-xs text-amber-700 mt-0.5">
              Các thợ kỹ thuật chưa nhìn thấy đơn này. Nhấn nút bên cạnh để phát sóng ngay tới mạng lưới thợ.
            </p>
          </div>
          <Button onClick={handlePublishDraft} loading={publishing} className="shrink-0">
            📢 Phát sóng yêu cầu ngay
          </Button>
        </div>
      )}

      <div className="space-y-6">
        {/* Thông tin đơn */}
        <Card title="Thông tin yêu cầu sửa chữa">
          <dl className="divide-y divide-slate-100 text-sm">
            <Row label="Loại thiết bị" value={req.categoryName ?? '—'} />
            <Row label="Khu vực" value={req.areaName ?? 'Toàn khu vực'} />
            <Row label="Địa chỉ kiểm tra" value={req.address || req.addressLine} />
            <Row label="Mô tả sự cố" value={req.description} />
            {req.budgetRef > 0 && <Row label="Ngân sách dự kiến" value={formatCurrency(req.budgetRef)} />}
            {req.biddingDeadline && (
              <Row label="Hạn nhận báo giá" value={new Date(req.biddingDeadline).toLocaleString('vi-VN')} />
            )}
            {req.agreedPrice > 0 && <Row label="Giá chốt" value={formatCurrency(req.agreedPrice)} highlight />}
            {req.depositAmount > 0 && <Row label="Tiền cọc Escrow (30%)" value={formatCurrency(req.depositAmount)} highlight />}
            {req.cancelReason && <Row label="Lý do hủy" value={req.cancelReason} />}
          </dl>

          {/* Media Evidence Gallery */}
          <div className="mt-5 border-t border-slate-100 pt-4">
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <span>📷</span> Hình ảnh & Tệp bằng chứng hiện trường ({displayPhotos.length})
              </p>
              {displayPhotos.length < 6 && (
                <button
                  type="button"
                  onClick={() => setShowAttachModal(true)}
                  className="text-xs font-semibold text-brand hover:text-brand-strong transition-colors"
                >
                  ＋ Gắn thêm ảnh bằng chứng
                </button>
              )}
            </div>

            {displayPhotos.length > 0 ? (
              <div className="grid grid-cols-2 xs:grid-cols-3 sm:grid-cols-6 gap-3">
                {displayPhotos.map((url, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => setActiveMediaUrl(url)}
                    className="aspect-square rounded-xl border border-slate-200 overflow-hidden shadow-xs hover:border-brand/60 hover:shadow-card transition-all group relative"
                  >
                    <img
                      src={url}
                      alt={`Ảnh hiện trường ${i + 1}`}
                      className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-200"
                    />
                    <span className="absolute bottom-1 right-1 px-1.5 py-0.5 rounded bg-slate-900/60 text-[10px] text-white">
                      Phóng to 🔍
                    </span>
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400 italic">Chưa có ảnh đính kèm cho đơn này.</p>
            )}
          </div>
        </Card>

        {/* Báo giá từ thợ */}
        {detail.quotations.length > 0 && (
          <Card
            title={`Báo giá từ thợ kỹ thuật (${detail.quotations.length})`}
            description="So sánh giá công, chi phí linh kiện và đánh giá của từng thợ để chọn người phù hợp nhất."
          >
            <div className="grid gap-4 md:grid-cols-2">
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

        {detail.quotations.length === 0 && (req.status === 'BIDDING_OPEN' || req.status === 'PENDING') && !isDraft && (
          <Card title="Báo giá từ thợ kỹ thuật">
            <div className="py-6 text-center">
              <span className="text-3xl">⏳</span>
              <p className="mt-2 font-semibold text-slate-800">Đang chờ thợ gần khu vực báo giá</p>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                Yêu cầu của bạn đã được gửi tới các kỹ thuật viên. Thông thường bạn sẽ nhận được báo giá đầu tiên trong 15 – 30 phút.
              </p>
            </div>
          </Card>
        )}

        {/* Lịch hẹn khảo sát / thi công (Jira RC-48: Appointment Status Lifecycle) */}
        {(req.technicianId || appointments.length > 0) && (
          <Card
            title="Lịch hẹn khảo sát & thi công (RC-48)"
            description="Quản lý vòng đời trạng thái cuộc hẹn giữa bạn và kỹ thuật viên."
            actions={
              req.technicianId ? (
                <Button
                  variant="primary"
                  className="min-h-[36px] px-3.5 py-1 text-xs"
                  onClick={() => {
                    setCreateApptError(null);
                    setShowCreateApptModal(true);
                  }}
                >
                  + Đặt lịch hẹn mới
                </Button>
              ) : undefined
            }
          >
            {appointments.length === 0 ? (
              <div className="py-6 text-center text-slate-500 text-sm">
                Chưa có lịch hẹn nào được thiết lập. Hãy bấm <strong>Đặt lịch hẹn mới</strong> để hẹn thời gian kỹ thuật viên tới nhà.
              </div>
            ) : (
              <div className="space-y-4 my-2">
                {appointments.map((appt) => (
                  <AppointmentCard
                    key={appt.id}
                    appointment={appt}
                    currentUserRole="CUSTOMER"
                    onUpdated={loadDetail}
                  />
                ))}
              </div>
            )}
          </Card>
        )}

        {/* Timeline tiến trình */}
        {detail.workProgress.length > 0 && (
          <Card title="Tiến trình sửa chữa" description="Nhật ký các bước thực hiện trên thiết bị của bạn.">
            <ol className="relative border-l-2 border-blue-100 pl-6 ml-2 space-y-5 my-2">
              {detail.workProgress.map((wp) => (
                <li key={wp.id} className="relative">
                  <div className="absolute -left-[31px] top-1 flex h-4 w-4 items-center justify-center rounded-full border-2 border-brand bg-white shadow-xs">
                    <span className="h-1.5 w-1.5 rounded-full bg-brand" />
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusChip status={wp.toStatus} />
                    <span className="text-xs text-slate-400">
                      {new Date(wp.createdAt).toLocaleString('vi-VN')}
                    </span>
                  </div>
                  {wp.note && (
                    <div className="mt-2 rounded-xl bg-slate-50 border border-slate-200/80 p-3 text-sm text-slate-700">
                      {wp.note}
                    </div>
                  )}
                </li>
              ))}
            </ol>
          </Card>
        )}

        {/* Hủy đơn */}
        {canCancel && !showCancelForm && (
          <div className="flex justify-end">
            <Button variant="secondary" onClick={() => setShowCancelForm(true)}>
              Hủy yêu cầu này
            </Button>
          </div>
        )}

        {showCancelForm && (
          <Card title="Xác nhận hủy yêu cầu">
            <div className="max-w-md space-y-3">
              <textarea
                placeholder="Vui lòng nêu lý do hủy yêu cầu (ví dụ: đã tự sửa được, đổi thiết bị khác...)"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                rows={3}
                className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 shadow-xs focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
              />
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setShowCancelForm(false)}>
                  Thôi, giữ lại
                </Button>
                <Button
                  onClick={handleCancel}
                  loading={cancelling}
                  disabled={!cancelReason.trim()}
                >
                  Xác nhận hủy đơn
                </Button>
              </div>
            </div>
          </Card>
        )}
      </div>

      {/* Modal phóng to ảnh */}
      {activeMediaUrl && (
        <div
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center p-4"
          onClick={() => setActiveMediaUrl(null)}
        >
          <div className="relative max-w-4xl max-h-[90vh] bg-white rounded-2xl overflow-hidden shadow-2xl p-2" onClick={(e) => e.stopPropagation()}>
            <img
              src={activeMediaUrl}
              alt="Ảnh phóng to"
              className="max-h-[80vh] w-auto object-contain rounded-xl"
            />
            <div className="mt-2 flex justify-between items-center px-2">
              <a
                href={activeMediaUrl}
                target="_blank"
                rel="noreferrer"
                className="text-xs font-semibold text-brand hover:underline"
              >
                Mở ảnh gốc ↗
              </a>
              <button
                type="button"
                onClick={() => setActiveMediaUrl(null)}
                className="rounded-lg bg-slate-100 hover:bg-slate-200 px-3 py-1 text-xs font-bold text-slate-700 transition-colors"
              >
                Đóng ✕
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal gắn thêm ảnh bằng chứng */}
      {showAttachModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base">Gắn thêm ảnh bằng chứng hiện trường</h3>
              <button
                type="button"
                onClick={() => setShowAttachModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <MultiImageUploadField
              label="Chọn ảnh bổ sung"
              value={additionalMediaUrls}
              onChange={setAdditionalMediaUrls}
              maxFiles={Math.max(6 - displayPhotos.length, 1)}
              hint={`Bạn có thể gắn thêm tối đa ${6 - displayPhotos.length} ảnh.`}
            />

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <Button variant="secondary" onClick={() => setShowAttachModal(false)}>
                Hủy
              </Button>
              <Button
                onClick={handleAttachMedia}
                loading={attaching}
                disabled={additionalMediaUrls.length === 0}
              >
                Lưu ảnh bằng chứng
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Đặt lịch hẹn mới */}
      {showCreateApptModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base">Đặt lịch hẹn khảo sát / thi công</h3>
              <button
                type="button"
                onClick={() => setShowCreateApptModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            {createApptError && <Alert tone="error">{createApptError}</Alert>}

            <form onSubmit={handleCreateAppointment} className="space-y-4">
              <TextField
                label="Ngày hẹn *"
                type="date"
                min={new Date().toISOString().split('T')[0]}
                value={newApptDate}
                onChange={(e) => setNewApptDate(e.target.value)}
                required
              />

              <TextField
                label="Giờ hẹn *"
                type="time"
                value={newApptTime}
                onChange={(e) => setNewApptTime(e.target.value)}
                required
              />

              <TextArea
                label="Ghi chú cho thợ"
                value={newApptNotes}
                onChange={(e) => setNewApptNotes(e.target.value)}
                placeholder="Ví dụ: Có mặt trước 15 phút, gọi điện trước khi đến..."
                rows={3}
              />

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Button variant="secondary" type="button" onClick={() => setShowCreateApptModal(false)}>
                  Hủy
                </Button>
                <Button type="submit" loading={creatingAppt}>
                  Xác nhận đặt lịch
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}

function Row({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex flex-col gap-1 py-3 sm:flex-row sm:gap-4">
      <dt className="w-44 shrink-0 font-medium text-slate-500">{label}</dt>
      <dd className={highlight ? 'font-bold text-brand' : 'text-slate-900 font-medium'}>{value}</dd>
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
  const isAccepted = q.status === 'ACCEPTED';

  return (
    <div
      className={`rounded-2xl border p-5 transition-all duration-200 ${
        isAccepted
          ? 'border-brand bg-blue-50/60 shadow-sm ring-1 ring-brand'
          : q.status === 'REJECTED'
            ? 'border-slate-200 bg-slate-50/60 opacity-60'
            : 'border-slate-200 bg-white shadow-xs hover:border-brand/40 hover:shadow-card'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-100 font-bold text-brand text-base">
            {q.technicianName ? q.technicianName.charAt(0).toUpperCase() : 'T'}
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <p className="font-bold text-slate-900 text-sm">{q.technicianName ?? 'Thợ kỹ thuật'}</p>
              <span className="text-blue-600 text-xs" title="Thợ đã xác minh eKYC">✓</span>
            </div>
            <div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-slate-500">
              {q.avgRating != null && (
                <span className="font-semibold text-amber-600 flex items-center gap-0.5">
                  ★ {q.avgRating.toFixed(1)}
                </span>
              )}
              {q.completedJobs != null && <span>• {q.completedJobs} đơn</span>}
              {q.yearsExperience != null && <span>• {q.yearsExperience} năm kn</span>}
            </div>
          </div>
        </div>
        <StatusChip status={q.status} />
      </div>

      <div className="mt-3.5 rounded-xl bg-slate-50 p-3 border border-slate-100">
        <p className="text-xs font-semibold text-slate-500 mb-1">Phương án khắc phục:</p>
        <p className="text-xs sm:text-sm text-slate-800 leading-relaxed">{q.solution}</p>
      </div>

      <div className="mt-3.5 space-y-1.5 text-xs sm:text-sm">
        <div className="flex justify-between text-slate-600">
          <span>Tiền công thợ</span>
          <span className="font-medium text-slate-800">{formatCurrency(q.priceLaborVnd)}</span>
        </div>
        <div className="flex justify-between text-slate-600">
          <span>Linh kiện & vật tư thay thế</span>
          <span className="font-medium text-slate-800">{formatCurrency(q.priceMaterialsVnd)}</span>
        </div>
        <div className="flex justify-between border-t border-slate-200/80 pt-2 font-bold text-sm">
          <span className="text-slate-900">Tổng chi phí</span>
          <span className="text-base text-brand">{formatCurrency(q.totalPrice)}</span>
        </div>
      </div>

      {q.note && (
        <p className="mt-2.5 text-xs text-slate-500 italic">
          Ghi chú: {q.note}
        </p>
      )}

      {canAccept && (
        <div className="mt-4 pt-2">
          <Button onClick={onAccept} loading={accepting} className="w-full">
            {accepting ? 'Đang xác nhận...' : 'Chọn thợ này (Cọc 30% Escrow)'}
          </Button>
        </div>
      )}
    </div>
  );
}
