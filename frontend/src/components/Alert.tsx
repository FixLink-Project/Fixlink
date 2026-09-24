import type { ReactNode } from 'react';

type Tone = 'success' | 'error' | 'warning' | 'info';

interface AlertProps {
  tone?: Tone;
  title?: string;
  children: ReactNode;
}

const TONES: Record<Tone, { wrapper: string; icon: string; emoji: string }> = {
  success: {
    wrapper: 'border-emerald-500 bg-emerald-50 text-emerald-900',
    icon: 'text-emerald-600 bg-emerald-100',
    emoji: '✓'
  },
  error: {
    wrapper: 'border-rose-500 bg-rose-50 text-rose-900',
    icon: 'text-rose-600 bg-rose-100',
    emoji: '✕'
  },
  warning: {
    wrapper: 'border-amber-500 bg-amber-50 text-amber-900',
    icon: 'text-amber-600 bg-amber-100',
    emoji: '⚠'
  },
  info: {
    wrapper: 'border-blue-500 bg-blue-50 text-blue-900',
    icon: 'text-blue-600 bg-blue-100',
    emoji: 'ℹ'
  }
};

export default function Alert({ tone = 'info', title, children }: AlertProps) {
  const t = TONES[tone];

  return (
    <div
      className={`flex gap-3 rounded-xl border-l-4 px-4 py-3.5 ${t.wrapper} animate-fade-in`}
      role="alert"
    >
      <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold ${t.icon}`} aria-hidden="true">
        {t.emoji}
      </span>
      <div className="min-w-0">
        {title && <p className="font-display font-semibold text-ink">{title}</p>}
        <div className={`text-sm text-ink-soft ${title ? 'mt-0.5' : ''}`}>{children}</div>
      </div>
    </div>
  );
}
