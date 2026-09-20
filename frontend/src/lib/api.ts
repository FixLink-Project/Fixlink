import type { ApiErrorBody, ApiResponse, ServiceArea, ServiceCategory } from './types';

export const API_BASE_URL = '/api/v1';

const TOKEN_KEY = 'fixlink.accessToken';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

/** Lỗi nghiệp vụ do backend trả về, giữ nguyên errorCode để giao diện xử lý riêng. */
export class ApiError extends Error {
  readonly statusCode: number;
  readonly errorCode: string;
  readonly fieldErrors?: ApiErrorBody['errors'];

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.statusCode = body.statusCode;
    this.errorCode = body.errorCode;
    this.fieldErrors = body.errors;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<ApiResponse<T>> {
  const token = getToken();
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError({
      statusCode: 0,
      errorCode: 'NETWORK_ERROR',
      message: 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.'
    });
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new ApiError(
      (payload as ApiErrorBody | null) ?? {
        statusCode: response.status,
        errorCode: 'UNKNOWN_ERROR',
        message: 'Đã có lỗi xảy ra. Vui lòng thử lại.'
      }
    );
  }

  return payload as ApiResponse<T>;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' })
};

export function fetchCategories(): Promise<ApiResponse<ServiceCategory[]>> {
  return api.get<ServiceCategory[]>('/categories');
}

export function fetchAreas(): Promise<ApiResponse<ServiceArea[]>> {
  return api.get<ServiceArea[]>('/areas');
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}
