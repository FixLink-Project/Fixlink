import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ChatCards from '../src/components/chatbot/ChatCards';
import HandoffPanel from '../src/components/chatbot/HandoffPanel';
import MessageBubble from '../src/components/chatbot/MessageBubble';

describe('Thẻ thông tin trong khung chat', () => {
  it('hiển thị thẻ đơn sửa chữa với trạng thái, thông tin và liên kết chi tiết', () => {
    render(
      <BrowserRouter>
        <ChatCards
          cards={[
            {
              type: 'request',
              title: 'Sửa tủ lạnh Hitachi',
              badge: 'Chờ khách nghiệm thu',
              badgeTone: 'warning',
              steps: ['Đăng đơn', 'Có thợ nhận', 'Khảo sát', 'Sửa chữa', 'Chờ nghiệm thu', 'Hoàn thành'],
              currentStep: 4,
              fields: [
                { label: 'Mã đơn', value: 'REQ-20261023-A1B2C3' },
                { label: 'Tiền cọc (30%)', value: '120.000 đ' },
              ],
              note: 'Kiểm tra thiết bị rồi bấm Nghiệm thu.',
              url: '/yeu-cau-cua-toi/26',
              urlLabel: 'Xem chi tiết đơn',
            },
          ]}
        />
      </BrowserRouter>
    );

    expect(screen.getByText('Sửa tủ lạnh Hitachi')).toBeInTheDocument();
    expect(screen.getByText('Chờ khách nghiệm thu')).toBeInTheDocument();
    expect(screen.getByText('REQ-20261023-A1B2C3')).toBeInTheDocument();
    expect(screen.getByLabelText('Tiến độ: Chờ nghiệm thu')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Xem chi tiết đơn/ })).toHaveAttribute('href', '/yeu-cau-cua-toi/26');
  });

  it('hiện lời mời gặp nhân viên khi trợ lý đề nghị chuyển tiếp', () => {
    const onHandoff = vi.fn();
    render(
      <BrowserRouter>
        <MessageBubble
          message={{ id: 'b1', sender: 'bot', text: 'Mình chưa xử lý được yêu cầu này.', timestamp: Date.now(), offerHandoff: true }}
          onFeedback={vi.fn()}
          onRequestHandoff={onHandoff}
        />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /Gặp nhân viên/ }));
    expect(onHandoff).toHaveBeenCalled();
  });
});

describe('Form gặp nhân viên', () => {
  it('bắt buộc khách vãng lai để lại liên hệ trước khi gửi', async () => {
    const onSubmit = vi.fn().mockResolvedValue(null);
    render(<HandoffPanel isGuest defaultContact="" defaultIssue="Thợ không đến" onSubmit={onSubmit} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /Gửi yêu cầu hỗ trợ/ }));
    expect(await screen.findByText(/để lại số điện thoại hoặc email/)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();

    fireEvent.change(screen.getByPlaceholderText('SĐT hoặc email *'), { target: { value: '0912345678' } });
    fireEvent.click(screen.getByRole('button', { name: /Gửi yêu cầu hỗ trợ/ }));
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({ name: '', contact: '0912345678', issue: 'Thợ không đến' })
    );
  });
});
