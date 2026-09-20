interface Option {
  id: number;
  label: string;
  hint?: string;
}

interface CheckboxGroupProps {
  legend: string;
  description?: string;
  options: Option[];
  selected: number[];
  onChange: (next: number[]) => void;
  emptyText?: string;
}

export default function CheckboxGroup({
  legend,
  description,
  options,
  selected,
  onChange,
  emptyText = 'Chưa có lựa chọn nào.'
}: CheckboxGroupProps) {
  function toggle(id: number) {
    onChange(selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id]);
  }

  return (
    <fieldset>
      <legend className="mb-1.5 text-sm font-medium text-ink">{legend}</legend>
      {description && <p className="mb-2 text-sm text-ink-soft">{description}</p>}

      {options.length === 0 ? (
        <p className="text-sm text-ink-soft">{emptyText}</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {options.map((option) => {
            const checked = selected.includes(option.id);
            return (
              <label
                key={option.id}
                className={`flex min-h-[44px] cursor-pointer items-center gap-2 rounded-lg border px-3 text-sm transition-colors ${
                  checked
                    ? 'border-brand bg-brand/10 text-brand-ink'
                    : 'border-line bg-card text-ink-soft hover:bg-surface'
                }`}
              >
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => toggle(option.id)}
                  className="h-4 w-4 accent-brand"
                />
                <span>
                  {option.label}
                  {option.hint && <span className="text-ink-soft">, {option.hint}</span>}
                </span>
              </label>
            );
          })}
        </div>
      )}
    </fieldset>
  );
}
