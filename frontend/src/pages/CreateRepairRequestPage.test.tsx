import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CreateRepairRequestPage from './CreateRepairRequestPage';

const mocks = vi.hoisted(() => ({
  post: vi.fn(),
  fetchCategories: vi.fn(),
  fetchAreas: vi.fn()
}));

vi.mock('../lib/api', () => ({
  ApiError: class ApiError extends Error {},
  api: { post: mocks.post },
  fetchCategories: mocks.fetchCategories,
  fetchAreas: mocks.fetchAreas,
  formatCurrency: (amount: number) => `${amount} ₫`,
  toFieldErrors: () => ({})
}));

vi.mock('../components/DashboardLayout', () => ({
  default: ({ title, children }: React.PropsWithChildren<{ title: string }>) => <main><h1>{title}</h1>{children}</main>
}));

vi.mock('../components/Card', () => ({
  default: ({ title, children }: React.PropsWithChildren<{ title: string }>) => <section><h2>{title}</h2>{children}</section>
}));

vi.mock('../components/TextField', () => ({
  default: ({ label, value, onChange, ...props }: { label: string; value: string; onChange: React.ChangeEventHandler<HTMLInputElement> }) => (
    <label>{label}<input value={value} onChange={onChange} {...props} /></label>
  )
}));

vi.mock('../components/TextArea', () => ({
  default: ({ label, value, onChange, ...props }: { label: string; value: string; onChange: React.ChangeEventHandler<HTMLTextAreaElement> }) => (
    <label>{label}<textarea value={value} onChange={onChange} {...props} /></label>
  )
}));

vi.mock('../components/MultiImageUploadField', () => ({
  default: ({ label, onChange }: { label: string; onChange: (urls: string[]) => void }) => (
    <button type="button" onClick={() => onChange(['https://example.test/device.jpg'])}>{label}</button>
  )
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('CreateRepairRequestPage RC-36', () => {
  it('submits device details and attached photo URLs with the repair request', async () => {
    mocks.fetchCategories.mockResolvedValue({ data: [{ id: 1, name: 'Điện lạnh', isActive: true }] });
    mocks.fetchAreas.mockResolvedValue({ data: [] });
    mocks.post.mockResolvedValue({ data: {} });

    render(<MemoryRouter><CreateRepairRequestPage /></MemoryRouter>);
    fireEvent.click(await screen.findByRole('button', { name: /Điện lạnh/ }));
    fireEvent.click(screen.getAllByRole('button').find((button) => button.textContent?.includes('Ti'))!);

    const textboxes = screen.getAllByRole('textbox');
    fireEvent.change(textboxes[0], { target: { value: 'Máy lạnh không mát' } });
    fireEvent.change(textboxes[1], { target: { value: 'Máy chạy nhưng không làm lạnh.' } });
    fireEvent.change(textboxes[5], { target: { value: '123 Đường Demo, Quận 1' } });
    fireEvent.change(screen.getByLabelText('Thương hiệu'), { target: { value: 'Daikin' } });
    fireEvent.change(screen.getByLabelText('Model'), { target: { value: 'FTKF35' } });
    fireEvent.change(screen.getByLabelText('Số serial'), { target: { value: 'SN-12345' } });
    expect(textboxes[0]).toHaveValue('Máy lạnh không mát');
    expect(textboxes[1]).toHaveValue('Máy chạy nhưng không làm lạnh.');
    expect(textboxes[5]).toHaveValue('123 Đường Demo, Quận 1');
    fireEvent.click(screen.getAllByRole('button').find((button) => button.textContent?.includes('6'))!);
    fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra thông tin →' }));
    await screen.findByText('Xác nhận thông tin yêu cầu');
    const buttons = screen.getAllByRole('button');
    fireEvent.click(buttons[buttons.length - 1]);

    await waitFor(() => expect(mocks.post).toHaveBeenCalledWith('/repair-requests', expect.objectContaining({
      deviceBrand: 'Daikin',
      deviceModel: 'FTKF35',
      serialNumber: 'SN-12345',
      mediaUrls: ['https://example.test/device.jpg']
    })));
  });
});
