import { useId, type ReactNode } from 'react';

export interface TabItem<T extends string = string> {
  id: T;
  label: string;
  /** Con số phụ bên cạnh nhãn, ví dụ số hồ sơ đang chờ duyệt. */
  badge?: number;
}

interface TabsProps<T extends string> {
  items: Array<TabItem<T>>;
  active: T;
  onChange: (id: T) => void;
  /** Mô tả cho trình đọc màn hình biết nhóm tab này dùng để làm gì. */
  label: string;
  children?: ReactNode;
}

/**
 * Pill-style tabs với glow active state cho dark theme.
 */
export default function Tabs<T extends string>({
  items,
  active,
  onChange,
  label,
  children
}: TabsProps<T>) {
  const baseId = useId();

  function handleKeyDown(event: React.KeyboardEvent<HTMLButtonElement>) {
    const currentIndex = items.findIndex((item) => item.id === active);
    let nextIndex: number | null = null;

    if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % items.length;
    if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + items.length) % items.length;
    if (event.key === 'Home') nextIndex = 0;
    if (event.key === 'End') nextIndex = items.length - 1;

    if (nextIndex === null) return;
    event.preventDefault();
    const nextTab = items[nextIndex];
    onChange(nextTab.id);
    document.getElementById(`${baseId}-${nextTab.id}`)?.focus();
  }

  return (
    <div>
      <div
        role="tablist"
        aria-label={label}
        className="flex flex-wrap gap-1 rounded-2xl border border-slate-200 bg-slate-100/80 p-1.5 shadow-xs"
      >
        {items.map((item) => {
          const selected = item.id === active;
          return (
            <button
              key={item.id}
              id={`${baseId}-${item.id}`}
              role="tab"
              type="button"
              aria-selected={selected}
              aria-controls={`${baseId}-${item.id}-panel`}
              tabIndex={selected ? 0 : -1}
              onClick={() => onChange(item.id)}
              onKeyDown={handleKeyDown}
              className={`flex min-h-[40px] items-center gap-2 rounded-xl px-4 text-sm transition-all duration-200 ${
                selected
                  ? 'bg-white font-bold text-slate-900 shadow-sm border border-slate-200/80'
                  : 'border border-transparent text-slate-600 hover:text-slate-900 hover:bg-white/60 font-medium'
              }`}
            >
              {item.label}
              {item.badge != null && item.badge > 0 && (
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                    selected
                      ? 'bg-blue-50 text-brand border border-blue-200'
                      : 'bg-slate-200/80 text-slate-700'
                  }`}
                >
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {children && (
        <div
          role="tabpanel"
          id={`${baseId}-${active}-panel`}
          aria-labelledby={`${baseId}-${active}`}
          className="mt-6"
        >
          {children}
        </div>
      )}
    </div>
  );
}
