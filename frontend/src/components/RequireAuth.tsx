import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { HOME_BY_ROLE, useAuth } from '../lib/auth';
import type { Role } from '../lib/types';

interface RequireAuthProps {
  /** Vai trò được phép vào; bỏ trống nghĩa là chỉ cần đăng nhập. */
  roles?: Role[];
  children: ReactNode;
}

export default function RequireAuth({ roles, children }: RequireAuthProps) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    // Nhớ trang đang muốn vào để quay lại sau khi đăng nhập.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (roles && !roles.includes(user.role)) {
    // Đã đăng nhập nhưng sai vai trò: đưa về đúng trang chủ của vai trò đó.
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/'} replace />;
  }

  return <>{children}</>;
}
