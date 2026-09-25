import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MyRepairRequestsPage from './MyRepairRequestsPage';

const { get } = vi.hoisted(() => ({ get: vi.fn() }));

vi.mock('../lib/api', () => ({
  api: { get },
  formatCurrency: (amount: number) => `${amount} ₫`
}));

vi.mock('../components/DashboardLayout', () => ({
  default: ({ title, description, actions, children }: React.PropsWithChildren<{ title: string; description?: string; actions?: React.ReactNode }>) => (
    <main><h1>{title}</h1><p>{description}</p>{actions}{children}</main>
  )
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

function renderPage() {
  return render(<MemoryRouter><MyRepairRequestsPage /></MemoryRouter>);
}

const request = {
  id: 35,
  requestCode: 'RC-35',
  customerId: 10,
  technicianId: null,
  categoryId: 2,
  categoryName: 'Điện lạnh',
  areaId: null,
  areaName: null,
  status: 'IN_PROGRESS' as const,
  statusLabel: 'Đang thực hiện',
  title: 'Customer View My Repair Requests',
  description: 'Theo dõi yêu cầu sửa chữa',
  addressLine: 'Quận 1',
  latitude: null,
  longitude: null,
  preferredTime: null,
  agreedPrice: 0,
  depositAmount: 0,
  budgetRef: 500000,
  biddingDeadline: null,
  cancelReason: null,
  selectedQuotationId: null,
  mediaUrls: [],
  quotationCount: 2,
  createdAt: '2026-09-20T12:00:00Z',
  updatedAt: '2026-09-20T12:00:00Z'
};

describe('MyRepairRequestsPage', () => {
  it('loads and displays the customer repair request list with pagination metadata', async () => {
    get.mockResolvedValueOnce({ data: [request], meta: { currentPage: 1, limit: 10, totalItems: 1, totalPages: 1, hasNext: false, hasPrevious: false } });

    renderPage();

    expect(await screen.findByRole('heading', { name: request.title })).toBeInTheDocument();
    expect(screen.getByText('RC-35')).toBeInTheDocument();
    expect(screen.getByText('2 báo giá')).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith('/repair-requests?page=1&limit=10&tab=ALL');
  });

  it('requests a different status tab and shows a useful empty state', async () => {
    get.mockResolvedValueOnce({ data: [request] }).mockResolvedValueOnce({ data: [] });

    renderPage();
    await screen.findByRole('heading', { name: request.title });
    fireEvent.click(screen.getByRole('tab', { name: 'Đã hủy' }));

    expect(await screen.findByText('Chưa có yêu cầu nào trong mục “đã hủy”')).toBeInTheDocument();
    expect(get).toHaveBeenLastCalledWith('/repair-requests?page=1&limit=10&tab=CANCELLED');
  });

  it('lets the customer retry after a list request fails', async () => {
    get.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ data: [request] });

    renderPage();
    expect(await screen.findByText(/Không thể tải danh sách/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Thử tải lại' }));

    await waitFor(() => expect(get).toHaveBeenCalledTimes(2));
    expect(await screen.findByRole('heading', { name: request.title })).toBeInTheDocument();
  });
});
