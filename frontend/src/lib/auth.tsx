import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api } from './api';
import {
  SESSION_CLEARED_EVENT,
  clearSession,
  getSession,
  installNewTabHandoff,
  saveSession
} from './session';
import type { AuthResult, AuthUser, Role } from './types';

/** Trang mặc định của mỗi vai trò sau khi đăng nhập. */
export const HOME_BY_ROLE: Record<Role, string> = {
  CUSTOMER: '/khach-hang',
  TECHNICIAN: '/tho',
  STAFF: '/quan-tri',
  ADMIN: '/quan-tri'
};

interface AuthContextValue {
  user: AuthUser | null;
  signIn: (username: string, password: string, role?: string) => Promise<AuthUser>;
  signOut: () => void;
  updateUser: (patch: Partial<AuthUser>) => void;
  setSession: (result: AuthResult) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Phiên được giữ riêng theo tab, nên đăng nhập ở tab khác không làm đổi user ở đây.
  const [user, setUser] = useState<AuthUser | null>(() => getSession().user);

  useEffect(() => {
    // Phiên hết hạn (API trả 401) thì giao diện cũng phải về trạng thái chưa đăng nhập.
    const onCleared = () => setUser(null);
    window.addEventListener(SESSION_CLEARED_EVENT, onCleared);
    const uninstall = installNewTabHandoff();
    return () => {
      window.removeEventListener(SESSION_CLEARED_EVENT, onCleared);
      uninstall();
    };
  }, []);

  const startSession = useCallback((result: AuthResult) => {
    saveSession({
      accessToken: result.accessToken,
      refreshToken: result.refreshToken,
      user: result.user
    });
    setUser(result.user);
  }, []);

  const signIn = useCallback(
    async (username: string, password: string, role?: string) => {
      const body: Record<string, string> = { username, password };
      if (role) body.role = role;
      const res = await api.post<AuthResult>('/auth/login', body);
      startSession(res.data);
      return res.data.user;
    },
    [startSession]
  );

  const signOut = useCallback(() => {
    // Gọi thu hồi token ở máy chủ, nhưng không chặn người dùng nếu lỗi mạng.
    // Token được gắn vào request ngay khi gọi, nên xoá phiên sau đó vẫn an toàn.
    api.post('/auth/logout').catch(() => undefined);
    clearSession();
    setUser(null);
  }, []);

  const updateUser = useCallback(
    (patch: Partial<AuthUser>) => {
      setUser((current) => {
        if (!current) return current;
        const next = { ...current, ...patch };
        saveSession({ user: next });
        return next;
      });
    },
    []
  );

  const value = useMemo(
    () => ({ user, signIn, signOut, updateUser, setSession: startSession }),
    [user, signIn, signOut, updateUser, startSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth phải được dùng bên trong AuthProvider');
  return ctx;
}
