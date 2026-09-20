import { useId, type InputHTMLAttributes } from 'react';

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string;
  /** Thông báo lỗi của riêng trường này; có giá trị thì viền chuyển đỏ. */
  error?: string;
  hint?: string;
}

export default function TextField({ label, error, hint, className = '', ...rest }: TextFieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-ink">
        {label}
      </label>
      <input
        {...rest}
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        className={[
          'w-full min-h-[44px] rounded-lg border bg-card px-3.5 text-ink',
          'placeholder:text-ink-soft/60 disabled:cursor-not-allowed disabled:bg-surface disabled:text-ink-soft',
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
