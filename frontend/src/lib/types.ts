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
