import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import type { ChatMessageUI } from '../../lib/useChatbot';

interface MessageBubbleProps {
  message: ChatMessageUI;
  onFeedback: (messageId: string, rating: 'UP' | 'DOWN', comment?: string) => void;
  onSuggestionClick?: (text: string) => void;
}

export default function MessageBubble({ message, onFeedback }: MessageBubbleProps) {
  const isUser = message.sender === 'user';
  const [showSources, setShowSources] = useState(false);
  const [feedbackSent, setFeedbackSent] = useState<'UP' | 'DOWN' | null>(message.feedback || null);

  const handleFeedback = (rating: 'UP' | 'DOWN') => {
    setFeedbackSent(rating);
    onFeedback(message.id, rating);
  };

  const renderFormattedText = (raw: string) => {
    if (!raw) return null;

    // Split lines
    const lines = raw.split('\n');
    const elements: ReactNode[] = [];
    let listBuffer: string[] = [];
    let isNumberedList = false;

    const flushList = () => {
      if (listBuffer.length > 0) {
        if (isNumberedList) {
          elements.push(
            <ol key={`ol-${elements.length}`} className="list-decimal pl-5 my-1.5 space-y-1 text-inherit">
              {listBuffer.map((li, idx) => (
                <li key={idx}>{parseInlineMarkdown(li)}</li>
              ))}
            </ol>
          );
        } else {
          elements.push(
            <ul key={`ul-${elements.length}`} className="list-disc pl-5 my-1.5 space-y-1 text-inherit">
              {listBuffer.map((li, idx) => (
                <li key={idx}>{parseInlineMarkdown(li)}</li>
              ))}
            </ul>
          );
        }
        listBuffer = [];
      }
    };

    lines.forEach((line, lineIdx) => {
      const trimmed = line.trim();

      if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
        if (isNumberedList) flushList();
        isNumberedList = false;
        listBuffer.push(trimmed.substring(2));
        return;
      }

      const numberedMatch = trimmed.match(/^(\d+)\.\s+(.*)/);
      if (numberedMatch) {
        if (!isNumberedList) flushList();
        isNumberedList = true;
        listBuffer.push(numberedMatch[2]);
        return;
      }

      flushList();

      if (trimmed.startsWith('### ')) {
        elements.push(
          <h4 key={lineIdx} className="font-bold text-sm mt-2.5 mb-1 text-slate-900">
            {parseInlineMarkdown(trimmed.substring(4))}
          </h4>
        );
      } else if (trimmed.startsWith('## ')) {
        elements.push(
          <h3 key={lineIdx} className="font-bold text-base mt-3 mb-1 text-slate-900">
            {parseInlineMarkdown(trimmed.substring(3))}
          </h3>
        );
      } else if (trimmed === '') {
        // empty line spacing
        elements.push(<div key={`sp-${lineIdx}`} className="h-1.5" />);
      } else {
        elements.push(
          <p key={lineIdx} className="leading-relaxed">
            {parseInlineMarkdown(trimmed)}
          </p>
        );
      }
    });

    flushList();
    return elements;
  };

  const parseInlineMarkdown = (text: string): ReactNode => {
    // Replace markdown links [label](url), bold **bold**, code `code`
    const parts: ReactNode[] = [];
    const regex = /(\[([^\]]+)\]\(([^)]+)\)|\*\*([^*]+)\*\*|`([^`]+)`)/g;
    let lastIndex = 0;
    let match: RegExpExecArray | null;

    while ((match = regex.exec(text)) !== null) {
      if (match.index > lastIndex) {
        parts.push(text.substring(lastIndex, match.index));
      }

      if (match[2] && match[3]) {
        // Link [label](url)
        const label = match[2];
        const url = match[3];
        if (url.startsWith('/')) {
          parts.push(
            <Link
              key={match.index}
              to={url}
              className="inline-flex items-center text-blue-600 hover:text-blue-800 underline font-medium hover:underline-offset-2 transition-colors mx-0.5"
            >
              {label} ↗
            </Link>
          );
        } else {
          parts.push(
            <a
              key={match.index}
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center text-blue-600 hover:text-blue-800 underline font-medium hover:underline-offset-2 transition-colors mx-0.5"
            >
              {label} ↗
            </a>
          );
        }
      } else if (match[4]) {
        // Bold **text**
        parts.push(
          <strong key={match.index} className="font-semibold text-slate-900">
            {match[4]}
          </strong>
        );
      } else if (match[5]) {
        // Inline code `code`
        parts.push(
          <code
            key={match.index}
            className="px-1.5 py-0.5 bg-slate-100 text-slate-800 rounded text-xs font-mono border border-slate-200"
          >
            {match[5]}
          </code>
        );
      }

      lastIndex = regex.lastIndex;
    }

    if (lastIndex < text.length) {
      parts.push(text.substring(lastIndex));
    }

    return parts.length > 0 ? parts : text;
  };

  if (isUser) {
    return (
      <div className="flex justify-end mb-3">
        <div className="max-w-[85%] rounded-2xl rounded-tr-xs bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-4 py-2.5 text-sm shadow-sm break-words">
          <p className="leading-relaxed whitespace-pre-wrap">{message.text}</p>
          <div className="text-[10px] text-blue-100/70 text-right mt-1">
            {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-start gap-2.5 mb-4 group">
      {/* Bot Avatar */}
      <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white shadow-sm flex-shrink-0 mt-0.5 text-xs font-bold ring-2 ring-blue-100">
        AI
      </div>

      <div className="flex-1 max-w-[88%] space-y-2">
        <div className="rounded-2xl rounded-tl-xs bg-white border border-slate-200/90 text-slate-800 px-4 py-3 text-sm shadow-2xs space-y-1.5 break-words">
          {renderFormattedText(message.text)}

          {/* Ticket notification card */}
          {message.ticketId && (
            <div className="mt-2.5 p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 text-xs flex items-center justify-between">
              <div>
                <div className="font-semibold text-amber-950 flex items-center gap-1.5">
                  <span>🎫</span> Phiếu hỗ trợ đã được tạo
                </div>
                <div className="mt-0.5 font-mono font-bold text-amber-800">{message.ticketId}</div>
              </div>
              <div className="text-[11px] text-amber-700 bg-amber-100/80 px-2 py-1 rounded-md font-medium">
                Đang chờ xử lý
              </div>
            </div>
          )}

          {/* Action links */}
          {message.actions && message.actions.length > 0 && (
            <div className="mt-2.5 pt-2 border-t border-slate-100 flex flex-wrap gap-2">
              {message.actions.map((act, idx) => (
                <Link
                  key={idx}
                  to={act.url}
                  className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-50 text-blue-700 hover:bg-blue-100 rounded-lg text-xs font-medium border border-blue-200/60 transition-colors shadow-2xs"
                >
                  <span>{act.label}</span>
                  <span>→</span>
                </Link>
              ))}
            </div>
          )}

          {/* Sources reference toggle */}
          {message.sources && message.sources.length > 0 && (
            <div className="mt-2 pt-2 border-t border-slate-100 text-xs">
              <button
                type="button"
                onClick={() => setShowSources(!showSources)}
                className="text-slate-500 hover:text-slate-700 text-[11px] font-medium flex items-center gap-1 transition-colors"
              >
                <span>📚 Nguồn tài liệu ({message.sources.length})</span>
                <span className="text-[9px]">{showSources ? '▲' : '▼'}</span>
              </button>
              {showSources && (
                <div className="mt-1.5 p-2 bg-slate-50 rounded-lg border border-slate-200 text-[11px] text-slate-600 space-y-1">
                  {message.sources.map((s, idx) => (
                    <div key={idx} className="flex items-center gap-1.5">
                      <span className="text-slate-400">•</span>
                      <span className="font-medium text-slate-700">{s.section}</span>
                      <span className="text-slate-400">({s.documentName})</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Message Meta & Feedback Toolbar */}
        <div className="flex items-center justify-between px-1 text-[11px] text-slate-400">
          <div className="flex items-center gap-2">
            <span>{new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
            {message.modelUsed && (
              <span className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-500 text-[10px] font-mono">
                {message.modelUsed.replace('gemini-', '')}
              </span>
            )}
          </div>

          <div className="flex items-center gap-1">
            {feedbackSent ? (
              <span className="text-green-600 font-medium text-[10px]">
                {feedbackSent === 'UP' ? '✓ Cảm ơn bạn!' : '✓ Đã ghi nhận phản hồi'}
              </span>
            ) : (
              <div className="opacity-70 group-hover:opacity-100 transition-opacity flex items-center gap-1">
                <button
                  type="button"
                  title="Hữu ích"
                  onClick={() => handleFeedback('UP')}
                  className="p-1 hover:text-green-600 rounded hover:bg-slate-100 transition-colors"
                >
                  👍
                </button>
                <button
                  type="button"
                  title="Chưa hữu ích"
                  onClick={() => handleFeedback('DOWN')}
                  className="p-1 hover:text-red-500 rounded hover:bg-slate-100 transition-colors"
                >
                  👎
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
