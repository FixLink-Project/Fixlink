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
    <section className={`rounded-xl border border-line bg-card ${className}`}>
      {(title || actions) && (
        <header className="flex flex-wrap items-start justify-between gap-3 border-b border-line px-5 py-4">
          <div>
            {title && <h2 className="font-display text-lg font-semibold">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-ink-soft">{description}</p>}
          </div>
          {actions}
        </header>
      )}
      <div className="px-5 py-5">{children}</div>
    </section>
  );
}
