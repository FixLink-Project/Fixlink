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
        <p className="text-sm text-slate-400">{emptyText}</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {options.map((option) => {
            const checked = selected.includes(option.id);
            return (
              <label
                key={option.id}
                className={`flex min-h-[42px] cursor-pointer items-center gap-2 rounded-xl border px-3.5 text-xs sm:text-sm transition-all duration-200 ${
                  checked
                    ? 'border-brand bg-blue-50/90 text-brand font-bold shadow-xs ring-1 ring-brand/30'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50 shadow-xs font-medium'
                }`}
              >
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => toggle(option.id)}
                  className="h-4 w-4 rounded border-slate-300 accent-brand"
                />
                <span>
                  {option.label}
                  {option.hint && <span className="text-slate-400 text-xs"> ({option.hint})</span>}
                </span>
              </label>
            );
          })}
        </div>
      )}
    </fieldset>
  );
}
