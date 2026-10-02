import type { ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'secondary' | 'quiet' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
  fullWidth?: boolean;
  children: ReactNode;
}

const VARIANTS: Record<Variant, string> = {
  primary:
    'bg-gradient-to-r from-brand to-brand-strong text-white font-semibold shadow-sm hover:shadow-glow hover:brightness-105 active:scale-[0.98] disabled:hover:shadow-none disabled:hover:brightness-100 disabled:active:scale-100',
  secondary:
    'border border-line bg-surface-card text-ink font-medium hover:bg-surface-elevated hover:border-line-strong shadow-sm active:scale-[0.98] disabled:hover:bg-surface-card disabled:active:scale-100',
  quiet:
    'text-ink-soft font-medium hover:bg-surface-elevated hover:text-ink active:scale-[0.98] disabled:hover:bg-transparent disabled:active:scale-100',
  danger:
    'bg-danger text-white font-semibold shadow-sm hover:bg-danger-strong active:scale-[0.98] disabled:hover:bg-danger disabled:active:scale-100'
};

export default function Button({
  variant = 'primary',
  loading = false,
  fullWidth = false,
  disabled,
  className = '',
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={[
        'inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl px-5 text-sm font-medium',
        'transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-55',
        VARIANTS[variant],
        fullWidth ? 'w-full' : '',
        className
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {loading && (
        <span
          aria-hidden="true"
          className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
        />
      )}
      {children}
    </button>
  );
}
