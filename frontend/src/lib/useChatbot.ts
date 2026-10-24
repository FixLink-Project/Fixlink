import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from './auth';
import {
  chatbotApi,
  clearChatSessionId,
  getOrCreateChatSessionId,
  type ChatActionItem,
  type ChatCardItem,
  type SourceReferenceItem,
} from './chatbotApi';

export interface ChatMessageUI {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: number;
  suggestions?: string[];
  actions?: ChatActionItem[];
  cards?: ChatCardItem[];
  sources?: SourceReferenceItem[];
  ticketId?: string;
  modelUsed?: string;
  feedback?: 'UP' | 'DOWN';
  /** Trợ lý đề nghị chuyển nhân viên CSKH (bot bí, khách bức xúc, hỏi lặp lại). */
  offerHandoff?: boolean;
  /** Lời chào (được làm mới theo trang đang xem khi khách chưa nhắn gì). */
  isWelcome?: boolean;
}

const STORAGE_MESSAGES_PREFIX = 'fixlink.chatbot.history.';
const STORAGE_CSAT_PREFIX = 'fixlink.chatbot.csat.';
/** Số câu trả lời của trợ lý trước khi mời khách chấm điểm hài lòng. */
const CSAT_AFTER_BOT_REPLIES = 3;

const DEFAULT_SUGGESTIONS = [
  'Tư vấn sửa thiết bị đang hỏng',
  'Bảng giá tham khảo dịch vụ',
  'Tiền cọc và thanh toán thế nào?',
  'FixLink là gì, có uy tín không?',
];

function defaultWelcome(): ChatMessageUI {
  return {
    id: 'welcome-' + Date.now(),
    sender: 'bot',
    isWelcome: true,
    text: '👋 **Xin chào!** Mình là **Trợ lý FixLink** – hỗ trợ 24/7.\n\nMình có thể giúp bạn tư vấn sự cố, báo giá tham khảo, chuẩn bị yêu cầu sửa chữa, tra cứu đơn và lịch hẹn, hoặc chuyển bạn tới nhân viên CSKH.',
    timestamp: Date.now(),
    suggestions: DEFAULT_SUGGESTIONS,
  };
}

function readStorage<T>(key: string, fallback: T): T {
  try {
    const saved = sessionStorage.getItem(key);
    return saved ? (JSON.parse(saved) as T) : fallback;
  } catch {
    return fallback;
  }
}

function writeStorage(key: string, value: unknown) {
  try {
    sessionStorage.setItem(key, JSON.stringify(value));
  } catch {
    // ignore
  }
}

export function useChatbot() {
  const location = useLocation();
  const { user } = useAuth();
  const [sessionId, setSessionId] = useState<string>(getOrCreateChatSessionId);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [guestContact, setGuestContact] = useState<string>('');
  const [suggestions, setSuggestions] = useState<string[]>(DEFAULT_SUGGESTIONS);
  const [messages, setMessages] = useState<ChatMessageUI[]>(() =>
    readStorage<ChatMessageUI[]>(STORAGE_MESSAGES_PREFIX + sessionId, [defaultWelcome()])
  );
  const [csatScore, setCsatScore] = useState<number | null>(() =>
    readStorage<number | null>(STORAGE_CSAT_PREFIX + sessionId, null)
  );
  const lastWelcomeKey = useRef<string>('');

  useEffect(() => {
    writeStorage(STORAGE_MESSAGES_PREFIX + sessionId, messages);
  }, [messages, sessionId]);

  const hasUserMessages = messages.some((m) => m.sender === 'user');

  // Gợi ý câu hỏi theo trang và vai trò
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
        // giữ gợi ý mặc định
      });
    return () => {
      active = false;
    };
  }, [location.pathname, user?.role]);

  // Lời chào theo ngữ cảnh trang (ví dụ đang xem chi tiết đơn → hiện ngay trạng thái đơn), chỉ khi khách chưa nhắn gì
  useEffect(() => {
    if (!isOpen || hasUserMessages) return;
    const key = `${location.pathname}|${user?.id ?? 'guest'}`;
    if (lastWelcomeKey.current === key) return;
    lastWelcomeKey.current = key;
    let active = true;
    chatbotApi
      .getWelcome(location.pathname)
      .then((w) => {
        if (!active || !w) return;
        setMessages([
          {
            id: 'welcome-' + Date.now(),
            sender: 'bot',
            isWelcome: true,
            text: w.message,
            cards: w.cards,
            actions: w.actions,
            suggestions: w.suggestions,
            timestamp: Date.now(),
          },
        ]);
        if (w.suggestions?.length) setSuggestions(w.suggestions);
      })
      .catch(() => {
        // giữ lời chào mặc định
      });
    return () => {
      active = false;
    };
  }, [isOpen, hasUserMessages, location.pathname, user?.id]);

  const toggleOpen = useCallback(() => {
    setIsOpen((prev) => !prev);
  }, []);

  const sendMessage = useCallback(
    async (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || isLoading) return;

      setMessages((prev) => [
        ...prev,
        { id: 'user-' + Date.now(), sender: 'user', text: trimmed, timestamp: Date.now() },
      ]);
      setIsLoading(true);
      setError(null);

      try {
        const response = await chatbotApi.sendMessage({
          sessionId,
          message: trimmed,
          route: location.pathname,
          guestContact: !user ? guestContact : undefined,
        });

        setMessages((prev) => [
          ...prev,
          {
            id: 'bot-' + Date.now(),
            sender: 'bot',
            text: response.reply,
            timestamp: Date.now(),
            suggestions: response.suggestions,
            actions: response.actions,
            cards: response.cards,
            sources: response.sources,
            ticketId: response.ticketId,
            modelUsed: response.modelUsed,
            offerHandoff: response.offerHandoff,
          },
        ]);

        if (response.suggestions && response.suggestions.length > 0) {
          setSuggestions(response.suggestions);
        }
      } catch (err: unknown) {
        const errText = err instanceof Error && err.message ? err.message : 'Không thể kết nối tới máy chủ. Vui lòng thử lại sau.';
        setError(errText);
        setMessages((prev) => [
          ...prev,
          {
            id: 'err-' + Date.now(),
            sender: 'bot',
            text: `⚠️ **Lỗi:** ${errText}\n\nBạn vui lòng thử gửi lại sau ít giây. Nếu lỗi vẫn tiếp diễn, hãy tải lại trang hoặc để lại thông tin để gặp nhân viên hỗ trợ.`,
            timestamp: Date.now(),
            offerHandoff: true,
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
        prev.map((m) =>
          // Câu trả lời chưa hữu ích → mời khách gặp nhân viên ngay trên tin nhắn đó
          m.id === messageId ? { ...m, feedback: rating, offerHandoff: m.offerHandoff || rating === 'DOWN' } : m
        )
      );
      try {
        await chatbotApi.submitFeedback({ sessionId, messageId, rating, comment });
      } catch {
        // cập nhật lạc quan là đủ
      }
    },
    [sessionId]
  );

  /** Gặp nhân viên: tạo phiếu hỗ trợ kèm lịch sử hội thoại. Trả về thông báo lỗi nếu thất bại. */
  const requestHandoff = useCallback(
    async (form: { name?: string; contact?: string; issue: string }): Promise<string | null> => {
      try {
        const result = await chatbotApi.requestHandoff({
          sessionId,
          name: form.name?.trim() || undefined,
          contact: (form.contact || guestContact)?.trim() || undefined,
          issue: form.issue.trim(),
        });
        if (form.contact && !user) setGuestContact(form.contact.trim());
        setMessages((prev) => [
          ...prev,
          {
            id: 'handoff-' + Date.now(),
            sender: 'bot',
            text: result.message,
            ticketId: result.ticketCode,
            timestamp: Date.now(),
            suggestions: ['Xem trạng thái phiếu ' + result.ticketCode, 'Tư vấn sửa thiết bị đang hỏng'],
          },
        ]);
        return null;
      } catch (err: unknown) {
        return err instanceof Error && err.message ? err.message : 'Không gửi được yêu cầu, vui lòng thử lại.';
      }
    },
    [sessionId, guestContact, user]
  );

  const botReplies = messages.filter((m) => m.sender === 'bot' && !m.isWelcome && !m.id.startsWith('err-')).length;
  const showCsat = csatScore === null && botReplies >= CSAT_AFTER_BOT_REPLIES;

  const submitCsat = useCallback(
    async (score: 1 | 2 | 3 | 4 | 5, comment?: string) => {
      setCsatScore(score);
      writeStorage(STORAGE_CSAT_PREFIX + sessionId, score);
      try {
        await chatbotApi.submitFeedback({ sessionId, messageId: 'csat', rating: `CSAT_${score}`, comment });
      } catch {
        // cập nhật lạc quan là đủ
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
    setCsatScore(null);
    lastWelcomeKey.current = '';
    setMessages([defaultWelcome()]);
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
    requestHandoff,
    showCsat,
    csatScore,
    submitCsat,
  };
}
