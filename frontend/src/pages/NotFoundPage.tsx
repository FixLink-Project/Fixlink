import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-surface px-4">
      <div className="relative animate-fade-in text-center">
        {/* Decorative glow */}
        <div className="absolute -inset-20 rounded-full bg-brand/5 blur-3xl" />

        <div className="relative">
          <span className="text-8xl animate-float">🔧</span>
          <h1 className="mt-6 font-display text-7xl font-bold text-gradient">404</h1>
          <p className="mt-4 text-xl font-medium text-ink">Trang không tồn tại</p>
          <p className="mt-2 max-w-sm text-ink-soft">
            Trang bạn tìm có thể đã bị xoá, đổi tên hoặc chưa từng tồn tại.
          </p>
          <Link
            to="/"
            className="mt-8 inline-flex min-h-[48px] items-center rounded-xl bg-gradient-to-r from-brand to-brand-glow px-6 font-medium text-white shadow-glow transition-all hover:shadow-glow-lg"
          >
            ← Về trang chủ
          </Link>
        </div>
      </div>
    </div>
  );
}
