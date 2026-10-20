
export default function TypingIndicator() {
  return (
    <div className="flex items-center space-x-2 py-1.5 px-3 bg-slate-100 rounded-2xl w-fit text-slate-500 text-xs shadow-sm border border-slate-200/60 animate-pulse">
      <span className="flex space-x-1 items-center">
        <span className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-bounce [animation-delay:-0.3s]"></span>
        <span className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-bounce [animation-delay:-0.15s]"></span>
        <span className="w-1.5 h-1.5 bg-blue-600 rounded-full animate-bounce"></span>
      </span>
      <span className="text-slate-600 font-medium ml-1">FixLink đang suy nghĩ...</span>
    </div>
  );
}
