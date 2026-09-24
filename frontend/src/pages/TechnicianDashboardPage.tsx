import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, formatCurrency } from '../lib/api';
import type { PageMeta, RepairRequest as MatchingRequest, VerificationStatus } from '../lib/types';

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

  const [matching, setMatching] = useState<MatchingRequest[]>([]);
  const [matchingMeta, setMatchingMeta] = useState<PageMeta | null>(null);
  const [matchingPage, setMatchingPage] = useState(1);
  const [matchingLoading, setMatchingLoading] = useState(false);
  const [matchingError, setMatchingError] = useState<string | null>(null);

  // RC-38: State cho bộ lọc khu vực và tìm kiếm từ khóa
  const [allAreas, setAllAreas] = useState<SimpleRef[]>([]);
  const [selectedAreaId, setSelectedAreaId] = useState<string>('');
  const [searchInput, setSearchInput] = useState<string>('');
  const [appliedSearch, setAppliedSearch] = useState<string>('');

  const [quoteFor, setQuoteFor] = useState<MatchingRequest | null>(null);
  const [quoteForm, setQuoteForm] = useState({
    solution: '',
    priceLaborVnd: '',
    priceMaterialsVnd: '',
    note: ''
  });
  const [quoteSaving, setQuoteSaving] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [quoteSuccess, setQuoteSuccess] = useState<string | null>(null);

  const loadRequests = useCallback(async () => {
    try {
      const res = await api.get<RepairRequest[]>('/technicians/me/repair-requests');
      setRequests(res.data);
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
      const res = await api.get<MatchingRequest[]>(`/repair-requests/matching?${params.toString()}`);
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

  async function handleQuoteSubmit() {
    if (!quoteFor) return;
    setQuoteError(null);
    setQuoteSaving(true);
    try {
      await api.post(`/repair-requests/${quoteFor.id}/quotations`, {
        solution: quoteForm.solution.trim(),
        priceLaborVnd: Number(quoteForm.priceLaborVnd),
        priceMaterialsVnd: Number(quoteForm.priceMaterialsVnd),
        note: quoteForm.note.trim() || null
      });
      setQuoteSuccess(`Đã gửi báo giá cho "${quoteFor.title}".`);
      setQuoteFor(null);
      setQuoteForm({ solution: '', priceLaborVnd: '', priceMaterialsVnd: '', note: '' });
      loadMatching(matchingPage);
    } catch (err) {
      setQuoteError(err instanceof ApiError ? err.message : 'Không gửi được báo giá.');
    } finally {
      setQuoteSaving(false);
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
            {[
              { icon: '✅', label: 'Việc đã hoàn tất', value: profile.completedJobs, badgeBg: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
              { icon: '⭐', label: 'Điểm đánh giá', value: profile.completedJobs > 0 ? Number(profile.avgRating).toFixed(1) : 'Chưa có', badgeBg: 'bg-amber-50 text-amber-700 border-amber-200' },
              { icon: '💰', label: 'Số dư ví FixLink', value: formatCurrency(profile.walletBalance), badgeBg: 'bg-blue-50 text-brand border-blue-200' }
            ].map((stat) => (
              <div key={stat.label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
                <dt className="flex items-center justify-between text-xs font-semibold text-slate-500">
                  <span>{stat.label}</span>
                  <span className={`inline-flex items-center justify-center h-7 w-7 rounded-lg border text-sm ${stat.badgeBg}`}>
                    {stat.icon}
                  </span>
                </dt>
                <dd className="mt-3 font-display text-2xl font-bold text-slate-900">{stat.value}</dd>
              </div>
            ))}
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
                {profile.isOnline ? '⏸ Tạm dừng nhận việc' : '▶ Bật nhận việc'}
              </Button>
            }
          >
            {onlineError && <Alert tone="error">{onlineError}</Alert>}
            <p className="text-sm text-slate-600">
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
                <p className="text-3xl">🔒</p>
                <p className="mt-2 font-display font-semibold text-slate-900">Chưa mở khoá phần này</p>
                <p className="mx-auto mt-2 max-w-sm text-sm text-slate-500">
                  Yêu cầu sửa chữa chỉ hiện ra sau khi hồ sơ của bạn được xác minh. Trong lúc chờ,
                  hãy hoàn thiện nhóm việc và khu vực hoạt động để được ghép đúng khách.
                </p>
              </div>
            ) : requests.length === 0 ? (
              <div className="py-8 text-center">
                <p className="text-3xl">📭</p>
                <p className="mt-2 font-display font-semibold text-slate-900">Chưa có yêu cầu nào</p>
                <p className="mt-2 text-sm text-slate-500">
                  Bật chế độ nhận việc và chọn thêm khu vực để tiếp cận nhiều khách hơn.
                </p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {requests.map((request) => (
                  <li key={request.id} className="py-4 first:pt-0 last:pb-0">
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <h3 className="font-display font-semibold text-slate-900">{request.serviceType}</h3>
                      <StatusChip status={request.status} label="Yêu cầu mới" />
                    </div>
                    <p className="mt-1.5 text-sm text-slate-600">{request.description}</p>
                    <dl className="mt-3 space-y-1 text-sm">
                      <div className="flex gap-2">
                        <dt className="w-24 shrink-0 text-slate-400">Khách hàng</dt>
                        <dd className="text-slate-800 font-medium">
                          {request.customerName}{' '}
                          <a
                            href={`tel:${request.customerPhone}`}
                            className="text-brand hover:underline"
                          >
                            {request.customerPhone}
                          </a>
                        </dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-24 shrink-0 text-slate-400">Địa chỉ</dt>
                        <dd className="text-slate-800">{request.address}</dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-24 shrink-0 text-slate-400">Hẹn lúc</dt>
                        <dd className="text-slate-800">{request.scheduledTime}</dd>
                      </div>
                    </dl>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          {/* Matching requests for bidding */}
          {approved && (
            <Card
              title="Yêu cầu phù hợp & Tìm kiếm theo khu vực"
              description="Tìm kiếm và lọc các yêu cầu sửa chữa theo khu vực hoạt động để nhận đơn đúng địa bàn."
            >
              {quoteSuccess && (
                <div className="mb-4">
                  <Alert tone="success">{quoteSuccess}</Alert>
                </div>
              )}

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
                  {matching.map((req) => (
                    <div
                      key={req.id}
                      className="rounded-2xl border border-slate-200 bg-white p-4 shadow-xs transition-all duration-200 hover:border-brand/40 hover:shadow-card"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <Link to={`/tho/yeu-cau/${req.id}`} className="hover:text-brand transition-colors">
                              <h3 className="font-bold text-slate-900 text-sm sm:text-base hover:text-brand transition-colors">
                                {req.title}
                              </h3>
                            </Link>
                            <StatusChip status={req.status} />
                          </div>
                          <p className="mt-1 text-sm text-slate-600 line-clamp-2">{req.description}</p>
                          <div className="mt-2.5 flex flex-wrap gap-2 text-xs text-slate-500">
                            {req.categoryName && (
                              <span className="rounded-md bg-slate-100 px-2 py-0.5 font-medium text-slate-700">
                                📁 {req.categoryName}
                              </span>
                            )}
                            {req.areaName && <span>📍 {req.areaName}</span>}
                            <span>🏠 {req.addressLine}</span>
                            {req.budgetRef > 0 && (
                              <span className="font-semibold text-brand">💰 {formatCurrency(req.budgetRef)}</span>
                            )}
                            {req.biddingDeadline && (
                              <span>
                                ⏰ {new Date(req.biddingDeadline).toLocaleDateString('vi-VN')}
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Link to={`/tho/yeu-cau/${req.id}`}>
                            <Button variant="quiet">Xem chi tiết</Button>
                          </Link>
                          <Button
                            variant="secondary"
                            onClick={() => {
                              setQuoteFor(req);
                              setQuoteError(null);
                              setQuoteSuccess(null);
                            }}
                          >
                            Gửi báo giá
                          </Button>
                        </div>
                      </div>
                    </div>
                  ))}
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

          {/* Quotation submission modal */}
          {quoteFor && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs animate-fade-in">
              <div className="w-full max-w-lg rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl animate-slide-up">
                <h2 className="font-display text-lg font-bold text-slate-900">Gửi báo giá kỹ thuật</h2>
                <p className="mt-1 text-xs text-slate-500">
                  Đơn sửa chữa: <span className="font-semibold text-brand">{quoteFor.title}</span>
                </p>

                {quoteError && (
                  <div className="mt-3">
                    <Alert tone="error">{quoteError}</Alert>
                  </div>
                )}

                <div className="mt-4 space-y-4">
                  <TextArea
                    label="Giải pháp kỹ thuật đề xuất"
                    name="solution"
                    rows={3}
                    required
                    placeholder="Mô tả nguyên nhân phỏng đoán và cách bạn sẽ xử lý sự cố..."
                    value={quoteForm.solution}
                    onChange={(e) =>
                      setQuoteForm((f) => ({ ...f, solution: e.target.value }))
                    }
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
                      onChange={(e) =>
                        setQuoteForm((f) => ({ ...f, priceLaborVnd: e.target.value }))
                      }
                    />
                    <TextField
                      label="Giá linh kiện vật tư (VNĐ)"
                      name="priceMaterialsVnd"
                      type="number"
                      min={0}
                      required
                      placeholder="250000"
                      value={quoteForm.priceMaterialsVnd}
                      onChange={(e) =>
                        setQuoteForm((f) => ({ ...f, priceMaterialsVnd: e.target.value }))
                      }
                    />
                  </div>
                  {quoteForm.priceLaborVnd && quoteForm.priceMaterialsVnd && (
                    <div className="rounded-xl bg-blue-50 border border-blue-100 p-3 text-sm font-semibold text-slate-800 flex justify-between items-center">
                      <span>Tổng giá báo khách:</span>
                      <span className="text-base text-brand font-bold">
                        {formatCurrency(
                          Number(quoteForm.priceLaborVnd) + Number(quoteForm.priceMaterialsVnd)
                        )}
                      </span>
                    </div>
                  )}
                  <TextArea
                    label="Ghi chú (không bắt buộc)"
                    name="note"
                    rows={2}
                    placeholder="Chính sách bảo hành riêng, thời gian dự kiến có mặt..."
                    value={quoteForm.note}
                    onChange={(e) =>
                      setQuoteForm((f) => ({ ...f, note: e.target.value }))
                    }
                  />
                </div>

                <div className="mt-6 flex justify-end gap-2">
                  <Button variant="secondary" onClick={() => setQuoteFor(null)}>
                    Hủy
                  </Button>
                  <Button
                    onClick={handleQuoteSubmit}
                    loading={quoteSaving}
                    disabled={
                      !quoteForm.solution.trim() ||
                      !quoteForm.priceLaborVnd ||
                      !quoteForm.priceMaterialsVnd
                    }
                  >
                    {quoteSaving ? 'Đang gửi...' : 'Gửi báo giá tới khách'}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </DashboardLayout>
  );
}
