import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Alert from '../components/Alert';
import AppointmentCard from '../components/AppointmentCard';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import {
  ApiError,
  api,
  applyForJob,
  createAppointment,
  fetchAppointmentsForRequest,
  formatCurrency
} from '../lib/api';
import { useAuth } from '../lib/auth';
import type { Appointment, Quotation, RepairRequestDetail } from '../lib/types';

const TECHNICIAN_NAV = [
  { to: '/tho', label: 'Bảng điều khiển' },
  { to: '/tho/ho-so', label: 'Hồ sơ cá nhân' }
];

export default function TechnicianRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();

  const [detail, setDetail] = useState<RepairRequestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Appointments (RC-48)
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [showCreateApptModal, setShowCreateApptModal] = useState(false);
  const [newApptDate, setNewApptDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  });
  const [newApptTime, setNewApptTime] = useState('09:00');
  const [newApptType, setNewApptType] = useState<'SURVEY' | 'REPAIR' | 'WARRANTY'>('REPAIR');
  const [newApptNotes, setNewApptNotes] = useState('');
  const [creatingAppt, setCreatingAppt] = useState(false);
  const [createApptError, setCreateApptError] = useState<string | null>(null);

  async function handleCreateAppointment(e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    setCreatingAppt(true);
    setCreateApptError(null);
    try {
      const numericUserId = user?.id ? Number(user.id.replace('usr_', '')) : undefined;
      await createAppointment({
        repairRequestId: detail.request.id,
        technicianId: numericUserId,
        appointmentType: newApptType,
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

  // Modal xem ảnh phóng to
  const [activeMediaUrl, setActiveMediaUrl] = useState<string | null>(null);

  // State cho luồng "Nhận việc" (thay cho form gửi/sửa báo giá cũ)
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);

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
        setError(err instanceof ApiError ? err.message : 'Không tải được chi tiết yêu cầu sửa chữa.');
      })
      .finally(() => setLoading(false));

    if (id) {
      fetchAppointmentsForRequest(Number(id))
        .then((res) => setAppointments(res.data || []))
        .catch(() => setAppointments([]));
    }
  }

  // Gửi báo giá mới

  // Chỉnh sửa báo giá đã nộp

  // Rút báo giá

  if (loading) {
    return (
      <DashboardLayout nav={TECHNICIAN_NAV} title="Chi tiết yêu cầu sửa chữa">
        <div className="h-80 animate-pulse rounded-2xl border border-slate-200 bg-white" />
      </DashboardLayout>
    );
  }

  if (error || !detail) {
    return (
      <DashboardLayout nav={TECHNICIAN_NAV} title="Chi tiết yêu cầu sửa chữa">
        <div className="space-y-4">
          <Alert tone="error">{error ?? 'Không tìm thấy yêu cầu sửa chữa này.'}</Alert>
          <div>
            <Link to="/tho">
              <Button variant="secondary">← Quay lại danh sách việc</Button>
            </Link>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  const req = detail.request;
  async function handleApply() {
    const confirmed = window.confirm(
      `Bạn sẽ nhận việc với giá ${formatCurrency(detail!.request.budgetRef)}. Xác nhận?`
    );
    if (!confirmed) return;

    setApplyError(null);
    setApplying(true);
    try {
      await applyForJob(Number(id));
      loadDetail();
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === 'JOB_ALREADY_TAKEN') {
        setApplyError('Yêu cầu này vừa được thợ khác nhận.');
        loadDetail();
      } else {
        setApplyError(err instanceof ApiError ? err.message : 'Không nhận được việc. Vui lòng thử lại.');
      }
    } finally {
      setApplying(false);
    }
  }

  const myQuote: Quotation | undefined = detail.quotations?.[0];
  const isOpenForApply = req.status === 'OPEN';

  // Danh sách hình ảnh
  const displayPhotos: string[] = req.media && req.media.length > 0
    ? req.media.map((m) => m.url)
    : req.mediaUrls || [];

  return (
    <DashboardLayout
      nav={TECHNICIAN_NAV}
      title={req.title}
      description={
        <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
          <span className="font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded-md">
            {req.requestCode}
          </span>
          <span>•</span>
          <span>Ngày đăng: {new Date(req.createdAt).toLocaleDateString('vi-VN')}</span>
          {req.customerName && (
            <>
              <span>•</span>
              <span>Khách hàng: <strong className="text-slate-700">{req.customerName}</strong></span>
            </>
          )}
        </div>
      }
      actions={
        <div className="flex items-center gap-2">
          <Link to="/tho">
            <Button variant="quiet">← Về Bảng điều khiển</Button>
          </Link>
          <StatusChip status={req.status} label={req.statusLabel} />
        </div>
      }
    >
      <div className="space-y-6">
        {/* Thông tin sự cố và địa bàn */}
        <Card title="Thông tin chi tiết yêu cầu">
          <dl className="divide-y divide-slate-100 text-sm">
            <Row label="📁 Nhóm thiết bị" value={req.categoryName ?? '—'} />
            <Row label="📍 Khu vực phục vụ" value={req.areaName ?? 'Toàn thành phố'} />
            <Row label="🏠 Địa chỉ hiện trường" value={req.address || req.addressLine} />
            <Row
              label="⏰ Thời gian mong muốn"
              value={req.requestedTime ? new Date(req.requestedTime).toLocaleString('vi-VN') : 'Càng sớm càng tốt'}
            />
            <Row
              label="💰 Giá cố định khách đưa ra"
              value={formatCurrency(req.budgetRef)}
            />
            {req.applyDeadline && (
              <Row
                label="⏳ Hạn chót nhận việc"
                value={new Date(req.applyDeadline).toLocaleString('vi-VN')}
              />
            )}
            <div className="py-3 sm:grid sm:grid-cols-3 sm:gap-4">
              <dt className="font-semibold text-slate-600">📝 Mô tả hiện tượng hư hỏng</dt>
              <dd className="mt-1 text-slate-800 sm:col-span-2 sm:mt-0 leading-relaxed whitespace-pre-line bg-slate-50 p-3 rounded-xl border border-slate-200/80">
                {req.description}
              </dd>
            </div>
          </dl>
        </Card>

        {/* Hình ảnh hiện trường */}
        {displayPhotos.length > 0 && (
          <Card
            title={`Hình ảnh & Video hiện trường (${displayPhotos.length})`}
            description="Bấm vào từng ảnh để phóng to kiểm tra kỹ linh kiện và vị trí hỏng hóc."
          >
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
              {displayPhotos.map((url, idx) => (
                <div
                  key={idx}
                  onClick={() => setActiveMediaUrl(url)}
                  className="group relative aspect-4/3 cursor-pointer overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 shadow-xs transition hover:border-brand hover:shadow-md"
                >
                  <img
                    src={url}
                    alt={`Hiện trường ${idx + 1}`}
                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 bg-slate-900/20 opacity-0 transition group-hover:opacity-100 flex items-center justify-center">
                    <span className="rounded-full bg-white/90 p-2 text-xs font-bold text-slate-800 shadow-sm">
                      🔍 Phóng to
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        )}

        {/* Nhận việc — mô hình "ai nhận trước được trước" (thay cho gửi/sửa báo giá) */}
        {myQuote ? (
          <Card
            title="Bạn đã nhận việc này"
            description="Giá là mức khách đã ấn định. Hãy liên hệ khách để hẹn lịch khảo sát."
          >
            <dl className="divide-y divide-slate-100 text-sm">
              <Row label="💰 Giá đã chốt" value={formatCurrency(req.agreedPrice || req.budgetRef)} />
              <Row label="💵 Tiền cọc Escrow (30%)" value={formatCurrency(req.depositAmount)} />
              {myQuote.acceptedAt && (
                <Row label="🕒 Nhận việc lúc" value={new Date(myQuote.acceptedAt).toLocaleString('vi-VN')} />
              )}
            </dl>
          </Card>
        ) : isOpenForApply ? (
          <Card
            title="Nhận việc này"
            description="Thợ đầu tiên bấm nhận sẽ được giao việc ngay, khách không cần xác nhận thêm."
          >
            {applyError && (
              <div className="mb-4">
                <Alert tone="error">{applyError}</Alert>
              </div>
            )}
            <div className="rounded-xl border border-blue-100 bg-blue-50 p-4">
              <div className="flex items-center justify-between text-sm">
                <span className="font-semibold text-slate-700">Giá khách đưa ra (cố định):</span>
                <span className="font-display text-lg font-bold text-brand">
                  {formatCurrency(req.budgetRef)}
                </span>
              </div>
              {req.applyDeadline && (
                <p className="mt-2 text-xs text-slate-500">
                  Hạn nhận việc: {new Date(req.applyDeadline).toLocaleString('vi-VN')}
                </p>
              )}
            </div>
            <div className="mt-4 flex justify-end">
              <Button variant="primary" loading={applying} onClick={handleApply}>
                {applying ? 'Đang nhận...' : 'Nhận việc'}
              </Button>
            </div>
          </Card>
        ) : (
          <Card title="Không thể nhận việc">
            <div className="py-4 text-center text-sm text-slate-500">
              Đơn này hiện không mở cho thợ nhận (Trạng thái: <strong>{req.statusLabel || req.status}</strong>).
            </div>
          </Card>
        )}

        {/* Lịch hẹn làm việc / khảo sát (Jira RC-48: Appointment Status Lifecycle) */}
        {(() => {
          const numericUserId = user?.id ? Number(user.id.replace('usr_', '')) : null;
          const isAssignedToOther = detail.request.technicianId && numericUserId && detail.request.technicianId !== numericUserId;

          return (
            <Card
              title="Lịch hẹn khảo sát & thi công (RC-48)"
              description="Quản lý vòng đời trạng thái cuộc hẹn giữa bạn và khách hàng."
              actions={
                !isAssignedToOther ? (
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
              {isAssignedToOther && (
                <div className="mb-3 rounded-xl bg-amber-50 border border-amber-200 p-3 text-xs text-amber-800">
                  ⚠️ Đơn hàng này đang được phụ trách bởi một kỹ thuật viên khác. Bạn đang xem với quyền chỉ đọc.
                </div>
              )}
              {appointments.length === 0 ? (
                <div className="py-6 text-center text-slate-500 text-sm">
                  {isAssignedToOther
                    ? 'Chưa có lịch hẹn nào được thiết lập cho đơn này.'
                    : 'Chưa có lịch hẹn nào được thiết lập cho đơn này. Bấm nút + Đặt lịch hẹn mới để hẹn thời gian tới nhà khách hàng.'}
                </div>
              ) : (
                <div className="space-y-4 my-2">
                  {appointments.map((appt) => (
                    <AppointmentCard
                      key={appt.id}
                      appointment={appt}
                      currentUserRole="TECHNICIAN"
                      onUpdated={loadDetail}
                    />
                  ))}
                </div>
              )}
            </Card>
          );
        })()}

        {/* Tiến trình sửa chữa */}
        {detail.workProgress.length > 0 && (
          <Card title="Tiến trình công việc" description="Lịch sử các mốc trạng thái thực hiện trên đơn sửa chữa.">
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
      </div>

      {/* Modal phóng to ảnh */}
      {activeMediaUrl && (
        <div
          className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center p-4 animate-fade-in"
          onClick={() => setActiveMediaUrl(null)}
        >
          <div
            className="relative max-w-4xl max-h-[90vh] bg-white rounded-2xl overflow-hidden shadow-2xl p-2 animate-scale-in"
            onClick={(e) => e.stopPropagation()}
          >
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
                className="text-xs font-semibold text-slate-500 hover:text-slate-800"
              >
                ✕ Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Đặt lịch hẹn mới (Thợ) */}
      {showCreateApptModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base">Đặt lịch hẹn với khách hàng</h3>
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
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Mục đích lịch hẹn *
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {(['SURVEY', 'REPAIR', 'WARRANTY'] as const).map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setNewApptType(type)}
                      className={`py-2 px-3 text-xs font-semibold rounded-xl border transition-all ${
                        newApptType === type
                          ? 'bg-blue-50 border-brand text-brand ring-1 ring-brand'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      {type === 'SURVEY' ? 'Khảo sát' : type === 'REPAIR' ? 'Sửa chữa' : 'Bảo hành'}
                    </button>
                  ))}
                </div>
              </div>

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
                label="Ghi chú hẹn khách"
                value={newApptNotes}
                onChange={(e) => setNewApptNotes(e.target.value)}
                placeholder="Ví dụ: Thợ sẽ gọi điện trước khi đến 15 phút, quý khách vui lòng giữ máy..."
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

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="py-3 sm:grid sm:grid-cols-3 sm:gap-4">
      <dt className="font-semibold text-slate-600">{label}</dt>
      <dd className="mt-1 text-slate-900 sm:col-span-2 sm:mt-0 font-medium">{value}</dd>
    </div>
  );
}
