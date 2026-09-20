import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-surface px-4 text-center">
      <p className="font-display text-5xl font-bold text-brand">404</p>
      <h1 className="mt-4 font-display text-2xl font-semibold">Không tìm thấy trang này</h1>
      <p className="mt-2 max-w-md text-ink-soft">
        Đường dẫn có thể đã đổi hoặc bị gõ sai. Quay về trang chủ để đăng yêu cầu sửa chữa.
      </p>
      <Link
        to="/"
        className="mt-6 flex min-h-[44px] items-center rounded-lg bg-brand px-5 font-medium text-white hover:bg-brand-strong"
      >
        Về trang chủ
      </Link>
    </main>
  );
}
