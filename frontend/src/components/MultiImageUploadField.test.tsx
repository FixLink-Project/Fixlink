import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import MultiImageUploadField from './MultiImageUploadField';

const { uploadImageToFirebase } = vi.hoisted(() => ({ uploadImageToFirebase: vi.fn() }));
vi.mock('../lib/firebaseStorage', () => ({
  uploadImageToFirebase,
  validateImageFile: (file: File) => file.type.startsWith('image/') ? null : 'Chỉ hỗ trợ tệp ảnh'
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('MultiImageUploadField RC-36', () => {
  it('uploads, previews, and removes an attached device photo', async () => {
    const onChange = vi.fn();
    uploadImageToFirebase.mockResolvedValue({ url: 'https://example.test/device.jpg' });
    render(<MultiImageUploadField label="Ảnh thiết bị" value={[]} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Ảnh thiết bị'), {
      target: { files: [new File(['photo'], 'device.jpg', { type: 'image/jpeg' })] }
    });

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(['https://example.test/device.jpg']));
    render(<MultiImageUploadField label="Ảnh đã tải" value={['https://example.test/device.jpg']} onChange={onChange} />);
    expect(screen.getByRole('img', { name: 'Ảnh thiết bị 1' })).toHaveAttribute('src', 'https://example.test/device.jpg');
    fireEvent.click(screen.getByRole('button', { name: 'Xóa ảnh 1' }));
    expect(onChange).toHaveBeenLastCalledWith([]);
  });

  it('rejects unsupported file types before upload', async () => {
    const onChange = vi.fn();
    render(<MultiImageUploadField label="Ảnh thiết bị" value={[]} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('Ảnh thiết bị'), {
      target: { files: [new File(['text'], 'notes.txt', { type: 'text/plain' })] }
    });

    expect(await screen.findByRole('alert')).toHaveTextContent('Chỉ hỗ trợ tệp ảnh');
    expect(uploadImageToFirebase).not.toHaveBeenCalled();
    expect(onChange).not.toHaveBeenCalled();
  });
});
