import { useState } from 'react';
import { confirmPayment, formatCurrency } from '../lib/api';
import type { BankInfo, Bill } from '../lib/types';
import Button from './Button';

interface QRPaymentModalProps {
  paymentId: number;
  qrUrl: string;
  amount: number;
  transferContent: string;
  bankInfo: BankInfo;
  onClose: () => void;
  onConfirmed: (bill: Bill) => void;
}

export default function QRPaymentModal({
  paymentId,
  qrUrl,
  amount,
  transferContent,
  bankInfo,
  onClose,
  onConfirmed
}: QRPaymentModalProps) {
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    setConfirming(true);
    setError(null);
    try {
      const res = await confirmPayment(paymentId);
      onConfirmed(res.data);
    } catch (err: any) {
      setError(err.message || 'Xác nhận thất bại.');
    } finally {
      setConfirming(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-surface-card p-6 shadow-xl">
        <h3 className="mb-4 text-center font-display text-lg font-bold text-ink">
          Thanh toán qua QR
        </h3>

        <div className="mb-4 flex justify-center">
          <img
            src={qrUrl}
            alt="QR Code thanh toán"
            className="h-56 w-56 rounded-xl border border-line"
          />
        </div>

        <div className="mb-4 space-y-2 rounded-xl bg-surface-elevated/50 p-4 text-sm">
          <div className="flex justify-between">
            <span className="text-ink-soft">Ngân hàng</span>
            <span className="font-medium text-ink">{bankInfo.bankName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-ink-soft">Số tài khoản</span>
            <span className="font-mono font-medium text-ink">{bankInfo.accountNo}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-ink-soft">Chủ tài khoản</span>
            <span className="font-medium text-ink">{bankInfo.accountName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-ink-soft">Số tiền</span>
            <span className="font-bold text-brand">{formatCurrency(amount)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-ink-soft">Nội dung CK</span>
            <span className="font-mono text-xs font-medium text-ink">{transferContent}</span>
          </div>
        </div>

        <p className="mb-4 text-center text-xs text-ink-soft">
          Quét mã QR hoặc chuyển khoản thủ công theo thông tin trên.
          Sau khi chuyển xong, bấm xác nhận bên dưới.
        </p>

        {error && (
          <p className="mb-3 rounded-lg bg-danger/10 px-3 py-2 text-sm text-danger">{error}</p>
        )}

        <div className="flex gap-3">
          <Button variant="secondary" fullWidth onClick={onClose} disabled={confirming}>
            Đóng
          </Button>
          <Button fullWidth loading={confirming} onClick={handleConfirm}>
            Tôi đã chuyển khoản
          </Button>
        </div>
      </div>
    </div>
  );
}
