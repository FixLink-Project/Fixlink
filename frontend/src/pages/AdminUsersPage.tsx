import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import Tabs from '../components/Tabs';
import { ApiError, api } from '../lib/api';
import type { PageMeta, Role, UserStatus, VerificationStatus } from '../lib/types';

interface TechnicianProfileBrief {
  fullName: string;
  phone: string;
  email: string;
  citizenId: string;
  verificationStatus: VerificationStatus;
  yearsExperience: number | null;
  avgRating: number;
  completedJobs: number;
  walletBalance: number;
  isOnline: boolean;
}

interface CustomerProfileBrief {
  fullName: string;
  phone: string;
  email: string;
  avatarUrl: string | null;
  membershipTier: string | null;
}

interface AdminUser {
  id: string;
  username: string;
  role: Role;
  status: UserStatus;
  createdAt: string;
  customerProfile?: CustomerProfileBrief | null;
  technicianProfile?: TechnicianProfileBrief | null;
}

const ROLE_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'ALL', label: 'Mọi vai trò' },
  { value: 'CUSTOMER', label: 'Khách hàng' },
  { value: 'TECHNICIAN', label: 'Kỹ thuật viên' },
  { value: 'STAFF', label: 'Nhân viên' },
  { value: 'ADMIN', label: 'Quản trị viên' }
];

function formatDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(date);
}

const SELECT_CLASS =
  'min-h-[44px] w-full rounded-lg border border-line bg-card px-3 text-sm text-ink sm:w-auto';

/**
 * Mỗi tab là một bộ lọc đặt sẵn. Bộ lọc vai trò và từ khóa bên dưới vẫn áp thêm
 * lên kết quả của tab đang chọn.
 */
type TabId = 'ALL' | 'PENDING' | 'ACTIVE' | 'BLOCKED';

const TABS: Array<{ id: TabId; label: string; status: string }> = [
  { id: 'ALL', label: 'Tất cả', status: 'ALL' },
  { id: 'PENDING', label: 'Chờ duyệt KYC', status: 'PENDING' },
  { id: 'ACTIVE', label: 'Đang hoạt động', status: 'ACTIVE' },
  { id: 'BLOCKED', label: 'Đã khoá', status: 'BLOCKED' }
];

const ADMIN_NAV = [
  { to: '/quan-tri', label: 'Người dùng' },
  { to: '/quan-tri/danh-muc', label: 'Danh mục dịch vụ' }
];

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  // Bộ lọc; đổi bất kỳ giá trị nào cũng quay về trang 1.
  const [page, setPage] = useState(1);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('ALL');
  const [tab, setTab] = useState<TabId>('ALL');
  const [pendingCount, setPendingCount] = useState(0);

  const [detail, setDetail] = useState<AdminUser | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionNote, setActionNote] = useState<string | null>(null);
  const [busyUserId, setBusyUserId] = useState<string | null>(null);

  const query = useMemo(() => {
    const params = new URLSearchParams({
      page: String(page),
      limit: '10',
      role,
      status: TABS.find((t) => t.id === tab)?.status ?? 'ALL'
    });
    if (search.trim()) params.set('search', search.trim());
    return params.toString();
  }, [page, role, tab, search]);

  const load = useCallback(
    async (isRefresh = false) => {
      if (isRefresh) setRefreshing(true);
      try {
        const res = await api.get<AdminUser[]>(`/admin/users?${query}`);
        setUsers(res.data);
        if (res.meta) setMeta(res.meta);
        setStatus('ready');
        setLoadError(null);
      } catch (err) {
        setLoadError(
          err instanceof ApiError ? err.message : 'Không kết nối được máy chủ. Thử tải lại trang.'
        );
        setStatus('error');
      } finally {
        setRefreshing(false);
      }
    },
    [query]
  );

  useEffect(() => {
    void load(true);
  }, [load]);

  // Con số trên tab "Chờ duyệt KYC" phải đúng kể cả khi đang ở tab khác,
  // nên đếm bằng một lượt gọi riêng thay vì lấy từ danh sách đang hiển thị.
  const refreshPendingCount = useCallback(async () => {
    try {
      const res = await api.get<AdminUser[]>('/admin/users?page=1&limit=1&status=PENDING');
      setPendingCount(res.meta?.totalItems ?? 0);
    } catch {
      // Không đếm được thì ẩn con số đi, không làm hỏng cả trang.
      setPendingCount(0);
    }
  }, []);

  useEffect(() => {
    void refreshPendingCount();
  }, [refreshPendingCount, users]);

  function applyFilter(next: () => void) {
    setPage(1);
    setActionNote(null);
    next();
  }

  async function openDetail(userId: string) {
    setDetail(null);
    setDetailLoading(true);
    setActionError(null);
    try {
      const res = await api.get<AdminUser>(`/admin/users/${userId}`);
      setDetail(res.data);
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : 'Không xem được hồ sơ. Thử lại sau.'
      );
    } finally {
      setDetailLoading(false);
    }
  }

  async function changeStatus(user: AdminUser, nextStatus: 'ACTIVE' | 'BLOCKED') {
    setActionError(null);
    setActionNote(null);
    setBusyUserId(user.id);
    try {
      await api.patch(`/admin/users/${user.id}/status`, { status: nextStatus });
      setActionNote(
        nextStatus === 'BLOCKED'
          ? `Đã khoá tài khoản ${user.username}.`
          : `Đã mở khoá tài khoản ${user.username}.`
      );
      await load(true);
      if (detail?.id === user.id) await openDetail(user.id);
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : 'Không đổi được trạng thái. Thử lại sau.'
      );
    } finally {
      setBusyUserId(null);
    }
  }

  async function verifyTechnician(user: AdminUser, decision: 'APPROVED' | 'REJECTED') {
    setActionError(null);
    setActionNote(null);
    setBusyUserId(user.id);
    try {
      await api.patch(`/admin/technicians/${user.id.replace('usr_', '')}/verify`, {
        verificationStatus: decision,
        note:
          decision === 'APPROVED'
            ? 'Đã đối chiếu căn cước, thông tin khớp hồ sơ'
            : 'Ảnh căn cước chưa rõ hoặc không khớp thông tin khai báo'
      });
      setActionNote(
        decision === 'APPROVED'
          ? `Đã duyệt hồ sơ của ${user.username}. Thợ có thể bắt đầu nhận việc.`
          : `Đã từ chối hồ sơ của ${user.username}.`
      );
      await load(true);
      if (detail?.id === user.id) await openDetail(user.id);
    } catch (err) {
      setActionError(
        err instanceof ApiError ? err.message : 'Không cập nhật được hồ sơ. Thử lại sau.'
      );
    } finally {
      setBusyUserId(null);
    }
  }

  return (
    <DashboardLayout
      nav={ADMIN_NAV}
      title="Quản lý người dùng"
      description="Tìm tài khoản, duyệt hồ sơ thợ và khoá tài khoản vi phạm."
    >
      <div className="space-y-6">
        {actionNote && <Alert tone="success">{actionNote}</Alert>}
        {actionError && <Alert tone="error">{actionError}</Alert>}

        <Tabs
          label="Lọc tài khoản theo trạng thái"
          items={TABS.map((t) => ({
            id: t.id,
            label: t.label,
            badge: t.id === 'PENDING' ? pendingCount : undefined
          }))}
          active={tab}
          onChange={(next) => applyFilter(() => setTab(next))}
        />

        <Card>
          <form
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
            onSubmit={(e) => {
              e.preventDefault();
              applyFilter(() => setSearch(searchInput));
            }}
          >
            <div className="flex-1">
              <label htmlFor="admin-search" className="mb-1.5 block text-sm font-medium">
                Tìm theo tên đăng nhập
              </label>
              <input
                id="admin-search"
                type="search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Ví dụ: tho_dien_lanh"
                className="min-h-[44px] w-full rounded-lg border border-line bg-card px-3.5 text-ink placeholder:text-ink-soft/60"
              />
            </div>

            <div>
              <label htmlFor="admin-role" className="mb-1.5 block text-sm font-medium">
                Vai trò
              </label>
              <select
                id="admin-role"
                value={role}
                onChange={(e) => applyFilter(() => setRole(e.target.value))}
                className={SELECT_CLASS}
              >
                {ROLE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>

            <Button type="submit" variant="secondary">
              Tìm
            </Button>
          </form>
        </Card>

        {status === 'loading' && (
          <div className="space-y-2">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="h-16 animate-pulse rounded-xl border border-line bg-card" />
            ))}
          </div>
        )}

        {status === 'error' && (
          <Alert tone="error" title="Chưa tải được danh sách">
            {loadError}
          </Alert>
        )}

        {status === 'ready' && (
          <Card>
            {users.length === 0 ? (
              <div className="py-10 text-center">
                <p className="font-display font-semibold">Không có tài khoản nào khớp</p>
                <p className="mt-1 text-sm text-ink-soft">
                  Thử bỏ bớt bộ lọc hoặc tìm bằng từ khoá ngắn hơn.
                </p>
              </div>
            ) : (
              <ul className={`divide-y divide-line ${refreshing ? 'opacity-60' : ''}`}>
                {users.map((user) => {
                  const pendingTechnician =
                    user.role === 'TECHNICIAN' && user.status === 'PENDING';
                  const blocked = user.status === 'BLOCKED';
                  const busy = busyUserId === user.id;

                  return (
                    <li key={user.id} className="py-4 first:pt-0 last:pb-0">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="font-display font-semibold">{user.username}</p>
                            <StatusChip status={user.role} />
                            <StatusChip status={user.status} />
                          </div>
                          <p className="mt-1 text-sm text-ink-soft">
                            Tạo ngày {formatDate(user.createdAt)}
                          </p>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          <Button
                            variant="quiet"
                            onClick={() => openDetail(user.id)}
                            disabled={busy}
                          >
                            Xem hồ sơ
                          </Button>

                          {pendingTechnician && (
                            <>
                              <Button
                                loading={busy}
                                onClick={() => verifyTechnician(user, 'APPROVED')}
                              >
                                Duyệt hồ sơ
                              </Button>
                              <Button
                                variant="secondary"
                                disabled={busy}
                                onClick={() => verifyTechnician(user, 'REJECTED')}
                              >
                                Từ chối
                              </Button>
                            </>
                          )}

                          {!pendingTechnician &&
                            (blocked ? (
                              <Button
                                variant="secondary"
                                loading={busy}
                                onClick={() => changeStatus(user, 'ACTIVE')}
                              >
                                Mở khoá
                              </Button>
                            ) : (
                              <Button
                                variant="danger"
                                loading={busy}
                                onClick={() => changeStatus(user, 'BLOCKED')}
                              >
                                Khoá tài khoản
                              </Button>
                            ))}
                        </div>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}

            {meta && (
              <div className="mt-5">
                <Pagination
                  meta={meta}
                  disabled={refreshing}
                  itemNoun="tài khoản"
                  onPageChange={(next) => {
                    setPage(next);
                    setActionNote(null);
                  }}
                />
              </div>
            )}
          </Card>
        )}

        {(detailLoading || detail) && (
          <Card
            title="Hồ sơ chi tiết"
            actions={
              <Button variant="quiet" onClick={() => setDetail(null)}>
                Đóng
              </Button>
            }
          >
            {detailLoading ? (
              <div className="space-y-3">
                <div className="h-5 w-48 animate-pulse rounded bg-line" />
                <div className="h-5 w-64 animate-pulse rounded bg-line/70" />
              </div>
            ) : detail ? (
              <dl className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
                <div>
                  <dt className="text-sm text-ink-soft">Tên đăng nhập</dt>
                  <dd className="font-medium">{detail.username}</dd>
                </div>
                <div>
                  <dt className="text-sm text-ink-soft">Vai trò và trạng thái</dt>
                  <dd className="mt-0.5 flex gap-2">
                    <StatusChip status={detail.role} />
                    <StatusChip status={detail.status} />
                  </dd>
                </div>

                {detail.customerProfile && (
                  <>
                    <div>
                      <dt className="text-sm text-ink-soft">Họ và tên</dt>
                      <dd className="font-medium">{detail.customerProfile.fullName}</dd>
                    </div>
                    <div>
                      <dt className="text-sm text-ink-soft">Liên hệ</dt>
                      <dd>
                        {detail.customerProfile.phone}, {detail.customerProfile.email}
                      </dd>
                    </div>
                  </>
                )}

                {detail.technicianProfile && (
                  <>
                    <div>
                      <dt className="text-sm text-ink-soft">Họ và tên</dt>
                      <dd className="font-medium">{detail.technicianProfile.fullName}</dd>
                    </div>
                    <div>
                      <dt className="text-sm text-ink-soft">Liên hệ</dt>
                      <dd>
                        {detail.technicianProfile.phone}, {detail.technicianProfile.email}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sm text-ink-soft">Số căn cước</dt>
                      <dd>{detail.technicianProfile.citizenId}</dd>
                    </div>
                    <div>
                      <dt className="text-sm text-ink-soft">Xác minh</dt>
                      <dd className="mt-0.5">
                        <StatusChip status={detail.technicianProfile.verificationStatus} />
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sm text-ink-soft">Kinh nghiệm</dt>
                      <dd>
                        {detail.technicianProfile.yearsExperience != null
                          ? `${detail.technicianProfile.yearsExperience} năm`
                          : 'Chưa khai'}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sm text-ink-soft">Việc đã hoàn tất</dt>
                      <dd>{detail.technicianProfile.completedJobs}</dd>
                    </div>
                  </>
                )}
              </dl>
            ) : null}
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
