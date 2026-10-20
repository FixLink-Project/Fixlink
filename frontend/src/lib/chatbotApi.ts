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

export interface ChatbotMessageResponse {
  reply: string;
  suggestions: string[];
  actions: ChatActionItem[];
  sources: SourceReferenceItem[];
  needsHuman: boolean;
  ticketId?: string;
  sessionId: string;
  modelUsed: string;
}

export interface ChatbotFeedbackPayload {
  sessionId: string;
  messageId: string;
  rating: 'UP' | 'DOWN';
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
