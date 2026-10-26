import type {
  AcceptQuotationResult,
  AdditionalCost,
  ApiErrorBody,
  ApiResponse,
  Appointment,
  Bill,
  CancelAppointmentPayload,
  CreateAppointmentPayload,
  Payment,
  PaymentResult,
  PriceQuote,
  Quotation,
  RescheduleAppointmentPayload,
  ServiceArea,
  ServiceCategory
} from './types';
import { clearSession, getSession, saveSession } from './session';

export const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

/** Token của phiên trong tab hiện tại (mỗi tab giữ phiên riêng, xem session.ts). */
export function getToken(): string | null {
  return getSession().accessToken;
}

export function setToken(token: string | null): void {
  if (token) saveSession({ accessToken: token });
  else clearSession();
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
    // Khi phiên đăng nhập hết hạn/bị thu hồi (ĐÃ có token mà vẫn 401) thì xoá
    // token và đưa về trang đăng nhập. CHỈ làm vậy khi trước đó có token:
    // khách vãng lai (chưa đăng nhập) xem trang công khai như "/" vẫn có thể gọi
    // một số API cần auth (vd danh mục) và nhận 401 — không được đá họ về /login,
    // để trang tự xử lý/ hiển thị dữ liệu thay thế. Bỏ qua chính request login.
    const isLoginCall = path.startsWith('/auth/login');
    // Chỉ xử lý khi token bị từ chối vẫn là phiên hiện tại của tab: request cũ
    // trả về muộn sau khi tab đã đăng nhập tài khoản khác thì bỏ qua.
    if (response.status === 401 && !isLoginCall && token && getToken() === token) {
      // Thợ đăng nhập lại ở trang dành cho thợ, các vai trò khác ở trang chung.
      const loginPath = getSession().user?.role === 'TECHNICIAN' ? '/dang-nhap-tho' : '/dang-nhap';
      setToken(null);
      const p = typeof window !== 'undefined' ? window.location.pathname : '';
      if (p && !p.startsWith('/login') && !p.startsWith('/dang-nhap') && p !== '/') {
        window.location.assign(`${loginPath}?phien=het-han`);
      }
    }
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

export function updateRequestStatus(
  requestId: number,
  status: string,
  note?: string,
  newAgreedPrice?: number
): Promise<ApiResponse<any>> {
  return api.patch(`/repair-requests/${requestId}/status`, { status, note, newAgreedPrice });
}

export interface CreateQuotationPayload {
  solution: string;
  priceLaborVnd: number;
  priceMaterialsVnd: number;
  inspectionTime?: string;
  estimatedFinish?: string;
  note?: string;
}

export function createQuotation(requestId: number, data: CreateQuotationPayload): Promise<ApiResponse<Quotation>> {
  return api.post<Quotation>(`/repair-requests/${requestId}/quotations`, data);
}

export function updateQuotation(requestId: number, quotationId: number, data: CreateQuotationPayload): Promise<ApiResponse<Quotation>> {
  return api.put<Quotation>(`/repair-requests/${requestId}/quotations/${quotationId}`, data);
}

export function withdrawQuotation(requestId: number, quotationId: number): Promise<ApiResponse<void>> {
  return api.delete<void>(`/repair-requests/${requestId}/quotations/${quotationId}`);
}

export function acceptQuotation(requestId: number, quotationId: number): Promise<ApiResponse<AcceptQuotationResult>> {
  return api.post<AcceptQuotationResult>(`/repair-requests/${requestId}/quotations/${quotationId}/accept`);
}

export function switchTechnician(requestId: number, quotationId: number): Promise<ApiResponse<AcceptQuotationResult>> {
  return api.post<AcceptQuotationResult>(`/repair-requests/${requestId}/switch-technician/${quotationId}`);
}

/**
 * Thợ nhận việc (mô hình "ai nhận trước được trước").
 *
 * Lỗi 409 với errorCode JOB_ALREADY_TAKEN nghĩa là thợ khác vừa nhận trước.
 */
export function applyForJob(requestId: number): Promise<ApiResponse<AcceptQuotationResult>> {
  return api.post<AcceptQuotationResult>(`/repair-requests/${requestId}/apply`);
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

export function approveAppointment(id: number): Promise<ApiResponse<Appointment>> {
  return api.patch<Appointment>(`/appointments/${id}/approve`);
}

export function rejectAppointment(id: number, payload: CancelAppointmentPayload): Promise<ApiResponse<Appointment>> {
  return api.patch<Appointment>(`/appointments/${id}/reject`, payload);
}

export function cancelAppointment(id: number, payload: CancelAppointmentPayload): Promise<ApiResponse<Appointment>> {
  return api.patch<Appointment>(`/appointments/${id}/cancel`, payload);
}

// ── Payment QR + Price Quote + Additional Cost + Billing ──

export function createDepositPayment(requestId: number): Promise<ApiResponse<PaymentResult>> {
  return api.post<PaymentResult>(`/payments/deposit/${requestId}`);
}

export function createFinalPayment(requestId: number): Promise<ApiResponse<PaymentResult>> {
  return api.post<PaymentResult>(`/payments/final/${requestId}`);
}

export function confirmPayment(paymentId: number): Promise<ApiResponse<Bill>> {
  return api.post<Bill>(`/payments/${paymentId}/confirm`);
}

export function fetchPayments(requestId: number): Promise<ApiResponse<Payment[]>> {
  return api.get<Payment[]>(`/payments/request/${requestId}`);
}

export function fetchBills(requestId: number): Promise<ApiResponse<Bill[]>> {
  return api.get<Bill[]>(`/payments/bills/request/${requestId}`);
}

export function fetchBill(billId: number): Promise<ApiResponse<Bill>> {
  return api.get<Bill>(`/payments/bills/${billId}`);
}

export function submitPriceQuote(requestId: number, price: number, note?: string): Promise<ApiResponse<PriceQuote>> {
  return api.post<PriceQuote>('/price-quotes', { requestId, price, note });
}

export function acceptPriceQuote(quoteId: number): Promise<ApiResponse<void>> {
  return api.post<void>(`/price-quotes/${quoteId}/accept`);
}

export function fetchPriceQuotes(requestId: number): Promise<ApiResponse<PriceQuote[]>> {
  return api.get<PriceQuote[]>(`/price-quotes/request/${requestId}`);
}

export function addAdditionalCost(requestId: number, description: string, amount: number): Promise<ApiResponse<AdditionalCost>> {
  return api.post<AdditionalCost>('/additional-costs', { requestId, description, amount });
}

export function approveAdditionalCost(costId: number): Promise<ApiResponse<AdditionalCost>> {
  return api.post<AdditionalCost>(`/additional-costs/${costId}/approve`);
}

export function rejectAdditionalCost(costId: number): Promise<ApiResponse<AdditionalCost>> {
  return api.post<AdditionalCost>(`/additional-costs/${costId}/reject`);
}

export function fetchAdditionalCosts(requestId: number): Promise<ApiResponse<AdditionalCost[]>> {
  return api.get<AdditionalCost[]>(`/additional-costs/request/${requestId}`);
}

export function getBillHtmlUrl(billId: number): string {
  return `${API_BASE_URL}/payments/bills/${billId}/html`;
}

