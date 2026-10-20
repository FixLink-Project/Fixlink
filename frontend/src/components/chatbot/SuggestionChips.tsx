
interface SuggestionChipsProps {
  suggestions: string[];
  onSelect: (suggestion: string) => void;
  disabled?: boolean;
}

export default function SuggestionChips({ suggestions, onSelect, disabled }: SuggestionChipsProps) {
  if (!suggestions || suggestions.length === 0) return null;

  return (
    <div className="flex gap-1.5 overflow-x-auto py-2 px-1 no-scrollbar scroll-smooth">
      {suggestions.map((item, idx) => (
        <button
          key={idx}
          type="button"
          disabled={disabled}
          onClick={() => onSelect(item)}
          className="whitespace-nowrap px-3 py-1.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 hover:text-blue-800 border border-blue-200/70 transition-all duration-150 active:scale-95 disabled:opacity-50 disabled:pointer-events-none flex-shrink-0 shadow-2xs"
        >
          💡 {item}
        </button>
      ))}
    </div>
  );
}
