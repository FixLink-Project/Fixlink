import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import StatusChip from '../components/StatusChip';
import { ApiError, api, formatCurrency } from '../lib/api';
import type { VerificationStatus } from '../lib/types';

interface SimpleRef {
  id: number;
  name: string;
}

interface TechnicianProfile {
  userId: number;
  fullName: string;
  verificationStatus: VerificationStatus;
  isOnline: boolean;
  avgRating: number;
  completedJobs: number;
  walletBalance: number;
  categories: SimpleRef[];
  areas: SimpleRef[];
}

interface RepairRequest {
  id: string;
  customerName: string;
  customerPhone: string;
  serviceType: string;
  address: string;
  description: string;
  status: string;
  scheduledTime: string;
}

const VERIFICATION_NOTICE: Record<
  VerificationStatus,
  { tone: 'warning' | 'success' | 'error'; title: string; body: string }
> = {
  PENDING: {
    tone: 'warning',
    title: 'Hồ sơ đang chờ xác minh',
    body: 'Quản trị viên đang đối chiếu căn cước của bạn. Trong lúc chờ, bạn xem và sửa được hồ sơ nhưng chưa nhận được yêu cầu sửa chữa.'
  },
  APPROVED: {
    tone: 'success',
    title: 'Hồ sơ đã được xác minh',
    body: 'Bật chế độ nhận việc để khách hàng quanh khu vực bạn chọn nhìn thấy bạn.'
  },
  REJECTED: {
    tone: 'error',
    title: 'Hồ sơ bị từ chối',
    body: 'Ảnh căn cước chưa đạt yêu cầu đối chiếu. Cập nhật lại ảnh rõ nét hai mặt rồi gửi lại, hoặc gọi 1900 1234 để được hướng dẫn.'
  }
};

const TECHNICIAN_NAV = [
  { to: '/tho', label: 'Bảng điều khiển' },
  { to: '/tho/ho-so', label: 'Hồ sơ của tôi' }
];

export default function TechnicianDashboardPage() {
  const [profile, setProfile] = useState<TechnicianProfile | null>(null);
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [requestsLocked, setRequestsLocked] = useState(false);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);

  const [togglingOnline, setTogglingOnline] = useState(false);
  const [onlineError, setOnlineError] = useState<string | null>(null);

  const loadRequests = useCallback(async () => {
    try {
      const res = await api.get<RepairRequest[]>('/technicians/me/repair-requests');
      setRequests(res.data);
      setRequestsLocked(false);
    } catch (err) {
      // Thợ chưa được duyệt bị chặn 403; đó là trạng thái hợp lệ, không phải lỗi tải.
      if (err instanceof ApiError && err.statusCode === 403) {
        setRequests([]);
        setRequestsLocked(true);
      } else {
        throw err;
      }
    }
  }, []);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const res = await api.get<TechnicianProfile>('/technicians/me/profile');
        if (!alive) return;
        setProfile(res.data);
        setStatus('ready');
        await loadRequests();
      } catch (err) {
        if (!alive) return;
        setLoadError(
          err instanceof ApiError ? err.message : 'Không kết nối được máy chủ. Thử tải lại trang.'
        );
        setStatus('error');
      }
    })();
    return () => {
      alive = false;
    };
  }, [loadRequests]);

  async function handleToggleOnline() {
    if (!profile) return;
    setOnlineError(null);
    setTogglingOnline(true);
    try {
      const res = await api.patch<TechnicianProfile>('/technicians/me/status/online', {
        isOnline: !profile.isOnline
      });
      setProfile((current) => (current ? { ...current, isOnline: res.data.isOnline } : current));
    } catch (err) {
      setOnlineError(
        err instanceof ApiError ? err.message : 'Không đổi được trạng thái. Thử lại sau.'
      );
    } finally {
      setTogglingOnline(false);
    }
  }

  const approved = profile?.verificationStatus === 'APPROVED';
  const notice = profile ? VERIFICATION_NOTICE[profile.verificationStatus] : null;
  const missingCoverage =
    profile != null && (profile.categories.length === 0 || profile.areas.length === 0);

  return (
    <DashboardLayout
      nav={TECHNICIAN_NAV}
      title="Bảng điều khiển thợ"
      description="Theo dõi trạng thái hồ sơ, bật tắt nhận việc và xem yêu cầu sửa chữa gửi tới bạn."
      actions={profile && <StatusChip status={profile.verificationStatus} />}
    >
      {status === 'loading' && (
        <div className="space-y-6">
          <div className="h-20 animate-pulse rounded-xl border border-line bg-card" />
          <div className="h-32 animate-pulse rounded-xl border border-line bg-card" />
        </div>
      )}

      {status === 'error' && (
        <Alert tone="error" title="Chưa tải được bảng điều khiển">
          {loadError}
        </Alert>
      )}

      {status === 'ready' && profile && notice && (
        <div className="space-y-6">
          <Alert tone={notice.tone} title={notice.title}>
            {notice.body}
          </Alert>

          {missingCoverage && (
            <Alert tone="info" title="Chưa chọn nhóm việc hoặc khu vực">
              Hệ thống chỉ gửi yêu cầu khớp cả nhóm việc lẫn địa bàn bạn nhận.{' '}
              <Link to="/tho/ho-so" className="text-brand underline hover:text-brand-strong">
                Bổ sung trong hồ sơ
              </Link>{' '}
              để bắt đầu nhận việc.
            </Alert>
          )}

          {/* Ba số liệu, dùng Space Grotesk cho phần số */}
          <dl className="grid gap-px overflow-hidden rounded-xl border border-line bg-line sm:grid-cols-3">
            <div className="bg-card px-5 py-4">
              <dt className="text-sm text-ink-soft">Việc đã hoàn tất</dt>
              <dd className="mt-1 font-display text-2xl font-semibold">{profile.completedJobs}</dd>
            </div>
            <div className="bg-card px-5 py-4">
              <dt className="text-sm text-ink-soft">Điểm đánh giá</dt>
              <dd className="mt-1 font-display text-2xl font-semibold">
                {profile.completedJobs > 0 ? Number(profile.avgRating).toFixed(1) : 'Chưa có'}
              </dd>
            </div>
            <div className="bg-card px-5 py-4">
              <dt className="text-sm text-ink-soft">Số dư ví</dt>
              <dd className="mt-1 font-display text-2xl font-semibold">
                {formatCurrency(profile.walletBalance)}
              </dd>
            </div>
          </dl>

          <Card
            title="Nhận yêu cầu sửa chữa"
            description={
              approved
                ? 'Bật lên khi bạn đang rảnh. Tắt đi thì khách không nhìn thấy bạn nữa.'
                : 'Mở khoá sau khi quản trị viên xác minh hồ sơ.'
            }
            actions={
              <Button
                variant={profile.isOnline ? 'secondary' : 'primary'}
                loading={togglingOnline}
                disabled={!approved}
                onClick={handleToggleOnline}
              >
                {profile.isOnline ? 'Tạm dừng nhận việc' : 'Bật nhận việc'}
              </Button>
            }
          >
            {onlineError && <Alert tone="error">{onlineError}</Alert>}
            <p className="text-ink-soft">
              Trạng thái hiện tại:{' '}
              <StatusChip
                status={profile.isOnline ? 'ACTIVE' : 'INACTIVE'}
                label={profile.isOnline ? 'Đang nhận việc' : 'Đang tạm dừng'}
              />
            </p>
          </Card>

          <Card
            title="Yêu cầu gửi tới bạn"
            description={approved ? 'Gọi cho khách để xác nhận trước khi tới.' : undefined}
          >
            {requestsLocked ? (
              <div className="py-8 text-center">
                <p className="font-display font-semibold">Chưa mở khoá phần này</p>
                <p className="mx-auto mt-2 max-w-sm text-sm text-ink-soft">
                  Yêu cầu sửa chữa chỉ hiện ra sau khi hồ sơ của bạn được xác minh. Trong lúc chờ,
                  hãy hoàn thiện nhóm việc và khu vực hoạt động để được ghép đúng khách.
                </p>
              </div>
            ) : requests.length === 0 ? (
              <div className="py-8 text-center">
                <p className="font-display font-semibold">Chưa có yêu cầu nào</p>
                <p className="mt-2 text-sm text-ink-soft">
                  Bật chế độ nhận việc và chọn thêm khu vực để tiếp cận nhiều khách hơn.
                </p>
              </div>
            ) : (
              <ul className="divide-y divide-line">
                {requests.map((request) => (
                  <li key={request.id} className="py-4 first:pt-0 last:pb-0">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <h3 className="font-display font-semibold">{request.serviceType}</h3>
                      <StatusChip status={request.status} label="Yêu cầu mới" />
                    </div>
                    <p className="mt-1.5 text-sm text-ink-soft">{request.description}</p>
                    <dl className="mt-3 space-y-1 text-sm">
                      <div className="flex gap-2">
                        <dt className="w-24 shrink-0 text-ink-soft">Khách hàng</dt>
                        <dd>
                          {request.customerName}{' '}
                          <a
                            href={`tel:${request.customerPhone}`}
                            className="text-brand hover:text-brand-strong hover:underline"
                          >
                            {request.customerPhone}
                          </a>
                        </dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-24 shrink-0 text-ink-soft">Địa chỉ</dt>
                        <dd>{request.address}</dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-24 shrink-0 text-ink-soft">Hẹn lúc</dt>
                        <dd>{request.scheduledTime}</dd>
                      </div>
                    </dl>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      )}
    </DashboardLayout>
  );
}
