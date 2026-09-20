import type { ReactNode } from 'react';

type Tone = 'error' | 'warning' | 'success' | 'info';

const TONES: Record<Tone, string> = {
  error: 'border-rose/30 bg-rose-soft',
  warning: 'border-amber/30 bg-amber-soft',
  success: 'border-brand/25 bg-brand/5',
  info: 'border-line bg-surface'
};

interface AlertProps {
  tone?: Tone;
  title?: string;
  children: ReactNode;
}

export default function Alert({ tone = 'error', title, children }: AlertProps) {
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={`rounded-xl border px-4 py-3 text-sm ${TONES[tone]}`}
    >
      {title && <p className="font-display font-semibold text-ink">{title}</p>}
      <div className={title ? 'mt-1 text-ink-soft' : 'text-ink'}>{children}</div>
    </div>
  );
}
