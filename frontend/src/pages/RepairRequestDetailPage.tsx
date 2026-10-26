import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import AdditionalCostPanel from '../components/AdditionalCostPanel';
import Alert from '../components/Alert';
import AppointmentCard from '../components/AppointmentCard';
import BillView from '../components/BillView';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import MultiImageUploadField from '../components/MultiImageUploadField';
import PriceQuoteList from '../components/PriceQuoteList';
import QRPaymentModal from '../components/QRPaymentModal';
import RequestStatusStepper from '../components/RequestStatusStepper';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, acceptQuotation, api, attachRequestMedia, createAppointment, createDepositPayment, createFinalPayment, fetchAdditionalCosts, fetchAppointmentsForRequest, formatCurrency, updateRequestStatus } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { AdditionalCost, Appointment, Bill, PaymentResult, Quotation, RepairRequestDetail } from '../lib/types';

const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Bảng điều khiển' },
  { to: '/yeu-cau-cua-toi', label: 'Yêu cầu của tôi' },
  { to: '/dang-yeu-cau', label: 'Đăng yêu cầu' }
];

export default function RepairRequestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const currentRole = user?.role ?? 'CUSTOMER';
  const isCustomer = currentRole === 'CUSTOMER';
  const isTechnician = currentRole === 'TECHNICIAN';

  const [detail, setDetail] = useState<RepairRequestDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [showCancelForm, setShowCancelForm] = useState(false);

  // Chọn báo giá của thợ
  const [acceptingQuoteId, setAcceptingQuoteId] = useState<number | null>(null);
  const [quoteActionError, setQuoteActionError] = useState<string | null>(null);

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

  // QR Payment
  const [qrPayment, setQrPayment] = useState<PaymentResult | null>(null);
  const [creatingPayment, setCreatingPayment] = useState(false);
  const [approvedCostTotal, setApprovedCostTotal] = useState(0);

  // Kích hoạt bản nháp
  const [publishing, setPublishing] = useState(false);

  // Thao tác vòng đời (Khách duyệt giá phát sinh & Nghiệm thu)
  const [updatingLifecycle, setUpdatingLifecycle] = useState(false);
  const [lifecycleMessage, setLifecycleMessage] = useState<string | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubmitted, setReviewSubmitted] = useState(false);

  async function handleCustomerAction(targetStatus: string, note?: string) {
    if (!detail) return;
    setUpdatingLifecycle(true);
    setActionError(null);
    setLifecycleMessage(null);
    try {
      const completingWithReview = targetStatus === 'COMPLETED' && detail.request.status === 'AWAITING_ACCEPTANCE';
      if (completingWithReview) {
        if (!reviewComment.trim()) throw new Error('Vui lòng nhập phản hồi trước khi hoàn tất yêu cầu.');
        await api.post(`/repair-requests/${detail.request.id}/review`, { rating: reviewRating, comment: reviewComment.trim() });
        setReviewSubmitted(true);
      } else {
        await updateRequestStatus(detail.request.id, targetStatus, note);
      }
      await loadDetail();
      if (targetStatus === 'COMPLETED') {
        setLifecycleMessage('Chúc mừng! Bạn đã hoàn tất nghiệm thu và kích hoạt bảo hành điện tử 90 ngày thành công.');
      } else if (targetStatus === 'IN_PROGRESS') {
        setLifecycleMessage('Đã chấp thuận chi phí phát sinh. Kỹ thuật viên sẽ tiếp tục sửa chữa.');
      }
    } catch (err: any) {
      setActionError(err?.message || 'Có lỗi xảy ra khi thực hiện thao tác');
    } finally {
      setUpdatingLifecycle(false);
    }
  }

  useEffect(() => {
    loadDetail();
  }, [id]);

  function loadDetail() {
    setLoading(true);
    api.get<RepairRequestDetail>(`/repair-requests/${id}`)
      .then((res) => {
        setDetail(res.data);
        setError(null);
        if (id && res.data.request.status === 'AWAITING_ACCEPTANCE') {
          api.get<{ rating: number; comment: string } | null>(`/repair-requests/${id}/review`)
            .then((review) => {
              if (review.data) {
                setReviewRating(review.data.rating);
                setReviewComment(review.data.comment);
                setReviewSubmitted(true);
              }
            }).catch(() => setReviewSubmitted(false));
        }
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Không tải được chi tiết yêu cầu.');
      })
      .finally(() => setLoading(false));

    if (id) {
      fetchAppointmentsForRequest(Number(id))
        .then((res) => setAppointments(res.data || []))
        .catch(() => setAppointments([]));
      fetchAdditionalCosts(Number(id))
        .then((res) => {
          const total = (res.data || [])
            .filter((c: AdditionalCost) => c.status === 'APPROVED')
            .reduce((sum: number, c: AdditionalCost) => sum + c.amount, 0);
          setApprovedCostTotal(total);
        })
        .catch(() => setApprovedCostTotal(0));
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
      setActionError(err instanceof ApiError ? err.message : 'Lỗi khi hủy đơn.');
    } finally {
      setCancelling(false);
    }
  }

  async function handlePublishDraft() {
    if (!detail) return;
    setPublishing(true);
    try {
      await updateRequestStatus(detail.request.id, 'OPEN', 'Khách hàng đăng bản nháp');
      loadDetail();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Lỗi khi phát sóng bản nháp.');
    } finally {
      setPublishing(false);
    }
  }

  async function handleAcceptQuotation(quotationId: number) {
    if (!detail) return;
    setAcceptingQuoteId(quotationId);
    setQuoteActionError(null);
    try {
      await acceptQuotation(detail.request.id, quotationId);
      await loadDetail();
    } catch (err: any) {
      setQuoteActionError(err instanceof ApiError ? err.message : 'Không thể chọn báo giá này.');
    } finally {
      setAcceptingQuoteId(null);
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
      setActionError(err instanceof ApiError ? err.message : 'Lỗi khi gắn ảnh bằng chứng.');
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

  async function handleCreateDeposit() {
    if (!detail) return;
    setCreatingPayment(true);
    setActionError(null);
    try {
      const res = await createDepositPayment(detail.request.id);
      setQrPayment(res.data);
    } catch (err: any) {
      setActionError(err.message || 'Không tạo được QR đặt cọc.');
    } finally {
      setCreatingPayment(false);
    }
  }

  async function handleCreateFinal() {
    if (!detail) return;
    setCreatingPayment(true);
    setActionError(null);
    try {
      const res = await createFinalPayment(detail.request.id);
      setQrPayment(res.data);
    } catch (err: any) {
      setActionError(err.message || 'Không tạo được QR thanh toán.');
    } finally {
      setCreatingPayment(false);
    }
  }

  function handlePaymentConfirmed(_bill: Bill) {
    setQrPayment(null);
    loadDetail();
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

      {actionError && (
        <div className="mb-4">
          <Alert tone="error">{actionError}</Alert>
        </div>
      )}

      <div className="space-y-6">
        {/* Thông báo thao tác thành công */}
        {lifecycleMessage && (
          <div className="rounded-2xl border border-emerald-300 bg-emerald-50 p-4 shadow-sm flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <span className="text-xl">🎉</span>
              <p className="text-xs sm:text-sm font-bold text-emerald-900">{lifecycleMessage}</p>
            </div>
            <button
              type="button"
              onClick={() => setLifecycleMessage(null)}
              className="text-xs text-emerald-700 hover:text-emerald-900 font-bold"
            >
              ✕
            </button>
          </div>
        )}

        {/* Thanh trạng thái tiến trình & theo dõi hiệu lực */}
        <RequestStatusStepper
          request={req}
          workProgress={detail.workProgress}
          onPublishDraft={handlePublishDraft}
          publishing={publishing}
        />

        {/* KHỐI 1: KHÁCH DUYỆT BÁO GIÁ PHÁT SINH KHI KHẢO SÁT */}
        {req.status === 'AWAITING_COST_APPROVAL' && (
          <div className="rounded-2xl border-2 border-amber-300 bg-amber-50/90 p-5 shadow-sm space-y-3.5 animate-scale-in">
            <div className="flex items-center gap-2">
              <span className="text-2xl">⚠️</span>
              <div>
                <h3 className="font-display font-bold text-amber-950 text-base">
                  Kỹ thuật viên đề xuất điều chỉnh chi phí sau khảo sát
                </h3>
                <p className="text-xs text-amber-800 mt-0.5">
                  Sau khi tháo máy kiểm tra thực tế, thợ phát hiện linh kiện hỏng cần thay thế ngoài dự kiến.
                </p>
              </div>
            </div>

            <div className="rounded-xl bg-white border border-amber-200 p-3.5 text-xs text-slate-700 leading-relaxed font-medium">
              <span className="font-bold text-amber-900">Chi tiết từ thợ:</span>{' '}
              {detail.workProgress[detail.workProgress.length - 1]?.note || 'Cần bổ sung thay thế linh kiện chuyên dụng'}
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t border-amber-200/80 pt-3">
              <div>
                <span className="text-xs text-amber-800">Tổng chi phí mới trọn gói:</span>
                <span className="ml-2 font-display text-lg font-bold text-brand">
                  {formatCurrency(req.agreedPrice)}
                </span>
                <span className="ml-2 text-[11px] text-slate-500">
                  (Đặt cọc 30%: {formatCurrency(req.depositAmount)})
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="primary"
                  loading={updatingLifecycle}
                  onClick={() => handleCustomerAction('IN_PROGRESS', 'Khách hàng đồng ý chi phí phát sinh, cho phép tiếp tục sửa chữa')}
                  className="text-xs py-2 px-4 shadow-sm"
                >
                  ✅ Đồng ý chi phí mới
                </Button>
                <Button
                  variant="secondary"
                  loading={updatingLifecycle}
                  onClick={() => handleCustomerAction('CANCELLED', 'Khách hàng không đồng ý chi phí phát sinh và hủy yêu cầu')}
                  className="text-xs py-2 px-4 text-rose-700 border-rose-300 bg-white hover:bg-rose-50"
                >
                  ❌ Không đồng ý (Hủy đơn)
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* KHỐI 2: KHÁCH NGHIỆM THU THIẾT BỊ KHI SỬA XONG */}
        {req.status === 'AWAITING_ACCEPTANCE' && (
          <div className="rounded-2xl border-2 border-emerald-300 bg-emerald-50/90 p-5 shadow-sm space-y-3.5 animate-scale-in">
            <div className="flex items-center gap-2">
              <span className="text-2xl">📋</span>
              <div>
                <h3 className="font-display font-bold text-emerald-950 text-base">
                  Kỹ thuật viên đã hoàn thành sửa chữa & Mời bạn nghiệm thu
                </h3>
                <p className="text-xs text-emerald-800 mt-0.5">
                  Thiết bị đã được lắp ráp hoàn chỉnh và chạy thử nghiệm xong.
                </p>
              </div>
            </div>

            <p className="text-xs text-emerald-900 leading-relaxed">
              Quý khách vui lòng trực tiếp kiểm tra thiết bị xem đã hoạt động ổn định và hài lòng chưa. Sau khi bạn xác nhận nghiệm thu đạt, tiền công sẽ được giải ngân cho thợ và <strong>Bảo hành điện tử 90 ngày</strong> sẽ được kích hoạt ngay lập tức.
            </p>

            <div className="space-y-3 border-t border-emerald-200/80 pt-3">
              <div className="rounded-xl border border-emerald-200 bg-white/80 p-4">
                <p className="text-sm font-bold text-slate-800">Đánh giá bắt buộc trước khi hoàn tất</p>
                <div className="mt-2 flex items-center gap-1" role="radiogroup" aria-label="Chấm điểm thợ từ 1 đến 5 sao">
                  {[1, 2, 3, 4, 5].map((star) => <button key={star} type="button" role="radio" aria-checked={reviewRating === star} aria-label={`${star} sao`} disabled={reviewSubmitted} onClick={() => setReviewRating(star)} className={`text-2xl transition ${star <= reviewRating ? 'text-amber-500' : 'text-slate-300'} disabled:cursor-default`}>★</button>)}
                  <span className="ml-2 text-sm font-semibold text-slate-600">{reviewRating}/5</span>
                </div>
                <textarea value={reviewComment} onChange={(event) => setReviewComment(event.target.value)} disabled={reviewSubmitted} maxLength={2000} rows={3} placeholder="Chia sẻ trải nghiệm sửa chữa của bạn..." className="mt-3 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-emerald-400 focus:ring-2 focus:ring-emerald-100 disabled:bg-slate-50" />
                {reviewSubmitted && <p className="mt-1 text-xs font-medium text-emerald-700">Đã lưu đánh giá của bạn.</p>}
              </div>
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <span className="text-xs font-semibold text-emerald-800">⭐ Tổng chi phí quyết toán: <strong>{formatCurrency(req.agreedPrice)}</strong></span>
                <Button variant="primary" loading={updatingLifecycle} onClick={() => handleCustomerAction('COMPLETED', 'Khách hàng đã nghiệm thu thiết bị đạt chuẩn và hoàn tất đơn')} className="text-xs py-2.5 px-5 shadow-md bg-emerald-600 hover:bg-emerald-700">
                  {reviewSubmitted ? 'Nghiệm thu đạt & Hoàn tất đơn' : 'Gửi đánh giá & Hoàn tất đơn'}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* KHỐI 3: PHIẾU BẢO HÀNH ĐIỆN TỬ KHI ĐÃ HOÀN THÀNH */}
        {req.status === 'COMPLETED' && (
          <div className="rounded-2xl border border-emerald-300 bg-gradient-to-r from-emerald-50 to-teal-50 p-5 shadow-xs space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xl">🛡️</span>
                <h3 className="font-display font-bold text-emerald-950 text-sm">
                  Phiếu bảo hành điện tử chính hãng FixLink (90 Ngày)
                </h3>
              </div>
              <span className="rounded-full bg-emerald-200/80 text-emerald-800 px-3 py-0.5 text-xs font-bold">
                Đang hiệu lực
              </span>
            </div>
            <p className="text-xs text-emerald-800">
              Mã bảo hành: <strong className="font-mono text-slate-800">{req.requestCode}-WTY</strong> | Thiết bị được hỗ trợ kỹ thuật và kiểm tra định kỳ miễn phí nếu phát sinh sự cố tương tự.
            </p>
          </div>
        )}

        {/* Thông tin đơn */}
        <Card title="Thông tin yêu cầu sửa chữa">
          <dl className="divide-y divide-slate-100 text-sm">
            <Row label="Loại thiết bị" value={req.categoryName ?? '—'} />
            <Row label="Khu vực" value={req.areaName ?? 'Toàn khu vực'} />
            <Row label="Địa chỉ kiểm tra" value={req.address || req.addressLine} />
            <Row label="Mô tả sự cố" value={req.description} />
            {req.budgetRef > 0 && (
              <Row label="Giá cố định khách đưa ra" value={formatCurrency(req.budgetRef)} highlight />
            )}
            {req.applyDeadline && req.status === 'OPEN' && (
              <Row
                label="Thời hạn nhận thợ"
                value={`${describeRemaining(req.applyDeadline)} (hết hạn ${new Date(req.applyDeadline).toLocaleString('vi-VN')})`}
                highlight
              />
            )}
            {req.agreedPrice > 0 && <Row label="Giá chốt" value={formatCurrency(req.agreedPrice)} highlight />}
            {req.depositAmount > 0 && <Row label="Tiền đặt cọc (30%)" value={formatCurrency(req.depositAmount)} highlight />}
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

        {/* Danh sách báo giá từ các thợ (Mô hình đấu thầu & chọn thợ) */}
        {req.status === 'OPEN' && detail.quotations.length > 0 && (
          <Card
            title={`Danh sách thợ báo giá & trả giá (${detail.quotations.length})`}
            description="So sánh chi phí, phương án sửa chữa và kinh nghiệm của các thợ để chọn người phù hợp nhất."
          >
            {quoteActionError && (
              <div className="mb-4">
                <Alert tone="error">{quoteActionError}</Alert>
              </div>
            )}
            <div className="grid gap-4 md:grid-cols-2">
              {detail.quotations.map((q) => (
                <QuotationCard
                  key={q.id}
                  quotation={q}
                  canAccept={req.status === 'OPEN'}
                  isAccepting={acceptingQuoteId === q.id}
                  onAccept={handleAcceptQuotation}
                />
              ))}
            </div>
          </Card>
        )}

        {/* Thợ kỹ thuật đã được chọn để thực hiện đơn */}
        {req.status !== 'OPEN' && req.status !== 'DRAFT' && req.status !== 'CANCELLED' && detail.quotations.some((q) => q.status === 'ACCEPTED') && (
          <Card
            title="Thợ kỹ thuật thực hiện"
            description="Báo giá và kỹ thuật viên đã được bạn lựa chọn thực hiện công việc."
          >
            <div className="grid gap-4 md:grid-cols-2">
              {detail.quotations.filter((q) => q.status === 'ACCEPTED').map((q) => (
                <QuotationCard key={q.id} quotation={q} />
              ))}
            </div>
          </Card>
        )}

        {/* Đang chờ thợ gửi báo giá */}
        {detail.quotations.length === 0 && req.status === 'OPEN' && !isDraft && (
          <Card title="Chờ thợ gửi báo giá">
            <div className="py-8 text-center max-w-md mx-auto">
              <span className="text-4xl">⏳</span>
              <p className="mt-3 font-bold text-slate-800 text-base">Đang phát sóng yêu cầu tới các thợ trong khu vực</p>
              <p className="text-xs text-slate-500 mt-1.5 leading-relaxed">
                Ngân sách dự kiến bạn đưa ra là{' '}
                <strong className="text-brand">{formatCurrency(req.budgetRef)}</strong>. Khi các thợ gửi báo giá và phương án xử lý, danh sách báo giá sẽ lập tức hiển thị tại đây để bạn so sánh và chọn thợ.
              </p>
              {req.applyDeadline && (
                <div className="mt-3 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 border border-blue-200 text-xs text-blue-700 font-medium">
                  <span>⏱️</span>
                  <span>Thời hạn nhận báo giá: {describeRemaining(req.applyDeadline)}</span>
                </div>
              )}
            </div>
          </Card>
        )}

        {/* Báo giá deal giá (Price Quotes) */}
        <PriceQuoteList
          requestId={req.id}
          isCustomer={isCustomer}
          requestStatus={req.status}
          onQuoteAccepted={loadDetail}
        />

        {/* Đặt cọc QR khi đã giao thợ */}
        {req.status === 'ASSIGNED' && (
          <Card title="Thanh toán đặt cọc">
            <div className="space-y-3 text-center py-4">
              <p className="text-sm text-ink-soft">
                Yêu cầu đã được giao cho thợ. Vui lòng đặt cọc <strong>30%</strong> để thợ bắt đầu khảo sát.
              </p>
              <p className="text-lg font-bold text-brand">{formatCurrency(Math.round(req.agreedPrice * 0.3))}</p>
              <Button loading={creatingPayment} onClick={handleCreateDeposit}>
                Tạo QR đặt cọc
              </Button>
            </div>
          </Card>
        )}

        {/* Thanh toán tổng khi chờ nghiệm thu */}
        {req.status === 'AWAITING_ACCEPTANCE' && (() => {
          const depositPaid = req.depositAmount > 0 ? req.depositAmount : Math.round(req.agreedPrice * 0.3);
          const remaining = req.agreedPrice + approvedCostTotal - depositPaid;
          return (
            <Card title="Thanh toán tổng">
              <div className="space-y-4 py-2">
                <p className="text-sm text-ink-soft text-center">
                  Sửa chữa đã hoàn tất. Thanh toán phần còn lại sau khi trừ đặt cọc.
                </p>
                <div className="mx-auto max-w-sm space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-ink-soft">Giá deal</span>
                    <span className="font-medium text-ink">{formatCurrency(req.agreedPrice)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-ink-soft">Đã cọc</span>
                    <span className="font-medium text-danger">-{formatCurrency(depositPaid)}</span>
                  </div>
                  {approvedCostTotal > 0 && (
                    <div className="flex justify-between">
                      <span className="text-ink-soft">Phát sinh</span>
                      <span className="font-medium text-amber-600">+{formatCurrency(approvedCostTotal)}</span>
                    </div>
                  )}
                  <div className="flex justify-between border-t border-line pt-2">
                    <span className="font-bold text-ink">Còn phải trả</span>
                    <span className="text-lg font-bold text-brand">{formatCurrency(Math.max(0, remaining))}</span>
                  </div>
                </div>
                <div className="text-center">
                  <Button loading={creatingPayment} onClick={handleCreateFinal}>
                    Tạo QR thanh toán tổng
                  </Button>
                </div>
              </div>
            </Card>
          );
        })()}

        {/* Chi phí phát sinh */}
        {['INSPECTING', 'DEPOSIT_PAID', 'IN_PROGRESS', 'AWAITING_COST_APPROVAL', 'AWAITING_ACCEPTANCE', 'COMPLETED'].includes(req.status) && (
          <AdditionalCostPanel
            requestId={req.id}
            isTechnician={isTechnician}
            isCustomer={isCustomer}
            onCostChanged={loadDetail}
          />
        )}

        {/* Hóa đơn */}
        {['DEPOSIT_PAID', 'INSPECTING', 'IN_PROGRESS', 'AWAITING_COST_APPROVAL', 'AWAITING_ACCEPTANCE', 'COMPLETED'].includes(req.status) && (
          <BillView requestId={req.id} />
        )}

        {/* QR Payment Modal */}
        {qrPayment && (
          <QRPaymentModal
            paymentId={qrPayment.paymentId}
            qrUrl={qrPayment.qrUrl}
            amount={qrPayment.amount}
            transferContent={qrPayment.transferContent}
            bankInfo={qrPayment.bankInfo}
            onClose={() => setQrPayment(null)}
            onConfirmed={handlePaymentConfirmed}
          />
        )}

        {/* Lịch hẹn khảo sát / thi công (Jira RC-48: Appointment Status Lifecycle) */}
        {(req.technicianId || appointments.length > 0) && (
          <Card
            title="Lịch hẹn khảo sát & thi công"
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
                    currentUserRole={currentRole as 'CUSTOMER' | 'TECHNICIAN' | 'ADMIN'}
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
                      {wp.note.replace(/\s*\(ai nhận trước được trước\)/gi, '')}
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

/**
 * Đếm ngược tới hạn nhận việc, ví dụ "còn 2 ngày 5 giờ".
 * Trả về "đã hết hạn" nếu mốc thời gian đã qua.
 */
function describeRemaining(deadlineIso: string): string {
  const remainingMs = new Date(deadlineIso).getTime() - Date.now();
  if (Number.isNaN(remainingMs) || remainingMs <= 0) return 'đã hết hạn';

  const totalHours = Math.floor(remainingMs / 3_600_000);
  const days = Math.floor(totalHours / 24);
  const hours = totalHours % 24;

  if (days > 0) return `còn ${days} ngày${hours > 0 ? ` ${hours} giờ` : ''}`;
  if (totalHours > 0) return `còn ${totalHours} giờ`;
  return `còn ${Math.max(1, Math.floor(remainingMs / 60_000))} phút`;
}

function Row({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex flex-col gap-1 py-3 sm:flex-row sm:gap-4">
      <dt className="w-44 shrink-0 font-medium text-slate-500">{label}</dt>
      <dd className={highlight ? 'font-bold text-brand' : 'text-slate-900 font-medium'}>{value}</dd>
    </div>
  );
}

/**
 * Thẻ hiển thị báo giá của từng thợ để khách hàng so sánh và chốt deal.
 */
function QuotationCard({
  quotation: q,
  canAccept = false,
  isAccepting = false,
  onAccept
}: {
  quotation: Quotation;
  canAccept?: boolean;
  isAccepting?: boolean;
  onAccept?: (id: number) => void;
}) {
  const isAccepted = q.status === 'ACCEPTED';

  return (
    <div
      className={`rounded-2xl border p-5 transition-all duration-200 flex flex-col justify-between ${
        isAccepted
          ? 'border-brand bg-blue-50/60 shadow-sm ring-1 ring-brand'
          : q.status === 'REJECTED'
            ? 'border-slate-200 bg-slate-50/60 opacity-60'
            : 'border-slate-200 bg-white shadow-xs hover:border-brand/40 hover:shadow-card'
      }`}
    >
      <div>
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
      </div>

      {canAccept && q.status === 'PENDING' && (
        <div className="mt-4 pt-3 border-t border-slate-100 flex justify-end">
          <Button
            variant="primary"
            loading={isAccepting}
            onClick={() => onAccept?.(q.id)}
            className="w-full sm:w-auto text-xs py-2 px-4 shadow-sm"
          >
            🤝 Chọn thợ này (Chốt deal)
          </Button>
        </div>
      )}
    </div>
  );
}
