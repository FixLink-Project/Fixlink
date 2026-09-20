import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import Tabs, { type TabItem } from '../src/components/Tabs';

const ITEMS: TabItem[] = [
  { id: 'a', label: 'Tất cả' },
  { id: 'b', label: 'Chờ duyệt', badge: 3 },
  { id: 'c', label: 'Đã khoá' }
];

function Harness() {
  const [active, setActive] = useState('a');
  return (
    <Tabs label="Lọc" items={ITEMS} active={active} onChange={setActive}>
      <p>Nội dung {active}</p>
    </Tabs>
  );
}

describe('Tabs', () => {
  it('đánh dấu đúng tab đang chọn và chỉ tab đó nhận được tiêu điểm bằng phím Tab', () => {
    render(<Harness />);
    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[0]).toHaveAttribute('tabindex', '0');
    expect(tabs[1]).toHaveAttribute('tabindex', '-1');
  });

  it('hiện con số phụ khi lớn hơn 0', () => {
    render(<Harness />);
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('bấm vào một tab thì đổi nội dung hiển thị', () => {
    render(<Harness />);
    fireEvent.click(screen.getByRole('tab', { name: /Đã khoá/ }));
    expect(screen.getByText('Nội dung c')).toBeInTheDocument();
  });

  it('phím mũi tên phải chuyển sang tab kế và quay vòng ở cuối danh sách', () => {
    render(<Harness />);
    fireEvent.keyDown(screen.getAllByRole('tab')[0], { key: 'ArrowRight' });
    expect(screen.getByText('Nội dung b')).toBeInTheDocument();

    fireEvent.keyDown(screen.getByRole('tab', { name: /Chờ duyệt/ }), { key: 'End' });
    expect(screen.getByText('Nội dung c')).toBeInTheDocument();

    fireEvent.keyDown(screen.getByRole('tab', { name: /Đã khoá/ }), { key: 'ArrowRight' });
    expect(screen.getByText('Nội dung a')).toBeInTheDocument();
  });

  it('gắn tabpanel với đúng tab đang chọn', () => {
    render(<Harness />);
    const panel = screen.getByRole('tabpanel');
    const selected = screen.getByRole('tab', { selected: true });
    expect(panel).toHaveAttribute('aria-labelledby', selected.id);
  });
});
