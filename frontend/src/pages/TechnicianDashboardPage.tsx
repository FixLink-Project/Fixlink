import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import { ApiError, api, formatCurrency, topUpTechnicianWallet } from '../lib/api';
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

  // Nạp tiền ví FixLink
  const [showTopUpModal, setShowTopUpModal] = useState(false);
  const [topUpAmount, setTopUpAmount] = useState<number>(500000);
  const [customTopUpInput, setCustomTopUpInput] = useState<string>('500000');
  const [toppingUp, setToppingUp] = useState(false);
  const [topUpSuccess, setTopUpSuccess] = useState<string | null>(null);
  const [topUpError, setTopUpError] = useState<string | null>(null);

  const handleTopUp = async () => {
    if (topUpAmount < 10000) {
      setTopUpError('Số tiền nạp tối thiểu là 10.000 VNĐ');
      return;
    }
    setToppingUp(true);
    setTopUpError(null);
    try {
      const res = await topUpTechnicianWallet(topUpAmount);
      if (profile) {
        setProfile({ ...profile, walletBalance: res.data.newBalance });
      }
      setTopUpSuccess(`Nạp thành công ${formatCurrency(topUpAmount)} vào ví FixLink!`);
      setTimeout(() => {
        setShowTopUpModal(false);
        setTopUpSuccess(null);
      }, 1500);
    } catch (err: any) {
      setTopUpError(err.message || 'Nạp tiền thất bại.');
    } finally {
      setToppingUp(false);
    }
  };



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
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs flex flex-col justify-between">
              <div>
                <dt className="flex items-center justify-between text-xs font-semibold text-slate-500">
                  <span>Số dư ví FixLink</span>
                  <span className="inline-flex items-center justify-center h-7 w-7 rounded-lg border text-sm bg-blue-50 text-brand border-blue-200">
                    💰
                  </span>
                </dt>
                <dd className={`mt-2 font-display text-2xl font-bold ${
                  profile.walletBalance < 0
                    ? 'text-rose-600'
                    : profile.walletBalance < 500000
                    ? 'text-amber-600'
                    : 'text-slate-900'
                }`}>
                  {formatCurrency(profile.walletBalance)}
                </dd>
                <div className="mt-1 flex items-center gap-1.5">
                  {profile.walletBalance >= 500000 && (
                    <span className="inline-block rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-medium text-emerald-700 border border-emerald-200">
                      ✓ An toàn (&ge;500k)
                    </span>
                  )}
                  {profile.walletBalance >= 0 && profile.walletBalance < 500000 && (
                    <span className="inline-block rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700 border border-amber-200">
                      ⚠️ Dưới ngưỡng 500k
                    </span>
                  )}
                  {profile.walletBalance < 0 && (
                    <span className="inline-block rounded-full bg-rose-50 px-2 py-0.5 text-[11px] font-medium text-rose-700 border border-rose-200">
                      ⛔ Công nợ âm
                    </span>
                  )}
                </div>
              </div>
              <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between">
                <span className="text-[11px] text-slate-400">Ngưỡng chuẩn: 500k</span>
                <button
                  type="button"
                  onClick={() => {
                    const need = profile.walletBalance < 500000 ? Math.max(500000 - profile.walletBalance, 200000) : 500000;
                    setTopUpAmount(need);
                    setCustomTopUpInput(String(need));
                    setShowTopUpModal(true);
                  }}
                  className="inline-flex items-center gap-1 rounded-lg bg-blue-50 hover:bg-blue-100 text-brand px-2.5 py-1 text-xs font-bold transition-colors cursor-pointer"
                >
                  <span>+</span> Nạp tiền ví
                </button>
              </div>
            </div>
          </dl>

          {/* CẢNH BÁO CÔNG NỢ VÍ ÂM */}
          {profile.walletBalance < 0 && (
            <div className="mb-6 rounded-2xl border border-rose-300 bg-rose-50/90 p-4 shadow-xs animate-fade-in">
              <div className="flex items-start gap-3">
                <span className="text-2xl">⛔</span>
                <div className="flex-1">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                    <p className="text-xs font-bold text-rose-950 uppercase tracking-wider">
                      Công nợ ví FixLink: {formatCurrency(profile.walletBalance)}
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        const need = Math.abs(profile.walletBalance) + 500000;
                        setTopUpAmount(need);
                        setCustomTopUpInput(String(need));
                        setShowTopUpModal(true);
                      }}
                      className="self-start sm:self-auto text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 px-3 py-1.5 rounded-lg shadow-xs transition-colors cursor-pointer"
                    >
                      + Nạp tiền cân bằng ví
                    </button>
                  </div>
                  <p className="text-xs text-rose-900 mt-1.5 leading-relaxed">
                    Ví của bạn đang có khoản công nợ hoa hồng cần thanh toán sau các đơn hàng thanh toán tiền mặt trực tiếp. Quyền gửi báo giá cho các yêu cầu mới tạm hoãn cho đến khi số dư được nạp lại. Vui lòng nạp tiền vào ví để tiếp tục hoạt động!
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* CẢNH BÁO DƯỚI NGƯỠNG AN TOÀN 500K (CHẾ ĐỘ HỖ TRỢ LINH HOẠT) */}
          {profile.walletBalance >= 0 && profile.walletBalance < 500000 && (
            <div className="mb-6 rounded-2xl border border-amber-300 bg-amber-50/90 p-4 shadow-xs animate-fade-in">
              <div className="flex items-start gap-3">
                <span className="text-2xl">⚠️</span>
                <div className="flex-1">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                    <p className="text-xs font-bold text-amber-950 uppercase tracking-wider">
                      Số dư ví dưới ngưỡng ký quỹ an toàn 500.000đ
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        const need = 500000 - profile.walletBalance;
                        setTopUpAmount(need);
                        setCustomTopUpInput(String(need));
                        setShowTopUpModal(true);
                      }}
                      className="self-start sm:self-auto text-xs font-bold text-amber-950 bg-amber-200/80 hover:bg-amber-300 border border-amber-300 px-3 py-1.5 rounded-lg transition-colors cursor-pointer"
                    >
                      + Nạp tiền bổ sung
                    </button>
                  </div>
                  <p className="text-xs text-amber-900 mt-1.5 leading-relaxed">
                    Số dư ví hiện tại là <strong>{formatCurrency(profile.walletBalance)}</strong> (thấp hơn ngưỡng tiêu chuẩn 500.000đ). Hệ thống đang kích hoạt <strong>Chế độ hỗ trợ linh hoạt</strong>: bạn vẫn có thể nhận việc và gửi báo giá bình thường. Vui lòng nạp thêm tiền vào ví để duy trì nhận việc ổn định và khấu trừ hoa hồng cho các đơn thanh toán tiền mặt tiếp theo.
                  </p>
                </div>
              </div>
            </div>
          )}


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

      {/* MODAL NẠP TIỀN VÀO VÍ FIXLINK */}
      {showTopUpModal && profile && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs animate-fade-in">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="text-2xl">💰</span>
                <h3 className="font-bold text-slate-900 text-base">
                  Nạp tiền vào Ví FixLink
                </h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  setShowTopUpModal(false);
                  setTopUpError(null);
                  setTopUpSuccess(null);
                }}
                className="text-slate-400 hover:text-slate-600 font-bold p-1 text-lg cursor-pointer"
              >
                ✕
              </button>
            </div>

            {topUpSuccess ? (
              <div className="py-6 text-center space-y-2">
                <span className="text-4xl">🎉</span>
                <p className="font-bold text-emerald-700 text-sm">{topUpSuccess}</p>
                <p className="text-xs text-slate-500">Số dư ví của bạn đã được cập nhật thành công.</p>
              </div>
            ) : (
              <div className="space-y-4 text-xs text-slate-600">
                <p>
                  Chọn hoặc nhập số tiền bạn muốn nạp vào ví để duy trì số dư an toàn trên <strong>500.000đ</strong>:
                </p>

                {/* Quick preset buttons */}
                <div className="grid grid-cols-3 gap-2">
                  {[200000, 500000, 1000000].map((amt) => (
                    <button
                      key={amt}
                      type="button"
                      onClick={() => {
                        setTopUpAmount(amt);
                        setCustomTopUpInput(String(amt));
                      }}
                      className={`rounded-xl border py-2 text-xs font-bold transition-all cursor-pointer ${
                        topUpAmount === amt
                          ? 'border-brand bg-brand/10 text-brand ring-1 ring-brand'
                          : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      {formatCurrency(amt)}
                    </button>
                  ))}
                </div>

                {/* Custom amount input */}
                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                    Số tiền muốn nạp (VNĐ):
                  </label>
                  <input
                    type="number"
                    min="10000"
                    step="10000"
                    value={customTopUpInput}
                    onChange={(e) => {
                      setCustomTopUpInput(e.target.value);
                      const val = parseInt(e.target.value, 10);
                      if (!isNaN(val)) setTopUpAmount(val);
                    }}
                    className="w-full rounded-xl border border-slate-200 p-2.5 text-sm font-semibold text-slate-900 focus:border-brand focus:outline-none"
                    placeholder="Nhập số tiền..."
                  />
                </div>

                {/* QR Code and bank details */}
                <div className="rounded-xl bg-slate-50 border border-slate-200 p-3.5 space-y-3">
                  <div className="flex flex-col items-center">
                    <img
                      src={`https://api.vietqr.io/image/970422-0912345678-compact2.png?amount=${topUpAmount}&addInfo=NAPVI%20${profile.userId}&accountName=CONG%20TY%20CO%20PHAN%20FIXLINK`}
                      alt="VietQR nạp tiền"
                      className="h-44 w-44 object-contain rounded-lg border border-slate-200 bg-white p-1"
                    />
                    <p className="mt-1 text-[11px] text-slate-500 font-medium">Quét mã QR bằng App Ngân hàng</p>
                  </div>

                  <div className="space-y-1.5 text-[11px]">
                    <div className="flex justify-between">
                      <span className="text-slate-500">Ngân hàng:</span>
                      <span className="font-bold text-slate-800">MB Bank (Quân Đội)</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Số tài khoản:</span>
                      <span className="font-mono font-bold text-slate-800">0912345678</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Chủ tài khoản:</span>
                      <span className="font-bold text-slate-800">CONG TY CO PHAN FIXLINK</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-500">Nội dung chuyển khoản:</span>
                      <span className="font-mono font-bold text-brand bg-blue-50 px-1.5 py-0.5 rounded">
                        NAPVI {profile.userId}
                      </span>
                    </div>
                    <div className="flex justify-between border-t border-slate-200 pt-1.5">
                      <span className="text-slate-700 font-bold">Số tiền nạp:</span>
                      <span className="font-bold text-emerald-700 text-xs">{formatCurrency(topUpAmount)}</span>
                    </div>
                  </div>
                </div>

                {topUpError && (
                  <p className="text-xs text-rose-600 font-medium bg-rose-50 border border-rose-200 p-2 rounded-lg">
                    {topUpError}
                  </p>
                )}

                <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setShowTopUpModal(false);
                      setTopUpError(null);
                    }}
                    className="text-xs py-2 px-4"
                  >
                    Đóng
                  </Button>
                  <Button
                    variant="primary"
                    loading={toppingUp}
                    onClick={handleTopUp}
                    className="text-xs py-2 px-4 bg-emerald-600 hover:bg-emerald-700 font-bold"
                  >
                    ✓ Tôi đã chuyển khoản nạp tiền
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
