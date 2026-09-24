import type { ReactNode } from 'react';

interface FormSectionProps {
  title: string;
  description?: string;
  children: ReactNode;
}

/** Nhóm các trường liên quan lại với nhau để biểu mẫu dài vẫn đọc được. */
export default function FormSection({ title, description, children }: FormSectionProps) {
  return (
    <section className="border-t border-slate-200/80 pt-6 first:border-t-0 first:pt-0">
      <h2 className="font-display text-base sm:text-lg font-bold text-slate-900">{title}</h2>
      {description && <p className="mt-1 text-xs sm:text-sm text-slate-500">{description}</p>}
      <div className="mt-4 space-y-5">{children}</div>
    </section>
  );
}
