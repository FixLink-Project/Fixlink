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
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-surface">
      {/* Clean consumer header */}
      <header className="sticky top-0 z-40 border-b border-line bg-white/95 backdrop-blur-md shadow-sm">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3.5 sm:px-6">
          <Link to="/" className="flex items-center gap-2.5 font-display text-lg font-bold">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand to-brand-strong text-sm text-white shadow-sm">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
            <div>
              <span className="text-gradient text-xl font-extrabold tracking-tight">FixLink</span>
              <span className="hidden sm:inline-block ml-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
                Sửa Chữa Điện Tử
              </span>
            </div>
          </Link>

          <div className="flex items-center gap-3">
            {user && (
              <div className="flex items-center gap-2.5 bg-slate-50 border border-slate-200/80 rounded-xl px-3 py-1.5">
                <div className="h-7 w-7 rounded-full bg-brand-light flex items-center justify-center text-xs font-bold text-brand-strong">
                  {user.fullName?.charAt(0) || user.username.charAt(0).toUpperCase()}
                </div>
                <div className="text-left">
                  <p className="text-xs font-bold leading-tight text-ink">
                    {user.fullName?.trim() || user.username}
                  </p>
                  <p className="text-[11px] text-ink-muted leading-tight">{user.username}</p>
                </div>
              </div>
            )}
            <button
              type="button"
              onClick={handleSignOut}
              className="min-h-[40px] rounded-xl border border-line bg-white px-3.5 text-xs font-semibold text-slate-700 shadow-sm transition-all duration-200 hover:border-slate-300 hover:bg-slate-50"
            >
              Đăng xuất
            </button>
          </div>
        </div>
        {nav && nav.length > 0 && (
          <nav className="mx-auto max-w-6xl px-4 sm:px-6">
            <ul className="flex flex-wrap gap-2 border-t border-line/60 pt-1">
              {nav.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end
                    className={({ isActive }) =>
                      `flex min-h-[42px] items-center border-b-2 px-3 text-sm font-medium transition-all duration-200 ${
                        isActive
                          ? 'border-brand font-bold text-brand'
                          : 'border-transparent text-ink-soft hover:text-ink hover:border-slate-300'
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
        <div className="flex flex-wrap items-start justify-between gap-4 animate-fade-in">
          <div>
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="font-display text-2xl font-bold text-ink sm:text-3xl">{title}</h1>
              {user && <StatusChip status={user.role} />}
            </div>
            {description && <div className="mt-1.5 max-w-2xl text-sm text-ink-soft">{description}</div>}
          </div>
          {actions}
        </div>
        <div className="mt-7">{children}</div>
      </main>
    </div>
  );
}
