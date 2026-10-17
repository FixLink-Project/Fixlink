import { useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from './auth';
import {
  chatbotApi,
  clearChatSessionId,
  getOrCreateChatSessionId,
  type ChatActionItem,
  type SourceReferenceItem,
} from './chatbotApi';

export interface ChatMessageUI {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: number;
  suggestions?: string[];
  actions?: ChatActionItem[];
  sources?: SourceReferenceItem[];
  ticketId?: string;
  modelUsed?: string;
  feedback?: 'UP' | 'DOWN';
}

const STORAGE_MESSAGES_PREFIX = 'fixlink.chatbot.history.';

export function useChatbot() {
  const location = useLocation();
  const { user } = useAuth();
  const [sessionId, setSessionId] = useState<string>(getOrCreateChatSessionId);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [guestContact, setGuestContact] = useState<string>('');
  const [suggestions, setSuggestions] = useState<string[]>([
    'Cách đăng yêu cầu sửa chữa?',
    'Phí dịch vụ và đặt cọc tính thế nào?',
    'Làm thế nào để đăng ký làm thợ?',
    'Tra cứu trạng thái đơn hàng của tôi',
  ]);

  // Read saved messages from sessionStorage
  const [messages, setMessages] = useState<ChatMessageUI[]>(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_MESSAGES_PREFIX + sessionId);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch {
      // ignore
    }
    return [
      {
        id: 'welcome-1',
        sender: 'bot',
        text: '👋 **Xin chào!** Tôi là trợ lý hỗ trợ khách hàng FixLink.\n\nTôi có thể hỗ trợ bạn:\n- Hướng dẫn tạo yêu cầu sửa chữa nhanh\n- Tra cứu trạng thái đơn & lịch hẹn thợ\n- Báo giá, chính sách đặt cọc minh bạch\n- Tạo phiếu hỗ trợ sự cố với ban quản trị\n\nBạn cần hỗ trợ điều gì hôm nay?',
        timestamp: Date.now(),
        suggestions: [
          'Cách đăng yêu cầu sửa chữa?',
          'Phí dịch vụ và đặt cọc tính thế nào?',
          'Làm thế nào để đăng ký làm thợ?',
          'Tra cứu trạng thái đơn hàng của tôi',
        ],
      },
    ];
  });

  // Persist messages
  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_MESSAGES_PREFIX + sessionId, JSON.stringify(messages));
    } catch {
      // ignore
    }
  }, [messages, sessionId]);

  // Fetch suggestions when route or role changes
  useEffect(() => {
    let active = true;
    chatbotApi
      .getSuggestions(location.pathname)
      .then((items) => {
        if (active && items && items.length > 0) {
          setSuggestions(items);
        }
      })
      .catch(() => {
        // use default fallback suggestions
      });
    return () => {
      active = false;
    };
  }, [location.pathname, user?.role]);

  const toggleOpen = useCallback(() => {
    setIsOpen((prev) => !prev);
  }, []);

  const sendMessage = useCallback(
    async (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || isLoading) return;

      const userMsg: ChatMessageUI = {
        id: 'user-' + Date.now(),
        sender: 'user',
        text: trimmed,
        timestamp: Date.now(),
      };

      setMessages((prev) => [...prev, userMsg]);
      setIsLoading(true);
      setError(null);

      try {
        const response = await chatbotApi.sendMessage({
          sessionId,
          message: trimmed,
          route: location.pathname,
          guestContact: !user ? guestContact : undefined,
        });

        const botMsg: ChatMessageUI = {
          id: 'bot-' + Date.now(),
          sender: 'bot',
          text: response.reply,
          timestamp: Date.now(),
          suggestions: response.suggestions,
          actions: response.actions,
          sources: response.sources,
          ticketId: response.ticketId,
          modelUsed: response.modelUsed,
        };

        setMessages((prev) => [...prev, botMsg]);

        if (response.suggestions && response.suggestions.length > 0) {
          setSuggestions(response.suggestions);
        }
      } catch (err: any) {
        const errText = err?.message || 'Không thể kết nối tới máy chủ. Vui lòng thử lại sau.';
        setError(errText);
        setMessages((prev) => [
          ...prev,
          {
            id: 'err-' + Date.now(),
            sender: 'bot',
            text: `⚠️ **Lỗi:** ${errText}\n\nBạn có thể thử đặt lại câu hỏi ngắn hơn hoặc liên hệ tổng đài FixLink 1900-xxxx.`,
            timestamp: Date.now(),
          },
        ]);
      } finally {
        setIsLoading(false);
      }
    },
    [sessionId, location.pathname, user, guestContact, isLoading]
  );

  const sendFeedback = useCallback(
    async (messageId: string, rating: 'UP' | 'DOWN', comment?: string) => {
      setMessages((prev) =>
        prev.map((m) => (m.id === messageId ? { ...m, feedback: rating } : m))
      );
      try {
        await chatbotApi.submitFeedback({
          sessionId,
          messageId,
          rating,
          comment,
        });
      } catch {
        // optimistic update is fine
      }
    },
    [sessionId]
  );

  const resetSession = useCallback(async () => {
    try {
      await chatbotApi.resetSession(sessionId);
    } catch {
      // ignore
    }
    clearChatSessionId();
    const newSid = getOrCreateChatSessionId();
    setSessionId(newSid);
    setMessages([
      {
        id: 'welcome-new-' + Date.now(),
        sender: 'bot',
        text: 'Cuộc trò chuyện đã được làm mới. Tôi có thể hỗ trợ gì cho bạn?',
        timestamp: Date.now(),
        suggestions: [
          'Cách đăng yêu cầu sửa chữa?',
          'Phí dịch vụ và đặt cọc tính thế nào?',
          'Làm thế nào để đăng ký làm thợ?',
        ],
      },
    ]);
  }, [sessionId]);

  return {
    isOpen,
    setIsOpen,
    toggleOpen,
    messages,
    isLoading,
    error,
    suggestions,
    guestContact,
    setGuestContact,
    sendMessage,
    sendFeedback,
    resetSession,
  };
}
