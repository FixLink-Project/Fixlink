/** Khối phân trang backend trả kèm mọi danh sách. */
export interface PageMeta {
  currentPage: number;
  limit: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/** Lớp bọc chuẩn cho mọi response thành công. */
export interface ApiResponse<T> {
  statusCode: number;
  message: string;
  data: T;
  meta?: PageMeta;
}

/** Lớp bọc chuẩn cho mọi response lỗi. */
export interface ApiErrorBody {
  statusCode: number;
  errorCode: string;
  message: string;
  errors?: Record<string, string> | Array<{ field: string; message: string }>;
  /** Chỉ có khi 429: số giây còn lại trước khi được thử đăng nhập lại. */
  remainingSeconds?: number;
}

export interface ServiceCategory {
  id: number;
  code: string;
  name: string;
  description: string | null;
  iconUrl: string | null;
  displayOrder: number;
  isActive: boolean;
}

export interface ServiceArea {
  id: number;
  code: string;
  name: string;
  city: string;
}

export type Role = 'CUSTOMER' | 'TECHNICIAN' | 'STAFF' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type VerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AuthUser {
  id: string;
  username: string;
  role: Role;
  status: UserStatus;
  fullName: string | null;
  avatarUrl: string | null;
  isVerified: boolean;
  verificationStatus: VerificationStatus | null;
}

export interface AuthResult {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}
