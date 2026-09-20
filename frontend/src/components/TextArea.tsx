import { useId, type TextareaHTMLAttributes } from 'react';

interface TextAreaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id'> {
  label: string;
  error?: string;
  hint?: string;
}

export default function TextArea({ label, error, hint, className = '', ...rest }: TextAreaProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink">
        {label}
      </label>
      <textarea
        {...rest}
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        className={[
          'w-full rounded-lg border bg-card px-3.5 py-2.5 text-ink',
          'placeholder:text-ink-soft/60 disabled:cursor-not-allowed disabled:bg-surface',
          error ? 'border-rose focus-visible:ring-rose' : 'border-line',
          className
        ]
          .filter(Boolean)
          .join(' ')}
      />
      {error ? (
        <p id={errorId} className="mt-1.5 text-sm text-rose">
          {error}
        </p>
      ) : hint ? (
        <p id={hintId} className="mt-1.5 text-sm text-ink-soft">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
