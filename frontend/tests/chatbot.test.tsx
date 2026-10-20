import { fireEvent, render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MessageBubble from '../src/components/chatbot/MessageBubble';
import SuggestionChips from '../src/components/chatbot/SuggestionChips';
import TypingIndicator from '../src/components/chatbot/TypingIndicator';
import { clearChatSessionId, getOrCreateChatSessionId } from '../src/lib/chatbotApi';

describe('Chatbot API Utilities', () => {
  beforeEach(() => {
    clearChatSessionId();
  });

  it('tạo sessionId mới nếu chưa có trong sessionStorage', () => {
    const id1 = getOrCreateChatSessionId();
    expect(id1).toMatch(/^ses-/);
    const id2 = getOrCreateChatSessionId();
    expect(id2).toBe(id1);
  });

  it('làm mới sessionId khi gọi clearChatSessionId', () => {
    const id1 = getOrCreateChatSessionId();
    clearChatSessionId();
    const id2 = getOrCreateChatSessionId();
    expect(id2).not.toBe(id1);
  });
});

describe('SuggestionChips Component', () => {
  it('hiển thị danh sách gợi ý và phản hồi khi nhấn', () => {
    const onSelect = vi.fn();
    render(
      <SuggestionChips
        suggestions={['Cách tạo yêu cầu', 'Chính sách đặt cọc']}
        onSelect={onSelect}
      />
    );

    const chip1 = screen.getByText(/Cách tạo yêu cầu/);
    expect(chip1).toBeInTheDocument();
    fireEvent.click(chip1);
    expect(onSelect).toHaveBeenCalledWith('Cách tạo yêu cầu');
  });

  it('không hiển thị gì nếu danh sách rỗng', () => {
    const { container } = render(<SuggestionChips suggestions={[]} onSelect={vi.fn()} />);
    expect(container.firstChild).toBeNull();
  });
});

describe('TypingIndicator Component', () => {
  it('hiển thị trạng thái đang suy nghĩ', () => {
    render(<TypingIndicator />);
    expect(screen.getByText('FixLink đang suy nghĩ...')).toBeInTheDocument();
  });
});

describe('MessageBubble Component', () => {
  it('hiển thị tin nhắn người dùng', () => {
    render(
      <BrowserRouter>
        <MessageBubble
          message={{
            id: 'm1',
            sender: 'user',
            text: 'Tôi cần tìm thợ sửa máy giặt',
            timestamp: Date.now(),
          }}
          onFeedback={vi.fn()}
        />
      </BrowserRouter>
    );
    expect(screen.getByText('Tôi cần tìm thợ sửa máy giặt')).toBeInTheDocument();
  });

  it('hiển thị tin nhắn trợ lý bot với markdown và mã phiếu hỗ trợ', () => {
    render(
      <BrowserRouter>
        <MessageBubble
          message={{
            id: 'm2',
            sender: 'bot',
            text: 'Yêu cầu của bạn đã được ghi nhận. Vui lòng xem [chi tiết](/yeu-cau/10).',
            timestamp: Date.now(),
            ticketId: 'TCK-1002',
          }}
          onFeedback={vi.fn()}
        />
      </BrowserRouter>
    );

    expect(screen.getByText('TCK-1002')).toBeInTheDocument();
    expect(screen.getByText(/chi tiết/)).toBeInTheDocument();
  });

  it('gửi phản hồi hữu ích khi bấm nút thumbs up', () => {
    const onFeedback = vi.fn();
    render(
      <BrowserRouter>
        <MessageBubble
          message={{
            id: 'm3',
            sender: 'bot',
            text: 'FixLink hoạt động theo quy tắc First-Come-First-Served.',
            timestamp: Date.now(),
          }}
          onFeedback={onFeedback}
        />
      </BrowserRouter>
    );

    const upButton = screen.getByTitle('Hữu ích');
    fireEvent.click(upButton);
    expect(onFeedback).toHaveBeenCalledWith('m3', 'UP');
  });
});
