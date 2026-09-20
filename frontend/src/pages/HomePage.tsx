import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchCategories } from '../lib/api';
import type { ServiceCategory } from '../lib/types';

const STEPS = [
  {
    title: 'Mô tả đồ hỏng',
    body: 'Chụp ảnh, chọn khu vực và khung giờ bạn rảnh. Mất khoảng hai phút.'
  },
  {
    title: 'Nhận báo giá',
    body: 'Thợ đã xác minh danh tính gửi giá và thời gian có mặt. Bạn so rồi chọn.'
  },
  {
    title: 'Trả tiền khi xong',
    body: 'Tiền cọc giữ trong tài khoản trung gian, chỉ chuyển cho thợ khi bạn nghiệm thu.'
  }
];

export default function HomePage() {
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
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
    <div className="min-h-screen bg-surface">
      {/* Khối teal full-bleed: điểm nhấn mạnh duy nhất của trang */}
      <div className="bg-brand-ink text-white">
        <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5 sm:px-6">
          <Link to="/" className="font-display text-xl font-bold tracking-tight">
            FixLink
          </Link>
          <nav className="flex items-center gap-2 sm:gap-5">
            <a
              href="#danh-muc"
              className="hidden rounded-lg px-3 py-2 text-sm text-white/80 hover:text-white sm:block"
            >
              Dịch vụ
            </a>
            <a
              href="#cach-hoat-dong"
              className="hidden rounded-lg px-3 py-2 text-sm text-white/80 hover:text-white sm:block"
            >
              Cách hoạt động
            </a>
            <Link
              to="/dang-nhap"
              className="flex min-h-[44px] items-center rounded-lg border border-white/25 px-4 text-sm font-medium hover:bg-white/10"
            >
              Đăng nhập
            </Link>
          </nav>
        </header>

        <section className="mx-auto max-w-6xl px-4 pb-16 pt-10 sm:px-6 sm:pb-24 sm:pt-16">
          <div className="max-w-2xl">
            <h1 className="font-display text-[34px] font-bold leading-[1.12] sm:text-5xl">
              Đồ hỏng hôm nay, có thợ tới trong hôm nay
            </h1>
            <p className="mt-5 max-w-lg text-[17px] leading-relaxed text-white/75">
              Đăng yêu cầu một lần, nhận báo giá từ thợ đã xác minh căn cước quanh khu vực
              bạn. Tiền giữ ở tài khoản trung gian tới khi bạn hài lòng.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link
                to="/dang-yeu-cau"
                className="flex min-h-[48px] items-center justify-center rounded-lg bg-white px-6 font-medium text-brand-ink transition-colors hover:bg-white/90"
              >
                Đăng yêu cầu sửa chữa
              </Link>
              <Link
                to="/dang-ky-tho"
                className="flex min-h-[48px] items-center justify-center rounded-lg border border-white/25 px-6 font-medium transition-colors hover:bg-white/10"
              >
                Tôi là thợ, muốn nhận việc
              </Link>
            </div>
          </div>

          {/* Ba lời hứa: chữ thường, chia cột bằng đường kẻ mảnh */}
          <dl className="mt-14 grid gap-px overflow-hidden rounded-xl bg-white/15 sm:grid-cols-3">
            {[
              ['Xác minh căn cước', 'Mọi thợ nhận việc đều qua đối chiếu CCCD và số điện thoại.'],
              ['Giữ tiền trung gian', 'Cọc chỉ chuyển cho thợ sau khi bạn xác nhận đã xong.'],
              ['Bảo hành 30 ngày', 'Hỏng lại trong tháng đầu, thợ quay lại xử lý miễn phí.']
            ].map(([term, detail]) => (
              <div key={term} className="bg-brand-ink px-5 py-6">
                <dt className="font-display text-base font-semibold">{term}</dt>
                <dd className="mt-1.5 text-sm leading-relaxed text-white/70">{detail}</dd>
              </div>
            ))}
          </dl>
        </section>
      </div>

      {/* Danh mục dịch vụ, lấy từ API */}
      <section id="danh-muc" className="mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-20">
        <h2 className="font-display text-2xl font-semibold sm:text-3xl">Thợ nhận những việc gì</h2>
        <p className="mt-2 max-w-xl text-ink-soft">
          Chọn nhóm việc gần nhất với thứ đang hỏng, bạn mô tả chi tiết ở bước sau.
        </p>

        {status === 'loading' && (
          <ul className="mt-8 divide-y divide-line border-y border-line">
            {[0, 1, 2, 3].map((i) => (
              <li key={i} className="flex animate-pulse flex-col gap-2 py-5">
                <span className="h-5 w-48 rounded bg-line" />
                <span className="h-4 w-full max-w-md rounded bg-line/70" />
              </li>
            ))}
          </ul>
        )}

        {status === 'error' && (
          <div className="mt-8 rounded-xl border border-rose/30 bg-rose-soft px-5 py-4">
            <p className="font-medium text-ink">Chưa tải được danh mục dịch vụ.</p>
            <p className="mt-1 text-sm text-ink-soft">
              Máy chủ đang không phản hồi. Bạn tải lại trang giúp, hoặc gọi 1900 1234 để đặt
              lịch trực tiếp.
            </p>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="mt-3 min-h-[44px] rounded-lg bg-brand px-4 text-sm font-medium text-white hover:bg-brand-strong"
            >
              Tải lại trang
            </button>
          </div>
        )}

        {status === 'ready' && categories.length === 0 && (
          <div className="mt-8 border-y border-line py-10 text-center">
            <p className="font-medium">Chưa có nhóm dịch vụ nào đang mở.</p>
            <p className="mt-1 text-sm text-ink-soft">
              Quản trị viên cần bật danh mục trước khi khách đăng yêu cầu.
            </p>
          </div>
        )}

        {status === 'ready' && categories.length > 0 && (
          <ul className="mt-8 divide-y divide-line border-y border-line">
            {categories.map((cat) => (
              <li key={cat.id}>
                <Link
                  to={`/dang-yeu-cau?danh-muc=${cat.code}`}
                  className="flex min-h-[44px] flex-col gap-1 py-5 transition-colors hover:bg-card sm:flex-row sm:items-baseline sm:gap-8 sm:px-3"
                >
                  <span className="font-display text-lg font-semibold sm:w-56 sm:shrink-0">
                    {cat.name}
                  </span>
                  <span className="text-ink-soft">{cat.description}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Ba bước, đánh số to bằng Space Grotesk */}
      <section id="cach-hoat-dong" className="border-t border-line bg-card">
        <div className="mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-20">
          <h2 className="font-display text-2xl font-semibold sm:text-3xl">Cách hoạt động</h2>
          <ol className="mt-8 grid gap-10 sm:grid-cols-3 sm:gap-8">
            {STEPS.map((step, i) => (
              <li key={step.title}>
                <span className="font-display text-4xl font-bold text-brand">{i + 1}</span>
                <h3 className="mt-3 font-display text-lg font-semibold">{step.title}</h3>
                <p className="mt-1.5 text-ink-soft">{step.body}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-8 text-sm text-ink-soft sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p>FixLink — nền tảng kết nối sửa chữa gia dụng tại Việt Nam.</p>
          <p>Hỗ trợ khách hàng: 1900 1234</p>
        </div>
      </footer>
    </div>
  );
}
