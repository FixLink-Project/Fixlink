import { useMemo } from 'react';
import type { RepairRequest, WorkProgress } from '../lib/types';
import StatusChip from './StatusChip';

interface RequestStatusStepperProps {
  request: RepairRequest;
  workProgress?: WorkProgress[];
  onPublishDraft?: () => void;
  publishing?: boolean;
}

interface StepDefinition {
  key: string;
  label: string;
  subLabel: string;
  icon: string;
  statuses: string[];
}

const LIFECYCLE_STEPS: StepDefinition[] = [
  {
    key: 'OPEN',
    label: 'Đăng đơn',
    subLabel: 'Chờ thợ nhận',
    icon: '📝',
    statuses: ['OPEN', 'PENDING', 'BIDDING_OPEN']
  },
  {
    key: 'ASSIGNED',
    label: 'Đã nhận việc',
    subLabel: 'Thợ chốt giá',
    icon: '🤝',
    statuses: ['ASSIGNED', 'MATCHED_AWAITING_DEPOSIT']
  },
  {
    key: 'INSPECTING',
    label: 'Khảo sát',
    subLabel: 'Kiểm tra hiện trường',
    icon: '🔍',
    statuses: ['INSPECTING', 'AWAITING_COST_APPROVAL']
  },
  {
    key: 'IN_PROGRESS',
    label: 'Sửa chữa',
    subLabel: 'Đang thi công',
    icon: '🛠️',
    statuses: ['IN_PROGRESS']
  },
  {
    key: 'AWAITING_ACCEPTANCE',
    label: 'Nghiệm thu',
    subLabel: 'Chờ khách duyệt',
    icon: '📋',
    statuses: ['AWAITING_ACCEPTANCE']
  },
  {
    key: 'COMPLETED',
    label: 'Hoàn thành',
    subLabel: 'Kích hoạt bảo hành',
    icon: '✅',
    statuses: ['COMPLETED']
  }
];

export default function RequestStatusStepper({
  request: req,
  workProgress = [],
  onPublishDraft,
  publishing = false
}: RequestStatusStepperProps) {
  const isDraft = req.status === 'DRAFT';
  const isCancelled = req.status === 'CANCELLED';

  // Xác định vị trí bước hiện tại (0 -> 5)
  const currentStepIndex = useMemo(() => {
    if (isDraft) return -1;
    if (isCancelled) {
      // Tìm mốc trạng thái trước khi hủy từ workProgress nếu có
      const lastProgress = workProgress[workProgress.length - 1];
      if (lastProgress?.fromStatus) {
        const idx = LIFECYCLE_STEPS.findIndex((s) => s.statuses.includes(lastProgress.fromStatus));
        return idx >= 0 ? idx : 0;
      }
      return 0;
    }
    const idx = LIFECYCLE_STEPS.findIndex((s) => s.statuses.includes(req.status));
    return idx >= 0 ? idx : 0;
  }, [req.status, isDraft, isCancelled, workProgress]);

  // Map ngày giờ hoàn thành của từng bước từ workProgress
  const stepTimestampMap = useMemo(() => {
    const map: Record<string, string> = {};
    if (req.createdAt) {
      map['OPEN'] = req.createdAt;
    }
    for (const wp of workProgress) {
      for (const step of LIFECYCLE_STEPS) {
        if (step.statuses.includes(wp.toStatus) && !map[step.key]) {
          map[step.key] = wp.createdAt;
        }
      }
    }
    return map;
  }, [req.createdAt, workProgress]);

  // Tính toán thời gian hiệu lực của đơn OPEN (SLA 3 ngày = 72 giờ)
  const validityInfo = useMemo(() => {
    if (!req.applyDeadline || (req.status !== 'OPEN' && req.status !== 'PENDING')) {
      return null;
    }

    const deadlineMs = new Date(req.applyDeadline).getTime();
    const createdMs = req.createdAt ? new Date(req.createdAt).getTime() : deadlineMs - 72 * 3600 * 1000;
    const nowMs = Date.now();
    const remainingMs = deadlineMs - nowMs;
    const totalWindowMs = Math.max(1, deadlineMs - createdMs);
    const elapsedPercent = Math.min(100, Math.max(0, Math.round(((nowMs - createdMs) / totalWindowMs) * 100)));

    const isExpired = remainingMs <= 0;
    const isUrgent = !isExpired && remainingMs <= 24 * 3600 * 1000; // Còn dưới 24 giờ

    let timeText = '';
    if (isExpired) {
      const pastHours = Math.floor(Math.abs(remainingMs) / 3600000);
      timeText = pastHours > 0 ? `Đã quá hạn ${pastHours} giờ` : 'Vừa hết hạn nhận việc';
    } else {
      const totalHours = Math.floor(remainingMs / 3600000);
      const days = Math.floor(totalHours / 24);
      const hours = totalHours % 24;
      const minutes = Math.floor((remainingMs % 3600000) / 60000);

      if (days > 0) {
        timeText = `Còn ${days} ngày ${hours > 0 ? `${hours} giờ` : ''}`;
      } else if (totalHours > 0) {
        timeText = `Còn ${totalHours} giờ ${minutes > 0 ? `${minutes} phút` : ''}`;
      } else {
        timeText = `Còn ${Math.max(1, minutes)} phút`;
      }
    }

    return {
      deadline: req.applyDeadline,
      remainingMs,
      elapsedPercent,
      isExpired,
      isUrgent,
      timeText
    };
  }, [req.applyDeadline, req.createdAt, req.status]);

  return (
    <div className="rounded-2xl border border-slate-200/90 bg-white p-5 sm:p-6 shadow-xs space-y-5">
      {/* Tiêu đề thanh trạng thái + Chip hiện tại */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="flex h-2.5 w-2.5 rounded-full bg-brand" />
            <h3 className="font-display font-bold text-slate-900 text-base">
              Tiến trình & Vòng đời yêu cầu sửa chữa
            </h3>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Mô hình nhận việc trực tiếp — Theo dõi liên tục từng mốc thực hiện
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400 hidden sm:inline">Trạng thái hiện tại:</span>
          <StatusChip status={req.status} label={req.statusLabel} />
        </div>
      </div>

      {/* Trường hợp đặc biệt: DRAFT */}
      {isDraft && (
        <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="text-2xl">📝</span>
            <div>
              <p className="font-bold text-amber-900 text-sm">Yêu cầu đang ở trạng thái Bản nháp</p>
              <p className="text-xs text-amber-700 mt-0.5">
                Các kỹ thuật viên chưa nhìn thấy đơn này. Hãy bấm phát sóng để gửi đơn tới mạng lưới thợ.
              </p>
            </div>
          </div>
          {onPublishDraft && (
            <button
              type="button"
              onClick={onPublishDraft}
              disabled={publishing}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-brand hover:bg-brand-strong text-white px-4 py-2 text-xs font-bold transition-all shadow-xs shrink-0 disabled:opacity-50"
            >
              {publishing ? 'Đang phát sóng...' : '📢 Phát sóng đơn ngay'}
            </button>
          )}
        </div>
      )}

      {/* Trường hợp đặc biệt: CANCELLED */}
      {isCancelled && (
        <div className="rounded-xl border border-rose-200 bg-rose-50/90 p-4">
          <div className="flex items-start gap-3">
            <span className="text-2xl">⛔</span>
            <div className="flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-bold text-rose-900 text-sm">Yêu cầu sửa chữa này đã bị hủy</p>
                {req.cancelReason?.includes('Hết hạn 3 ngày') && (
                  <span className="rounded-full bg-rose-200/80 text-rose-800 px-2.5 py-0.5 text-[11px] font-bold">
                    ⏳ Tự động hủy do hết hiệu lực
                  </span>
                )}
              </div>
              <p className="text-xs text-rose-700 mt-1">
                <span className="font-semibold">Lý do hủy:</span>{' '}
                {req.cancelReason || 'Người dùng hoặc hệ thống đã hủy yêu cầu.'}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* THANH TIẾN TRÌNH 6 BƯỚC CHUẨN FIXLINK */}
      {!isDraft && (
        <div className="py-2">
          {/* Desktop / Tablet Stepper */}
          <div className="hidden md:grid md:grid-cols-6 gap-2 relative">
            {LIFECYCLE_STEPS.map((step, idx) => {
              const isPassed = !isCancelled && idx < currentStepIndex;
              const isCurrent = !isCancelled && idx === currentStepIndex;
              const timestamp = stepTimestampMap[step.key];

              return (
                <div key={step.key} className="flex flex-col items-center text-center relative group">
                  {/* Đường nối giữa các bước */}
                  {idx < LIFECYCLE_STEPS.length - 1 && (
                    <div
                      className={`absolute top-4 left-1/2 w-full h-1 -z-0 transition-all ${
                        isPassed
                          ? 'bg-emerald-500'
                          : isCurrent
                          ? 'bg-gradient-to-r from-emerald-500 to-slate-200'
                          : 'bg-slate-100'
                      }`}
                    />
                  )}

                  {/* Icon Node tròn */}
                  <div
                    className={`relative z-10 flex h-9 w-9 items-center justify-center rounded-full text-sm font-bold transition-all duration-300 ${
                      isPassed
                        ? 'bg-emerald-500 text-white shadow-xs'
                        : isCurrent
                        ? 'bg-brand text-white ring-4 ring-teal-100 shadow-sm animate-pulse'
                        : 'bg-white border-2 border-slate-200 text-slate-400'
                    }`}
                  >
                    {isPassed ? '✓' : step.icon}
                  </div>

                  {/* Nhãn bước */}
                  <p
                    className={`mt-2 text-xs font-bold transition-colors ${
                      isCurrent
                        ? 'text-brand'
                        : isPassed
                        ? 'text-slate-900'
                        : 'text-slate-400'
                    }`}
                  >
                    {step.label}
                  </p>

                  <p className="text-[10px] text-slate-400 mt-0.5 leading-tight">{step.subLabel}</p>

                  {/* Badge thời điểm nếu đã qua */}
                  {timestamp && (
                    <span className="mt-1 text-[9px] font-mono text-slate-400">
                      {new Date(timestamp).toLocaleDateString('vi-VN', {
                        day: '2-digit',
                        month: '2-digit'
                      })}
                    </span>
                  )}

                  {isCurrent && (
                    <span className="mt-1 inline-flex items-center px-1.5 py-0.5 rounded-full text-[9px] font-bold bg-teal-50 text-teal-700 border border-teal-200">
                      Đang ở bước này
                    </span>
                  )}
                </div>
              );
            })}
          </div>

          {/* Mobile Stepper (Hiển thị dạng dọc thu gọn trên điện thoại) */}
          <div className="md:hidden space-y-3">
            {LIFECYCLE_STEPS.map((step, idx) => {
              const isPassed = !isCancelled && idx < currentStepIndex;
              const isCurrent = !isCancelled && idx === currentStepIndex;
              const timestamp = stepTimestampMap[step.key];

              return (
                <div
                  key={step.key}
                  className={`flex items-start gap-3 p-2.5 rounded-xl transition-all ${
                    isCurrent
                      ? 'bg-teal-50/80 border border-teal-200 shadow-xs'
                      : isPassed
                      ? 'bg-slate-50/60'
                      : 'opacity-50'
                  }`}
                >
                  <div
                    className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                      isPassed
                        ? 'bg-emerald-500 text-white'
                        : isCurrent
                        ? 'bg-brand text-white ring-2 ring-teal-200'
                        : 'bg-slate-100 text-slate-400'
                    }`}
                  >
                    {isPassed ? '✓' : step.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <p className={`text-xs font-bold ${isCurrent ? 'text-brand' : 'text-slate-900'}`}>
                        {step.label}
                      </p>
                      {timestamp && (
                        <span className="text-[10px] text-slate-400 font-mono">
                          {new Date(timestamp).toLocaleDateString('vi-VN')}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">{step.subLabel}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* THANH CHỈ BÁO HIỆU LỰC (VALIDITY SLA TRACKER) — Khi đơn đang OPEN */}
      {validityInfo && (
        <div
          className={`rounded-xl border p-4 transition-all ${
            validityInfo.isExpired
              ? 'border-rose-200 bg-rose-50/70 text-rose-900'
              : validityInfo.isUrgent
              ? 'border-amber-200 bg-amber-50/80 text-amber-900'
              : 'border-teal-200 bg-teal-50/60 text-teal-900'
          }`}
        >
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <span className="text-lg">
                {validityInfo.isExpired ? '⛔' : validityInfo.isUrgent ? '⚠️' : '⏳'}
              </span>
              <div>
                <p className="text-xs font-bold uppercase tracking-wide">
                  {validityInfo.isExpired
                    ? 'Thời hạn hiệu lực: Đã hết hạn nhận việc'
                    : validityInfo.isUrgent
                    ? 'Cảnh báo hiệu lực: Sắp hết hạn nhận việc!'
                    : 'Hiệu lực mở nhận việc (SLA 3 ngày)'}
                </p>
                <p className="text-xs mt-0.5">
                  {validityInfo.isExpired ? (
                    <span>
                      Đơn đã quá hạn <strong>{validityInfo.timeText}</strong>. Thợ không thể nhận việc được nữa.
                    </span>
                  ) : (
                    <span>
                      Thời gian còn lại: <strong>{validityInfo.timeText}</strong> (Hạn chót:{' '}
                      {new Date(validityInfo.deadline).toLocaleString('vi-VN', {
                        hour: '2-digit',
                        minute: '2-digit',
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric'
                      })}
                      )
                    </span>
                  )}
                </p>
              </div>
            </div>

            <div className="text-right shrink-0">
              <span
                className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-extrabold ${
                  validityInfo.isExpired
                    ? 'bg-rose-200 text-rose-800'
                    : validityInfo.isUrgent
                    ? 'bg-amber-200 text-amber-800 animate-pulse'
                    : 'bg-teal-200/80 text-teal-800'
                }`}
              >
                {validityInfo.isExpired ? 'ĐÃ HẾT HẠN' : validityInfo.timeText}
              </span>
            </div>
          </div>

          {/* Thanh phần trăm thời gian hiệu lực */}
          {!validityInfo.isExpired && (
            <div className="mt-3">
              <div className="flex justify-between text-[10px] text-slate-500 font-semibold mb-1">
                <span>Thời gian đã trôi: {validityInfo.elapsedPercent}%</span>
                <span>Cửa sổ hiệu lực: 72 giờ</span>
              </div>
              <div className="w-full h-2 rounded-full bg-white/80 overflow-hidden border border-slate-200/50">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    validityInfo.isUrgent
                      ? 'bg-amber-500'
                      : 'bg-brand'
                  }`}
                  style={{ width: `${validityInfo.elapsedPercent}%` }}
                />
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
