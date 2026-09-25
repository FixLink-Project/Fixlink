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
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'PENDING';
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

// ── Sprint 2 & RC-30: Repair Requests & Quotations ──

export type RequestStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'BIDDING_OPEN'
  | 'MATCHED_AWAITING_DEPOSIT'
  | 'ASSIGNED'
  | 'INSPECTING'
  | 'AWAITING_COST_APPROVAL'
  | 'IN_PROGRESS'
  | 'AWAITING_ACCEPTANCE'
  | 'COMPLETED'
  | 'CANCELLED';

export type QuotationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export type MediaType = 'IMAGE' | 'VIDEO' | 'DOCUMENT';

export interface MediaItem {
  id?: number;
  url: string;
  mediaType: MediaType;
  uploadedBy?: number;
  createdAt?: string;
}

export type RequestTabCode = 'ALL' | 'AWAITING_QUOTE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface RequestTabItem {
  code: RequestTabCode;
  label: string;
  statuses: RequestStatus[];
}

export interface RepairRequest {
  id: number;
  requestCode: string;
  customerId: number;
  customerName?: string | null;
  technicianId: number | null;
  technicianName?: string | null;
  categoryId: number;
  categoryName: string | null;
  areaId: number | null;
  areaName: string | null;
  status: RequestStatus;
  statusLabel?: string;
  title: string;
  description: string;
  deviceBrand?: string | null;
  deviceModel?: string | null;
  serialNumber?: string | null;
  address?: string;
  addressLine: string;
  latitude: number | null;
  longitude: number | null;
  preferredTime: string | null;
  requestedTime?: string | null;
  agreedPrice: number;
  depositAmount: number;
  budgetRef: number;
  biddingDeadline: string | null;
  cancelReason: string | null;
  selectedQuotationId: number | null;
  mediaUrls: string[];
  media?: MediaItem[];
  quotationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Quotation {
  id: number;
  requestId: number;
  technicianId: number;
  technicianName: string | null;
  avgRating: number | null;
  completedJobs: number | null;
  yearsExperience: number | null;
  solution: string;
  priceLaborVnd: number;
  priceMaterialsVnd: number;
  totalPrice: number;
  inspectionTime: string | null;
  estimatedFinish: string | null;
  note: string | null;
  status: QuotationStatus;
  createdAt: string;
}

export interface WorkProgress {
  id: number;
  fromStatus: RequestStatus;
  toStatus: RequestStatus;
  note: string | null;
  createdAt: string;
}

export interface RepairRequestDetail {
  request: RepairRequest;
  quotations: Quotation[];
  workProgress: WorkProgress[];
}

export interface AcceptQuotationResult {
  requestId: number;
  selectedQuotationId: number;
  technicianName: string;
  agreedPrice: number;
  depositAmount: number;
  status: RequestStatus;
}

// ── Jira RC-48: Appointment Status Lifecycle ──

export type AppointmentStatus = 'CONFIRMED' | 'RESCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface Appointment {
  id: number;
  repairRequestId: number;
  requestCode?: string | null;
  requestTitle?: string | null;
  requestAddress?: string | null;
  customerId: number;
  customerName?: string | null;
  customerPhone?: string | null;
  technicianId: number;
  technicianName?: string | null;
  technicianPhone?: string | null;
  appointmentType?: string | null;
  address?: string | null;
  scheduledDate: string;
  scheduledTime: string;
  actualStartAt?: string | null;
  actualEndAt?: string | null;
  status: AppointmentStatus;
  statusLabel: string;
  notes?: string | null;
  cancelReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAppointmentPayload {
  repairRequestId: number;
  technicianId?: number;
  appointmentType?: string;
  address?: string;
  scheduledDate: string;
  scheduledTime: string;
  notes?: string;
}

export interface RescheduleAppointmentPayload {
  scheduledDate: string;
  scheduledTime: string;
  notes?: string;
}

export interface CancelAppointmentPayload {
  cancelReason: string;
}
