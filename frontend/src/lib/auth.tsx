import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { api, setToken } from './api';
import type { AuthResult, AuthUser, Role } from './types';

const USER_KEY = 'fixlink.user';
const REFRESH_KEY = 'fixlink.refreshToken';

/** Trang mặc định của mỗi vai trò sau khi đăng nhập. */
export const HOME_BY_ROLE: Record<Role, string> = {
  CUSTOMER: '/khach-hang',
  TECHNICIAN: '/tho',
  STAFF: '/quan-tri',
  ADMIN: '/quan-tri'
};

function readStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

interface AuthContextValue {
  user: AuthUser | null;
  signIn: (username: string, password: string) => Promise<AuthUser>;
  signOut: () => void;
  updateUser: (patch: Partial<AuthUser>) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser);

  const persist = useCallback((next: AuthUser | null) => {
    setUser(next);
    try {
      if (next) localStorage.setItem(USER_KEY, JSON.stringify(next));
      else localStorage.removeItem(USER_KEY);
    } catch {
      // Chế độ riêng tư có thể chặn localStorage; phiên vẫn dùng được trong tab hiện tại.
    }
  }, []);

  const signIn = useCallback(
    async (username: string, password: string) => {
      const res = await api.post<AuthResult>('/auth/login', { username, password });
      setToken(res.data.accessToken);
      try {
        localStorage.setItem(REFRESH_KEY, res.data.refreshToken);
      } catch {
        // Không lưu được refresh token thì phiên chỉ sống tới khi access token hết hạn.
      }
      persist(res.data.user);
      return res.data.user;
    },
    [persist]
  );

  const signOut = useCallback(() => {
    // Gọi thu hồi token ở máy chủ, nhưng không chặn người dùng nếu lỗi mạng.
    api.post('/auth/logout').catch(() => undefined);
    setToken(null);
    try {
      localStorage.removeItem(REFRESH_KEY);
    } catch {
      // Bỏ qua: phiên phía client đã bị xoá bên dưới.
    }
    persist(null);
  }, [persist]);

  const updateUser = useCallback(
    (patch: Partial<AuthUser>) => {
      setUser((current) => {
        if (!current) return current;
        const next = { ...current, ...patch };
        try {
          localStorage.setItem(USER_KEY, JSON.stringify(next));
        } catch {
          // Không lưu được thì vẫn giữ trong bộ nhớ của tab.
        }
        return next;
      });
    },
    []
  );

  const value = useMemo(
    () => ({ user, signIn, signOut, updateUser }),
    [user, signIn, signOut, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth phải được dùng bên trong AuthProvider');
  return ctx;
}
