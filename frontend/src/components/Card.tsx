import type { ReactNode } from 'react';

interface CardProps {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}

export default function Card({ title, description, actions, children, className = '' }: CardProps) {
  return (
    <section
      className={`overflow-hidden rounded-2xl border border-line bg-surface-card shadow-sm transition-all duration-200 hover:shadow-md ${className}`}
    >
      {(title || actions) && (
        <header className="flex flex-wrap items-start justify-between gap-3 border-b border-line bg-surface-elevated/30 px-5 py-4">
          <div>
            {title && <h2 className="font-display text-lg font-bold text-ink">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-ink-soft">{description}</p>}
          </div>
          {actions}
        </header>
      )}
      <div className="px-5 py-5">{children}</div>
    </section>
  );
}

