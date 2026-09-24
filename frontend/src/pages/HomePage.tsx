import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchCategories } from '../lib/api';
import type { ServiceCategory } from '../lib/types';

// Danh sách các thiết bị & sự cố phổ biến
const POPULAR_SEARCHES = [
  'Điều hòa không lạnh',
  'Tủ lạnh kêu to / mất đông',
  'Smart TV mất nguồn',
  'Máy giặt không vắt',
  'Bếp từ báo lỗi E1-E9',
  'Sửa bo mạch Inverter'
];

// Dịch vụ tiêu biểu với icon SVG chuẩn kỹ thuật
const FEATURED_SERVICES = [
  {
    code: 'DIEN_LANH',
    name: 'Sửa Chữa Điện Lạnh',
    devices: 'Điều hòa, Tủ lạnh, Máy giặt, Tủ đông',
    commonIssues: 'Không mát, hết gas, kêu to, chảy nước, hỏng block',
    price: 'Từ 150.000đ',
    warranty: 'Bảo hành 90 ngày',
    badge: 'Phổ biến nhất',
    icon: (
      <svg className="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 3v18m0-18l3 3m-3-3l-3 3m0 12l3 3m-3-3l-3-3m15-6H3m18 0l-3-3m3 3l-3 3M6 9l-3 3m3 3l-3-3" />
      </svg>
    )
  },
  {
    code: 'DIEN_TU',
    name: 'Sửa Điện Tử & Nghe Nhìn',
    devices: 'Smart TV, Amply, Dàn âm thanh, Màn hình',
    commonIssues: 'Mất nguồn, chớp màn hình, mất tiếng, chập cháy bo',
    price: 'Từ 200.000đ',
    warranty: 'Bảo hành 90 ngày',
    badge: 'Kỹ sư chuyên sâu',
    icon: (
      <svg className="w-6 h-6 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
      </svg>
    )
  },
  {
    code: 'DO_GIA_DUNG',
    name: 'Thiết Bị Bếp & Gia Dụng',
    devices: 'Bếp từ đôi, Lò vi sóng, Nồi cao tần, Robot hút bụi',
    commonIssues: 'Không nhận nồi, chập mâm từ, hỏng cảm ứng, báo lỗi mạch',
    price: 'Từ 150.000đ',
    warranty: 'Bảo hành 60 ngày',
    badge: 'Linh kiện zin',
    icon: (
      <svg className="w-6 h-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
    )
  },
  {
    code: 'MAY_TINH',
    name: 'Máy Tính & Thiết Bị Số',
    devices: 'Laptop, PC văn phòng, Bộ nguồn, Màn hình máy tính',
    commonIssues: 'Không lên nguồn, chập mạch, thay linh kiện, nâng cấp',
    price: 'Từ 180.000đ',
    warranty: 'Bảo hành 90 ngày',
    badge: 'Lấy ngay trong ngày',
    icon: (
      <svg className="w-6 h-6 text-sky-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 18h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    )
  },
  {
    code: 'KHOA_CUA',
    name: 'Khóa Điện Tử & Cửa Cuốn',
    devices: 'Khóa cửa vân tay thông minh, Remote & motor cửa cuốn',
    commonIssues: 'Kẹt khóa, không nhận vân tay/thẻ, liệt điều khiển 24/7',
    price: 'Từ 150.000đ',
    warranty: 'Bảo hành 60 ngày',
    badge: 'Cứu hộ khẩn cấp',
    icon: (
      <svg className="w-6 h-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
      </svg>
    )
  },
  {
    code: 'DIEN_NUOC',
    name: 'Sửa Chữa Điện Dân Dụng',
    devices: 'Chập điện âm tường, Máy bơm nước, Bình nóng lạnh',
    commonIssues: 'Nhảy Aptomat, rò rỉ điện, mất nước, hỏng rơ-le nhiệt',
    price: 'Từ 150.000đ',
    warranty: 'Bảo hành 30 ngày',
    badge: 'An toàn tuyệt đối',
    icon: (
      <svg className="w-6 h-6 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
      </svg>
    )
  }
];

// Bảng giá tham khảo minh bạch
const PRICE_BENCHMARKS = [
  { service: 'Kiểm tra & chẩn đoán lỗi tại nhà', price: '50.000đ – 100.000đ', note: 'Miễn phí nếu đồng ý sửa chữa' },
  { service: 'Vệ sinh & bảo dưỡng điều hòa (1.0 – 2.5 HP)', price: '150.000đ – 250.000đ', note: 'Bao gồm kiểm tra gas miễn phí' },
  { service: 'Nạp gas bổ sung điều hòa (Gas R32 / R410A)', price: '250.000đ – 450.000đ', note: 'Áp suất chuẩn hãng' },
  { service: 'Sửa bo mạch điều hòa / tủ lạnh Inverter', price: '450.000đ – 750.000đ', note: 'Bảo hành mạch 3 tháng' },
  { service: 'Thay tụ quạt / sò công suất bếp từ', price: '280.000đ – 550.000đ', note: 'Linh kiện chính hãng' },
  { service: 'Sửa nguồn Smart TV (Sony, Samsung, LG)', price: '400.000đ – 850.000đ', note: 'Kiểm tra linh kiện tại chỗ' }
];

// Thợ tiêu biểu đã xác minh CCCD
const VERIFIED_TECHNICIANS = [
  {
    name: 'Trần Văn Bình',
    specialty: 'Chuyên gia Điện Lạnh Inverter',
    experience: '8 năm kinh nghiệm',
    rating: '4.95',
    jobs: '284',
    areas: 'Q.1, Q.3, Bình Thạnh, Phú Nhuận',
    verified: true
  },
  {
    name: 'Nguyễn Quốc Cường',
    specialty: 'Kỹ sư Bo mạch Điện tử & Smart TV',
    experience: '10 năm kinh nghiệm',
    rating: '4.98',
    jobs: '342',
    areas: 'TP. Thủ Đức, Q.7, Tân Bình',
    verified: true
  },
  {
    name: 'Lê Minh Tuấn',
    specialty: 'Sửa Bếp Từ & Thiết Bị Gia Dụng',
    experience: '6 năm kinh nghiệm',
    rating: '4.91',
    jobs: '196',
    areas: 'Gò Vấp, Tân Bình, Q.12',
    verified: true
  }
];

export default function HomePage() {
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');

  useEffect(() => {
    let alive = true;
    fetchCategories()
      .then((res) => {
        if (!alive) return;
        setCategories(res.data.filter((c) => c.isActive));
        setStatus('ready');
      })
      .catch(() => alive && setStatus('error'));
    return () => {
      alive = false;
    };
  }, []);

  return (
    <div className="min-h-screen bg-surface text-ink">
      {/* ===== TOP ANNOUNCEMENT BAR ===== */}
      <div className="bg-gradient-to-r from-blue-700 via-brand-strong to-indigo-800 text-white text-xs sm:text-sm font-medium py-2 px-4 text-center">
        <span className="inline-flex items-center gap-2">
          <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Dịch vụ gọi thợ sửa chữa đồ điện tử & điện lạnh khẩn cấp tận nhà — Có mặt sau 30 phút</span>
          <span className="hidden md:inline font-bold text-amber-300">| Hotline: 1900 8899</span>
        </span>
      </div>

      {/* ===== MAIN HEADER ===== */}
      <header className="sticky top-0 z-40 border-b border-line bg-white/95 backdrop-blur-md shadow-xs">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3.5 sm:px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-brand to-brand-strong text-white shadow-md">
              <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
            <div>
              <span className="text-gradient text-2xl font-black tracking-tight">FixLink</span>
              <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400 -mt-1">
                Điện Tử & Gia Dụng
              </p>
            </div>
          </Link>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-6 text-sm font-semibold text-slate-600">
            <a href="#danh-muc" className="transition-colors hover:text-brand">Dịch vụ sửa chữa</a>
            <a href="#bang-gia" className="transition-colors hover:text-brand">Bảng giá niêm yết</a>
            <a href="#quy-trinh" className="transition-colors hover:text-brand">Quy trình 4 bước</a>
            <a href="#tho-uy-tin" className="transition-colors hover:text-brand">Đội ngũ thợ eKYC</a>
          </nav>

          {/* Right Action Buttons */}
          <div className="flex items-center gap-3">
            <Link
              to="/login"
              className="text-sm font-semibold text-slate-700 hover:text-brand transition-colors px-3 py-2"
            >
              Đăng nhập
            </Link>
            <Link
              to="/dang-yeu-cau"
              className="inline-flex items-center gap-2 rounded-xl bg-brand hover:bg-brand-strong text-white px-4 py-2 text-sm font-semibold shadow-sm hover:shadow-glow transition-all"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              <span>Đặt thợ ngay</span>
            </Link>
          </div>
        </div>
      </header>

      {/* ===== HERO SECTION ===== */}
      <section className="relative overflow-hidden bg-gradient-to-b from-blue-50/70 via-slate-50/50 to-surface pt-10 pb-16 sm:pt-14 sm:pb-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="text-center max-w-3xl mx-auto">
            {/* Top Tag */}
            <div className="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50/90 px-3.5 py-1 text-xs font-semibold text-brand mb-5 shadow-xs">
              <span className="flex h-2 w-2 rounded-full bg-brand" />
              <span>Nền Tảng Gọi Thợ Sửa Chữa Đồ Điện Tử Uy Tín #1</span>
            </div>

            {/* Headline */}
            <h1 className="font-display text-3xl sm:text-5xl lg:text-[54px] font-extrabold tracking-tight text-slate-900 leading-[1.15]">
              Sửa chữa đồ điện tử tại nhà,{' '}
              <span className="text-gradient">báo giá chuẩn trước khi làm</span>
            </h1>

            {/* Subheading */}
            <p className="mt-4 text-base sm:text-lg text-slate-600 max-w-2xl mx-auto leading-relaxed">
              Kết nối kỹ thuật viên điện tử, điện lạnh gần bạn nhất. Cam kết thợ chính chủ đã đối chiếu CCCD, 
              tiền cọc giữ qua tài khoản trung gian Escrow, bảo hành điện tử chính hãng 30–90 ngày.
            </p>

            {/* Search Box */}
            <div className="mt-8 max-w-2xl mx-auto">
              <div className="flex flex-col sm:flex-row items-center gap-2 p-2 bg-white rounded-2xl border border-slate-200 shadow-md">
                <div className="flex items-center gap-2.5 flex-1 w-full px-3">
                  <svg className="w-5 h-5 text-slate-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <input
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder="Bạn cần sửa thiết bị gì? (ví dụ: Điều hòa không lạnh, Tủ lạnh kêu to...)"
                    className="w-full text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none bg-transparent"
                  />
                </div>
                <Link
                  to={searchTerm.trim() ? `/dang-yeu-cau?search=${encodeURIComponent(searchTerm)}` : '/dang-yeu-cau'}
                  className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-xl bg-brand hover:bg-brand-strong text-white px-6 py-3 text-sm font-semibold shadow-sm transition-all whitespace-nowrap"
                >
                  <span>Tìm thợ ngay</span>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                  </svg>
                </Link>
              </div>

              {/* Popular quick tags */}
              <div className="mt-3.5 flex flex-wrap items-center justify-center gap-1.5 text-xs text-slate-500">
                <span className="font-semibold text-slate-600">Sự cố phổ biến:</span>
                {categories.length > 0
                  ? categories.slice(0, 6).map((cat) => (
                      <button
                        key={cat.id}
                        type="button"
                        onClick={() => setSearchTerm(cat.name)}
                        className="rounded-lg bg-white border border-slate-200/80 px-2.5 py-1 text-slate-700 hover:border-brand hover:text-brand transition-colors shadow-xs"
                      >
                        {cat.name}
                      </button>
                    ))
                  : POPULAR_SEARCHES.map((tag) => (
                      <button
                        key={tag}
                        type="button"
                        onClick={() => setSearchTerm(tag)}
                        className="rounded-lg bg-white border border-slate-200/80 px-2.5 py-1 text-slate-700 hover:border-brand hover:text-brand transition-colors shadow-xs"
                      >
                        {tag}
                      </button>
                    ))}
              </div>
            </div>

            {/* Trust Marks Row */}
            <div className="mt-10 grid grid-cols-2 md:grid-cols-4 gap-4 text-left">
              {[
                { icon: '🛡️', title: 'Thợ eKYC chính chủ', desc: '100% đối chiếu CCCD & tay nghề' },
                { icon: '💳', title: 'Ký quỹ Escrow', desc: 'Hài lòng mới giải ngân tiền công' },
                { icon: '📄', title: 'Bảo hành điện tử', desc: 'Cam kết 30 – 90 ngày trên app' },
                { icon: '⚡', title: 'Có mặt sau 30 phút', desc: 'Thợ xung quanh khu vực bạn ở' }
              ].map((item, idx) => (
                <div key={idx} className="flex items-start gap-3 p-3.5 rounded-xl bg-white border border-slate-200/80 shadow-xs">
                  <span className="text-2xl shrink-0">{item.icon}</span>
                  <div>
                    <p className="text-xs font-bold text-slate-900">{item.title}</p>
                    <p className="text-[11px] text-slate-500 mt-0.5">{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ===== POPULAR DEVICE CATEGORIES ===== */}
      <section id="danh-muc" className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-brand">Danh mục thiết bị</span>
            <h2 className="font-display text-2xl sm:text-3xl font-bold text-slate-900 mt-1">
              Các thiết bị điện tử & điện lạnh nhận sửa chữa
            </h2>
            <p className="text-sm text-slate-600 mt-1.5 max-w-xl">
              Đội ngũ thợ lành nghề, được trang bị đầy đủ máy đo sóng, đồng hồ đo dòng và linh kiện thay thế chính hãng.
            </p>
          </div>
          <div className="flex items-center gap-3">
            {status === 'loading' ? (
              <span className="text-xs text-slate-400">Đang kiểm tra danh mục...</span>
            ) : status === 'ready' && categories.length > 0 ? (
              <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-brand border border-blue-200">
                <span className="h-1.5 w-1.5 rounded-full bg-brand animate-pulse" />
                {categories.length} thiết bị hỗ trợ
              </span>
            ) : null}
            <Link
              to="/dang-yeu-cau"
              className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand hover:text-brand-strong"
            >
              <span>Tất cả thiết bị</span>
              <span>→</span>
            </Link>
          </div>
        </div>

        {/* Featured Service Cards Grid */}
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURED_SERVICES.map((srv) => (
            <div
              key={srv.code}
              className="relative flex flex-col justify-between rounded-2xl border border-slate-200 bg-white p-6 shadow-sm hover:shadow-hover hover:border-brand/40 transition-all duration-300"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 border border-blue-100">
                    {srv.icon}
                  </div>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-semibold text-slate-600">
                    {srv.badge}
                  </span>
                </div>

                <h3 className="font-display text-lg font-bold text-slate-900">{srv.name}</h3>
                <p className="text-xs font-semibold text-brand mt-1">{srv.devices}</p>
                <p className="text-xs text-slate-500 mt-2 leading-relaxed">
                  <span className="font-medium text-slate-700">Lỗi thường gặp:</span> {srv.commonIssues}
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                <div>
                  <span className="text-[11px] text-slate-400 block">Giá tham khảo</span>
                  <span className="text-sm font-bold text-slate-900">{srv.price}</span>
                </div>
                <Link
                  to={`/dang-yeu-cau?danh-muc=${srv.code}`}
                  className="inline-flex items-center gap-1 rounded-xl bg-slate-100 hover:bg-brand hover:text-white px-3.5 py-1.5 text-xs font-semibold text-slate-700 transition-colors"
                >
                  <span>Đặt thợ</span>
                  <span>→</span>
                </Link>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ===== TRANSPARENT PRICING BENCHMARKS ===== */}
      <section id="bang-gia" className="bg-slate-50 border-y border-line py-16 sm:py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <span className="text-xs font-bold uppercase tracking-wider text-brand">Minh bạch chi phí</span>
            <h2 className="font-display text-2xl sm:text-3xl font-bold text-slate-900 mt-1">
              Bảng giá tham khảo dịch vụ sửa chữa
            </h2>
            <p className="text-sm text-slate-600 mt-2">
              Cam kết báo giá chính xác sau khi thợ khảo sát tận nơi. Tuyệt đối không phát sinh chi phí vô lý.
            </p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50/80 text-xs font-bold uppercase text-slate-500 border-b border-line">
                  <tr>
                    <th className="px-6 py-4">Hạng mục sửa chữa & kiểm tra</th>
                    <th className="px-6 py-4">Mức giá tham khảo</th>
                    <th className="px-6 py-4">Chính sách & Ghi chú</th>
                    <th className="px-6 py-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {PRICE_BENCHMARKS.map((item, idx) => (
                    <tr key={idx} className="hover:bg-blue-50/30 transition-colors">
                      <td className="px-6 py-4 font-semibold text-slate-900">{item.service}</td>
                      <td className="px-6 py-4 font-bold text-brand">{item.price}</td>
                      <td className="px-6 py-4 text-xs text-slate-500">{item.note}</td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          to="/dang-yeu-cau"
                          className="inline-flex rounded-lg bg-blue-50 px-3 py-1.5 text-xs font-semibold text-brand hover:bg-brand hover:text-white transition-colors"
                        >
                          Đặt lịch
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      {/* ===== 4 STEPS WORKFLOW ===== */}
      <section id="quy-trinh" className="mx-auto max-w-6xl px-4 py-16 sm:py-24 sm:px-6">
        <div className="text-center max-w-2xl mx-auto mb-14">
          <span className="text-xs font-bold uppercase tracking-wider text-brand">Quy trình đơn giản</span>
          <h2 className="font-display text-2xl sm:text-3xl font-bold text-slate-900 mt-1">
            4 bước gọi thợ chuẩn an toàn tại FixLink
          </h2>
          <p className="text-sm text-slate-600 mt-2">
            Mọi thao tác đều được bảo vệ bởi máy trạng thái minh bạch và cơ chế ký quỹ Escrow.
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-4">
          {[
            {
              step: '01',
              title: 'Mô tả sự cố & Đăng yêu cầu',
              body: 'Chụp ảnh thiết bị, chọn địa chỉ và thời gian rảnh. Chỉ mất khoảng 2 phút trên điện thoại.'
            },
            {
              step: '02',
              title: 'So sánh báo giá từ các thợ',
              body: 'Các thợ gần bạn sẽ gửi phương án xử lý, giá công, linh kiện và cam kết bảo hành.'
            },
            {
              step: '03',
              title: 'Đặt cọc Escrow & Khảo sát',
              body: 'Khách cọc 30% vào hệ thống giữ tiền. Thợ đến tận nhà khảo sát và xác nhận chi phí minh bạch.'
            },
            {
              step: '04',
              title: 'Nghiệm thu & Nhận bảo hành',
              body: 'Thiết bị hoạt động ưng ý mới thanh toán phần còn lại. Hệ thống xuất ngay phiếu bảo hành điện tử.'
            }
          ].map((item) => (
            <div
              key={item.step}
              className="relative rounded-2xl border border-slate-200 bg-white p-6 shadow-sm hover:shadow-card transition-all"
            >
              <span className="font-display text-4xl font-black text-blue-100">{item.step}</span>
              <h3 className="font-display text-base font-bold text-slate-900 mt-3">{item.title}</h3>
              <p className="text-xs text-slate-500 mt-2 leading-relaxed">{item.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ===== VERIFIED TECHNICIANS SHOWCASE ===== */}
      <section id="tho-uy-tin" className="bg-slate-50 border-t border-line py-16 sm:py-24">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-12">
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-brand">Đội ngũ tay nghề cao</span>
              <h2 className="font-display text-2xl sm:text-3xl font-bold text-slate-900 mt-1">
                Kỹ thuật viên tiêu biểu đã xác minh eKYC
              </h2>
              <p className="text-sm text-slate-600 mt-2">
                Hồ sơ lý lịch rõ ràng, có chứng chỉ hành nghề và được đánh giá thực tế bởi khách hàng.
              </p>
            </div>
            <Link
              to="/dang-ky-tho"
              className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-semibold text-slate-800 hover:border-brand hover:text-brand shadow-xs transition-colors"
            >
              <span>Bạn là thợ có tay nghề? Đăng ký nhận việc</span>
              <span>→</span>
            </Link>
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            {VERIFIED_TECHNICIANS.map((tech, idx) => (
              <div
                key={idx}
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm hover:shadow-md transition-all"
              >
                <div className="flex items-center gap-3.5 mb-4">
                  <div className="h-12 w-12 rounded-full bg-blue-100 text-brand-strong font-bold flex items-center justify-center text-base">
                    {tech.name.charAt(0)}
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h3 className="font-display text-base font-bold text-slate-900">{tech.name}</h3>
                      <span className="inline-flex items-center text-[11px] font-semibold text-emerald-600" title="Đã đối soát CCCD">
                        ✓ eKYC
                      </span>
                    </div>
                    <p className="text-xs font-medium text-brand">{tech.specialty}</p>
                  </div>
                </div>

                <div className="space-y-1.5 text-xs text-slate-600 border-t border-slate-100 pt-3">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Kinh nghiệm:</span>
                    <span className="font-semibold text-slate-800">{tech.experience}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Đánh giá khách hàng:</span>
                    <span className="font-bold text-amber-500">★ {tech.rating} / 5.0 ({tech.jobs} đơn)</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Khu vực phục vụ:</span>
                    <span className="font-medium text-slate-700">{tech.areas}</span>
                  </div>
                </div>

                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-emerald-600">
                    <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
                    Sẵn sàng nhận việc
                  </span>
                  <Link
                    to="/dang-yeu-cau"
                    className="text-xs font-bold text-brand hover:text-brand-strong"
                  >
                    Gửi yêu cầu →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ===== 3 LAYERS OF CUSTOMER PROTECTION (ESCROW) ===== */}
      <section className="mx-auto max-w-6xl px-4 py-16 sm:py-24 sm:px-6">
        <div className="rounded-3xl bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 text-white p-8 sm:p-12 relative overflow-hidden shadow-xl">
          <div className="absolute right-0 top-0 h-96 w-96 rounded-full bg-brand/10 blur-3xl" />
          <div className="relative max-w-2xl">
            <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3.5 py-1 text-xs font-semibold text-blue-200 mb-4 border border-white/15">
              <span>🛡️ An tâm 100% khi gọi thợ qua FixLink</span>
            </span>
            <h2 className="font-display text-2xl sm:text-4xl font-bold leading-tight">
              3 Tấm lá chắn bảo vệ quyền lợi khách hàng
            </h2>
            <p className="mt-3 text-sm sm:text-base text-blue-100/90 leading-relaxed">
              Giải quyết triệt để nỗi sợ thợ báo giá ảo, vẽ lỗi phát sinh hoặc bỏ rơi khách khi thiết bị gặp sự cố tái diễn.
            </p>

            <div className="mt-8 space-y-4">
              {[
                {
                  title: 'Giữ tiền ký quỹ Escrow',
                  desc: 'Khoản tiền cọc 30% và thanh toán đều được lưu giữ tại cổng thanh toán trung gian, chỉ giải ngân cho thợ sau khi bạn bấm nghiệm thu hài lòng.'
                },
                {
                  title: 'Minh bạch chi phí phát sinh',
                  desc: 'Nếu phát hiện lỗi mới sau khi khảo sát, thợ bắt buộc phải lập phiếu báo giá trên ứng dụng. Khách duyệt thì thợ mới được phép thi công.'
                },
                {
                  title: 'Phiếu bảo hành điện tử chính thức',
                  desc: 'Mọi đơn hoàn tất đều tự động phát hành mã bảo hành có hạn dùng 30–90 ngày. Thiết bị hỏng lại trong thời hạn bảo hành, thợ phải đến khắc phục miễn phí.'
                }
              ].map((shield, i) => (
                <div key={i} className="flex items-start gap-3.5 bg-white/5 border border-white/10 rounded-2xl p-4 backdrop-blur-sm">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 font-bold text-xs">
                    ✓
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white">{shield.title}</h3>
                    <p className="text-xs text-blue-200 mt-1 leading-relaxed">{shield.desc}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/dang-yeu-cau"
                className="rounded-xl bg-brand hover:bg-brand-strong text-white px-6 py-3 text-sm font-semibold shadow-glow transition-all"
              >
                Đăng sự cố ngay bây giờ
              </Link>
              <a
                href="tel:19008899"
                className="rounded-xl border border-white/20 bg-white/10 px-5 py-3 text-sm font-semibold text-white hover:bg-white/20 transition-colors"
              >
                Hotline hỗ trợ: 1900 8899
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* ===== FOOTER ===== */}
      <footer className="border-t border-line bg-white text-slate-600">
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
          <div className="grid gap-8 sm:grid-cols-2 md:grid-cols-4 mb-10">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand text-white font-bold text-sm">
                  ⚡
                </span>
                <span className="font-display text-xl font-bold text-slate-900">FixLink</span>
              </div>
              <p className="text-xs text-slate-500 leading-relaxed">
                Nền tảng kết nối thợ sửa chữa đồ điện tử & điện lạnh chuyên nghiệp tại Việt Nam.
              </p>
              <div className="mt-4 text-xs font-semibold text-slate-700">
                Hotline cứu hộ: <span className="text-brand font-bold">1900 8899</span>
              </div>
            </div>

            <div>
              <h4 className="font-display text-sm font-bold text-slate-900 mb-3">Dịch vụ sửa chữa</h4>
              <ul className="space-y-2 text-xs">
                <li><Link to="/dang-yeu-cau?danh-muc=DIEN_LANH" className="hover:text-brand">Sửa chữa điều hòa & tủ lạnh</Link></li>
                <li><Link to="/dang-yeu-cau?danh-muc=DIEN_TU" className="hover:text-brand">Sửa Smart TV & thiết bị âm thanh</Link></li>
                <li><Link to="/dang-yeu-cau?danh-muc=DO_GIA_DUNG" className="hover:text-brand">Sửa bếp từ & nồi chiên không dầu</Link></li>
                <li><Link to="/dang-yeu-cau?danh-muc=MAY_TINH" className="hover:text-brand">Sửa laptop & máy tính để bàn</Link></li>
              </ul>
            </div>

            <div>
              <h4 className="font-display text-sm font-bold text-slate-900 mb-3">Dành cho Thợ</h4>
              <ul className="space-y-2 text-xs">
                <li><Link to="/dang-ky-tho" className="hover:text-brand">Đăng ký hồ sơ nhận việc</Link></li>
                <li><Link to="/tho" className="hover:text-brand">Bảng tin nhận việc thợ</Link></li>
                <li><Link to="/tho/ho-so" className="hover:text-brand">Xác minh danh tính CCCD</Link></li>
                <li><span className="text-slate-400">Quy chuẩn ứng xử thợ FixLink</span></li>
              </ul>
            </div>

            <div>
              <h4 className="font-display text-sm font-bold text-slate-900 mb-3">Chính sách & An toàn</h4>
              <ul className="space-y-2 text-xs">
                <li><span className="hover:text-brand cursor-pointer">Chính sách ký quỹ Escrow</span></li>
                <li><span className="hover:text-brand cursor-pointer">Quy định bảo hành điện tử</span></li>
                <li><span className="hover:text-brand cursor-pointer">Bảo mật thông tin khách hàng</span></li>
                <li><span className="hover:text-brand cursor-pointer">Quy trình xử lý khiếu nại</span></li>
              </ul>
            </div>
          </div>

          <div className="border-t border-slate-100 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-400">
            <p>© 2026 FixLink Vietnam. Bảo lưu mọi quyền.</p>
            <p>Đồ án tốt nghiệp Chuyên ngành Kỹ thuật Phần mềm — Spring Boot & React Vite</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
