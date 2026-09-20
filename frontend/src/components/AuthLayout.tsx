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
    <div className="flex min-h-screen flex-col bg-surface">
      <header className="border-b border-line bg-card">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6">
          <Link to="/" className="font-display text-lg font-bold text-brand-ink">
            FixLink
          </Link>
          <Link to="/" className="text-sm text-ink-soft hover:text-ink">
            Về trang chủ
          </Link>
        </div>
      </header>

      <main
        className={`mx-auto flex w-full flex-1 flex-col justify-center px-4 py-10 sm:px-6 sm:py-16 ${
          wide ? 'max-w-2xl' : 'max-w-md'
        }`}
      >
        <h1 className="font-display text-2xl font-semibold sm:text-3xl">{title}</h1>
        {description && <div className="mt-2 text-ink-soft">{description}</div>}
        {children}
        {footer && <div className="mt-8 border-t border-line pt-6">{footer}</div>}
      </main>
    </div>
  );
}
