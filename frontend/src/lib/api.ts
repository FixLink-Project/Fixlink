import type {
  ApiErrorBody,
  ApiResponse,
  Appointment,
  CancelAppointmentPayload,
  CreateAppointmentPayload,
  RescheduleAppointmentPayload,
  ServiceArea,
  ServiceCategory
} from './types';

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
  /** Chỉ có khi bị tạm khoá (429): số giây còn lại trước khi được thử lại. */
  readonly remainingSeconds?: number;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.statusCode = body.statusCode;
    this.errorCode = body.errorCode;
    this.fieldErrors = body.errors;
    this.remainingSeconds = body.remainingSeconds;
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

export function fetchRequestTabs(): Promise<ApiResponse<Array<{ code: string; label: string; statuses: string[] }>>> {
  return api.get('/repair-requests/tabs');
}

export function attachRequestMedia(requestId: number, mediaUrls: string[]): Promise<ApiResponse<any>> {
  return api.post(`/repair-requests/${requestId}/media`, { mediaUrls });
}

export function updateRequestStatus(requestId: number, status: string, note?: string): Promise<ApiResponse<any>> {
  return api.patch(`/repair-requests/${requestId}/status`, { status, note });
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

/**
 * Chuẩn hoá khối `errors` của response lỗi về dạng { tên trường: thông báo }.
 *
 * <p>Backend có thể trả về một object, hoặc một mảng { field, message }.
 */
export function toFieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || !error.fieldErrors) return {};
  if (Array.isArray(error.fieldErrors)) {
    return Object.fromEntries(error.fieldErrors.map((e) => [e.field, e.message]));
  }
  return error.fieldErrors;
}

// ── Jira RC-48: Appointment Status Lifecycle API ──

export function createAppointment(payload: CreateAppointmentPayload): Promise<ApiResponse<Appointment>> {
  return api.post<Appointment>('/appointments', payload);
}

export function getAppointmentDetail(id: number): Promise<ApiResponse<Appointment>> {
  return api.get<Appointment>(`/appointments/${id}`);
}

export function fetchAppointmentsForRequest(requestId: number): Promise<ApiResponse<Appointment[]>> {
  return api.get<Appointment[]>(`/repair-requests/${requestId}/appointments`);
}

export function fetchMyAppointments(): Promise<ApiResponse<Appointment[]>> {
  return api.get<Appointment[]>('/appointments/my');
}

export function rescheduleAppointment(id: number, payload: RescheduleAppointmentPayload): Promise<ApiResponse<Appointment>> {
  return api.patch<Appointment>(`/appointments/${id}/reschedule`, payload);
}

export function completeAppointment(id: number, note?: string): Promise<ApiResponse<Appointment>> {
  const query = note ? `?note=${encodeURIComponent(note)}` : '';
  return api.patch<Appointment>(`/appointments/${id}/complete${query}`);
}

export function cancelAppointment(id: number, payload: CancelAppointmentPayload): Promise<ApiResponse<Appointment>> {
  return api.patch<Appointment>(`/appointments/${id}/cancel`, payload);
}

