import type { ReactNode } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import StatusChip from './StatusChip';
import { useAuth } from '../lib/auth';

interface NavItem {
  to: string;
  label: string;
}

interface DashboardLayoutProps {
  /** Liên kết giữa các màn hình cùng vai trò, ví dụ khu vực quản trị. */
  nav?: NavItem[];
  title: string;
  description?: ReactNode;
  /** Nội dung phụ nằm bên phải tiêu đề, ví dụ nút hành động chính. */
  actions?: ReactNode;
  children: ReactNode;
}

export default function DashboardLayout({
  nav,
  title,
  description,
  actions,
  children
}: DashboardLayoutProps) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  function handleSignOut() {
    signOut();
    navigate('/dang-nhap', { replace: true });
  }

  return (
    <div className="min-h-screen bg-surface">
      {/* Thanh điều hướng teal: mảng màu nhận diện duy nhất của khu vực đăng nhập */}
      <header className="bg-brand-ink text-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6">
          <Link to="/" className="font-display text-lg font-bold">
            FixLink
          </Link>

          <div className="flex items-center gap-3">
            {user && (
              <div className="text-right">
                <p className="text-sm font-medium leading-tight">
                  {user.fullName?.trim() || user.username}
                </p>
                <p className="text-xs text-white/60">{user.username}</p>
              </div>
            )}
            <button
              type="button"
              onClick={handleSignOut}
              className="min-h-[44px] rounded-lg border border-white/25 px-4 text-sm font-medium transition-colors hover:bg-white/10"
            >
              Đăng xuất
            </button>
          </div>
        </div>
        {nav && nav.length > 0 && (
          <nav className="mx-auto max-w-6xl px-4 sm:px-6">
            <ul className="flex flex-wrap gap-1 border-t border-white/10 pt-1">
              {nav.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end
                    className={({ isActive }) =>
                      `flex min-h-[44px] items-center border-b-2 px-3 text-sm transition-colors ${
                        isActive
                          ? 'border-white font-medium text-white'
                          : 'border-transparent text-white/60 hover:text-white'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        )}
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="font-display text-2xl font-semibold sm:text-3xl">{title}</h1>
              {user && <StatusChip status={user.role} />}
            </div>
            {description && <div className="mt-2 max-w-2xl text-ink-soft">{description}</div>}
          </div>
          {actions}
        </div>

        <div className="mt-8">{children}</div>
      </main>
    </div>
  );
}
