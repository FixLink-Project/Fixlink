import type { ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'secondary' | 'quiet' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
  fullWidth?: boolean;
  children: ReactNode;
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-brand text-white hover:bg-brand-strong disabled:hover:bg-brand',
  secondary: 'border border-line bg-card text-ink hover:bg-surface disabled:hover:bg-card',
  quiet: 'text-ink-soft hover:bg-surface hover:text-ink disabled:hover:bg-transparent',
  danger: 'bg-rose text-white hover:bg-rose/90 disabled:hover:bg-rose'
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
        'inline-flex min-h-[44px] items-center justify-center gap-2 rounded-lg px-5 text-sm font-medium',
        'transition-colors disabled:cursor-not-allowed disabled:opacity-55',
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
