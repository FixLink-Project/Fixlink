import { useEffect, useState } from 'react';
import { useChatbot } from '../../lib/useChatbot';
import ChatWindow from './ChatWindow';

export default function ChatWidget() {
  const {
    isOpen,
    toggleOpen,
    setIsOpen,
    messages,
    isLoading,
    suggestions,
    guestContact,
    setGuestContact,
    sendMessage,
    sendFeedback,
    resetSession,
  } = useChatbot();

  const [hasInteracted, setHasInteracted] = useState(false);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, setIsOpen]);

  const handleOpenClick = () => {
    setHasInteracted(true);
    toggleOpen();
  };

  return (
    <>
      {/* Floating Action Button */}
      <button
        type="button"
        onClick={handleOpenClick}
        aria-label="Mở cửa sổ hỗ trợ FixLink AI"
        className="fixed bottom-5 right-5 z-50 flex items-center gap-2.5 px-4 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-full shadow-elevated hover:shadow-2xl transition-all duration-200 active:scale-95 focus:outline-none focus:ring-4 focus:ring-blue-300/50 group"
      >
        <div className="relative">
          {/* Chat Icon */}
          <svg
            className="w-6 h-6 transform transition-transform duration-200 group-hover:scale-110"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
            />
          </svg>

          {/* Pulse notification dot */}
          {!hasInteracted && (
            <span className="absolute -top-1 -right-1 flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
            </span>
          )}
        </div>

        <span className="hidden sm:inline font-semibold text-sm tracking-tight pr-1">
          Hỗ trợ AI
        </span>
      </button>

      {/* Embedded Chat Window */}
      <ChatWindow
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        messages={messages}
        isLoading={isLoading}
        suggestions={suggestions}
        guestContact={guestContact}
        onSetGuestContact={setGuestContact}
        onSendMessage={sendMessage}
        onFeedback={sendFeedback}
        onResetSession={resetSession}
      />
    </>
  );
}
