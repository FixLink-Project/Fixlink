type Tone = 'pending' | 'success' | 'danger' | 'neutral';

/**
 * Chip trạng thái dùng chung cho mọi màn hình.
 *
 * <p>Quy ước màu: chờ duyệt → amber, đã duyệt/hoàn tất → teal, bị từ chối/khoá →
 * rose, còn lại → xám. Chữ luôn dùng màu mực để giữ độ tương phản; màu trạng thái
 * nằm ở nền nhạt và chấm tròn bên trái.
 */
const TONES: Record<Tone, { chip: string; dot: string }> = {
  pending: { chip: 'bg-amber-soft text-ink', dot: 'bg-amber' },
  success: { chip: 'bg-brand/10 text-brand-ink', dot: 'bg-brand' },
  danger: { chip: 'bg-rose-soft text-ink', dot: 'bg-rose' },
  neutral: { chip: 'bg-surface text-ink-soft', dot: 'bg-ink-soft' }
};

const LABELS: Record<string, { label: string; tone: Tone }> = {
  // Trạng thái xác minh hồ sơ
  PENDING: { label: 'Chờ duyệt', tone: 'pending' },
  APPROVED: { label: 'Đã duyệt', tone: 'success' },
  VERIFIED: { label: 'Đã xác minh', tone: 'success' },
  REJECTED: { label: 'Bị từ chối', tone: 'danger' },
  // Trạng thái tài khoản
  ACTIVE: { label: 'Đang hoạt động', tone: 'success' },
  INACTIVE: { label: 'Ngừng hoạt động', tone: 'neutral' },
  BLOCKED: { label: 'Bị khoá', tone: 'danger' },
  // Trạng thái yêu cầu sửa chữa
  DRAFT: { label: 'Bản nháp', tone: 'neutral' },
  BIDDING_OPEN: { label: 'Đang nhận báo giá', tone: 'pending' },
  MATCHED_AWAITING_DEPOSIT: { label: 'Chờ đặt cọc', tone: 'pending' },
  ASSIGNED: { label: 'Đã nhận việc', tone: 'success' },
  INSPECTING: { label: 'Đang khảo sát', tone: 'pending' },
  AWAITING_COST_APPROVAL: { label: 'Chờ duyệt chi phí', tone: 'pending' },
  IN_PROGRESS: { label: 'Đang sửa chữa', tone: 'success' },
  AWAITING_ACCEPTANCE: { label: 'Chờ nghiệm thu', tone: 'pending' },
  COMPLETED: { label: 'Đã hoàn tất', tone: 'success' },
  CANCELLED: { label: 'Đã huỷ', tone: 'neutral' },
  // Trạng thái báo giá
  ACCEPTED: { label: 'Đã chọn', tone: 'success' },
  WITHDRAWN: { label: 'Đã rút', tone: 'neutral' },
  // Vai trò
  CUSTOMER: { label: 'Khách hàng', tone: 'neutral' },
  TECHNICIAN: { label: 'Kỹ thuật viên', tone: 'neutral' },
  STAFF: { label: 'Nhân viên', tone: 'neutral' },
  ADMIN: { label: 'Quản trị viên', tone: 'neutral' }
};

interface StatusChipProps {
  status: string | null | undefined;
  /** Ghi đè nhãn hiển thị khi cần diễn đạt riêng cho một màn hình. */
  label?: string;
}

export default function StatusChip({ status, label }: StatusChipProps) {
  if (!status) return null;

  const known = LABELS[status];
  const tone = TONES[known?.tone ?? 'neutral'];

  return (
    <span
      className={`inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-medium ${tone.chip}`}
    >
      <span aria-hidden="true" className={`h-1.5 w-1.5 rounded-full ${tone.dot}`} />
      {label ?? known?.label ?? status}
    </span>
  );
}
