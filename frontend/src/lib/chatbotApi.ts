import { API_BASE_URL, getToken } from './api';

export interface ChatbotMessagePayload {
  sessionId: string;
  message: string;
  route?: string;
  guestContact?: string;
}

export interface ChatActionItem {
  type: string;
  label: string;
  url: string;
}

export interface SourceReferenceItem {
  documentName: string;
  section: string;
}

export interface ChatCardField {
  label: string;
  value: string;
}

/** Thẻ thông tin trực quan dựng từ dữ liệu thật của hệ thống (đơn, lịch hẹn, bảng giá...). */
export interface ChatCardItem {
  type: 'request' | 'appointment' | 'price_list' | 'deposit' | 'technician' | 'job' | 'draft' | string;
  title: string;
  subtitle?: string;
  badge?: string;
  badgeTone?: 'info' | 'success' | 'warning' | 'danger' | 'neutral' | string;
  fields?: ChatCardField[];
  steps?: string[];
  currentStep?: number;
  note?: string;
  url?: string;
  urlLabel?: string;
}

export interface ChatbotWelcomeResponse {
  message: string;
  suggestions: string[];
  cards: ChatCardItem[];
  actions: ChatActionItem[];
}

export interface ChatbotHandoffPayload {
  sessionId: string;
  name?: string;
  contact?: string;
  issue: string;
}

export interface ChatbotHandoffResult {
  ticketCode: string;
  priority: string;
  message: string;
}

export interface ChatbotMessageResponse {
  reply: string;
  suggestions: string[];
  actions: ChatActionItem[];
  sources: SourceReferenceItem[];
  needsHuman: boolean;
  ticketId?: string;
  sessionId: string;
  modelUsed: string;
  cards?: ChatCardItem[];
  offerHandoff?: boolean;
}

export interface ChatbotFeedbackPayload {
  sessionId: string;
  messageId: string;
  rating: 'UP' | 'DOWN' | `CSAT_${1 | 2 | 3 | 4 | 5}`;
  comment?: string;
}

const SESSION_KEY = 'fixlink.chatbot.sessionId';

export function getOrCreateChatSessionId(): string {
  try {
    let sid = sessionStorage.getItem(SESSION_KEY);
    if (!sid) {
      sid = 'ses-' + Math.random().toString(36).substring(2, 10) + '-' + Date.now().toString(36);
      sessionStorage.setItem(SESSION_KEY, sid);
    }
    return sid;
  } catch {
    return 'ses-fallback-' + Date.now();
  }
}

export function clearChatSessionId(): void {
  try {
    sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // ignore
  }
}

async function requestChatbot<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE_URL}/chatbot${path}`, {
    ...options,
    headers,
  });

  if (!res.ok) {
    let errorMsg = 'Có lỗi xảy ra khi liên lạc với trợ lý ảo';
    try {
      const errJson = await res.json();
      if (errJson && errJson.message) {
        errorMsg = errJson.message;
      }
    } catch {
      // ignore
    }
    throw new Error(errorMsg);
  }

  const json = await res.json().catch(() => null);
  if (json && typeof json === 'object' && 'data' in json && 'statusCode' in json) {
    return json.data as T;
  }
  return json as T;
}

export const chatbotApi = {
  sendMessage(payload: ChatbotMessagePayload): Promise<ChatbotMessageResponse> {
    return requestChatbot<ChatbotMessageResponse>('/messages', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  getSuggestions(route: string): Promise<string[]> {
    const encoded = encodeURIComponent(route || '/');
    return requestChatbot<string[]>(`/suggestions?route=${encoded}`, {
      method: 'GET',
    });
  },

  getWelcome(route: string): Promise<ChatbotWelcomeResponse> {
    const encoded = encodeURIComponent(route || '/');
    return requestChatbot<ChatbotWelcomeResponse>(`/welcome?route=${encoded}`, { method: 'GET' });
  },

  requestHandoff(payload: ChatbotHandoffPayload): Promise<ChatbotHandoffResult> {
    return requestChatbot<ChatbotHandoffResult>('/handoff', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  submitFeedback(payload: ChatbotFeedbackPayload): Promise<{ success: boolean }> {
    return requestChatbot<{ success: boolean }>('/feedback', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  resetSession(sessionId: string): Promise<{ success: boolean }> {
    return requestChatbot<{ success: boolean }>(`/sessions/${encodeURIComponent(sessionId)}`, {
      method: 'DELETE',
    });
  },
};
