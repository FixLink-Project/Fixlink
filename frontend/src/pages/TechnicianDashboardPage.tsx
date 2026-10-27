import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import { ApiError, api, formatCurrency } from '../lib/api';
import type { PageMeta, RepairRequest, VerificationStatus } from '../lib/types';

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
  { to: '/tho/lich-su', label: 'Việc đã hoàn thành' },
  { to: '/tho/ho-so', label: 'Hồ sơ của tôi' }
];

export default function TechnicianDashboardPage() {
  const [profile, setProfile] = useState<TechnicianProfile | null>(null);
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [requestsLocked, setRequestsLocked] = useState(false);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);

  // Đã gỡ bỏ toggle online theo luồng đấu giá mở

  const [matching, setMatching] = useState<RepairRequest[]>([]);
  const [matchingMeta, setMatchingMeta] = useState<PageMeta | null>(null);
  const [matchingPage, setMatchingPage] = useState(1);
  const [matchingLoading, setMatchingLoading] = useState(false);
  const [matchingError, setMatchingError] = useState<string | null>(null);

  // RC-38: State cho bộ lọc khu vực và tìm kiếm từ khóa
  const [allAreas, setAllAreas] = useState<SimpleRef[]>([]);
  const [selectedAreaId, setSelectedAreaId] = useState<string>('');
  const [searchInput, setSearchInput] = useState<string>('');
  const [appliedSearch, setAppliedSearch] = useState<string>('');



  const loadRequests = useCallback(async () => {
    try {
      const res = await api.get<RepairRequest[]>('/repair-requests?tab=ALL&limit=50');
      setRequests(res.data || []);
      setRequestsLocked(false);
    } catch (err) {
      if (err instanceof ApiError && err.statusCode === 403) {
        setRequests([]);
        setRequestsLocked(true);
      } else {
        throw err;
      }
    }
  }, []);

  const loadMatching = useCallback(async (page: number, areaId?: string, search?: string) => {
    setMatchingLoading(true);
    setMatchingError(null);
    try {
      const params = new URLSearchParams();
      params.append('page', String(page));
      params.append('limit', '10');
      if (areaId) {
        params.append('areaId', areaId);
      }
      if (search && search.trim()) {
        params.append('search', search.trim());
      }
      const res = await api.get<RepairRequest[]>(`/repair-requests/matching?${params.toString()}`);
      setMatching(res.data);
      if (res.meta) setMatchingMeta(res.meta);
    } catch (err) {
      if (err instanceof ApiError && err.statusCode === 403) {
        setMatching([]);
      } else {
        setMatchingError('Không tải được danh sách yêu cầu phù hợp.');
      }
    } finally {
      setMatchingLoading(false);
    }
  }, []);

  // Tải danh sách toàn bộ khu vực hoạt động để chọn lọc
  useEffect(() => {
    api.get<SimpleRef[]>('/areas')
      .then((res) => {
        if (Array.isArray(res.data)) {
          setAllAreas(res.data);
        }
      })
      .catch(() => {});
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
        if (res.data.verificationStatus === 'APPROVED') {
          await loadMatching(1, selectedAreaId, appliedSearch);
        }
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
  }, [loadRequests, loadMatching]);

  const isMatchingFirstMount = useRef(true);
  useEffect(() => {
    if (isMatchingFirstMount.current) {
      isMatchingFirstMount.current = false;
      return;
    }
    loadMatching(matchingPage, selectedAreaId, appliedSearch);
  }, [matchingPage, loadMatching]);

  const handleSearchSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setAppliedSearch(searchInput);
    setMatchingPage(1);
    loadMatching(1, selectedAreaId, searchInput);
  };

  const handleAreaChange = (newAreaId: string) => {
    setSelectedAreaId(newAreaId);
    setMatchingPage(1);
    loadMatching(1, newAreaId, appliedSearch);
  };

  const handleResetFilters = () => {
    setSelectedAreaId('');
    setSearchInput('');
    setAppliedSearch('');
    setMatchingPage(1);
    loadMatching(1, '', '');
  };

  const approved = profile?.verificationStatus === 'APPROVED';
  const notice = profile ? VERIFICATION_NOTICE[profile.verificationStatus] : null;
  const missingCoverage =
    profile != null && (profile.categories.length === 0 || profile.areas.length === 0);

  return (
    <DashboardLayout
      nav={TECHNICIAN_NAV}
      title="Bảng điều khiển thợ"
      description="Theo dõi số dư ví, quản lý công việc đang thực hiện và tham gia đấu giá nhận việc mới."
      actions={profile && <StatusChip status={profile.verificationStatus} />}
    >
      {status === 'loading' && (
        <div className="space-y-6">
          <div className="h-20 animate-pulse rounded-2xl border border-white/10 bg-surface-card" />
          <div className="h-32 animate-pulse rounded-2xl border border-white/10 bg-surface-card" />
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
              <Link to="/tho/ho-so" className="text-brand-glow underline hover:text-brand">
                Bổ sung trong hồ sơ
              </Link>{' '}
              để bắt đầu nhận việc.
            </Alert>
          )}

          {/* Stats cards with clean white backgrounds and soft tinted badges */}
          <dl className="grid gap-4 sm:grid-cols-3">
            <Link to="/tho/lich-su" className="group block transition-transform hover:-translate-y-0.5">
              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition group-hover:border-emerald-300 group-hover:shadow-md h-full">
                <dt className="flex items-center justify-between text-xs font-semibold text-slate-500">
                  <span className="group-hover:text-emerald-700">Việc đã hoàn tất ↗</span>
                  <span className="inline-flex items-center justify-center h-7 w-7 rounded-lg border text-sm bg-emerald-50 text-emerald-700 border-emerald-200">
                    ✅
                  </span>
                </dt>
                <dd className="mt-3 font-display text-2xl font-bold text-slate-900 group-hover:text-emerald-600">
                  {profile.completedJobs}
                </dd>
              </div>
            </Link>
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
              <dt className="flex items-center justify-between text-xs font-semibold text-slate-500">
                <span>Điểm đánh giá</span>
                <span className="inline-flex items-center justify-center h-7 w-7 rounded-lg border text-sm bg-amber-50 text-amber-700 border-amber-200">
                  ⭐
                </span>
              </dt>
              <dd className="mt-3 font-display text-2xl font-bold text-slate-900">
                {profile.completedJobs > 0 ? Number(profile.avgRating).toFixed(1) : 'Chưa có'}
              </dd>
            </div>
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
              <dt className="flex items-center justify-between text-xs font-semibold text-slate-500">
                <span>Số dư ví FixLink</span>
                <span className="inline-flex items-center justify-center h-7 w-7 rounded-lg border text-sm bg-blue-50 text-brand border-blue-200">
                  💰
                </span>
              </dt>
              <dd className="mt-3 font-display text-2xl font-bold text-slate-900">
                {formatCurrency(profile.walletBalance)}
              </dd>
            </div>
          </dl>


          {/* Lọc riêng các công việc đang thực hiện (chưa hoàn thành và chưa hủy) */}
          {(() => {
            const ongoingRequests = requests.filter(
              (r) => r.status !== 'COMPLETED' && r.status !== 'CANCELLED'
            );
            const completedJobsCount = profile.completedJobs || requests.filter((r) => r.status === 'COMPLETED').length;

            return (
              <Card
                title="Công việc đang thực hiện"
                description={
                  approved
                    ? 'Danh sách các yêu cầu bạn đang phụ trách. Nhấn vào đơn để xem số điện thoại khách, đặt lịch hẹn và tiến hành sửa chữa.'
                    : undefined
                }
                actions={
                  completedJobsCount > 0 ? (
                    <Link to="/tho/lich-su">
                      <Button variant="quiet" className="text-xs">
                        Xem việc đã hoàn thành ({completedJobsCount}) →
                      </Button>
                    </Link>
                  ) : undefined
                }
              >
                {requestsLocked ? (
                  <div className="py-8 text-center">
                    <p className="text-3xl">🔒</p>
                    <p className="mt-2 font-display font-semibold text-slate-900">Chưa mở khoá phần này</p>
                    <p className="mx-auto mt-2 max-w-sm text-sm text-slate-500">
                      Yêu cầu sửa chữa chỉ hiện ra sau khi hồ sơ của bạn được xác minh. Trong lúc chờ,
                      hãy hoàn thiện nhóm việc và khu vực hoạt động để được ghép đúng khách.
                    </p>
                  </div>
                ) : ongoingRequests.length === 0 ? (
                  <div className="py-8 text-center">
                    <p className="text-3xl">📭</p>
                    <p className="mt-2 font-display font-semibold text-slate-900">Không có công việc nào đang thực hiện</p>
                    <p className="mt-2 text-sm text-slate-500">
                      Xem danh sách "Yêu cầu phù hợp" bên dưới và bấm "Nhận việc" hoặc tham gia báo giá để nhận đơn mới.
                    </p>
                    {completedJobsCount > 0 && (
                      <div className="mt-4">
                        <Link to="/tho/lich-su">
                          <Button variant="secondary" className="text-xs">
                            👉 Xem {completedJobsCount} công việc đã hoàn thành trong trang riêng →
                          </Button>
                        </Link>
                      </div>
                    )}
                  </div>
                ) : (
                  <ul className="divide-y divide-slate-100">
                    {ongoingRequests.map((request) => (
                      <li
                        key={request.id}
                        className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 py-4.5 first:pt-0 last:pb-0"
                      >
                        <div className="space-y-1.5 min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="font-mono text-xs font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                              {request.requestCode || `#${request.id}`}
                            </span>
                            <StatusChip status={request.status} label={request.statusLabel} />
                            {request.categoryName && (
                              <span className="text-xs text-slate-500 font-medium">
                                • {request.categoryName}
                              </span>
                            )}
                            <span className="text-xs text-slate-400">
                              {new Date(request.createdAt).toLocaleDateString('vi-VN')}
                            </span>
                          </div>
                          <Link
                            to={`/tho/yeu-cau/${request.id}`}
                            className="block font-bold text-slate-900 hover:text-brand transition-colors text-base"
                          >
                            {request.title}
                          </Link>
                          {request.description && (
                            <p className="text-sm text-slate-600 line-clamp-2">
                              {request.description}
                            </p>
                          )}
                          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500 pt-1">
                            {request.customerName && (
                              <span>
                                Khách hàng: <strong className="text-slate-700">{request.customerName}</strong>
                              </span>
                            )}
                            <span>
                              Địa chỉ: <strong className="text-slate-700">{request.addressLine || request.address || '—'}</strong>
                            </span>
                            <span>
                              Giá chốt: <strong className="text-emerald-600 font-semibold">{formatCurrency(request.agreedPrice || request.budgetRef)}</strong>
                            </span>
                          </div>
                        </div>
                        <div className="shrink-0 flex items-center gap-2 pt-2 sm:pt-0">
                          <Link to={`/tho/yeu-cau/${request.id}`}>
                            <Button variant="primary">
                              Làm việc với khách →
                            </Button>
                          </Link>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            );
          })()}

          {/* Matching requests for bidding */}
          {approved && (
            <Card
              title="Yêu cầu phù hợp & Tìm kiếm theo khu vực"
              description="Tìm kiếm và lọc các yêu cầu sửa chữa theo khu vực hoạt động để nhận đơn đúng địa bàn."
            >

              {/* RC-38: Filter & Search Toolbar */}
              <div className="mb-6 rounded-2xl border border-slate-200 bg-slate-50/80 p-4 shadow-xs">
                <form onSubmit={handleSearchSubmit} className="flex flex-col gap-3 md:flex-row md:items-end">
                  <div className="flex-1">
                    <label htmlFor="area-select" className="mb-1 block text-xs font-semibold text-slate-700">
                      📍 Khu vực phục vụ (Service Area)
                    </label>
                    <select
                      id="area-select"
                      value={selectedAreaId}
                      onChange={(e) => handleAreaChange(e.target.value)}
                      className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 shadow-xs transition-colors focus:border-brand focus:outline-hidden focus:ring-2 focus:ring-brand/20"
                    >
                      <option value="">Tất cả khu vực của tôi ({profile.areas.length} khu vực)</option>
                      {allAreas.length > 0
                        ? allAreas.map((area) => (
                            <option key={area.id} value={area.id}>
                              {area.name} {profile.areas.some((a) => a.id === area.id) ? '★ (Đã đăng ký)' : ''}
                            </option>
                          ))
                        : profile.areas.map((area) => (
                            <option key={area.id} value={area.id}>
                              {area.name}
                            </option>
                          ))}
                    </select>
                  </div>

                  <div className="flex-[1.5]">
                    <label htmlFor="search-input" className="mb-1 block text-xs font-semibold text-slate-700">
                      🔍 Từ khóa tìm kiếm
                    </label>
                    <input
                      id="search-input"
                      type="text"
                      value={searchInput}
                      onChange={(e) => setSearchInput(e.target.value)}
                      placeholder="Tìm theo thiết bị, sự cố, đường phố..."
                      className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-800 shadow-xs placeholder:text-slate-400 focus:border-brand focus:outline-hidden focus:ring-2 focus:ring-brand/20"
                    />
                  </div>

                  <div className="flex items-center gap-2">
                    <Button type="submit" variant="primary" loading={matchingLoading}>
                      Tìm kiếm
                    </Button>
                    {(selectedAreaId || appliedSearch) && (
                      <Button type="button" variant="secondary" onClick={handleResetFilters}>
                        Đặt lại
                      </Button>
                    )}
                  </div>
                </form>

                {/* Filter tags & Total Count */}
                <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-slate-200/80 pt-3 text-xs text-slate-600">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-slate-500">Đang lọc theo:</span>
                    {selectedAreaId ? (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-0.5 font-medium text-brand border border-blue-200">
                        📍 {allAreas.find((a) => String(a.id) === selectedAreaId)?.name || profile.areas.find((a) => String(a.id) === selectedAreaId)?.name || `Khu vực #${selectedAreaId}`}
                        <button
                          type="button"
                          onClick={() => handleAreaChange('')}
                          className="hover:text-red-600 text-brand font-bold"
                          title="Xóa lọc khu vực"
                        >
                          ✕
                        </button>
                      </span>
                    ) : (
                      <span className="inline-flex items-center rounded-full bg-slate-200/80 px-2.5 py-0.5 text-slate-700">
                        Toàn bộ khu vực đã đăng ký
                      </span>
                    )}

                    {appliedSearch && (
                      <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-0.5 font-medium text-amber-800 border border-amber-200">
                        Từ khóa: "{appliedSearch}"
                        <button
                          type="button"
                          onClick={() => {
                            setSearchInput('');
                            setAppliedSearch('');
                            setMatchingPage(1);
                            loadMatching(1, selectedAreaId, '');
                          }}
                          className="hover:text-red-600 text-amber-800 font-bold"
                          title="Xóa từ khóa"
                        >
                          ✕
                        </button>
                      </span>
                    )}
                  </div>

                  {matchingMeta && (
                    <div className="font-medium text-slate-500">
                      Tìm thấy <span className="font-bold text-brand">{matchingMeta.totalItems}</span> yêu cầu
                    </div>
                  )}
                </div>
              </div>

              {matchingError && <Alert tone="error">{matchingError}</Alert>}

              {matchingLoading && (
                <div className="h-24 animate-pulse rounded-xl border border-slate-200 bg-slate-50" />
              )}

              {!matchingLoading && matching.length === 0 && (
                <div className="py-10 text-center">
                  <p className="text-4xl">🔍</p>
                  <p className="mt-2 font-display font-semibold text-slate-900">
                    {selectedAreaId || appliedSearch ? 'Không tìm thấy yêu cầu phù hợp với bộ lọc' : 'Chưa có yêu cầu phù hợp'}
                  </p>
                  <p className="mx-auto mt-2 max-w-md text-sm text-slate-500">
                    {selectedAreaId || appliedSearch
                      ? 'Thử chọn khu vực khác, xóa bớt từ khóa hoặc bấm Đặt lại để xem tất cả đơn mở thầu.'
                      : 'Thêm nhóm việc và khu vực trong hồ sơ để tiếp cận nhiều khách hàng hơn.'}
                  </p>
                  {(selectedAreaId || appliedSearch) && (
                    <div className="mt-4">
                      <Button variant="secondary" onClick={handleResetFilters}>
                        Quay lại tất cả yêu cầu
                      </Button>
                    </div>
                  )}
                </div>
              )}

              {!matchingLoading && matching.length > 0 && (
                <div className="space-y-3">
                  {matching.map((req) => {
                    // Bỏ tiền tố nhãn trong ngoặc vuông ở đầu tiêu đề (vd "[Sắp hết hạn] ",
                    // "[Nhận việc ngay] ") cho đỡ rối — deadline đã thể hiện ở hàng meta.
                    const cleanTitle = req.title.replace(/^\s*\[[^\]]*\]\s*/, '');
                    // Hiệu lực nhận việc: tính trạng thái hết hạn / sắp hết hạn (<24h).
                    const deadlineMs = req.applyDeadline
                      ? new Date(req.applyDeadline).getTime() - Date.now()
                      : null;
                    const isExpired = deadlineMs !== null && deadlineMs <= 0;
                    const isUrgent = deadlineMs !== null && deadlineMs > 0 && deadlineMs <= 24 * 3600 * 1000;
                    // Gom meta về một dòng, ngăn cách bằng dấu "·" cho dễ quét mắt.
                    const meta = [
                      req.categoryName ? { text: req.categoryName } : null,
                      req.areaName ? { text: req.areaName } : null,
                      req.addressLine ? { text: req.addressLine } : null,
                      req.budgetRef > 0 ? { text: formatCurrency(req.budgetRef), highlight: true } : null,
                      req.quotationCount > 0
                        ? { text: `🏷️ ${req.quotationCount} thợ đã báo giá` }
                        : { text: '🏷️ Chưa có báo giá' },
                      req.applyDeadline
                        ? {
                            text: isExpired
                              ? 'Đã hết hạn nhận'
                              : isUrgent
                                ? 'Sắp hết hạn'
                                : `Hạn ${new Date(req.applyDeadline).toLocaleDateString('vi-VN')}`,
                            tone: isExpired ? 'expired' : isUrgent ? 'urgent' : undefined
                          }
                        : null
                    ].filter(Boolean) as {
                      text: string;
                      highlight?: boolean;
                      tone?: 'expired' | 'urgent';
                    }[];

                    return (
                      <div
                        key={req.id}
                        className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs transition-colors duration-200 hover:border-brand/40"
                      >
                        {/* Hàng 1: tiêu đề + 1 badge trạng thái */}
                        <div className="flex items-start justify-between gap-3">
                          <Link
                            to={`/tho/yeu-cau/${req.id}`}
                            className="min-w-0 flex-1 transition-colors hover:text-brand"
                          >
                            <h3 className="truncate font-bold text-slate-900 text-sm sm:text-base">
                              {cleanTitle}
                            </h3>
                          </Link>
                          <div className="shrink-0">
                            <StatusChip status={req.status} />
                          </div>
                        </div>

                        {/* Hàng 2: mô tả ngắn (tối đa 2 dòng) */}
                        {req.description && (
                          <p className="mt-1.5 text-sm text-slate-600 line-clamp-2">{req.description}</p>
                        )}

                        {/* Hàng 3: meta ngăn cách bằng dấu chấm giữa */}
                        <div className="mt-2.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-slate-500">
                          {meta.map((m, i) => (
                            <span key={i} className="flex items-center gap-2">
                              {i > 0 && <span className="text-slate-300">·</span>}
                              <span
                                className={
                                  m.tone === 'expired'
                                    ? 'font-semibold text-rose-600'
                                    : m.tone === 'urgent'
                                      ? 'font-semibold text-amber-600'
                                      : m.highlight
                                        ? 'font-semibold text-brand'
                                        : undefined
                                }
                              >
                                {m.text}
                              </span>
                            </span>
                          ))}
                        </div>

                        {/* Nút xem chi tiết & gửi báo giá cạnh tranh */}
                        <div className="mt-3 flex justify-end border-t border-slate-100 pt-3">
                          <Link to={`/tho/yeu-cau/${req.id}`}>
                            <Button
                              variant="primary"
                              disabled={isExpired}
                              className="text-xs sm:text-sm py-1.5 px-4"
                            >
                              {isExpired ? 'Đã hết hạn' : 'Xem & Báo giá cạnh tranh →'}
                            </Button>
                          </Link>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {matchingMeta && matchingMeta.totalPages > 1 && (
                <div className="mt-4">
                  <Pagination
                    meta={matchingMeta}
                    onPageChange={setMatchingPage}
                    disabled={matchingLoading}
                    itemNoun="yêu cầu"
                  />
                </div>
              )}
            </Card>
          )}

        </div>
      )}
    </DashboardLayout>
  );
}
