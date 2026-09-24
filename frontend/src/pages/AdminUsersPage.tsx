import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import Tabs from '../components/Tabs';
import { ApiError, api, formatCurrency } from '../lib/api';
import type { PageMeta, Role, UserStatus, VerificationStatus } from '../lib/types';

interface TechnicianProfileBrief {
  fullName: string;
  phone: string;
  email: string;
  citizenId: string;
  avatarUrl?: string | null;
  verificationStatus: VerificationStatus;
  yearsExperience: number | null;
  avgRating: number;
  completedJobs: number;
  walletBalance: number;
  isOnline: boolean;
  idCardFrontUrl?: string | null;
  idCardBackUrl?: string | null;
  bio?: string | null;
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
  const [pageSize, setPageSize] = useState(5);
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
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        if (previewImage) setPreviewImage(null);
        else if (detail) setDetail(null);
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [detail, previewImage]);

  const query = useMemo(() => {
    const params = new URLSearchParams({
      page: String(page),
      limit: String(pageSize),
      role,
      status: TABS.find((t) => t.id === tab)?.status ?? 'ALL'
    });
    if (search.trim()) params.set('search', search.trim());
    return params.toString();
  }, [page, pageSize, role, tab, search]);

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
                {users.map((user, index) => {
                  const itemIndex = (page - 1) * pageSize + index + 1;
                  const pendingTechnician =
                    user.role === 'TECHNICIAN' && user.status === 'PENDING';
                  const blocked = user.status === 'BLOCKED';
                  const busy = busyUserId === user.id;

                  return (
                    <li key={user.id} className="py-4 first:pt-0 last:pb-0">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="flex items-start gap-3 min-w-0">
                          {/* Đánh số thứ tự trong ô bo góc */}
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 border border-slate-200/80 font-mono font-bold text-slate-700 text-sm shadow-2xs mt-0.5">
                            {itemIndex}
                          </div>

                          <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className="font-display font-semibold text-slate-900">{user.username}</p>
                              <StatusChip status={user.role} />
                              <StatusChip status={user.status} />
                            </div>
                            <p className="mt-1 text-sm text-ink-soft">
                              Tạo ngày {formatDate(user.createdAt)}
                            </p>
                          </div>
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
                  pageSize={pageSize}
                  pageSizeOptions={[5, 10, 20]}
                  onPageSizeChange={(newSize) => {
                    setPageSize(newSize);
                    setPage(1);
                  }}
                  onPageChange={(next) => {
                    setPage(next);
                    setActionNote(null);
                  }}
                />
              </div>
            )}
          </Card>
        )}

        {/* Modal Popup: Chi tiết Hồ sơ Người dùng / Kỹ thuật viên */}
        {(detailLoading || detail) && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs animate-in fade-in duration-200"
            onClick={() => {
              if (!detailLoading) setDetail(null);
            }}
          >
            <div
              className="relative max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl transition-all border border-slate-100 space-y-6"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Modal Header */}
              <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand/10 text-brand font-bold text-lg">
                    {detail?.role === 'TECHNICIAN' ? '🔧' : '👤'}
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-slate-900">
                      {detail?.role === 'TECHNICIAN' ? 'Hồ sơ Thợ sửa chữa' : 'Thông tin tài khoản'}
                    </h2>
                    <p className="text-xs text-slate-500">
                      {detail ? `Mã tài khoản: ${detail.id}` : 'Đang tải thông tin...'}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setDetail(null)}
                  className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors"
                  title="Đóng popup (Esc)"
                >
                  ✕
                </button>
              </div>

              {/* Modal Body */}
              {detailLoading ? (
                <div className="space-y-4 py-8">
                  <div className="flex items-center gap-4">
                    <div className="h-14 w-14 rounded-full bg-slate-100 animate-pulse" />
                    <div className="space-y-2 flex-1">
                      <div className="h-5 w-48 rounded bg-slate-100 animate-pulse" />
                      <div className="h-4 w-32 rounded bg-slate-100 animate-pulse" />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4 pt-4">
                    <div className="h-12 rounded-xl bg-slate-100 animate-pulse" />
                    <div className="h-12 rounded-xl bg-slate-100 animate-pulse" />
                    <div className="h-12 rounded-xl bg-slate-100 animate-pulse" />
                    <div className="h-12 rounded-xl bg-slate-100 animate-pulse" />
                  </div>
                </div>
              ) : detail ? (
                <div className="space-y-5">
                  {/* User Basic Info Header */}
                  <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                    <div className="flex items-center gap-3.5">
                      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-brand text-white font-bold text-lg shadow-xs">
                        {(detail.technicianProfile?.fullName || detail.customerProfile?.fullName || detail.username).charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <h3 className="font-bold text-slate-900 text-base">
                          {detail.technicianProfile?.fullName || detail.customerProfile?.fullName || detail.username}
                        </h3>
                        <p className="text-xs text-slate-500 font-mono">@{detail.username}</p>
                      </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusChip status={detail.role} />
                      <StatusChip status={detail.status} />
                      {detail.technicianProfile && (
                        <StatusChip status={detail.technicianProfile.verificationStatus} />
                      )}
                    </div>
                  </div>

                  {/* Grid Info */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="p-3.5 rounded-xl border border-slate-100 bg-white">
                      <span className="text-xs font-medium text-slate-400">Số điện thoại</span>
                      <p className="text-sm font-semibold text-slate-800 mt-0.5">
                        {detail.technicianProfile?.phone || detail.customerProfile?.phone || 'Chưa cập nhật'}
                      </p>
                    </div>
                    <div className="p-3.5 rounded-xl border border-slate-100 bg-white">
                      <span className="text-xs font-medium text-slate-400">Email liên hệ</span>
                      <p className="text-sm font-semibold text-slate-800 mt-0.5 truncate">
                        {detail.technicianProfile?.email || detail.customerProfile?.email || 'Chưa cập nhật'}
                      </p>
                    </div>
                    <div className="p-3.5 rounded-xl border border-slate-100 bg-white">
                      <span className="text-xs font-medium text-slate-400">Ngày tham gia hệ thống</span>
                      <p className="text-sm font-semibold text-slate-800 mt-0.5">
                        {formatDate(detail.createdAt)}
                      </p>
                    </div>

                    {/* Thợ sửa chữa chi tiết */}
                    {detail.technicianProfile && (
                      <>
                        <div className="p-3.5 rounded-xl border border-slate-100 bg-white">
                          <span className="text-xs font-medium text-slate-400">Số CCCD / CMND</span>
                          <p className="text-sm font-semibold text-slate-800 font-mono mt-0.5">
                            {detail.technicianProfile.citizenId || 'Chưa khai'}
                          </p>
                        </div>
                        <div className="p-3.5 rounded-xl border border-slate-100 bg-white">
                          <span className="text-xs font-medium text-slate-400">Kinh nghiệm hành nghề</span>
                          <p className="text-sm font-semibold text-slate-800 mt-0.5">
                            {detail.technicianProfile.yearsExperience != null ? `${detail.technicianProfile.yearsExperience} năm kinh nghiệm` : 'Chưa khai'}
                          </p>
                        </div>
                        <div className="p-3.5 rounded-xl border border-slate-100 bg-white">
                          <span className="text-xs font-medium text-slate-400">Số việc hoàn thành & Đánh giá</span>
                          <p className="text-sm font-semibold text-slate-800 mt-0.5 flex items-center gap-2">
                            <span>{detail.technicianProfile.completedJobs} đơn</span>
                            <span className="text-amber-500 font-bold">★ {detail.technicianProfile.avgRating?.toFixed(1) || '5.0'}</span>
                          </p>
                        </div>
                        <div className="p-3.5 rounded-xl border border-slate-100 bg-white sm:col-span-2">
                          <span className="text-xs font-medium text-slate-400">Số dư ví thợ</span>
                          <p className="text-sm font-bold text-emerald-600 mt-0.5">
                            {formatCurrency(detail.technicianProfile.walletBalance || 0)}
                          </p>
                        </div>

                        {detail.technicianProfile.bio && (
                          <div className="p-3.5 rounded-xl border border-slate-100 bg-white sm:col-span-2">
                            <span className="text-xs font-medium text-slate-400">Tiểu sử & Chuyên môn</span>
                            <p className="text-sm text-slate-700 mt-1 leading-relaxed">
                              {detail.technicianProfile.bio}
                            </p>
                          </div>
                        )}

                        {/* Ảnh CCCD 2 mặt */}
                        {(detail.technicianProfile.idCardFrontUrl || detail.technicianProfile.idCardBackUrl) && (
                          <div className="sm:col-span-2 space-y-2 pt-2">
                            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                              Ảnh Căn cước công dân (KYC)
                            </span>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                              {detail.technicianProfile.idCardFrontUrl && (
                                <div className="group relative rounded-xl border border-slate-200 overflow-hidden bg-slate-50">
                                  <div className="px-3 py-1.5 bg-slate-100 border-b border-slate-200 text-xs font-semibold text-slate-600 flex justify-between items-center">
                                    <span>Mặt trước CCCD</span>
                                    <span className="text-2xs text-brand font-medium group-hover:underline">Bấm phóng to</span>
                                  </div>
                                  <img
                                    src={detail.technicianProfile.idCardFrontUrl}
                                    alt="Mặt trước CCCD"
                                    className="h-36 w-full object-cover cursor-pointer hover:opacity-90 transition-opacity"
                                    onClick={() => setPreviewImage(detail.technicianProfile!.idCardFrontUrl!)}
                                  />
                                </div>
                              )}
                              {detail.technicianProfile.idCardBackUrl && (
                                <div className="group relative rounded-xl border border-slate-200 overflow-hidden bg-slate-50">
                                  <div className="px-3 py-1.5 bg-slate-100 border-b border-slate-200 text-xs font-semibold text-slate-600 flex justify-between items-center">
                                    <span>Mặt sau CCCD</span>
                                    <span className="text-2xs text-brand font-medium group-hover:underline">Bấm phóng to</span>
                                  </div>
                                  <img
                                    src={detail.technicianProfile.idCardBackUrl}
                                    alt="Mặt sau CCCD"
                                    className="h-36 w-full object-cover cursor-pointer hover:opacity-90 transition-opacity"
                                    onClick={() => setPreviewImage(detail.technicianProfile!.idCardBackUrl!)}
                                  />
                                </div>
                              )}
                            </div>
                          </div>
                        )}
                      </>
                    )}

                    {/* Khách hàng chi tiết */}
                    {detail.customerProfile && (
                      <div className="p-3.5 rounded-xl border border-slate-100 bg-white sm:col-span-2">
                        <span className="text-xs font-medium text-slate-400">Hạng thành viên</span>
                        <p className="text-sm font-semibold text-brand mt-0.5">
                          {detail.customerProfile.membershipTier || 'Tiêu chuẩn'}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Modal Footer Actions */}
                  <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-slate-100">
                    <div className="flex flex-wrap gap-2">
                      {detail.role === 'TECHNICIAN' && detail.technicianProfile?.verificationStatus === 'PENDING' && (
                        <>
                          <Button
                            loading={busyUserId === detail.id}
                            onClick={async () => {
                              await verifyTechnician(detail, 'APPROVED');
                            }}
                          >
                            ✓ Duyệt hồ sơ thợ
                          </Button>
                          <Button
                            variant="secondary"
                            disabled={busyUserId === detail.id}
                            onClick={async () => {
                              await verifyTechnician(detail, 'REJECTED');
                            }}
                          >
                            ✕ Từ chối
                          </Button>
                        </>
                      )}

                      {!(detail.role === 'TECHNICIAN' && detail.technicianProfile?.verificationStatus === 'PENDING') && (
                        detail.status === 'BLOCKED' ? (
                          <Button
                            variant="secondary"
                            loading={busyUserId === detail.id}
                            onClick={() => changeStatus(detail, 'ACTIVE')}
                          >
                            Mở khoá tài khoản
                          </Button>
                        ) : (
                          <Button
                            variant="danger"
                            loading={busyUserId === detail.id}
                            onClick={() => changeStatus(detail, 'BLOCKED')}
                          >
                            Khoá tài khoản
                          </Button>
                        )
                      )}
                    </div>

                    <Button variant="secondary" onClick={() => setDetail(null)}>
                      Đóng
                    </Button>
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        )}

        {/* Lightbox Preview for CCCD Photo */}
        {previewImage && (
          <div
            className="fixed inset-0 z-60 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
            onClick={() => setPreviewImage(null)}
          >
            <div className="relative max-h-[90vh] max-w-[90vw]">
              <button
                type="button"
                onClick={() => setPreviewImage(null)}
                className="absolute -top-10 right-0 text-white font-bold text-base hover:text-slate-300"
              >
                ✕ Đóng (Esc)
              </button>
              <img
                src={previewImage}
                alt="Phóng to CCCD"
                className="max-h-[85vh] max-w-full rounded-xl object-contain shadow-2xl"
              />
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
