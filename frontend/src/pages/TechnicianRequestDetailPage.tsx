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
import { ApiError, api, createAppointment, fetchAppointmentsForRequest, formatCurrency } from '../lib/api';
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

  // State cho gửi / sửa báo giá
  const [quoteForm, setQuoteForm] = useState({
    solution: '',
    priceLaborVnd: '',
    priceMaterialsVnd: '',
    note: ''
  });
  const [isEditingQuote, setIsEditingQuote] = useState(false);
  const [quoteSubmitting, setQuoteSubmitting] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [quoteSuccess, setQuoteSuccess] = useState<string | null>(null);
  const [withdrawing, setWithdrawing] = useState(false);

  useEffect(() => {
    loadDetail();
  }, [id]);

  function loadDetail() {
    setLoading(true);
    api.get<RepairRequestDetail>(`/repair-requests/${id}`)
      .then((res) => {
        setDetail(res.data);
        setError(null);
        // Nếu thợ đã gửi báo giá trước đó, nạp dữ liệu vào form để có thể sửa
        if (res.data.quotations && res.data.quotations.length > 0) {
          const myQuote = res.data.quotations[0];
          setQuoteForm({
            solution: myQuote.solution || '',
            priceLaborVnd: String(myQuote.priceLaborVnd || ''),
            priceMaterialsVnd: String(myQuote.priceMaterialsVnd || ''),
            note: myQuote.note || ''
          });
        }
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
  async function handleSubmitQuote(e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    setQuoteSubmitting(true);
    setQuoteError(null);
    setQuoteSuccess(null);

    try {
      await api.post(`/repair-requests/${id}/quotations`, {
        solution: quoteForm.solution.trim(),
        priceLaborVnd: Number(quoteForm.priceLaborVnd),
        priceMaterialsVnd: Number(quoteForm.priceMaterialsVnd),
        note: quoteForm.note.trim() || undefined
      });
      setQuoteSuccess('Gửi báo giá kỹ thuật thành công! Khách hàng sẽ nhận được thông báo.');
      setIsEditingQuote(false);
      loadDetail();
    } catch (err) {
      setQuoteError(err instanceof ApiError ? err.message : 'Không gửi được báo giá. Vui lòng thử lại.');
    } finally {
      setQuoteSubmitting(false);
    }
  }

  // Chỉnh sửa báo giá đã nộp
  async function handleUpdateQuote(quotationId: number, e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    setQuoteSubmitting(true);
    setQuoteError(null);
    setQuoteSuccess(null);

    try {
      await api.put(`/repair-requests/${id}/quotations/${quotationId}`, {
        solution: quoteForm.solution.trim(),
        priceLaborVnd: Number(quoteForm.priceLaborVnd),
        priceMaterialsVnd: Number(quoteForm.priceMaterialsVnd),
        note: quoteForm.note.trim() || undefined
      });
      setQuoteSuccess('Cập nhật báo giá thành công!');
      setIsEditingQuote(false);
      loadDetail();
    } catch (err) {
      setQuoteError(err instanceof ApiError ? err.message : 'Không cập nhật được báo giá.');
    } finally {
      setQuoteSubmitting(false);
    }
  }

  // Rút báo giá
  async function handleWithdrawQuote(quotationId: number) {
    const confirmed = window.confirm('Bạn có chắc chắn muốn rút báo giá này? Khách hàng sẽ không thể chọn báo giá của bạn nữa.');
    if (!confirmed) return;

    setWithdrawing(true);
    setQuoteError(null);
    setQuoteSuccess(null);

    try {
      await api.delete(`/repair-requests/${id}/quotations/${quotationId}`);
      setQuoteSuccess('Đã rút báo giá thành công.');
      setIsEditingQuote(false);
      loadDetail();
    } catch (err) {
      setQuoteError(err instanceof ApiError ? err.message : 'Không rút được báo giá.');
    } finally {
      setWithdrawing(false);
    }
  }

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
  const myQuote: Quotation | undefined = detail.quotations?.[0];
  const isOpenForBidding = req.status === 'BIDDING_OPEN';

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
        {quoteSuccess && (
          <Alert tone="success">{quoteSuccess}</Alert>
        )}

        {quoteError && (
          <Alert tone="error">{quoteError}</Alert>
        )}

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
              label="💰 Ngân sách dự kiến"
              value={req.budgetRef > 0 ? formatCurrency(req.budgetRef) : 'Thỏa thuận theo sự cố'}
            />
            {req.biddingDeadline && (
              <Row
                label="⏳ Hạn chót nhận báo giá"
                value={new Date(req.biddingDeadline).toLocaleString('vi-VN')}
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

        {/* Khối Báo giá kỹ thuật tương tác (RC-39 & RC-37) */}
        {myQuote ? (
          /* Thợ ĐÃ gửi báo giá */
          <Card
            title="Báo giá của bạn"
            description="Thông tin báo giá kỹ thuật bạn đã gửi tới khách hàng cho đơn này."
          >
            {!isEditingQuote ? (
              <div className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-slate-500">Trạng thái báo giá:</span>
                    <span
                      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${
                        myQuote.status === 'ACCEPTED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : myQuote.status === 'REJECTED'
                          ? 'bg-red-100 text-red-800'
                          : myQuote.status === 'WITHDRAWN'
                          ? 'bg-slate-100 text-slate-600'
                          : 'bg-blue-100 text-brand'
                      }`}
                    >
                      {myQuote.status === 'ACCEPTED'
                        ? '✓ Được chọn làm thợ chính'
                        : myQuote.status === 'REJECTED'
                        ? 'Đã bị từ chối'
                        : myQuote.status === 'WITHDRAWN'
                        ? 'Đã rút báo giá'
                        : 'Đang chờ khách phản hồi'}
                    </span>
                  </div>
                  <span className="text-xs text-slate-400">
                    Gửi lúc: {new Date(myQuote.createdAt).toLocaleString('vi-VN')}
                  </span>
                </div>

                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
                    <span className="text-xs text-slate-500 block">Tiền công kỹ thuật</span>
                    <span className="text-base font-bold text-slate-900">
                      {formatCurrency(myQuote.priceLaborVnd)}
                    </span>
                  </div>
                  <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3">
                    <span className="text-xs text-slate-500 block">Tiền linh kiện vật tư</span>
                    <span className="text-base font-bold text-slate-900">
                      {formatCurrency(myQuote.priceMaterialsVnd)}
                    </span>
                  </div>
                  <div className="rounded-xl border border-blue-200 bg-blue-50/80 p-3">
                    <span className="text-xs text-brand block font-semibold">Tổng giá báo khách</span>
                    <span className="text-lg font-extrabold text-brand">
                      {formatCurrency(myQuote.totalPrice)}
                    </span>
                  </div>
                </div>

                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
                    Giải pháp kỹ thuật đề xuất
                  </h4>
                  <p className="rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-800 whitespace-pre-line">
                    {myQuote.solution}
                  </p>
                </div>

                {myQuote.note && (
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-1">
                      Ghi chú thêm
                    </h4>
                    <p className="rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-700">
                      {myQuote.note}
                    </p>
                  </div>
                )}

                {/* Các nút thao tác khi báo giá đang chờ duyệt */}
                {isOpenForBidding && myQuote.status === 'PENDING' && (
                  <div className="flex flex-wrap items-center justify-end gap-2 border-t border-slate-100 pt-3">
                    <Button
                      variant="secondary"
                      onClick={() => handleWithdrawQuote(myQuote.id)}
                      loading={withdrawing}
                    >
                      Rút báo giá
                    </Button>
                    <Button variant="primary" onClick={() => setIsEditingQuote(true)}>
                      Chỉnh sửa báo giá
                    </Button>
                  </div>
                )}
              </div>
            ) : (
              /* Form chỉnh sửa báo giá */
              <form onSubmit={(e) => handleUpdateQuote(myQuote.id, e)} className="space-y-4">
                <TextArea
                  label="Giải pháp kỹ thuật đề xuất"
                  name="solution"
                  rows={3}
                  required
                  placeholder="Mô tả lại phương án xử lý sự cố..."
                  value={quoteForm.solution}
                  onChange={(e) => setQuoteForm((f) => ({ ...f, solution: e.target.value }))}
                />
                <div className="grid gap-4 sm:grid-cols-2">
                  <TextField
                    label="Giá tiền công (VNĐ)"
                    name="priceLaborVnd"
                    type="number"
                    min={0}
                    required
                    value={quoteForm.priceLaborVnd}
                    onChange={(e) => setQuoteForm((f) => ({ ...f, priceLaborVnd: e.target.value }))}
                  />
                  <TextField
                    label="Giá linh kiện vật tư (VNĐ)"
                    name="priceMaterialsVnd"
                    type="number"
                    min={0}
                    required
                    value={quoteForm.priceMaterialsVnd}
                    onChange={(e) => setQuoteForm((f) => ({ ...f, priceMaterialsVnd: e.target.value }))}
                  />
                </div>
                {quoteForm.priceLaborVnd && quoteForm.priceMaterialsVnd && (
                  <div className="rounded-xl bg-blue-50 border border-blue-100 p-3 text-sm font-semibold text-slate-800 flex justify-between items-center">
                    <span>Tổng giá cập nhật:</span>
                    <span className="text-base text-brand font-bold">
                      {formatCurrency(Number(quoteForm.priceLaborVnd) + Number(quoteForm.priceMaterialsVnd))}
                    </span>
                  </div>
                )}
                <TextArea
                  label="Ghi chú thêm"
                  name="note"
                  rows={2}
                  value={quoteForm.note}
                  onChange={(e) => setQuoteForm((f) => ({ ...f, note: e.target.value }))}
                />
                <div className="flex justify-end gap-2 pt-2">
                  <Button variant="secondary" type="button" onClick={() => setIsEditingQuote(false)}>
                    Hủy bỏ
                  </Button>
                  <Button type="submit" variant="primary" loading={quoteSubmitting}>
                    Lưu cập nhật báo giá
                  </Button>
                </div>
              </form>
            )}
          </Card>
        ) : isOpenForBidding ? (
          /* Thợ CHƯA gửi báo giá và đơn đang mở thầu */
          <Card
            title="Gửi báo giá kỹ thuật cho đơn này"
            description="Khách hàng sẽ xem xét báo giá của bạn cùng với kinh nghiệm và đánh giá sao trên hồ sơ."
          >
            <form onSubmit={handleSubmitQuote} className="space-y-4">
              <TextArea
                label="Giải pháp kỹ thuật đề xuất"
                name="solution"
                rows={3}
                required
                placeholder="Mô tả nguyên nhân phỏng đoán và cách bạn sẽ xử lý sự cố..."
                value={quoteForm.solution}
                onChange={(e) => setQuoteForm((f) => ({ ...f, solution: e.target.value }))}
              />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField
                  label="Giá tiền công (VNĐ)"
                  name="priceLaborVnd"
                  type="number"
                  min={0}
                  required
                  placeholder="300000"
                  value={quoteForm.priceLaborVnd}
                  onChange={(e) => setQuoteForm((f) => ({ ...f, priceLaborVnd: e.target.value }))}
                />
                <TextField
                  label="Giá linh kiện vật tư (VNĐ)"
                  name="priceMaterialsVnd"
                  type="number"
                  min={0}
                  required
                  placeholder="150000"
                  value={quoteForm.priceMaterialsVnd}
                  onChange={(e) => setQuoteForm((f) => ({ ...f, priceMaterialsVnd: e.target.value }))}
                />
              </div>
              {quoteForm.priceLaborVnd && quoteForm.priceMaterialsVnd && (
                <div className="rounded-xl bg-blue-50 border border-blue-100 p-3 text-sm font-semibold text-slate-800 flex justify-between items-center">
                  <span>Tổng giá báo khách:</span>
                  <span className="text-base text-brand font-bold">
                    {formatCurrency(Number(quoteForm.priceLaborVnd) + Number(quoteForm.priceMaterialsVnd))}
                  </span>
                </div>
              )}
              <TextArea
                label="Ghi chú thêm (không bắt buộc)"
                name="note"
                rows={2}
                placeholder="Cam kết bảo hành, thời gian dự kiến có mặt..."
                value={quoteForm.note}
                onChange={(e) => setQuoteForm((f) => ({ ...f, note: e.target.value }))}
              />
              <div className="flex justify-end pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  loading={quoteSubmitting}
                  disabled={!quoteForm.solution.trim() || !quoteForm.priceLaborVnd || !quoteForm.priceMaterialsVnd}
                >
                  🚀 Gửi báo giá ngay
                </Button>
              </div>
            </form>
          </Card>
        ) : (
          /* Đơn không mở thầu và thợ không có báo giá */
          <Card title="Tình trạng nhận báo giá">
            <div className="py-4 text-center text-slate-500 text-sm">
              Đơn này hiện không mở nhận báo giá (Trạng thái: <strong>{req.statusLabel || req.status}</strong>).
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
