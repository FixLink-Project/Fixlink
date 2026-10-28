import { useCallback, useEffect, useMemo, useState } from 'react';
import Alert from '../components/Alert';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import { ApiError, api, fetchAreas } from '../lib/api';
import type { ServiceArea } from '../lib/types';

interface MonthlyStatistics {
  month: string;
  areaName: string;
  customerRegistrations: number;
  activeCustomers: number;
  technicianRegistrations: number;
  activeTechnicians: number;
  jobsCreated: number;
  jobsAccepted: number;
  cancelledJobs: number;
  quotesCreated: number;
  techniciansQuoting: number;
  customersReceivingQuotes: number;
  appointmentsCreated: number;
  customersBooking: number;
  techniciansBooked: number;
  progressUpdates: number;
  techniciansUpdatingProgress: number;
  completedJobs: number;
  acceptedJobsInCohort: number;
  completionRate: number;
  averageResponseMinutes: number;
  respondedJobs: number;
  completedJobValueVnd: number;
  averageCompletedOrderVnd: number;
  platformRevenueVnd?: number;
  platformFeeRatePercent?: number;
  requestedGmvVnd?: number;
  acceptedGmvVnd?: number;
  jobsWithQuotes: number;
  jobsWithAppointments: number;
  quoteConversionRate: number;
  appointmentConversionRate: number;
  completionConversionRate: number;
  categoryBreakdown: { categoryId: number; name: string; requests: number; percentage: number; availableTechnicians: number }[];
  areaBreakdown: { areaId: number; name: string; city: string; requests: number; assignedJobs?: number; completedJobs?: number; cancelledJobs?: number; acceptanceRate?: number; completionRate?: number; completedJobValueVnd?: number; availableTechnicians: number; gap: number }[];
  dailyTrend: { date: string; jobs: number }[];
  financialMetricNote: string;
  qualityMetricNote: string;
  averageRating: number;
  reviewCount: number;
  ratingBreakdown?: { rating: number; count: number }[];
}

const ADMIN_NAV = [
  { to: '/quan-tri', label: 'Người dùng' },
  { to: '/quan-tri/thong-ke', label: 'Thống kê' },
  { to: '/quan-tri/danh-muc', label: 'Danh mục dịch vụ' }
];

function monthKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function shiftMonth(month: string, amount: number): string {
  const [year, monthNumber] = month.split('-').map(Number);
  return monthKey(new Date(year, monthNumber - 1 + amount, 1));
}

function monthDate(month: string): Date {
  const [year, monthNumber] = month.split('-').map(Number);
  return new Date(year, monthNumber - 1, 1, 12);
}

function number(value: number | null | undefined): string {
  return (Number.isFinite(value) ? value! : 0).toLocaleString('vi-VN');
}

function currency(value: number | null | undefined): string {
  return `${number(value)} ₫`;
}

function MonthPicker({ month, maxMonth, onChange }: { month: string; maxMonth: string; onChange: (value: string) => void }) {
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear] = useState(Number(month.slice(0, 4)));
  const selectedMonth = Number(month.slice(5, 7));
  const maxYear = Number(maxMonth.slice(0, 4));
  const maxMonthNumber = Number(maxMonth.slice(5, 7));
  const monthNames = ['Thg 1', 'Thg 2', 'Thg 3', 'Thg 4', 'Thg 5', 'Thg 6', 'Thg 7', 'Thg 8', 'Thg 9', 'Thg 10', 'Thg 11', 'Thg 12'];
  const canAdvance = viewYear < maxYear;
  return (
    <div className="relative">
      <button type="button" aria-haspopup="dialog" aria-expanded={open} onClick={() => { setViewYear(Number(month.slice(0, 4))); setOpen((value) => !value); }}
        className="flex h-9 items-center gap-2 border-x border-slate-100 px-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">
        <span>{new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(monthDate(month))}</span>
        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" /><path strokeLinecap="round" d="M16 3v4M8 3v4M3 10h18" /></svg>
      </button>
      {open && <div role="dialog" aria-label="Chọn tháng" style={{ width: 'min(460px, calc(100vw - 2rem))', left: '50%', transform: 'translateX(-50%)' }} className="absolute top-full z-50 mt-3 max-w-[calc(100vw-2rem)] rounded-xl border border-slate-200 bg-white p-5 shadow-xl sm:p-6">
        <div className="mb-4 flex items-center justify-between">
          <button type="button" aria-label="Năm trước" onClick={() => setViewYear((year) => year - 1)} className="flex h-10 w-10 items-center justify-center rounded-lg text-xl text-slate-600 hover:bg-slate-100">‹</button>
          <span className="text-lg font-bold text-slate-800">{viewYear}</span>
          <button type="button" aria-label="Năm sau" disabled={!canAdvance} onClick={() => setViewYear((year) => year + 1)} className="flex h-10 w-10 items-center justify-center rounded-lg text-xl text-slate-600 hover:bg-slate-100 disabled:text-slate-300">›</button>
        </div>
        <div className="grid gap-3 sm:gap-4" style={{ gridTemplateColumns: 'repeat(4, minmax(0, 1fr))' }}>
          {monthNames.map((name, index) => {
            const monthNumber = index + 1;
            const disabled = viewYear > maxYear || (viewYear === maxYear && monthNumber > maxMonthNumber);
            const selected = viewYear === Number(month.slice(0, 4)) && monthNumber === selectedMonth;
            return <button key={name} type="button" disabled={disabled} aria-pressed={selected} onClick={() => { onChange(`${viewYear}-${String(monthNumber).padStart(2, '0')}`); setOpen(false); }}
              className={`h-14 rounded-lg text-lg font-semibold transition ${selected ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-700 hover:bg-blue-50 hover:text-blue-700'} disabled:cursor-not-allowed disabled:text-slate-300`}>{name}</button>;
          })}
        </div>
        <div className="mt-4 border-t border-slate-100 pt-3 text-right">
          <button type="button" onClick={() => { onChange(maxMonth); setOpen(false); }} className="rounded-lg px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50">Tháng này</button>
        </div>
      </div>}
    </div>
  );
}

function Delta({ current, previous }: { current: number; previous: number }) {
  if (previous === 0) {
    return <span className="text-xs font-medium text-slate-400">{current === 0 ? 'Không đổi' : 'Chưa có kỳ trước để so sánh'}</span>;
  }
  const change = Math.round(((current - previous) / previous) * 100);
  const positive = change >= 0;
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-bold ${positive ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
      <span aria-hidden="true">{positive ? '↑' : '↓'}</span>{Math.abs(change)}%
      <span className="font-medium text-slate-500">so với tháng trước</span>
    </span>
  );
}

function MetricCard({
  label, value, previous, detail, icon, color
}: {
  label: string;
  value: number;
  previous: number;
  detail: string;
  icon: string;
  color: string;
}) {
  return (
    <Card className="h-full">
      <div className="flex items-start justify-between gap-3">
        <div className={`flex h-11 w-11 items-center justify-center rounded-xl text-lg font-bold ${color}`} aria-hidden="true">{icon}</div>
        <Delta current={value} previous={previous} />
      </div>
      <p className="mt-5 text-sm font-semibold text-slate-500">{label}</p>
      <p className="mt-1 font-display text-3xl font-extrabold tracking-tight text-slate-900">{number(value)}</p>
      <p className="mt-1 text-xs text-slate-400">{detail}</p>
    </Card>
  );
}

function ActivityRow({
  label, value, previous, detail, color, max
}: {
  label: string;
  value: number;
  previous: number;
  detail: string;
  color: string;
  max: number;
}) {
  const width = max === 0 ? 0 : Math.max(value > 0 ? 3 : 0, Math.round((value / max) * 100));
  return (
    <div className="py-4 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-slate-800">{label}</p>
          <p className="mt-0.5 text-xs text-slate-500">{detail}</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="font-display text-xl font-bold tabular-nums text-slate-900">{number(value)}</span>
          <Delta current={value} previous={previous} />
        </div>
      </div>
      <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">
        <div className={`h-full rounded-full transition-all duration-500 ${color}`} style={{ width: `${width}%` }} />
      </div>
    </div>
  );
}

export default function AdminStatisticsPage() {
  const thisMonth = monthKey(new Date());
  const [month, setMonth] = useState(thisMonth);
  const [stats, setStats] = useState<MonthlyStatistics | null>(null);
  const [previousStats, setPreviousStats] = useState<MonthlyStatistics | null>(null);
  const [aovTrend, setAovTrend] = useState<{ month: string; value: number }[]>([]);
  const [areas, setAreas] = useState<ServiceArea[]>([]);
  const [areaId, setAreaId] = useState('');
  const [expandedSupplyArea, setExpandedSupplyArea] = useState<number | null>(null);
  const [supplyCandidates, setSupplyCandidates] = useState<Record<number, { technicianId: number; name: string; phone: string; specialization?: string; rating?: number; completedJobs?: number }[]>>({});
  const [supplyLoadingArea, setSupplyLoadingArea] = useState<number | null>(null);
  const [supplyErrorArea, setSupplyErrorArea] = useState<number | null>(null);
  const [showAllAreas, setShowAllAreas] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const trendMonths = Array.from({ length: 6 }, (_, index) => shiftMonth(month, index - 5));
      const areaQuery = areaId ? `&areaId=${encodeURIComponent(areaId)}` : '';
      const trendResults = await Promise.all(trendMonths.map((value) => api.get<MonthlyStatistics>(`/admin/statistics/monthly?month=${value}-01${areaQuery}`)));
      const selectedIndex = trendMonths.length - 1;
      setStats(trendResults[selectedIndex].data);
      setPreviousStats(trendResults[selectedIndex - 1].data);
      setAovTrend(trendResults.map((result, index) => ({ month: trendMonths[index], value: result.data.averageCompletedOrderVnd })));
      setError(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được số liệu thống kê. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [month, areaId]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { fetchAreas().then((response) => setAreas(response.data ?? [])).catch(() => setAreas([])); }, []);

  const monthLabel = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(monthDate(month));
  const previousMonthLabel = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' })
    .format(monthDate(shiftMonth(month, -1)));
  const monthIsCurrent = month === thisMonth;

  const activityRows = useMemo(() => {
    if (!stats || !previousStats) return [];
    return [
      { label: 'Yêu cầu sửa chữa mới', value: stats.jobsCreated, previous: previousStats.jobsCreated, detail: `${number(stats.activeCustomers)} khách đã tạo yêu cầu`, color: 'bg-sky-500' },
      { label: 'Job được thợ nhận', value: stats.jobsAccepted, previous: previousStats.jobsAccepted, detail: `${number(stats.activeTechnicians)} thợ có nhận job`, color: 'bg-indigo-500' },
      { label: 'Báo giá được gửi', value: stats.quotesCreated, previous: previousStats.quotesCreated, detail: `${number(stats.techniciansQuoting)} thợ gửi báo giá`, color: 'bg-violet-500' },
      { label: 'Lịch hẹn được tạo', value: stats.appointmentsCreated, previous: previousStats.appointmentsCreated, detail: `${number(stats.customersBooking)} khách đặt lịch · ${number(stats.techniciansBooked)} thợ có lịch`, color: 'bg-amber-500' },
      { label: 'Cập nhật tiến độ', value: stats.progressUpdates, previous: previousStats.progressUpdates, detail: `${number(stats.techniciansUpdatingProgress)} thợ có cập nhật`, color: 'bg-emerald-500' },
      { label: 'Yêu cầu bị hủy', value: stats.cancelledJobs, previous: previousStats.cancelledJobs, detail: 'Sự kiện chuyển trạng thái sang đã hủy', color: 'bg-rose-500' }
    ];
  }, [stats, previousStats]);
  const maxActivity = Math.max(1, ...activityRows.map((row) => row.value));
  const rankedAreas = useMemo(() => [...(stats?.areaBreakdown ?? [])]
    .sort((left, right) => right.requests - left.requests || right.gap - left.gap), [stats?.areaBreakdown]);
  const visibleAreas = showAllAreas ? rankedAreas : rankedAreas.filter((area) => area.requests > 0 || (area.completedJobs ?? 0) > 0 || area.availableTechnicians > 0);
  const missedRequests = Math.max(0, (stats?.jobsCreated ?? 0) - (stats?.acceptedJobsInCohort ?? 0));
  const missedRequestRate = stats?.jobsCreated ? Math.round(missedRequests * 100 / stats.jobsCreated) : 0;
  const hotArea = rankedAreas.find((area) => area.requests > 0 && ((area.acceptanceRate ?? (area.requests ? Math.round((area.assignedJobs ?? 0) * 100 / area.requests) : 0)) < 50));
  const hotAreaAcceptanceRate = hotArea?.acceptanceRate ?? (hotArea?.requests ? Math.round((hotArea.assignedJobs ?? 0) * 100 / hotArea.requests) : 0);

  const toggleSupplyCandidates = async (targetAreaId: number) => {
    if (expandedSupplyArea === targetAreaId) { setExpandedSupplyArea(null); return; }
    setExpandedSupplyArea(targetAreaId);
    if (supplyCandidates[targetAreaId]) return;
    setSupplyLoadingArea(targetAreaId);
    try {
      const response = await api.get<{ technicianId: number; name: string; phone: string; specialization?: string; rating?: number; completedJobs?: number }[]>(`/admin/statistics/area-technicians?areaId=${targetAreaId}`);
      setSupplyCandidates((current) => ({ ...current, [targetAreaId]: response.data ?? [] }));
    } catch {
      setSupplyErrorArea(targetAreaId);
    } finally {
      setSupplyLoadingArea(null);
    }
  };

  return (
    <DashboardLayout
      nav={ADMIN_NAV}
      wide
      title="Dashboard hoạt động"
      description="Tổng quan khách hàng, thợ và các thao tác phát sinh trên FixLink."
    >
      <section className="mb-6 rounded-2xl border border-slate-200 bg-white shadow-sm" aria-label="Chọn kỳ thống kê">
        <div className="flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
              <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
                <rect x="3" y="5" width="18" height="16" rx="2" /><path strokeLinecap="round" d="M16 3v4M8 3v4M3 10h18" />
              </svg>
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500">Kỳ báo cáo</p>
              <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1">
                <h2 className="font-display text-lg font-bold capitalize leading-tight text-slate-900">{monthLabel}</h2>
                <span className="text-xs text-slate-400">So với {previousMonthLabel}</span>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2 sm:justify-end">
            <label className="flex h-9 items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-600 shadow-sm">
              <span>Khu vực</span>
              <select aria-label="Lọc theo khu vực" value={areaId} onChange={(event) => setAreaId(event.target.value)} className="max-w-40 cursor-pointer bg-transparent font-semibold text-slate-800 outline-none">
                <option value="">Toàn nền tảng</option>
                {areas.map((area) => <option key={area.id} value={area.id}>{area.name}{area.city ? ` · ${area.city}` : ''}</option>)}
              </select>
            </label>
            <div className="inline-flex items-center rounded-xl border border-slate-200 bg-white p-1 shadow-sm">
              <button type="button" aria-label="Tháng trước" title="Tháng trước" onClick={() => setMonth((value) => shiftMonth(value, -1))}
                className="flex h-9 w-9 items-center justify-center rounded-lg text-lg text-slate-600 transition hover:bg-slate-100 hover:text-slate-900">‹</button>
              <MonthPicker month={month} maxMonth={thisMonth} onChange={setMonth} />
              <button type="button" aria-label="Tháng sau" title="Tháng sau" disabled={monthIsCurrent}
                onClick={() => setMonth((value) => shiftMonth(value, 1))}
                className="flex h-9 w-9 items-center justify-center rounded-lg text-lg text-slate-600 transition hover:bg-slate-100 hover:text-slate-900 disabled:cursor-not-allowed disabled:text-slate-300">›</button>
            </div>
            <button type="button" disabled={monthIsCurrent} onClick={() => setMonth(thisMonth)}
              className="h-11 rounded-xl border border-blue-200 bg-blue-50 px-3.5 text-sm font-semibold text-blue-700 transition hover:border-blue-300 hover:bg-blue-100 disabled:cursor-default disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400">
              Tháng này
            </button>
          </div>
        </div>
        <div className="flex min-h-12 items-center gap-2.5 border-t border-slate-100 bg-slate-50/70 px-4 py-2 text-sm leading-5 text-slate-500 sm:px-5">
          <span className={`h-2 w-2 shrink-0 rounded-full ${loading ? 'animate-pulse bg-amber-400' : 'bg-emerald-500'}`} aria-hidden="true" />
          {loading ? 'Đang cập nhật dữ liệu…' : areaId ? `Chỉ số job đang lọc theo ${stats?.areaName ?? 'khu vực đã chọn'}; đăng ký tài khoản tính toàn nền tảng` : 'Các chỉ số được lọc theo tháng đang chọn'}
        </div>
      </section>

      {error && <Alert tone="error" title="Không thể tải thống kê">{error}</Alert>}
      {loading && !stats ? (
        <div className="grid gap-5 sm:grid-cols-2 2xl:grid-cols-4" aria-label="Đang tải thống kê">
          {[1, 2, 3, 4].map((item) => <div key={item} className="h-48 animate-pulse rounded-2xl bg-slate-200" />)}
        </div>
      ) : stats && previousStats && (
        <div className="space-y-8">
          {(hotArea || missedRequests > 0) && <div role="alert" className="flex flex-col gap-4 rounded-2xl border border-rose-200 bg-gradient-to-r from-rose-50 to-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3"><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-xl font-bold text-rose-700" aria-hidden="true">!</span><div><p className="font-bold text-rose-900">Điểm nóng cung – cầu</p><p className="mt-1 text-sm leading-6 text-rose-800">{hotArea ? `${hotArea.name}: ${number(hotArea.requests)} yêu cầu, ${hotAreaAcceptanceRate}% đã có thợ nhận, hiện có ${number(hotArea.availableTechnicians)} thợ online.` : `Có ${number(missedRequests)} / ${number(stats.jobsCreated)} yêu cầu chưa được nhận (${missedRequestRate}%).`}</p><p className="mt-1 text-xs text-rose-700">Tỷ lệ lỡ đơn toàn kỳ: {number(missedRequests)} yêu cầu · {missedRequestRate}%</p></div></div>
            {hotArea && <button type="button" onClick={() => { void toggleSupplyCandidates(hotArea.areaId); document.getElementById('area-breakdown')?.scrollIntoView({ behavior: 'smooth', block: 'center' }); }} className="shrink-0 rounded-xl bg-rose-600 px-4 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-rose-700">Điều phối thợ tại {hotArea.name}</button>}
          </div>}
          <section aria-labelledby="overview-heading">
            <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
              <div>
                <h2 id="overview-heading" className="font-display text-xl font-bold text-slate-900">Chỉ số chính</h2>
                <p className="mt-1 text-sm text-slate-500">Đăng ký mới và tài khoản thực sự tham gia hoạt động.</p>
              </div>
              <span className="text-xs text-slate-400">Đơn vị: tài khoản duy nhất</span>
            </div>
            <div className="grid gap-5 sm:grid-cols-2 2xl:grid-cols-4">
              <MetricCard label="Khách đăng ký" value={stats.customerRegistrations} previous={previousStats.customerRegistrations} detail="Tài khoản tạo trong kỳ" icon="K" color="bg-sky-100 text-sky-700" />
              <MetricCard label="Khách hoạt động" value={stats.activeCustomers} previous={previousStats.activeCustomers} detail="Có tạo ít nhất một yêu cầu" icon="↗" color="bg-emerald-100 text-emerald-700" />
              <MetricCard label="Thợ đăng ký" value={stats.technicianRegistrations} previous={previousStats.technicianRegistrations} detail="Tài khoản thợ tạo trong kỳ" icon="T" color="bg-violet-100 text-violet-700" />
              <MetricCard label="Thợ hoạt động" value={stats.activeTechnicians} previous={previousStats.activeTechnicians} detail="Có nhận job trong kỳ" icon="⚒" color="bg-indigo-100 text-indigo-700" />
            </div>
          </section>

          <section aria-labelledby="financial-heading">
            <div className="mb-4">
              <h2 id="financial-heading" className="font-display text-xl font-bold text-slate-900">Giá trị dịch vụ</h2>
              <p className="mt-1 text-sm text-slate-500">Ước tính từ các job đã hoàn thành trong tháng.</p>
            </div>
            <div className="grid gap-5 sm:grid-cols-2 2xl:grid-cols-4">
              <Card className="h-full"><p className="text-sm font-semibold text-slate-500">GMV yêu cầu</p><p className="mt-2 font-display text-2xl font-extrabold text-slate-900">{currency(stats.requestedGmvVnd)}</p><p className="mt-2 text-xs leading-5 text-slate-400">Tổng giá đã thỏa thuận hoặc ngân sách dự kiến của yêu cầu tạo trong kỳ; không đồng nghĩa tiền đã thanh toán.</p></Card>
              <Card className="h-full"><p className="text-sm font-semibold text-slate-500">GMV job đã nhận</p><p className="mt-2 font-display text-2xl font-extrabold text-slate-900">{currency(stats.acceptedGmvVnd)}</p><p className="mt-2 text-xs leading-5 text-slate-400">Giá trị ước tính của các yêu cầu đã từng có thợ nhận trong kỳ.</p></Card>
              <Card className="h-full"><p className="text-sm font-semibold text-slate-500">Giá trị job hoàn thành</p><p className="mt-2 font-display text-2xl font-extrabold text-slate-900">{currency(stats.completedJobValueVnd)}</p><p className="mt-2 text-xs leading-5 text-slate-400">Tổng giá thỏa thuận của {number(stats.completedJobs)} job đã hoàn thành.</p></Card>
              <Card className="h-full"><p className="text-sm font-semibold text-slate-500">Giá trị trung bình / job</p><p className="mt-2 font-display text-2xl font-extrabold text-slate-900">{currency(stats.averageCompletedOrderVnd)}</p><p className="mt-2 text-xs leading-5 text-slate-400">Tính trên các job hoàn thành trong kỳ.</p></Card>
            </div>
            <div className="mt-5 flex flex-col gap-3 rounded-2xl border border-emerald-200 bg-gradient-to-r from-emerald-50/70 to-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between sm:px-5">
              <div>
                <p className="text-sm font-bold text-emerald-950 flex items-center gap-1.5">
                  <span>💼</span> Doanh thu phí nền tảng FixLink (Hoa hồng 10%)
                </p>
                <p className="mt-1 text-xs leading-5 text-emerald-800">
                  {stats.platformRevenueVnd != null && stats.platformRevenueVnd > 0
                    ? `Đã thu từ ${number(stats.completedJobs)} đơn hoàn thành trong kỳ (trích 10% hoa hồng trên ${currency(stats.completedJobValueVnd)}).`
                    : 'Tự động trích 10% hoa hồng nền tảng khi khách hàng nghiệm thu hoàn tất đơn.'}
                </p>
              </div>
              <span className="shrink-0 rounded-xl bg-emerald-600 px-4 py-2 text-base font-extrabold text-white shadow-xs">
                {currency(stats.platformRevenueVnd ?? Math.round((stats.completedJobValueVnd ?? 0) * 0.1))}
              </span>
            </div>
            <Card className="mt-5" title="Xu hướng giá trị trung bình / job" description="AOV của job hoàn thành trong 6 tháng gần nhất.">
              {aovTrend.some((point) => point.value > 0) ? (() => { const max = Math.max(1, ...aovTrend.map((point) => point.value)); const left = 42; const right = 588; const top = 20; const bottom = 190; const x = (index: number) => left + index * ((right - left) / Math.max(1, aovTrend.length - 1)); const y = (value: number) => bottom - (value / max) * (bottom - top); const points = aovTrend.map((point, index) => `${x(index)},${y(point.value)}`).join(' '); return <div className="pt-3"><svg viewBox="0 0 620 220" className="h-52 w-full" role="img" aria-label="Biểu đồ đường AOV sáu tháng" preserveAspectRatio="xMidYMid meet">{[0, 1, 2, 3].map((line) => { const lineY = top + line * ((bottom - top) / 3); return <g key={line}><line x1={left} x2={right} y1={lineY} y2={lineY} stroke="#e2e8f0" strokeDasharray="4 5"/><text x={left - 8} y={lineY + 4} textAnchor="end" fontSize="10" fill="#94a3b8">{number(Math.round(max * (3 - line) / 3 / 100000) * 100000)}</text></g>; })}<polyline points={points} fill="none" stroke="#7c3aed" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />{aovTrend.map((point, index) => <g key={point.month}><circle cx={x(index)} cy={y(point.value)} r="5" fill="white" stroke="#7c3aed" strokeWidth="3"><title>{new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(monthDate(point.month))}: {currency(point.value)}</title></circle></g>)}</svg><div className="grid grid-cols-3 gap-2 text-center text-xs text-slate-500 sm:grid-cols-6">{aovTrend.map((point) => <span key={point.month} className="whitespace-nowrap">{new Intl.DateTimeFormat('vi-VN', { month: 'short' }).format(monthDate(point.month))}<span className="block font-semibold tabular-nums text-slate-700">{number(point.value ? Math.round(point.value / 1000) : 0)}k ₫</span></span>)}</div></div>; })() : <p className="py-8 text-center text-sm text-slate-500">Chưa có job hoàn thành để tính AOV.</p>}
            </Card>
          </section>

          <div className="grid gap-6 2xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.8fr)]">
            <Card title="Hoạt động theo tính năng" description="Số lượt phát sinh trong tháng đã chọn; thanh tiến độ cho biết tỷ trọng giữa các mục.">
              {activityRows.length === 0 ? (
                <p className="py-8 text-center text-sm text-slate-500">Chưa có dữ liệu hoạt động cho kỳ này.</p>
              ) : (
                <div className="divide-y divide-slate-100">
                  {activityRows.map((row) => <ActivityRow key={row.label} {...row} max={maxActivity} />)}
                </div>
              )}
            </Card>

            <Card title="Hành trình job" description="Nhìn nhanh job từ lúc đăng đến các bước xử lý tiếp theo.">
              <div className="space-y-5">
                {[
                  { label: 'Được đăng', value: stats.jobsCreated, total: stats.jobsCreated, tone: 'bg-sky-500' },
                  { label: 'Được thợ nhận', value: stats.jobsAccepted, total: stats.jobsCreated, tone: 'bg-indigo-500' },
                  { label: 'Có lịch hẹn', value: stats.appointmentsCreated, total: stats.jobsCreated, tone: 'bg-amber-500' },
                  { label: 'Bị hủy', value: stats.cancelledJobs, total: stats.jobsCreated, tone: 'bg-rose-500' }
                ].map((step) => {
                  const percent = step.total === 0 ? 0 : Math.min(100, Math.round((step.value / step.total) * 100));
                  return (
                    <div key={step.label}>
                      <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                        <span className="font-medium text-slate-600">{step.label}</span>
                        <span className="font-bold tabular-nums text-slate-900">{number(step.value)} <span className="font-medium text-slate-400">({percent}%)</span></span>
                      </div>
                      <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
                        <div className={`h-full rounded-full transition-all duration-500 ${step.tone}`} style={{ width: `${percent}%` }} />
                      </div>
                    </div>
                  );
                })}
                <p className="border-t border-slate-100 pt-4 text-xs leading-5 text-slate-400">Tỷ lệ lấy số job đăng trong kỳ làm mốc. Một job có thể có nhiều lịch hẹn hoặc cập nhật, nên số lượt này không phải số job duy nhất.</p>
                <div className="rounded-xl bg-slate-50 p-4"><p className="text-xs font-semibold text-slate-500">Thời gian phản hồi đầu tiên</p><p className="mt-1 font-display text-2xl font-extrabold text-slate-900">{stats.averageResponseMinutes < 0 ? 'Chưa có dữ liệu' : stats.averageResponseMinutes < 60 ? `${number(stats.averageResponseMinutes)} phút` : `${Math.floor(stats.averageResponseMinutes / 60)} giờ ${stats.averageResponseMinutes % 60} phút`}</p><p className="mt-1 text-xs text-slate-500">Từ lúc tạo yêu cầu đến khi được thợ nhận hoặc nhận báo giá · {number(stats.respondedJobs)} job đã có phản hồi</p></div>
              </div>
            </Card>
          </div>

          <section className="grid gap-6 xl:grid-cols-2" aria-label="Xu hướng và danh mục dịch vụ">
            <Card title="Yêu cầu theo ngày" description="Số yêu cầu sửa chữa được tạo mỗi ngày trong tháng.">
              {stats.dailyTrend.length > 0 && stats.dailyTrend.some((point) => point.jobs > 0) ? (() => {
                const max = Math.max(1, ...stats.dailyTrend.map((point) => point.jobs));
                const points = stats.dailyTrend.map((point, index) => `${(index / Math.max(1, stats.dailyTrend.length - 1)) * 100},${94 - (point.jobs / max) * 82}`).join(' ');
                return <div className="pt-4"><svg viewBox="0 0 100 100" preserveAspectRatio="none" className="h-52 w-full overflow-visible" role="img" aria-label="Biểu đồ yêu cầu theo ngày"><polyline points={points} fill="none" stroke="#2563eb" strokeWidth="1.8" vectorEffect="non-scaling-stroke" strokeLinejoin="round" strokeLinecap="round" />{stats.dailyTrend.map((point, index) => <circle key={point.date} cx={(index / Math.max(1, stats.dailyTrend.length - 1)) * 100} cy={94 - (point.jobs / max) * 82} r="1.2" fill="#2563eb"><title>{new Date(`${point.date}T12:00:00`).toLocaleDateString('vi-VN')}: {number(point.jobs)} yêu cầu</title></circle>)}</svg><div className="mt-2 flex justify-between text-xs text-slate-400"><span>Ngày 1</span><span>Ngày {stats.dailyTrend.length}</span></div></div>;
              })() : <p className="py-10 text-center text-sm text-slate-500">Chưa có yêu cầu trong tháng này.</p>}
            </Card>
            <Card title="Nhu cầu theo danh mục" description="Tỷ trọng yêu cầu sửa chữa theo loại dịch vụ trong tháng.">
              {stats.categoryBreakdown.length ? <div className="space-y-5">{stats.categoryBreakdown.map((category) => <div key={category.categoryId}><div className="mb-2 flex flex-wrap items-center justify-between gap-2 text-sm"><span className="font-semibold text-slate-700">{category.name}</span><span className="tabular-nums text-slate-500">{number(category.requests)} · {category.percentage}%</span></div><div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-blue-500" style={{ width: `${category.percentage}%` }} /></div><div className="mt-1.5 flex items-center justify-between text-xs"><span className="text-slate-500">{number(category.availableTechnicians)} thợ đã xác minh đang online</span>{category.requests > 0 && category.availableTechnicians === 0 && <span className="font-semibold text-rose-600">Cần bổ sung thợ</span>}</div></div>)}</div> : <p className="py-10 text-center text-sm text-slate-500">Chưa có dữ liệu danh mục trong tháng này.</p>}
            </Card>
          </section>

          <Card id="area-breakdown" title="Báo cáo theo quận / huyện" description="So sánh nhu cầu, khả năng tiếp nhận, kết quả job và giá trị dịch vụ trong kỳ đã chọn." actions={<button type="button" onClick={() => setShowAllAreas((value) => !value)} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50">{showAllAreas ? 'Chỉ khu vực có dữ liệu' : `Hiển thị tất cả (${rankedAreas.length})`}</button>}>
            {visibleAreas.length ? <div className="overflow-x-auto"><table className="w-full min-w-[920px] border-separate border-spacing-0 text-left text-sm"><thead><tr className="text-xs uppercase tracking-wide text-slate-500"><th className="border-b border-slate-200 px-3 py-3 font-semibold">Khu vực</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Yêu cầu</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Đã nhận</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Tỷ lệ nhận</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Hoàn thành</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Đã hủy</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Thợ online</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Giá trị hoàn thành</th><th className="border-b border-slate-200 px-3 py-3 text-right font-semibold">Thao tác</th></tr></thead><tbody>{visibleAreas.map((area) => { const acceptanceRate = area.acceptanceRate ?? (area.requests ? Math.round((area.assignedJobs ?? 0) * 100 / area.requests) : 0); const completionRate = area.completionRate ?? ((area.assignedJobs ?? 0) ? Math.round((area.completedJobs ?? 0) * 100 / (area.assignedJobs ?? 0)) : 0); const candidates = supplyCandidates[area.areaId] ?? []; return <><tr key={area.areaId} className="transition hover:bg-slate-50"><td className="border-b border-slate-100 px-3 py-4"><p className="font-bold text-slate-800">{area.name}</p><p className="mt-0.5 text-xs text-slate-500">{area.city}</p>{area.requests > 0 && area.availableTechnicians === 0 && <span className="mt-1 inline-flex rounded-full bg-rose-50 px-2 py-0.5 text-[11px] font-semibold text-rose-700">Thiếu thợ online</span>}</td><td className="border-b border-slate-100 px-3 py-4 text-right font-semibold tabular-nums">{number(area.requests)}</td><td className="border-b border-slate-100 px-3 py-4 text-right tabular-nums">{number(area.assignedJobs)}</td><td className="border-b border-slate-100 px-3 py-4 text-right tabular-nums"><span className={acceptanceRate < 50 && area.requests > 0 ? 'font-bold text-rose-600' : 'font-medium text-slate-700'}>{acceptanceRate}%</span></td><td className="border-b border-slate-100 px-3 py-4 text-right tabular-nums">{number(area.completedJobs)} <span className="text-xs text-slate-400">({completionRate}%)</span></td><td className="border-b border-slate-100 px-3 py-4 text-right tabular-nums">{number(area.cancelledJobs)}</td><td className="border-b border-slate-100 px-3 py-4 text-right tabular-nums">{number(area.availableTechnicians)}</td><td className="border-b border-slate-100 px-3 py-4 text-right font-semibold tabular-nums">{currency(area.completedJobValueVnd)}</td><td className="border-b border-slate-100 px-3 py-4 text-right"><button type="button" onClick={() => void toggleSupplyCandidates(area.areaId)} className="rounded-lg border border-blue-200 px-3 py-2 text-xs font-semibold text-blue-700 hover:bg-blue-50">{expandedSupplyArea === area.areaId ? 'Đóng' : 'Tìm thợ'}</button></td></tr>{expandedSupplyArea === area.areaId && <tr key={`${area.areaId}-candidates`}><td colSpan={9} className="border-b border-slate-200 bg-slate-50 px-5 py-4"><p className="mb-3 text-sm font-bold text-slate-800">Thợ đã xác minh, đang hoạt động tài khoản nhưng tạm offline tại {area.name}</p>{supplyLoadingArea === area.areaId ? <p className="text-sm text-slate-500">Đang tải danh sách…</p> : supplyErrorArea === area.areaId ? <p className="text-sm text-rose-600">Không tải được danh sách thợ. Hãy kiểm tra backend rồi thử lại.</p> : candidates.length ? <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{candidates.map((tech) => <div key={tech.technicianId} className="rounded-xl border border-slate-200 bg-white p-4"><p className="font-semibold text-slate-800">{tech.name}</p><p className="mt-1 text-xs text-slate-500">{tech.specialization || 'Chưa cập nhật chuyên môn'} · {number(tech.completedJobs)} job hoàn thành · ★ {Number(tech.rating ?? 0).toFixed(1)}</p><a className="mt-3 inline-flex rounded-lg bg-blue-600 px-3 py-2 text-xs font-bold text-white hover:bg-blue-700" href={`tel:${tech.phone}`}>Liên hệ {tech.phone}</a></div>)}</div> : <p className="text-sm text-slate-500">Chưa có thợ offline đã xác minh ở khu vực này. Cần tuyển thêm hoặc mở rộng vùng phục vụ lân cận.</p>}</td></tr>}</>; })}</tbody></table><p className="mt-3 text-xs text-slate-400">Tỷ lệ hoàn thành = job hoàn thành / job đã được nhận. Giá trị dựa trên giá thỏa thuận, không phải doanh thu đã thu.</p></div> : <p className="py-8 text-center text-sm text-slate-500">Không có khu vực phát sinh dữ liệu trong kỳ.</p>}
          </Card>

          <section aria-labelledby="funnel-heading">
            <div className="mb-4"><h2 id="funnel-heading" className="font-display text-xl font-bold text-slate-900">Hiệu suất chuyển đổi</h2><p className="mt-1 text-sm text-slate-500">Theo dõi job từ lúc tạo yêu cầu đến khi hoàn thành.</p></div>
            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
              {[
                { label: 'Yêu cầu có báo giá', value: stats.jobsWithQuotes, rate: stats.quoteConversionRate, base: stats.jobsCreated },
                { label: 'Báo giá thành lịch hẹn', value: stats.jobsWithAppointments, rate: stats.appointmentConversionRate, base: stats.jobsWithQuotes },
                { label: 'Job được thợ nhận', value: stats.acceptedJobsInCohort, rate: stats.jobsCreated ? Math.round(stats.acceptedJobsInCohort * 100 / stats.jobsCreated) : 0, base: stats.jobsCreated },
                { label: 'Job hoàn thành', value: stats.completedJobs, rate: stats.completionConversionRate, base: stats.jobsWithAppointments }
              ].map((step) => <Card key={step.label} className="h-full"><p className="text-sm font-semibold text-slate-500">{step.label}</p><p className="mt-2 font-display text-2xl font-extrabold text-slate-900">{number(step.value)}</p><div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-indigo-500" style={{ width: `${Math.min(100, step.rate)}%` }} /></div><p className="mt-2 text-xs text-slate-500">{step.rate}% · {number(step.value)} / {number(step.base)}</p></Card>)}
            </div>
          </section>

          <section aria-labelledby="quality-heading">
            <div className="mb-4"><h2 id="quality-heading" className="font-display text-xl font-bold text-slate-900">Chất lượng dịch vụ</h2><p className="mt-1 text-sm text-slate-500">Các chỉ số cần được lưu trong quy trình đánh giá và hỗ trợ sau sửa chữa.</p></div>
            <div className="grid gap-5 md:grid-cols-2">
              <Card><p className="text-sm font-semibold text-slate-700">Điểm hài lòng khách hàng</p>{stats.reviewCount ? <><p className="mt-2 font-display text-3xl font-extrabold text-amber-500">★ {stats.averageRating.toFixed(2)} / 5</p><p className="mt-1 text-sm text-slate-500">Từ {number(stats.reviewCount)} đánh giá trong tháng.</p><div className="mt-4 space-y-2">{(stats.ratingBreakdown ?? []).map((item) => <div key={item.rating} className="flex items-center gap-3 text-xs"><span className="w-10 shrink-0 font-semibold text-slate-600">{item.rating} ★</span><div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-amber-400" style={{ width: `${stats.reviewCount ? Math.round(item.count * 100 / stats.reviewCount) : 0}%` }} /></div><span className="w-8 text-right tabular-nums text-slate-500">{number(item.count)}</span></div>)}</div></> : <><p className="mt-2 text-sm leading-6 text-slate-500">Chưa có đánh giá trong tháng được chọn.</p><div className="mt-4 space-y-2">{[5, 4, 3, 2, 1].map((rating) => <div key={rating} className="flex items-center gap-3 text-xs"><span className="w-10 font-semibold text-slate-400">{rating} ★</span><div className="h-2 flex-1 rounded-full bg-slate-100" /><span className="w-8 text-right text-slate-400">0</span></div>)}</div></>}</Card>
              <Card><p className="text-sm font-semibold text-slate-700">Khiếu nại và hoàn tiền</p><p className="mt-2 text-sm leading-6 text-slate-500">Chưa có dữ liệu khiếu nại hoặc hoàn tiền để tính tỷ lệ sự cố.</p></Card>
            </div>
          </section>

          <section aria-labelledby="detail-heading">
            <div className="mb-4">
              <h2 id="detail-heading" className="font-display text-xl font-bold text-slate-900">Chi tiết mức độ tham gia</h2>
              <p className="mt-1 text-sm text-slate-500">Số lượt thao tác đi kèm số khách hoặc thợ duy nhất tham gia.</p>
            </div>
            <div className="grid gap-5 sm:grid-cols-2 2xl:grid-cols-4">
              <MetricCard label="Báo giá đã gửi" value={stats.quotesCreated} previous={previousStats.quotesCreated} detail={`${number(stats.techniciansQuoting)} thợ gửi báo giá`} icon="$" color="bg-purple-100 text-purple-700" />
              <MetricCard label="Khách nhận báo giá" value={stats.customersReceivingQuotes} previous={previousStats.customersReceivingQuotes} detail="Khách có ít nhất một báo giá" icon="✓" color="bg-blue-100 text-blue-700" />
              <MetricCard label="Lịch hẹn tạo" value={stats.appointmentsCreated} previous={previousStats.appointmentsCreated} detail={`${number(stats.customersBooking)} khách · ${number(stats.techniciansBooked)} thợ`} icon="▦" color="bg-amber-100 text-amber-700" />
              <MetricCard label="Cập nhật tiến độ" value={stats.progressUpdates} previous={previousStats.progressUpdates} detail={`${number(stats.techniciansUpdatingProgress)} thợ có cập nhật`} icon="↻" color="bg-teal-100 text-teal-700" />
            </div>
          </section>

          <p className="text-xs leading-5 text-slate-400">Giá trị dịch vụ là ước tính từ giá đã thỏa thuận, không phải doanh thu đã thu. Các chỉ số hoạt động tính theo dữ liệu hệ thống ghi nhận trong kỳ; tỷ lệ hoàn thành tính trên job tạo trong tháng đã được thợ nhận.</p>
        </div>
      )}
    </DashboardLayout>
  );
}


