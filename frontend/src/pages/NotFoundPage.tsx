import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-surface px-4">
      <div className="animate-fade-in text-center">
        <div>
          <span className="text-7xl">🔧</span>
          <h1 className="mt-6 font-display text-7xl font-bold text-brand-strong">404</h1>
          <p className="mt-4 text-xl font-medium text-ink">Trang không tồn tại</p>
          <p className="mt-2 max-w-sm text-ink-soft">
            Trang bạn tìm có thể đã bị xoá, đổi tên hoặc chưa từng tồn tại.
          </p>
          <Link
            to="/"
            className="mt-8 inline-flex min-h-[48px] items-center rounded-lg bg-brand px-6 font-medium text-white transition-colors hover:bg-brand-strong"
          >
            ← Về trang chủ
          </Link>
        </div>
      </div>
    </div>
  );
}
