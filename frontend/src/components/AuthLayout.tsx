import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface AuthLayoutProps {
  title: string;
  description?: ReactNode;
  /** Rộng hơn cho biểu mẫu nhiều trường như đăng ký thợ. */
  wide?: boolean;
  children: ReactNode;
  footer?: ReactNode;
}

export default function AuthLayout({
  title,
  description,
  wide = false,
  children,
  footer
}: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen bg-surface">
      {/* Left panel — trustworthy tech hero */}
      <div className="relative hidden w-[45%] overflow-hidden bg-gradient-to-br from-blue-700 via-brand-strong to-slate-900 lg:block text-white">
        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:16px_16px]" />
        <div className="absolute -left-20 -top-20 h-72 w-72 rounded-full bg-blue-500/20 blur-3xl" />
        <div className="absolute -right-20 bottom-10 h-72 w-72 rounded-full bg-amber-500/15 blur-3xl" />

        <div className="relative flex h-full flex-col justify-between p-12">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 backdrop-blur-md border border-white/20 text-white shadow-lg">
              <svg className="w-6 h-6 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
            <div>
              <span className="font-display text-2xl font-black tracking-tight text-white">FixLink</span>
              <p className="text-xs text-blue-200 font-medium">Nền Tảng Sửa Chữa Đồ Điện Tử</p>
            </div>
          </Link>

          {/* Center message */}
          <div className="my-auto max-w-md">
            <div className="inline-flex items-center gap-2 rounded-full bg-blue-500/20 border border-blue-400/30 px-3.5 py-1 text-xs font-semibold text-blue-100 mb-6">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
              Kết nối hơn 500+ Kỹ thuật viên uy tín
            </div>
            <h2 className="font-display text-3xl font-bold leading-snug text-white">
              An tâm sửa chữa thiết bị điện tử tận nhà
            </h2>
            <p className="mt-3.5 text-base leading-relaxed text-blue-100/90">
              Minh bạch báo giá, ký quỹ Escrow bảo vệ tiền cọc 100%, bảo hành điện tử chính hãng từ 30 đến 90 ngày.
            </p>

            {/* Feature bullets */}
            <div className="mt-8 space-y-3">
              {[
                'Kiểm tra và chẩn đoán đúng lỗi, báo giá trước khi sửa',
                'Thợ điện lạnh & điện tử đã xác minh CCCD chính chủ',
                'Nghiệm thu hài lòng mới chuyển tiền thanh toán'
              ].map((item, idx) => (
                <div key={idx} className="flex items-center gap-3 text-sm text-blue-100">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-bold">✓</span>
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Stats footer */}
          <div className="grid grid-cols-3 gap-4 border-t border-white/15 pt-6">
            <div>
              <p className="font-display text-2xl font-bold text-white">15 phút</p>
              <p className="text-xs text-blue-200 mt-0.5">Có báo giá nhanh</p>
            </div>
            <div>
              <p className="font-display text-2xl font-bold text-amber-300">4.9 / 5.0★</p>
              <p className="text-xs text-blue-200 mt-0.5">Đánh giá thực tế</p>
            </div>
            <div>
              <p className="font-display text-2xl font-bold text-emerald-400">100%</p>
              <p className="text-xs text-blue-200 mt-0.5">Bảo hành điện tử</p>
            </div>
          </div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="flex flex-1 flex-col bg-surface">
        <header className="border-b border-line bg-white/95 backdrop-blur-md">
          <div className="mx-auto flex max-w-2xl items-center justify-between px-6 py-4">
            <Link to="/" className="flex items-center gap-2 font-display text-lg font-bold lg:hidden">
              <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand text-sm text-white">
                ⚡
              </span>
              <span className="text-gradient">FixLink</span>
            </Link>
            <span className="hidden lg:inline" />
            <Link
              to="/"
              className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 transition-colors hover:text-brand"
            >
              <span>←</span> Về trang chủ
            </Link>
          </div>
        </header>

        <main
          className={`mx-auto flex w-full flex-1 flex-col justify-center px-6 py-10 sm:py-14 ${
            wide ? 'max-w-2xl' : 'max-w-md'
          }`}
        >
          <div className="rounded-2xl border border-line bg-white p-7 sm:p-9 shadow-card animate-fade-in">
            <h1 className="font-display text-2xl font-bold text-ink sm:text-3xl">{title}</h1>
            {description && <div className="mt-1.5 text-sm text-ink-soft">{description}</div>}
            <div className="mt-6">{children}</div>
            {footer && <div className="mt-8 border-t border-line pt-5 text-center text-sm">{footer}</div>}
          </div>
        </main>
      </div>
    </div>
  );
}
