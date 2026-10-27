import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Alert from '../components/Alert';
import AppointmentCard from '../components/AppointmentCard';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import MultiImageUploadField from '../components/MultiImageUploadField';
import RequestStatusStepper from '../components/RequestStatusStepper';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import {
  ApiError,
  api,
  attachRequestMedia,
  createAppointment,
  createQuotation,
  fetchAppointmentsForRequest,
  formatCurrency,
  updateQuotation,
  updateRequestStatus,
  withdrawQuotation
} from '../lib/api';
import { useAuth } from '../lib/auth';
import { parseNumericUserId, type Appointment, type Quotation, type RepairRequestDetail } from '../lib/types';

const TECHNICIAN_NAV = [
  { to: '/tho', label: 'Bảng điều khiển' },
  { to: '/tho/lich-su', label: 'Việc đã hoàn thành' },
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

  // Thao tác vòng đời (Khảo sát, Sửa chữa, Nghiệm thu)
  const [updatingLifecycle, setUpdatingLifecycle] = useState(false);
  const [lifecycleError, setLifecycleError] = useState<string | null>(null);

  // Modal Báo giá phát sinh khi Khảo sát
  const [showCostApprovalModal, setShowCostApprovalModal] = useState(false);
  const [extraCostReason, setExtraCostReason] = useState('');
  const [extraPartsCost, setExtraPartsCost] = useState<number>(150000);
  const [extraCostImages, setExtraCostImages] = useState<string[]>([]);
  const [willRescheduleWithCost, setWillRescheduleWithCost] = useState(false);

  // Modal Hẹn lại ngày thi công (Luồng mới sau khảo sát: Cần chuẩn bị linh kiện/dụng cụ)
  const [showRescheduleRepairModal, setShowRescheduleRepairModal] = useState(false);
  const [rescheduleRepairReason, setRescheduleRepairReason] = useState('Cần chuẩn bị thêm đồ nghề chuyên dụng & đặt hàng linh kiện thay thế');
  const [newRepairDate, setNewRepairDate] = useState('');
  const [newRepairTime, setNewRepairTime] = useState('09:00');
  const [rescheduleRepairNote, setRescheduleRepairNote] = useState('');
  const [submittingReschedule, setSubmittingReschedule] = useState(false);
  const [rescheduleSuccessMsg, setRescheduleSuccessMsg] = useState<string | null>(null);

  // Modal Hoàn thành & Mời nghiệm thu
  const [showAcceptanceModal, setShowAcceptanceModal] = useState(false);
  const [acceptanceNote, setAcceptanceNote] = useState('');

  async function handleRescheduleRepair(e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    setSubmittingReschedule(true);
    setLifecycleError(null);
    try {
      const numericUserId = parseNumericUserId(user?.id) ?? undefined;
      await createAppointment({
        repairRequestId: detail.request.id,
        technicianId: numericUserId,
        appointmentType: 'REPAIR',
        scheduledDate: newRepairDate,
        scheduledTime: newRepairTime.length === 5 ? `${newRepairTime}:00` : newRepairTime,
        notes: `Hẹn lại ngày thi công: ${rescheduleRepairReason}. Ghi chú: ${rescheduleRepairNote.trim() || 'Thợ sẽ có mặt đúng giờ để tiến hành thi công.'}`
      });
      setShowRescheduleRepairModal(false);
      setRescheduleSuccessMsg(`Đã tạo lịch hẹn thi công lại vào ngày ${newRepairDate} lúc ${newRepairTime}. Khách hàng và Admin đã có thể theo dõi.`);
      loadDetail();
    } catch (err: any) {
      setLifecycleError(err instanceof ApiError ? err.message : 'Không tạo được lịch hẹn thi công mới.');
    } finally {
      setSubmittingReschedule(false);
    }
  }

  async function handleTransition(newStatus: string, note?: string, price?: number) {
    if (!detail) return;
    setUpdatingLifecycle(true);
    setLifecycleError(null);
    try {
      await updateRequestStatus(detail.request.id, newStatus, note, price);
      await loadDetail();
      setShowCostApprovalModal(false);
      setShowAcceptanceModal(false);
    } catch (err: any) {
      setLifecycleError(err?.message || 'Có lỗi xảy ra khi cập nhật tiến trình');
    } finally {
      setUpdatingLifecycle(false);
    }
  }

  async function handleCreateAppointment(e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    setCreatingAppt(true);
    setCreateApptError(null);
    try {
      const numericUserId = parseNumericUserId(user?.id) ?? undefined;
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

  // Báo giá & Đấu giá cạnh tranh (Chỉ đấu giá thuần túy về giá)
  const [bidPrice, setBidPrice] = useState<number>(0);
  const [isEditingQuote, setIsEditingQuote] = useState<boolean>(false);
  const [submittingQuote, setSubmittingQuote] = useState<boolean>(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [quoteSuccess, setQuoteSuccess] = useState<string | null>(null);
  const [withdrawingQuote, setWithdrawingQuote] = useState<boolean>(false);

  const numericUserId = parseNumericUserId(user?.id);

  async function loadDetail(silent = false) {
    if (!silent) setLoading(true);
    try {
      const res = await api.get<RepairRequestDetail>(`/repair-requests/${id}`);
      setDetail(res.data);
      setError(null);

      // Chỉ điền sẵn dữ liệu vào form ở lần tải đầu tiên (không silent) để tránh đè khi thợ đang nhập
      if (!silent) {
        const myQuote = res.data.quotations?.find(
          (q) => q.isMine || (numericUserId && q.technicianId === numericUserId)
        );
        if (myQuote && myQuote.status === 'PENDING') {
          setBidPrice(myQuote.totalPrice || myQuote.priceLaborVnd || 0);
        } else if (!myQuote) {
          // Mặc định giá báo là ngân sách khách đưa ra (nếu có)
          setBidPrice(res.data.request.budgetRef || 0);
        }
      }
    } catch (err) {
      if (!silent) {
        setError(err instanceof ApiError ? err.message : 'Không tải được chi tiết yêu cầu sửa chữa.');
      }
    } finally {
      if (!silent) setLoading(false);
    }

    if (id && !silent) {
      fetchAppointmentsForRequest(Number(id))
        .then((res) => setAppointments(res.data || []))
        .catch(() => setAppointments([]));
    }
  }

  useEffect(() => {
    loadDetail(false);
  }, [id]);

  // Cập nhật API liên tục ngầm (mỗi 3.5s) để sàn đấu giá luôn đồng bộ dữ liệu mới nhất
  useEffect(() => {
    if (!id) return;
    const intervalId = setInterval(() => {
      // Chỉ đồng bộ ngầm khi yêu cầu đang mở nhận báo giá (đấu giá)
      if (!detail || detail.request.status === 'OPEN' || detail.request.status === 'BIDDING_OPEN') {
        loadDetail(true);
      }
    }, 3500);

    return () => clearInterval(intervalId);
  }, [id, detail?.request?.status]);

  function handleStartEditQuote(quote: Quotation) {
    setBidPrice(quote.totalPrice || quote.priceLaborVnd || 0);
    setIsEditingQuote(true);
    setQuoteError(null);
    setQuoteSuccess(null);
  }

  async function handleSubmitQuote(e: React.FormEvent) {
    e.preventDefault();
    if (!detail) return;
    if (bidPrice <= 0) {
      setQuoteError('Vui lòng nhập mức giá báo hợp lệ (> 0 đ).');
      return;
    }

    setSubmittingQuote(true);
    setQuoteError(null);
    setQuoteSuccess(null);
    try {
      const myQuotation = detail.quotations?.find(
        (q) => q.isMine || (numericUserId && q.technicianId === numericUserId)
      );
      const payload = {
        solution: 'Báo giá sửa chữa theo yêu cầu',
        priceLaborVnd: bidPrice,
        priceMaterialsVnd: 0
      };

      if (myQuotation && isEditingQuote) {
        await updateQuotation(detail.request.id, myQuotation.id, payload);
        setQuoteSuccess('Đã cập nhật mức giá thành công lên sàn đấu giá.');
      } else {
        await createQuotation(detail.request.id, payload);
        setQuoteSuccess('Đã gửi mức giá thành công vào phiên đấu giá công khai.');
      }
      setIsEditingQuote(false);
      await loadDetail(true);
    } catch (err: any) {
      setQuoteError(err instanceof ApiError ? err.message : 'Lỗi khi gửi báo giá.');
    } finally {
      setSubmittingQuote(false);
    }
  }

  async function handleWithdrawQuote(quotationId: number) {
    if (!detail) return;
    const confirm = window.confirm('Bạn có chắc chắn muốn rút báo giá này? Bạn sẽ có thể gửi lại sau.');
    if (!confirm) return;

    setWithdrawingQuote(true);
    setQuoteError(null);
    setQuoteSuccess(null);
    try {
      await withdrawQuotation(detail.request.id, quotationId);
      setQuoteSuccess('Đã rút báo giá thành công khỏi sàn đấu giá.');
      setIsEditingQuote(false);
      await loadDetail(true);
    } catch (err: any) {
      setQuoteError(err instanceof ApiError ? err.message : 'Không thể rút báo giá.');
    } finally {
      setWithdrawingQuote(false);
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
        {/* Thanh trạng thái & vòng đời đơn */}
        <RequestStatusStepper request={req} workProgress={detail.workProgress} />
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

        {/* Nhận việc & Thao tác tiến trình thi công */}
        {(() => {
          const myQuote = detail.quotations?.find(
            (q) => q.isMine || (numericUserId && q.technicianId === numericUserId)
          );
          const isAssignedToMe = (req.technicianId && numericUserId && req.technicianId === numericUserId) || (myQuote?.status === 'ACCEPTED');
          const isOpenForBidding = req.status === 'OPEN' || req.status === 'BIDDING_OPEN';
          const isDeadlineExpired = req.applyDeadline ? new Date(req.applyDeadline).getTime() <= Date.now() : false;

          const activeAppointments = appointments.filter(
            (a) => a.technicianId === numericUserId && a.status !== 'CANCELLED' && a.status !== 'REJECTED'
          );
          const hasActiveAppointment = activeAppointments.length > 0;
          const latestAppt = activeAppointments[0];
          const hasConfirmedAppointment = activeAppointments.some((a) => a.status === 'CONFIRMED');

          if (isAssignedToMe) {
            const isSwitchedOrder = detail.workProgress?.some(
              (wp) => wp.note?.includes('đổi sang thợ') || wp.note?.includes('Khách hàng đã đổi sang thợ')
            );

            return (
              <Card
                title="Bạn đã được giao việc này"
                description="Theo dõi chi phí và cập nhật từng bước khảo sát, thi công, nghiệm thu."
              >
                {isSwitchedOrder && (
                  <div className="mb-4 rounded-xl border border-emerald-300 bg-emerald-50/90 p-4 shadow-xs">
                    <div className="flex items-start gap-2.5">
                      <span className="text-xl">🎉</span>
                      <div>
                        <p className="text-xs font-bold text-emerald-950 uppercase tracking-wider">
                          Đơn hàng đã được khách hàng chuyển giao cho bạn tiếp nhận
                        </p>
                        <p className="text-xs text-emerald-800 mt-1 leading-relaxed">
                          Yêu cầu được chuyển từ kỹ thuật viên trước. Tiền cọc 30% ({formatCurrency(req.depositAmount > 0 ? req.depositAmount : Math.round((req.agreedPrice || 0) * 0.3))}) đã được bảo chứng an toàn trong hệ thống. Vui lòng chủ động bấm nút <strong>📅 Đổi / Thêm lịch hẹn</strong> bên dưới hoặc gọi cho khách hàng để sắp xếp thi công sớm nhất!
                        </p>
                      </div>
                    </div>
                  </div>
                )}
                <dl className="divide-y divide-slate-100 text-sm">
                  <Row label="💰 Giá chốt hiện tại" value={formatCurrency(req.agreedPrice || req.budgetRef)} />
                  <Row label="💵 Tiền đặt cọc (30%)" value={formatCurrency(req.depositAmount > 0 ? req.depositAmount : Math.round((req.agreedPrice || req.budgetRef || 0) * 0.3))} />
                  {myQuote?.acceptedAt && (
                    <Row label="🕒 Nhận việc lúc" value={new Date(myQuote.acceptedAt).toLocaleString('vi-VN')} />
                  )}
                </dl>

                {/* KHỐI THAO TÁC TIẾN TRÌNH CÔNG VIỆC */}
                <div className="mt-5 border-t border-slate-100 pt-4">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-1.5">
                    <span>⚡</span> Thao tác tiến độ thực hiện
                  </h4>

                  {lifecycleError && (
                    <div className="mb-3">
                      <Alert tone="error">{lifecycleError}</Alert>
                    </div>
                  )}

                  {rescheduleSuccessMsg && (
                    <div className="mb-3 rounded-xl border border-emerald-300 bg-emerald-50 p-3.5 flex items-center justify-between text-xs font-semibold text-emerald-900 shadow-xs">
                      <div className="flex items-center gap-2">
                        <span>📅</span>
                        <span>{rescheduleSuccessMsg}</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setRescheduleSuccessMsg(null)}
                        className="text-emerald-700 hover:text-emerald-900 font-bold ml-2 text-sm"
                      >
                        ✕
                      </button>
                    </div>
                  )}

                  {req.status === 'ASSIGNED' && (
                    <div className="space-y-3 mb-3">
                      <div className="rounded-xl bg-amber-50/90 border border-amber-300 p-4 space-y-2">
                        <div className="flex items-start gap-2.5">
                          <span className="text-xl">⏳</span>
                          <div>
                            <p className="text-xs font-bold text-amber-950">
                              Khách hàng chưa hoàn tất đặt cọc 30%
                            </p>
                            <p className="text-xs text-amber-900 mt-0.5 leading-relaxed">
                              Yêu cầu đã được giao cho bạn. Bạn có thể chủ động liên hệ đặt lịch hẹn trước, nhưng <strong>chỉ được phép bắt đầu khảo sát sau khi khách hàng đã hoàn tất thanh toán cọc 30%</strong> ({formatCurrency(req.depositAmount > 0 ? req.depositAmount : Math.round((req.agreedPrice || 0) * 0.3))}) trên hệ thống.
                            </p>
                          </div>
                        </div>
                      </div>

                      {!hasActiveAppointment ? (
                        <div className="rounded-xl bg-amber-50/90 border border-amber-300 p-4 space-y-3">
                          <div className="flex items-start gap-2.5">
                            <span className="text-xl">📅</span>
                            <div>
                              <p className="text-xs font-bold text-amber-950">
                                Bước 1: Chốt lịch hẹn khảo sát với khách hàng
                              </p>
                              <p className="text-xs text-amber-900 mt-0.5 leading-relaxed">
                                Vui lòng liên hệ và đặt lịch hẹn khảo sát trên hệ thống trước khi đến nhà khách hàng.
                              </p>
                            </div>
                          </div>
                          <div className="flex flex-wrap items-center gap-3 pt-1">
                            <Button
                              variant="primary"
                              onClick={() => {
                                setCreateApptError(null);
                                setShowCreateApptModal(true);
                              }}
                              className="text-xs py-2 px-4 shadow-sm bg-amber-600 hover:bg-amber-700 text-white"
                            >
                              📅 + Đặt lịch hẹn khảo sát ngay
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="rounded-xl bg-amber-50/90 border border-amber-300 p-4 space-y-3">
                          <div className="flex items-start gap-2.5">
                            <span className="text-xl">📅</span>
                            <div>
                              <p className="text-xs font-bold text-amber-950">
                                Lịch hẹn: {latestAppt.scheduledTime?.slice(0, 5)} ngày {new Date(latestAppt.scheduledDate).toLocaleDateString('vi-VN')} ({latestAppt.statusLabel || latestAppt.status})
                              </p>
                              <p className="text-xs text-amber-900 mt-0.5 leading-relaxed">
                                Lịch hẹn đã được ghi nhận. Nút bắt đầu khảo sát sẽ tự động mở sau khi khách hàng thanh toán cọc 30%.
                              </p>
                            </div>
                          </div>
                          <div className="flex flex-wrap items-center gap-2.5">
                            <Button
                              variant="secondary"
                              onClick={() => {
                                setCreateApptError(null);
                                setShowCreateApptModal(true);
                              }}
                              className="text-xs py-2 px-3 text-slate-700 bg-white"
                            >
                              📅 Đổi / Thêm lịch hẹn
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {req.status === 'DEPOSIT_PAID' && (
                    <div className="space-y-3 mb-3">
                      <div className="rounded-xl bg-teal-50/90 border border-teal-300 p-4 space-y-2">
                        <div className="flex items-start gap-2.5">
                          <span className="text-xl">✅</span>
                          <div>
                            <p className="text-xs font-bold text-teal-950">
                              Khách hàng đã đặt cọc 30% thành công • Sẵn sàng khảo sát hiện trường
                            </p>
                            <p className="text-xs text-teal-900 mt-0.5 leading-relaxed">
                              Khoản tiền cọc đã được hệ thống FixLink xác nhận. Bạn hãy đến hiện trường theo đúng lịch hẹn để khảo sát thiết bị.
                            </p>
                          </div>
                        </div>
                      </div>

                      {!hasActiveAppointment ? (
                        <div className="rounded-xl bg-teal-50/90 border border-teal-200/90 p-4 space-y-3">
                          <div className="flex items-start gap-2.5">
                            <span className="text-xl">📅</span>
                            <div>
                              <p className="text-xs font-bold text-teal-950">
                                Đặt lịch hẹn khảo sát với khách hàng
                              </p>
                              <p className="text-xs text-teal-800 mt-0.5 leading-relaxed">
                                Vui lòng lên lịch hẹn với khách hàng trước khi đến khảo sát hiện trường.
                              </p>
                            </div>
                          </div>
                          <div className="flex flex-wrap items-center gap-3 pt-1">
                            <Button
                              variant="primary"
                              onClick={() => {
                                setCreateApptError(null);
                                setShowCreateApptModal(true);
                              }}
                              className="text-xs py-2 px-4 shadow-sm"
                            >
                              📅 + Đặt lịch hẹn khảo sát ngay
                            </Button>
                          </div>
                        </div>
                      ) : !hasConfirmedAppointment ? (
                        <div className="rounded-xl bg-amber-50/90 border border-amber-300 p-4 space-y-3">
                          <div className="flex items-start gap-2.5">
                            <span className="text-xl">⏳</span>
                            <div>
                              <p className="text-xs font-bold text-amber-950">
                                Lịch hẹn đang chờ khách hàng xác nhận ({latestAppt.scheduledTime?.slice(0, 5)} ngày {new Date(latestAppt.scheduledDate).toLocaleDateString('vi-VN')})
                              </p>
                              <p className="text-xs text-amber-900 mt-0.5 leading-relaxed">
                                Bạn đã đề xuất lịch hẹn. Nút <strong>Bắt đầu khảo sát hiện trường</strong> sẽ tự động mở ngay sau khi khách hàng bấm đồng ý xác nhận cuộc hẹn này.
                              </p>
                            </div>
                          </div>
                          <div className="flex flex-wrap items-center gap-2.5">
                            <Button
                              variant="secondary"
                              onClick={() => {
                                setCreateApptError(null);
                                setShowCreateApptModal(true);
                              }}
                              className="text-xs py-2 px-3 text-slate-700 bg-white"
                            >
                              📅 Đổi / Thêm lịch hẹn
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="rounded-xl bg-teal-50/90 border border-teal-200/90 p-4 space-y-3">
                          <div className="flex items-start gap-2.5">
                            <span className="text-xl">📍</span>
                            <div>
                              <p className="text-xs font-bold text-teal-950">
                                Khảo sát hiện trường (Đã có lịch hẹn & Đã cọc)
                              </p>
                              <p className="text-xs text-teal-800 mt-0.5 leading-relaxed">
                                Lịch hẹn đã được khách hàng xác nhận: <strong>{latestAppt.scheduledTime?.slice(0, 5)} ngày {new Date(latestAppt.scheduledDate).toLocaleDateString('vi-VN')}</strong>.
                                Khi bạn đã có mặt tại nhà khách hàng theo lịch hẹn, hãy bấm bắt đầu khảo sát để ghi nhận mốc hiện trường.
                              </p>
                            </div>
                          </div>
                          <div className="flex flex-wrap items-center gap-2.5">
                            <Button
                              variant="primary"
                              loading={updatingLifecycle}
                              onClick={() => handleTransition('INSPECTING', 'Kỹ thuật viên đã có mặt tại hiện trường theo lịch hẹn và bắt đầu khảo sát thiết bị')}
                              className="w-full sm:w-auto text-xs py-2 px-4 shadow-sm"
                            >
                              🔍 Bắt đầu khảo sát hiện trường
                            </Button>
                            <Button
                              variant="secondary"
                              onClick={() => {
                                setCreateApptError(null);
                                setShowCreateApptModal(true);
                              }}
                              className="text-xs py-2 px-3 text-slate-700 bg-white"
                            >
                              📅 Đổi / Thêm lịch hẹn
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {req.status === 'INSPECTING' && (
                    <div className="rounded-xl bg-amber-50/90 border border-amber-200/90 p-4 space-y-3">
                      <div className="flex items-start gap-2.5">
                        <span className="text-xl">🔍</span>
                        <div>
                          <p className="text-xs font-bold text-amber-950">
                            Đang trong quá trình khảo sát thiết bị thực tế
                          </p>
                          <p className="text-xs text-amber-800 mt-0.5 leading-relaxed">
                            Hãy kiểm tra chi tiết thiết bị. Chọn một trong các hướng xử lý minh bạch sau khảo sát:
                          </p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2.5 pt-1">
                        <Button
                          variant="primary"
                          loading={updatingLifecycle}
                          onClick={() => handleTransition('IN_PROGRESS', 'Khảo sát hoàn tất, tiến hành sửa chữa theo ngân sách đã chốt')}
                          className="text-xs py-2 px-4 shadow-sm"
                        >
                          🛠️ Bắt đầu sửa ngay (Đúng giá ban đầu)
                        </Button>
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setExtraCostReason('');
                            setExtraPartsCost(150000);
                            setExtraCostImages([]);
                            setShowCostApprovalModal(true);
                          }}
                          className="text-xs py-2 px-4 border-amber-300 text-amber-900 bg-white hover:bg-amber-100"
                        >
                          ⚠️ Báo giá phát sinh linh kiện
                        </Button>
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setRescheduleRepairReason('Cần chuẩn bị thêm đồ nghề chuyên dụng & đặt hàng linh kiện thay thế');
                            setNewRepairDate(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
                            setNewRepairTime('09:00');
                            setRescheduleRepairNote('');
                            setShowRescheduleRepairModal(true);
                          }}
                          className="text-xs py-2 px-4 border-indigo-300 text-indigo-900 bg-white hover:bg-indigo-50 shadow-xs"
                        >
                          📅 Hẹn lại ngày thi công (Cần chuẩn bị đồ nghề / linh kiện)
                        </Button>
                      </div>
                    </div>
                  )}

                  {req.status === 'AWAITING_COST_APPROVAL' && (() => {
                    const depositPaid = req.depositAmount > 0 ? req.depositAmount : Math.round((req.agreedPrice || 0) * 0.3);
                    const myAcceptedQuote = detail.quotations?.find((q) => q.status === 'ACCEPTED' || q.technicianId === numericUserId);
                    const initialPrice = myAcceptedQuote?.totalPrice || (req.depositAmount > 0 ? Math.round(req.depositAmount / 0.3) : (req.budgetRef || 0));
                    const surchargeAmount = Math.max(0, (req.agreedPrice || 0) - initialPrice);
                    const remainingAfterAcceptance = Math.max(0, (req.agreedPrice || 0) - depositPaid);

                    return (
                      <div className="rounded-xl bg-amber-50 border border-amber-300 p-4 space-y-3">
                        <div className="flex items-center gap-2">
                          <span className="text-xl">⏳</span>
                          <div>
                            <p className="text-xs font-bold text-amber-950">
                              Đang chờ khách hàng phê duyệt báo giá phát sinh ({formatCurrency(req.agreedPrice || 0)})
                            </p>
                            <p className="text-xs text-amber-800 mt-0.5 leading-relaxed">
                              Yêu cầu duyệt chi phí đã được gửi tới khách hàng. Kỹ thuật viên vui lòng tạm dừng thi công cho đến khi khách hàng bấm xác nhận trên ứng dụng.
                            </p>
                          </div>
                        </div>

                        {/* Bảng chiết tính minh bạch tuyệt đối */}
                        <div className="rounded-xl bg-white border border-amber-200 p-3.5 space-y-1.5 text-xs shadow-xs">
                          <div className="flex justify-between text-slate-600">
                            <span>1. Giá thỏa thuận ban đầu:</span>
                            <span className="font-semibold text-slate-800">{formatCurrency(initialPrice)}</span>
                          </div>
                          <div className="flex justify-between text-amber-700">
                            <span>2. Chi phí linh kiện phát sinh:</span>
                            <span className="font-bold text-amber-800">+{formatCurrency(surchargeAmount)}</span>
                          </div>
                          <div className="flex justify-between border-t border-slate-200 pt-1.5 font-bold text-slate-900">
                            <span>3. Tổng giá trị mới trọn gói:</span>
                            <span className="text-blue-700">{formatCurrency(req.agreedPrice || 0)}</span>
                          </div>
                          <div className="flex justify-between text-blue-700">
                            <span>4. Khách đã đặt cọc 30% (Không đổi):</span>
                            <span className="font-semibold">-{formatCurrency(depositPaid)}</span>
                          </div>
                          <div className="flex justify-between border-t border-dashed border-slate-300 pt-1.5 font-bold text-emerald-700 bg-emerald-50/70 p-2 rounded">
                            <span>5. Số tiền khách sẽ thanh toán khi nghiệm thu:</span>
                            <span className="text-emerald-800 font-bold">{formatCurrency(remainingAfterAcceptance)}</span>
                          </div>
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-amber-200/80">
                          <span className="text-[11px] text-amber-900 italic">
                            💡 Trong lúc chờ khách duyệt, bạn có thể thống nhất hẹn lại ngày thi công phù hợp.
                          </span>
                          <Button
                            variant="secondary"
                            onClick={() => {
                              setRescheduleRepairReason('Cần chuẩn bị linh kiện đã duyệt và hẹn lại ngày thi công');
                              setNewRepairDate(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
                              setNewRepairTime('09:00');
                              setRescheduleRepairNote('');
                              setShowRescheduleRepairModal(true);
                            }}
                            className="text-xs py-1.5 px-3 border-amber-300 text-amber-900 bg-white hover:bg-amber-100 shadow-xs"
                          >
                            📅 Hẹn lại ngày thi công mới
                          </Button>
                        </div>
                      </div>
                    );
                  })()}

                  {req.status === 'IN_PROGRESS' && (
                    <div className="rounded-xl bg-indigo-50/90 border border-indigo-200/90 p-4 space-y-3">
                      <div className="flex items-start gap-2.5">
                        <span className="text-xl">🛠️</span>
                        <div>
                          <p className="text-xs font-bold text-indigo-950">
                            Đang tiến hành sửa chữa thiết bị
                          </p>
                          <p className="text-xs text-indigo-800 mt-0.5 leading-relaxed">
                            Sau khi hoàn tất thay thế/sửa chữa và cho thiết bị chạy thử ổn định (test run 15–30 phút), hãy bấm gửi báo cáo nghiệm thu.
                          </p>
                        </div>
                      </div>
                      <div className="flex flex-wrap items-center gap-2.5 pt-1">
                        <Button
                          variant="primary"
                          onClick={() => {
                            setAcceptanceNote('Đã hoàn thành sửa chữa, chạy thử nghiệm thiết bị hoạt động tốt và bàn giao.');
                            setShowAcceptanceModal(true);
                          }}
                          className="text-xs py-2 px-4 shadow-sm bg-indigo-600 hover:bg-indigo-700"
                        >
                          📋 Báo cáo hoàn thành & Mời nghiệm thu
                        </Button>
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setRescheduleRepairReason('Cần chuẩn bị linh kiện đã duyệt và hẹn lại ngày thi công');
                            setNewRepairDate(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
                            setNewRepairTime('09:00');
                            setRescheduleRepairNote('');
                            setShowRescheduleRepairModal(true);
                          }}
                          className="text-xs py-2 px-4 border-indigo-300 text-indigo-900 bg-white hover:bg-indigo-50 shadow-xs"
                        >
                          📅 Hẹn lại ngày thi công (Chuẩn bị đồ nghề / linh kiện)
                        </Button>
                      </div>
                    </div>
                  )}

                  {req.status === 'AWAITING_ACCEPTANCE' && (
                    <div className="rounded-xl bg-blue-50 border border-blue-200 p-4">
                      <p className="text-xs font-bold text-blue-950 flex items-center gap-1.5">
                        <span>📋</span> Đã gửi thông báo nghiệm thu cho khách hàng
                      </p>
                      <p className="text-xs text-blue-800 mt-1 leading-relaxed">
                        Khách hàng đang kiểm tra thiết bị. Khi khách bấm nghiệm thu đạt, tiền công sẽ được giải ngân vào ví thợ và kích hoạt bảo hành điện tử.
                      </p>
                    </div>
                  )}

                  {req.status === 'COMPLETED' && (
                    <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-4">
                      <p className="text-xs font-bold text-emerald-950 flex items-center gap-1.5">
                        <span>✅</span> Yêu cầu sửa chữa đã hoàn thành
                      </p>
                      <p className="text-xs text-emerald-800 mt-1 leading-relaxed">
                        Đơn hàng đã được khách hàng nghiệm thu đạt chất lượng. Bảo hành điện tử chính thức đã được kích hoạt.
                      </p>
                    </div>
                  )}
                </div>
              </Card>
            );
          }

          if (isOpenForBidding) {
            const allQuotes = detail.quotations || [];
            const activeQuotes = allQuotes.filter((q) => q.status === 'PENDING' || q.status === 'ACCEPTED');
            // Sắp xếp báo giá cạnh tranh: mức giá tốt nhất (thấp nhất) xếp trên cùng
            const sortedQuotes = [...activeQuotes].sort((a, b) => (a.totalPrice || 0) - (b.totalPrice || 0));
            const lowestPrice = sortedQuotes.length > 0 ? sortedQuotes[0].totalPrice : null;

            return (
              <div className="space-y-6">
                {/* 1. SÀN ĐẤU GIÁ CÔNG KHAI & ẨN DANH */}
                <Card
                  title="Sàn đấu giá công khai (Ẩn danh đối thủ)"
                  description="Các mức giá báo được hiển thị công khai để thợ nắm thông tin, chủ động cân nhắc rút hoặc trả giá tiếp."
                  actions={
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                      <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
                      Tự động cập nhật trực tiếp
                    </span>
                  }
                >
                  {quoteSuccess && (
                    <div className="mb-4">
                      <Alert tone="success">{quoteSuccess}</Alert>
                    </div>
                  )}
                  {quoteError && (
                    <div className="mb-4">
                      <Alert tone="error">{quoteError}</Alert>
                    </div>
                  )}

                  {isDeadlineExpired && (
                    <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 mb-4 text-center">
                      <p className="font-bold text-rose-900 text-sm">⛔ Yêu cầu đã hết hạn nhận báo giá</p>
                      <p className="text-xs text-rose-700 mt-1">
                        Phiên đấu giá đã kết thúc vào lúc {new Date(req.applyDeadline!).toLocaleString('vi-VN')}.
                      </p>
                    </div>
                  )}

                  {/* Thanh thông số tổng quan */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5">
                    <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/80">
                      <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider">Ngân sách khách</span>
                      <p className="font-display font-bold text-base text-slate-800 mt-0.5">
                        {formatCurrency(req.budgetRef)}
                      </p>
                    </div>
                    <div className="bg-emerald-50/70 rounded-xl p-3 border border-emerald-200/80">
                      <span className="text-[11px] font-medium text-emerald-700 uppercase tracking-wider">Giá thấp nhất</span>
                      <p className="font-display font-bold text-base text-emerald-700 mt-0.5">
                        {lowestPrice != null ? formatCurrency(lowestPrice) : 'Chưa có'}
                      </p>
                    </div>
                    <div className="bg-blue-50/70 rounded-xl p-3 border border-blue-200/80">
                      <span className="text-[11px] font-medium text-blue-700 uppercase tracking-wider">Thợ tham gia</span>
                      <p className="font-display font-bold text-base text-blue-800 mt-0.5">
                        {sortedQuotes.length} báo giá
                      </p>
                    </div>
                    <div className="bg-amber-50/70 rounded-xl p-3 border border-amber-200/80">
                      <span className="text-[11px] font-medium text-amber-700 uppercase tracking-wider">Giá của bạn</span>
                      <p className="font-display font-bold text-base text-amber-900 mt-0.5">
                        {myQuote ? formatCurrency(myQuote.totalPrice) : 'Chưa báo giá'}
                      </p>
                    </div>
                  </div>

                  {/* Danh sách các mức giá đang cạnh tranh */}
                  <div className="space-y-3">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center justify-between">
                      <span>Bảng giá đang cạnh tranh ({sortedQuotes.length})</span>
                      <span className="text-[11px] font-normal text-slate-400">Sắp xếp từ giá thấp nhất</span>
                    </h4>

                    {sortedQuotes.length === 0 ? (
                      <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/50 p-6 text-center text-sm text-slate-500">
                        Chưa có thợ nào gửi báo giá. Hãy là người đầu tiên đưa ra mức giá tốt nhất để nhận việc!
                      </div>
                    ) : (
                      <div className="space-y-3">
                        {sortedQuotes.map((q, idx) => {
                          const isLowest = q.totalPrice === lowestPrice;
                          const isMine = q.isMine || (numericUserId && q.technicianId === numericUserId);

                          return (
                            <div
                              key={q.id}
                              className={`rounded-2xl p-4 transition-all ${
                                isMine
                                  ? 'border-2 border-brand/50 bg-blue-50/40 shadow-xs ring-1 ring-brand/20'
                                  : 'border border-slate-200 bg-white hover:border-slate-300'
                              }`}
                            >
                              <div className="flex flex-wrap items-start justify-between gap-3">
                                <div className="space-y-1">
                                  <div className="flex items-center gap-2 flex-wrap">
                                    {isMine ? (
                                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-brand text-white shadow-xs">
                                        ⭐ Báo giá của bạn
                                      </span>
                                    ) : (
                                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                                        🛡️ {q.technicianName || `Thợ ẩn danh #${idx + 1}`}
                                      </span>
                                    )}

                                    {isLowest && (
                                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-emerald-100 text-emerald-800">
                                        🏆 Giá tốt nhất
                                      </span>
                                    )}

                                    <span className="text-[11px] text-slate-400">
                                      {new Date(q.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} • {new Date(q.createdAt).toLocaleDateString('vi-VN')}
                                    </span>
                                  </div>
                                </div>

                                <div className="text-right">
                                  <span className="text-[11px] text-slate-400 font-medium">Mức giá đề xuất</span>
                                  <div className={`font-display font-bold text-lg ${isMine ? 'text-brand' : 'text-slate-900'}`}>
                                    {formatCurrency(q.totalPrice)}
                                  </div>
                                </div>
                              </div>

                              {/* Thao tác trực tiếp cho báo giá của chính mình */}
                              {isMine && q.status === 'PENDING' && (
                                <div className="mt-3 pt-3 border-t border-blue-100/80 flex flex-wrap items-center justify-between gap-2">
                                  <span className="text-xs text-blue-700 font-medium">
                                    💡 Bạn có thể điều chỉnh giá bất kỳ lúc nào để cạnh tranh hơn.
                                  </span>
                                  <div className="flex items-center gap-2">
                                    <Button
                                      variant="secondary"
                                      loading={withdrawingQuote}
                                      onClick={() => handleWithdrawQuote(q.id)}
                                      className="text-xs py-1.5 px-3 text-rose-600 border-rose-200 hover:bg-rose-50"
                                    >
                                      🗑️ Rút giá
                                    </Button>
                                    <Button
                                      variant="primary"
                                      onClick={() => handleStartEditQuote(q)}
                                      className="text-xs py-1.5 px-3"
                                    >
                                      ✏️ Điều chỉnh mức giá
                                    </Button>
                                  </div>
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                    <span>🔒 Danh tính các thợ khác được bảo mật tuyệt đối nhằm ngăn ngừa mâu thuẫn phát sinh.</span>
                  </div>
                </Card>

                {/* 2. FORM GỬI HOẶC ĐIỀU CHỈNH MỨC GIÁ (CHỈ NHẬP GIÁ THUẦN TÚY) */}
                {(!myQuote || isEditingQuote) ? (
                  <Card
                    title={isEditingQuote ? '✏️ Điều chỉnh mức giá / Trả giá lại' : '🚀 Gửi mức giá cạnh tranh'}
                    description={
                      isEditingQuote
                        ? 'Nhập lại mức giá bạn đề xuất để cạnh tranh giành quyền làm việc với khách hàng.'
                        : 'Nhập mức giá bạn đề xuất để tham gia vào phiên đấu giá công khai.'
                    }
                  >
                    <form onSubmit={handleSubmitQuote} className="space-y-4">
                      <div>
                        <TextField
                          label="Mức giá báo của bạn (VNĐ) *"
                          type="number"
                          min="0"
                          max={req.budgetRef ? String(req.budgetRef) : undefined}
                          step="10000"
                          value={bidPrice ? String(bidPrice) : ''}
                          onChange={(e) => setBidPrice(Number(e.target.value))}
                          placeholder="Ví dụ: 300000"
                          required
                        />
                        {req.budgetRef > 0 && bidPrice > req.budgetRef && (
                          <p className="text-xs text-rose-600 font-medium mt-1.5 flex items-center gap-1">
                            <span>⛔</span> Giá báo ({formatCurrency(bidPrice)}) vượt quá ngân sách khách hàng ({formatCurrency(req.budgetRef)}). Vui lòng nhập mức giá bằng hoặc thấp hơn ngân sách.
                          </p>
                        )}
                        {bidPrice > 0 && (!req.budgetRef || bidPrice <= req.budgetRef) && (
                          <p className="text-xs text-slate-500 mt-1.5 flex items-center gap-1.5">
                            <span>💰 Mức giá hiển thị trên sàn:</span>
                            <strong className="text-brand font-bold">{formatCurrency(bidPrice)}</strong>
                          </p>
                        )}
                        {req.budgetRef > 0 && (
                          <div className="flex flex-wrap items-center gap-1.5 mt-2">
                            <span className="text-[11px] text-slate-400">Chọn nhanh:</span>
                            <button
                              type="button"
                              onClick={() => setBidPrice(req.budgetRef)}
                              className="px-2 py-0.5 text-[11px] rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium"
                            >
                              Bằng ngân sách ({formatCurrency(req.budgetRef)})
                            </button>
                            {req.budgetRef >= 50000 && (
                              <button
                                type="button"
                                onClick={() => setBidPrice(Math.max(10000, req.budgetRef - 50000))}
                                className="px-2 py-0.5 text-[11px] rounded bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-medium"
                              >
                                -50.000đ ({formatCurrency(Math.max(10000, req.budgetRef - 50000))})
                              </button>
                            )}
                          </div>
                        )}
                      </div>

                      <div className="flex justify-end gap-2.5 pt-2 border-t border-slate-100">
                        {isEditingQuote && (
                          <Button
                            variant="secondary"
                            type="button"
                            onClick={() => setIsEditingQuote(false)}
                          >
                            Hủy
                          </Button>
                        )}
                        <Button
                          variant="primary"
                          type="submit"
                          loading={submittingQuote}
                          disabled={isDeadlineExpired || (req.budgetRef > 0 && bidPrice > req.budgetRef)}
                        >
                          {submittingQuote
                            ? 'Đang gửi...'
                            : isEditingQuote
                              ? '💾 Lưu mức giá mới'
                              : '🚀 Gửi giá đấu thầu'}
                        </Button>
                      </div>
                    </form>
                  </Card>
                ) : (
                  /* Thợ đã có báo giá và không trong chế độ edit */
                  <div className="rounded-2xl border border-blue-200/80 bg-blue-50/60 p-4 flex flex-wrap items-center justify-between gap-3">
                    <div className="text-xs text-blue-900 space-y-0.5">
                      <p className="font-bold">
                        Bạn đã gửi mức giá ({formatCurrency(myQuote.totalPrice)}) vào phiên đấu giá.
                      </p>
                      <p className="text-blue-700">
                        Dữ liệu đối thủ đang được cập nhật tự động. Bạn có thể tự do điều chỉnh mức giá bất cứ lúc nào để tăng tính cạnh tranh.
                      </p>
                    </div>
                    <Button
                      variant="primary"
                      onClick={() => handleStartEditQuote(myQuote)}
                      className="text-xs py-2 px-4 shadow-sm"
                    >
                      ✏️ Trả giá lại / Điều chỉnh mức giá
                    </Button>
                  </div>
                )}
              </div>
            );
          }

          if (myQuote && myQuote.status === 'REJECTED') {
            return (
              <div className="rounded-2xl border border-blue-100 bg-blue-50/70 p-5 shadow-xs space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">🛠️</span>
                    <h3 className="font-display font-bold text-slate-800 text-sm">
                      Tiến độ đơn hàng: <span className="text-blue-700 font-bold">Đang xử lý</span>
                    </h3>
                  </div>
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">
                    Đang xử lý
                  </span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Đơn hàng đang trong tiến trình triển khai thực hiện. Báo giá của bạn ({formatCurrency(myQuote.totalPrice || myQuote.priceLaborVnd || 0)}) đã được ghi nhận trong hệ thống.
                </p>
                <div className="pt-2 border-t border-blue-200/60 flex items-center justify-between">
                  <span className="text-[11px] text-slate-500">
                    Bạn có thể quay lại sàn việc để nộp báo giá cho các đơn mới trong khu vực.
                  </span>
                  <Link to="/tho">
                    <Button variant="secondary" className="text-xs py-1 px-3">
                      Quay lại sàn việc
                    </Button>
                  </Link>
                </div>
              </div>
            );
          }

          return (
            <Card title="Không thể gửi báo giá">
              <div className="py-4 text-center text-sm text-slate-500">
                Đơn này hiện không mở cho thợ nhận (Trạng thái: <strong>{req.statusLabel || req.status}</strong>).
              </div>
            </Card>
          );
        })()}

        {/* Lịch hẹn làm việc / khảo sát (Chỉ hiển thị cho thợ phụ trách, ẩn khi đang đấu giá hoặc thợ khác xem) */}
        {(() => {
          const numericUserId = parseNumericUserId(user?.id);
          const isAssignedToOther = detail.request.technicianId && numericUserId && detail.request.technicianId !== numericUserId;
          const isOpenForBidding = req.status === 'OPEN' || req.status === 'BIDDING_OPEN';

          // Khi đang đấu giá hoặc đơn do thợ khác phụ trách: Ẩn hoàn toàn khối lịch hẹn để tránh gây rối
          if (isOpenForBidding || isAssignedToOther) return null;

          return (
            <Card
              title="Lịch hẹn khảo sát & thi công"
              description="Quản lý vòng đời trạng thái cuộc hẹn giữa bạn và khách hàng."
              actions={
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
              }
            >
              {appointments.length === 0 ? (
                <div className="py-6 text-center text-slate-500 text-sm">
                  Chưa có lịch hẹn nào được thiết lập cho đơn này. Bấm nút + Đặt lịch hẹn mới để hẹn thời gian tới nhà khách hàng.
                </div>
              ) : (
                <div className="space-y-4 my-2">
                  {appointments.map((appt) => (
                    <AppointmentCard
                      key={appt.id}
                      appointment={appt}
                      currentUserRole="TECHNICIAN"
                      requestStatus={detail.request.status}
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
                      {wp.note.replace(/\s*\(ai nhận trước được trước\)/gi, '')}
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
      {/* Modal Báo giá phát sinh khi Khảo sát */}
      {showCostApprovalModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4 animate-scale-in max-h-[88vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base flex items-center gap-1.5">
                <span>⚠️</span> Báo giá phát sinh sau khảo sát
              </h3>
              <button
                type="button"
                onClick={() => setShowCostApprovalModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (extraCostImages.length > 0) {
                  try {
                    await attachRequestMedia(detail.request.id, extraCostImages);
                  } catch (err) {
                    console.warn('Attach media warning:', err);
                  }
                }
                if (willRescheduleWithCost && newRepairDate) {
                  try {
                    const numericUserId = parseNumericUserId(user?.id) ?? undefined;
                    await createAppointment({
                      repairRequestId: detail.request.id,
                      technicianId: numericUserId,
                      appointmentType: 'REPAIR',
                      scheduledDate: newRepairDate,
                      scheduledTime: newRepairTime.length === 5 ? `${newRepairTime}:00` : newRepairTime,
                      notes: `Lịch thi công sau khảo sát: Cần mua/chuẩn bị linh kiện phát sinh (${extraCostReason || 'Linh kiện thay thế'}).`
                    });
                  } catch (err) {
                    console.warn('Create appointment on surcharge warning:', err);
                  }
                }
                const calculatedTotal = (req.agreedPrice || req.budgetRef || 0) + extraPartsCost;
                handleTransition(
                  'AWAITING_COST_APPROVAL',
                  extraCostReason || 'Thợ đề xuất phát sinh linh kiện sau khảo sát thực tế',
                  calculatedTotal
                );
              }}
              className="space-y-4"
            >
              <div>
                <TextArea
                  label="Lý do / Linh kiện phát sinh *"
                  required
                  rows={3}
                  placeholder="Ví dụ: Tháo máy phát hiện hỏng tụ đề quạt dàn nóng và xì gas ống đồng..."
                  value={extraCostReason}
                  onChange={(e) => setExtraCostReason(e.target.value)}
                />
              </div>

              <div>
                <TextField
                  label="Số tiền linh kiện phát sinh (VNĐ) *"
                  type="number"
                  required
                  min={10000}
                  step={10000}
                  placeholder="Ví dụ: 150000"
                  value={extraPartsCost ? String(extraPartsCost) : ''}
                  onChange={(e) => setExtraPartsCost(Math.max(0, Number(e.target.value)))}
                />
              </div>

              <div className="rounded-xl bg-slate-50 border border-slate-200 p-3 text-xs space-y-1.5">
                <div className="flex justify-between text-slate-600">
                  <span>Giá chốt ban đầu:</span>
                  <span className="font-semibold">{formatCurrency(req.agreedPrice || req.budgetRef || 0)}</span>
                </div>
                <div className="flex justify-between text-amber-700">
                  <span>+ Phí linh kiện phát sinh:</span>
                  <span className="font-bold">+{formatCurrency(extraPartsCost)}</span>
                </div>
                <div className="flex justify-between border-t border-slate-200 pt-1.5 font-bold text-slate-900">
                  <span>Tổng chi phí mới:</span>
                  <span className="text-blue-700">{formatCurrency((req.agreedPrice || req.budgetRef || 0) + extraPartsCost)}</span>
                </div>
              </div>

              {/* Tùy chọn hẹn ngày thi công sau khi khảo sát phát hiện hư hỏng */}
              <div className="space-y-2 pt-2 border-t border-slate-100">
                <label className="block text-xs font-semibold text-slate-800">
                  Kế hoạch thi công thay thế linh kiện:
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setWillRescheduleWithCost(false)}
                    className={`p-2.5 rounded-xl border text-xs font-medium text-left transition-all ${
                      !willRescheduleWithCost
                        ? 'border-brand bg-blue-50/70 text-blue-900 ring-1 ring-brand'
                        : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    ⚡ Sửa ngay sau khi duyệt
                    <span className="block text-[11px] text-slate-500 font-normal mt-0.5">Có sẵn đồ nghề & phụ tùng</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setWillRescheduleWithCost(true);
                      if (!newRepairDate) {
                        setNewRepairDate(new Date(Date.now() + 86400000).toISOString().split('T')[0]);
                      }
                    }}
                    className={`p-2.5 rounded-xl border text-xs font-medium text-left transition-all ${
                      willRescheduleWithCost
                        ? 'border-indigo-400 bg-indigo-50/70 text-indigo-900 ring-1 ring-indigo-400'
                        : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    📅 Hẹn ngày thi công mới
                    <span className="block text-[11px] text-slate-500 font-normal mt-0.5">Cần đặt hàng linh kiện</span>
                  </button>
                </div>

                {willRescheduleWithCost && (
                  <div className="grid grid-cols-2 gap-2.5 bg-indigo-50/50 p-2.5 rounded-xl border border-indigo-100 mt-2">
                    <TextField
                      label="Ngày hẹn mới *"
                      type="date"
                      min={new Date().toISOString().split('T')[0]}
                      value={newRepairDate}
                      onChange={(e) => setNewRepairDate(e.target.value)}
                      required={willRescheduleWithCost}
                    />
                    <TextField
                      label="Giờ hẹn mới *"
                      type="time"
                      value={newRepairTime}
                      onChange={(e) => setNewRepairTime(e.target.value)}
                      required={willRescheduleWithCost}
                    />
                  </div>
                )}
              </div>

              <div>
                <MultiImageUploadField
                  label="Ảnh chụp linh kiện hỏng / bằng chứng phát sinh"
                  value={extraCostImages}
                  onChange={setExtraCostImages}
                  maxFiles={4}
                  hint="Chụp ảnh rõ linh kiện bị hao mòn/cháy hỏng để khách hàng duyệt nhanh chóng"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100 sticky bottom-0 bg-white/95 backdrop-blur-xs pb-1 mt-3">
                <Button variant="secondary" type="button" onClick={() => setShowCostApprovalModal(false)}>
                  Hủy
                </Button>
                <Button variant="primary" type="submit" loading={updatingLifecycle}>
                  Gửi khách hàng duyệt
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Báo cáo hoàn thành & Mời nghiệm thu */}
      {showAcceptanceModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 animate-scale-in max-h-[88vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-base flex items-center gap-1.5">
                <span>📋</span> Báo cáo hoàn thành & Mời nghiệm thu
              </h3>
              <button
                type="button"
                onClick={() => setShowAcceptanceModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleTransition('AWAITING_ACCEPTANCE', acceptanceNote);
              }}
              className="space-y-4"
            >
              <div>
                <TextArea
                  label="Ghi chú bàn giao & Kết quả chạy thử"
                  rows={3}
                  value={acceptanceNote}
                  onChange={(e) => setAcceptanceNote(e.target.value)}
                  placeholder="Ví dụ: Đã thay thế linh kiện, chạy thử máy 20 phút đạt độ lạnh sâu và êm ái..."
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Button variant="secondary" type="button" onClick={() => setShowAcceptanceModal(false)}>
                  Hủy
                </Button>
                <Button variant="primary" type="submit" loading={updatingLifecycle}>
                  Gửi thông báo nghiệm thu
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Hẹn lại ngày thi công (Luồng sau khảo sát) */}
      {showRescheduleRepairModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4 animate-scale-in">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="text-2xl">📅</span>
                <h3 className="font-bold text-slate-900 text-base">
                  Hẹn lại ngày thi công (Sau khảo sát)
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowRescheduleRepairModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold p-1"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed bg-blue-50/70 p-3 rounded-xl border border-blue-100">
              💡 Sử dụng luồng này khi khảo sát thực tế phát hiện thiết bị hư hỏng nghiêm trọng hoặc cấu trúc phức tạp, cần chuẩn bị thêm máy móc chuyên dụng hoặc đặt mua linh kiện chính hãng để quay lại thi công.
            </p>

            <form onSubmit={handleRescheduleRepair} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Lý do hẹn lại ngày thi công *
                </label>
                <select
                  value={rescheduleRepairReason}
                  onChange={(e) => setRescheduleRepairReason(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 p-2.5 text-xs text-slate-800 bg-white focus:outline-none focus:ring-2 focus:ring-brand/30"
                >
                  <option value="Cần chuẩn bị thêm đồ nghề chuyên dụng & đặt hàng linh kiện thay thế">
                    🛠️ Cần chuẩn bị thêm đồ nghề chuyên dụng & đặt hàng linh kiện thay thế
                  </option>
                  <option value="Thiết bị hư hỏng nặng, cần chuyển máy về trung tâm kỹ thuật">
                    🚚 Thiết bị hư hỏng nặng, cần chuyển máy về trung tâm kỹ thuật
                  </option>
                  <option value="Hiện trường nguy hiểm/phức tạp, cần thêm nhân lực hỗ trợ">
                    ⚡ Hiện trường nguy hiểm/phức tạp, cần thêm nhân lực hỗ trợ
                  </option>
                  <option value="Khách hàng có việc bận đột xuất, thống nhất hẹn lại ngày khác">
                    🤝 Khách hàng có việc bận đột xuất, thống nhất hẹn lại ngày khác
                  </option>
                  <option value="Khác (Ghi rõ chi tiết ở phần ghi chú bên dưới)">
                    📝 Khác (Ghi rõ chi tiết ở phần ghi chú bên dưới)
                  </option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <TextField
                    label="Ngày hẹn thi công lại *"
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={newRepairDate}
                    onChange={(e) => setNewRepairDate(e.target.value)}
                  />
                </div>
                <div>
                  <TextField
                    label="Giờ hẹn *"
                    type="time"
                    required
                    value={newRepairTime}
                    onChange={(e) => setNewRepairTime(e.target.value)}
                  />
                </div>
              </div>

              <div>
                <TextArea
                  label="Ghi chú kỹ thuật chi tiết cho khách hàng & Admin"
                  rows={3}
                  value={rescheduleRepairNote}
                  onChange={(e) => setRescheduleRepairNote(e.target.value)}
                  placeholder="Mô tả cụ thể linh kiện cần đặt mua, thời gian dự kiến nhận linh kiện hoặc dụng cụ đặc thù cần mang theo..."
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <Button variant="secondary" type="button" onClick={() => setShowRescheduleRepairModal(false)}>
                  Hủy
                </Button>
                <Button variant="primary" type="submit" loading={submittingReschedule}>
                  ✓ Xác nhận lịch hẹn thi công mới
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
