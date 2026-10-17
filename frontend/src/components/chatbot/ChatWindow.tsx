import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useAuth } from '../../lib/auth';
import type { ChatMessageUI } from '../../lib/useChatbot';
import MessageBubble from './MessageBubble';
import SuggestionChips from './SuggestionChips';
import TypingIndicator from './TypingIndicator';

interface ChatWindowProps {
  isOpen: boolean;
  onClose: () => void;
  messages: ChatMessageUI[];
  isLoading: boolean;
  suggestions: string[];
  guestContact: string;
  onSetGuestContact: (contact: string) => void;
  onSendMessage: (text: string) => void;
  onFeedback: (messageId: string, rating: 'UP' | 'DOWN', comment?: string) => void;
  onResetSession: () => void;
}

export default function ChatWindow({
  isOpen,
  onClose,
  messages,
  isLoading,
  suggestions,
  guestContact,
  onSetGuestContact,
  onSendMessage,
  onFeedback,
  onResetSession,
}: ChatWindowProps) {
  const { user } = useAuth();
  const [inputText, setInputText] = useState('');
  const [showContactPrompt, setShowContactPrompt] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
      textareaRef.current?.focus();
    }
  }, [messages, isOpen, isLoading]);

  if (!isOpen) return null;

  const handleSubmit = (e?: FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim() || isLoading) return;
    onSendMessage(inputText);
    setInputText('');
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="fixed bottom-20 right-4 sm:right-6 w-[calc(100vw-2rem)] sm:w-[420px] h-[580px] max-h-[82vh] bg-surface-card rounded-2xl shadow-elevated border border-line flex flex-col z-50 overflow-hidden animate-in fade-in slide-in-from-bottom-5 duration-200">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 via-blue-700 to-indigo-700 text-white px-4 py-3.5 flex items-center justify-between shadow-xs">
        <div className="flex items-center gap-2.5">
          <div className="relative">
            <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-xs flex items-center justify-center font-bold text-white text-xs border border-white/30">
              ⚡
            </div>
            <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-400 border-2 border-blue-600 rounded-full animate-pulse" />
          </div>
          <div>
            <div className="text-sm font-bold tracking-tight flex items-center gap-1.5">
              <span>Trợ lý FixLink AI</span>
              <span className="text-[10px] font-normal px-1.5 py-0.2 bg-white/20 rounded text-white/90">
                24/7
              </span>
            </div>
            <div className="text-[11px] text-blue-100 flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block" />
              <span>Sẵn sàng hỗ trợ trực tuyến</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <button
            type="button"
            title="Làm mới cuộc trò chuyện"
            onClick={onResetSession}
            className="p-1.5 hover:bg-white/15 rounded-lg text-white/80 hover:text-white transition-colors text-xs"
          >
            🔄
          </button>
          <button
            type="button"
            title="Đóng cửa sổ"
            onClick={onClose}
            className="p-1.5 hover:bg-white/15 rounded-lg text-white/80 hover:text-white transition-colors text-sm"
          >
            ✕
          </button>
        </div>
      </div>

      {/* Guest Contact Banner (if not logged in) */}
      {!user && (
        <div className="bg-blue-50/70 border-b border-blue-100 px-3 py-1.5 text-xs text-blue-900 flex items-center justify-between">
          <div className="flex items-center gap-1.5 overflow-hidden">
            <span>ℹ️</span>
            {guestContact ? (
              <span className="truncate">
                Liên hệ: <strong className="font-medium text-blue-950">{guestContact}</strong>
              </span>
            ) : (
              <span className="truncate text-slate-600">Khách vãng lai (ẩn danh)</span>
            )}
          </div>
          <button
            type="button"
            onClick={() => setShowContactPrompt(!showContactPrompt)}
            className="text-[11px] text-blue-700 hover:text-blue-900 font-semibold underline underline-offset-2 flex-shrink-0 ml-2"
          >
            {guestContact ? 'Đổi liên hệ' : '+ Thêm SĐT'}
          </button>
        </div>
      )}

      {/* Guest Contact Input Drawer */}
      {showContactPrompt && !user && (
        <div className="bg-slate-50 border-b border-slate-200 p-3 text-xs animate-in fade-in duration-150">
          <label className="block text-slate-700 font-medium mb-1">
            Số điện thoại hoặc Email (để FixLink liên hệ hỗ trợ):
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={guestContact}
              onChange={(e) => onSetGuestContact(e.target.value)}
              placeholder="0912345678 hoặc email@..."
              className="flex-1 px-2.5 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
            <button
              type="button"
              onClick={() => setShowContactPrompt(false)}
              className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-medium transition-colors"
            >
              Lưu
            </button>
          </div>
        </div>
      )}

      {/* Messages List Area */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-slate-50/50">
        {messages.map((msg) => (
          <MessageBubble
            key={msg.id}
            message={msg}
            onFeedback={onFeedback}
            onSuggestionClick={(s) => onSendMessage(s)}
          />
        ))}

        {isLoading && (
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-white text-xs font-bold ring-2 ring-blue-100">
              AI
            </div>
            <TypingIndicator />
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Dynamic Suggestions Carousel */}
      <div className="px-3 bg-white border-t border-slate-100">
        <SuggestionChips
          suggestions={suggestions}
          onSelect={(s) => onSendMessage(s)}
          disabled={isLoading}
        />
      </div>

      {/* Input Box Footer */}
      <form onSubmit={handleSubmit} className="p-3 bg-white border-t border-slate-200">
        <div className="flex items-end gap-2 bg-slate-50 border border-slate-300 rounded-xl p-1.5 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-100 transition-all">
          <textarea
            ref={textareaRef}
            rows={1}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Hỏi FixLink về dịch vụ, đặt cọc, tạo đơn..."
            className="flex-1 bg-transparent border-0 resize-none px-2 py-1 text-sm text-slate-800 placeholder-slate-400 focus:outline-none max-h-24 leading-5"
          />

          <button
            type="submit"
            disabled={!inputText.trim() || isLoading}
            className="w-8 h-8 rounded-lg bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed transition-all active:scale-95 flex-shrink-0 shadow-xs"
            title="Gửi tin nhắn"
          >
            <svg
              className="w-4 h-4 transform rotate-90 translate-x-px"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
              />
            </svg>
          </button>
        </div>

        <div className="mt-1.5 flex items-center justify-between text-[10px] text-slate-400 px-1">
          <span>Hỗ trợ khách hàng & thợ FixLink</span>
          <span>Nhấn Enter để gửi</span>
        </div>
      </form>
    </div>
  );
}
