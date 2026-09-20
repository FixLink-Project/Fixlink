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
 * Tab đang chọn được đánh dấu bằng viền dưới màu brand; các tab còn lại để chữ
 * mờ. Điều hướng bằng phím mũi tên trái/phải theo đúng thông lệ của tablist.
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
      <div role="tablist" aria-label={label} className="flex flex-wrap gap-1 border-b border-line">
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
              className={`-mb-px flex min-h-[44px] items-center gap-2 border-b-2 px-3 text-sm transition-colors ${
                selected
                  ? 'border-brand font-medium text-brand-ink'
                  : 'border-transparent text-ink-soft hover:text-ink'
              }`}
            >
              {item.label}
              {item.badge != null && item.badge > 0 && (
                <span
                  className={`rounded-full px-1.5 py-0.5 text-xs ${
                    selected ? 'bg-brand/10 text-brand-ink' : 'bg-surface text-ink-soft'
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
