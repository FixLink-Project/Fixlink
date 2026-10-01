import { useState } from 'react';
import Alert from './Alert';
import Button from './Button';
import TextField from './TextField';
import TextArea from './TextArea';
import { ApiError, cancelAppointment, completeAppointment, rescheduleAppointment } from '../lib/api';
import type { Appointment } from '../lib/types';

interface AppointmentCardProps {
  appointment: Appointment;
  currentUserRole?: 'CUSTOMER' | 'TECHNICIAN' | 'ADMIN';
  onUpdated?: () => void;
}

export default function AppointmentCard({
  appointment: appt,
  currentUserRole: _currentUserRole,
  onUpdated
}: AppointmentCardProps) {
  // Modal states
  const [showRescheduleModal, setShowRescheduleModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showCompleteModal, setShowCompleteModal] = useState(false);

  // Form states
  const [newDate, setNewDate] = useState(appt.scheduledDate || '');
  const [newTime, setNewTime] = useState(
    appt.scheduledTime ? appt.scheduledTime.slice(0, 5) : '09:00'
  );
  const [rescheduleNotes, setRescheduleNotes] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [completeNotes, setCompleteNotes] = useState('');

  // Loading & error states
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const isTerminal = appt.status === 'COMPLETED' || appt.status === 'CANCELLED';

  function getStatusBadge() {
    switch (appt.status) {
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
            <span className="h-2 w-2 rounded-full bg-blue-500 animate-pulse"></span>
            Đã xác nhận
          </span>
        );
      case 'RESCHEDULED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
            <span className="h-2 w-2 rounded-full bg-amber-500"></span>
            Đã dời lịch
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <span className="h-2 w-2 rounded-full bg-emerald-500"></span>
            Đã hoàn thành
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
            <span className="h-2 w-2 rounded-full bg-rose-500"></span>
            Đã hủy
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-700">
            {appt.statusLabel || appt.status}
          </span>
        );
    }
  }

  async function handleReschedule(e: React.FormEvent) {
    e.preventDefault();
    if (!newDate || !newTime) {
      setActionError('Vui lòng chọn ngày và giờ hẹn mới.');
      return;
    }

    setSubmitting(true);
    setActionError(null);
    try {
      await rescheduleAppointment(appt.id, {
        scheduledDate: newDate,
        scheduledTime: newTime.length === 5 ? `${newTime}:00` : newTime,
        notes: rescheduleNotes.trim() || undefined
      });
      setActionSuccess('Đã dời lịch hẹn thành công!');
      setShowRescheduleModal(false);
      onUpdated?.();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Lỗi khi dời lịch hẹn.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleComplete(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setActionError(null);
    try {
      await completeAppointment(appt.id, completeNotes.trim() || undefined);
      setActionSuccess('Đã hoàn thành lịch hẹn!');
      setShowCompleteModal(false);
      onUpdated?.();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Lỗi khi hoàn thành lịch hẹn.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(e: React.FormEvent) {
    e.preventDefault();
    if (!cancelReason.trim()) {
      setActionError('Lý do hủy cuộc hẹn không được để trống.');
      return;
    }

    setSubmitting(true);
    setActionError(null);
    try {
      await cancelAppointment(appt.id, { cancelReason: cancelReason.trim() });
      setActionSuccess('Đã hủy lịch hẹn!');
      setShowCancelModal(false);
      onUpdated?.();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Lỗi khi hủy lịch hẹn.');
    } finally {
      setSubmitting(false);
    }
  }

  const todayStr = new Date().toISOString().split('T')[0];

  return (
    <div
      className={`rounded-2xl border p-5 transition-all duration-200 shadow-xs ${
        appt.status === 'COMPLETED'
          ? 'border-emerald-200 bg-emerald-50/30'
          : appt.status === 'CANCELLED'
          ? 'border-slate-200 bg-slate-50/50 opacity-80'
          : appt.status === 'RESCHEDULED'
          ? 'border-amber-200 bg-amber-50/20'
          : 'border-blue-200 bg-white'
      }`}
    >
      {/* Thông báo trạng thái nhanh */}
      {actionSuccess && (
        <div className="mb-4">
          <Alert tone="success">{actionSuccess}</Alert>
        </div>
      )}

      {/* Header cuộc hẹn: Ngày giờ + Status Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-brand/10 text-brand font-bold text-lg">
            📅
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-extrabold text-slate-900 text-base">
                {appt.scheduledDate} Lúc {appt.scheduledTime ? appt.scheduledTime.slice(0, 5) : ''}
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Mã lịch hẹn: #{appt.id} • Yêu cầu: #{appt.repairRequestId}
            </p>
          </div>
        </div>
        <div>{getStatusBadge()}</div>
      </div>

      {/* Body: Người tham gia & Chi tiết */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 my-4 text-sm">
        {/* Khách hàng */}
        <div className="p-3 bg-slate-50 rounded-xl space-y-1">
          <p className="text-xs font-bold text-slate-500 uppercase tracking-wide">Khách hàng</p>
          <p className="font-semibold text-slate-900">{appt.customerName || 'Khách hàng'}</p>
          {appt.customerPhone && (
            <p className="text-xs text-slate-600 flex items-center gap-1">
              📞 <a href={`tel:${appt.customerPhone}`} className="text-blue-600 hover:underline">{appt.customerPhone}</a>
            </p>
          )}
          {appt.requestAddress && (
            <p className="text-xs text-slate-500 line-clamp-1">📍 {appt.requestAddress}</p>
          )}
        </div>

        {/* Kỹ thuật viên */}
        <div className="p-3 bg-slate-50 rounded-xl space-y-1">
          <p className="text-xs font-bold text-slate-500 uppercase tracking-wide">Kỹ thuật viên phụ trách</p>
          <p className="font-semibold text-slate-900">{appt.technicianName || 'Kỹ thuật viên'}</p>
          {appt.technicianPhone && (
            <p className="text-xs text-slate-600 flex items-center gap-1">
              📞 <a href={`tel:${appt.technicianPhone}`} className="text-blue-600 hover:underline">{appt.technicianPhone}</a>
            </p>
          )}
        </div>
      </div>

      {/* Ghi chú cuộc hẹn */}
      {appt.notes && (
        <div className="p-3 rounded-xl bg-blue-50/50 border border-blue-100 text-xs text-blue-900 my-3">
          <span className="font-bold">Ghi chú: </span>
          {appt.notes}
        </div>
      )}

      {/* Lý do hủy nếu có */}
      {appt.status === 'CANCELLED' && appt.cancelReason && (
        <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-900 my-3">
          <span className="font-bold">Lý do hủy: </span>
          {appt.cancelReason}
        </div>
      )}

      {/* Footer Hành động theo vòng đời */}
      <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3">
        {isTerminal ? (
          <span className="text-xs text-slate-500 italic">
            🔒 Cuộc hẹn này đã ở trạng thái kết thúc ({appt.statusLabel || appt.status}), không thể thay đổi thêm.
          </span>
        ) : (
          <div className="flex flex-wrap items-center gap-2">
            {/* Đổi lịch hẹn */}
            <Button
              variant="secondary"
              className="min-h-[36px] px-3.5 py-1 text-xs"
              onClick={() => {
                setActionError(null);
                setShowRescheduleModal(true);
              }}
            >
              🔄 Đổi lịch hẹn
            </Button>

            {/* Hoàn thành cuộc hẹn */}
            <Button
              variant="primary"
              className="min-h-[36px] px-3.5 py-1 text-xs"
              onClick={() => {
                setActionError(null);
                setShowCompleteModal(true);
              }}
            >
              ✓ Xác nhận hoàn thành
            </Button>

            {/* Hủy cuộc hẹn */}
            <Button
              variant="danger"
              className="min-h-[36px] px-3.5 py-1 text-xs"
              onClick={() => {
                setActionError(null);
                setShowCancelModal(true);
              }}
            >
              ✕ Hủy lịch hẹn
            </Button>
          </div>
        )}
      </div>

      {/* Modal Đổi lịch hẹn (Reschedule) */}
      {showRescheduleModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base">Đổi lịch hẹn (Reschedule)</h3>
              <button
                type="button"
                onClick={() => setShowRescheduleModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            {actionError && <Alert tone="error">{actionError}</Alert>}

            <form onSubmit={handleReschedule} className="space-y-4">
              <TextField
                label="Ngày hẹn mới"
                type="date"
                min={todayStr}
                value={newDate}
                onChange={(e) => setNewDate(e.target.value)}
                required
              />

              <TextField
                label="Giờ hẹn mới"
                type="time"
                value={newTime}
                onChange={(e) => setNewTime(e.target.value)}
                required
              />

              <TextArea
                label="Lý do đổi lịch / Ghi chú mới"
                value={rescheduleNotes}
                onChange={(e) => setRescheduleNotes(e.target.value)}
                placeholder="Ví dụ: Khách bận việc buổi sáng, đổi sang chiều..."
                rows={3}
              />

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Button
                  variant="secondary"
                  type="button"
                  onClick={() => setShowRescheduleModal(false)}
                >
                  Đóng
                </Button>
                <Button type="submit" loading={submitting}>
                  Lưu thay đổi lịch hẹn
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Xác nhận hoàn thành (Complete) */}
      {showCompleteModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base">Xác nhận hoàn thành lịch hẹn</h3>
              <button
                type="button"
                onClick={() => setShowCompleteModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <p className="text-sm text-slate-600">
              Bạn có chắc chắn muốn đánh dấu cuộc hẹn ngày <strong>{appt.scheduledDate}</strong> đã{' '}
              <strong className="text-emerald-600">HOÀN THÀNH</strong>?
            </p>

            {actionError && <Alert tone="error">{actionError}</Alert>}

            <form onSubmit={handleComplete} className="space-y-4">
              <TextArea
                label="Ghi chú kết quả (tùy chọn)"
                value={completeNotes}
                onChange={(e) => setCompleteNotes(e.target.value)}
                placeholder="Ví dụ: Kỹ thuật viên đã kiểm tra và khắc phục xong..."
                rows={3}
              />

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Button
                  variant="secondary"
                  type="button"
                  onClick={() => setShowCompleteModal(false)}
                >
                  Quay lại
                </Button>
                <Button variant="primary" type="submit" loading={submitting}>
                  Xác nhận Hoàn thành
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Hủy cuộc hẹn (Cancel) */}
      {showCancelModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-rose-700 text-base">Hủy cuộc hẹn</h3>
              <button
                type="button"
                onClick={() => setShowCancelModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <p className="text-sm text-slate-600">
              Lưu ý: Sau khi hủy, cuộc hẹn sẽ kết thúc và không thể mở lại.
            </p>

            {actionError && <Alert tone="error">{actionError}</Alert>}

            <form onSubmit={handleCancel} className="space-y-4">
              <TextArea
                label="Lý do hủy cuộc hẹn *"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Vui lòng nêu rõ lý do hủy (bắt buộc)..."
                rows={3}
                required
              />

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Button
                  variant="secondary"
                  type="button"
                  onClick={() => setShowCancelModal(false)}
                >
                  Bỏ qua
                </Button>
                <Button variant="danger" type="submit" loading={submitting}>
                  Xác nhận Hủy hẹn
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
