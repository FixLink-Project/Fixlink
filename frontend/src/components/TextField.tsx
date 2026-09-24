import type { InputHTMLAttributes } from 'react';

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export default function TextField({ label, error, hint, id, ...rest }: TextFieldProps) {
  const fieldId = id ?? rest.name;
  return (
    <div>
      <label htmlFor={fieldId} className="mb-1.5 block text-sm font-medium text-ink">
        {label}
        {rest.required && <span className="ml-0.5 text-brand-glow">*</span>}
      </label>
      <input
        {...rest}
        id={fieldId}
        className={[
          'min-h-[44px] w-full rounded-xl border bg-white px-3.5 text-sm text-ink',
          'placeholder:text-ink-muted transition-all duration-200 shadow-sm',
          'focus:outline-none focus:ring-4 focus:ring-brand/10 focus:border-brand',
          error
            ? 'border-danger focus:ring-danger/10 focus:border-danger'
            : 'border-line hover:border-line-strong'
        ].join(' ')}
      />
      {hint && !error && <p className="mt-1.5 text-sm text-ink-muted">{hint}</p>}
      {error && (
        <p className="mt-1.5 text-sm text-danger" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
