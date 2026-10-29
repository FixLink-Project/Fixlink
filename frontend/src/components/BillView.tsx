import { useEffect, useState } from 'react';
import { fetchBills, formatCurrency, fetchBillHtml } from '../lib/api';
import type { Bill } from '../lib/types';
import Card from './Card';

interface BillViewProps {
  requestId: number;
}

export default function BillView({ requestId }: BillViewProps) {
  const [bills, setBills] = useState<Bill[]>([]);
  const [loading, setLoading] = useState(true);
  const [printingId, setPrintingId] = useState<number | null>(null);

  useEffect(() => {
    fetchBills(requestId)
      .then(res => setBills(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [requestId]);

  const handlePrint = async (billId: number) => {
    setPrintingId(billId);
    try {
      const html = await fetchBillHtml(billId);
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.open();
        printWindow.document.write(html);
        printWindow.document.close();
        printWindow.focus();
      }
    } catch (err: any) {
      alert(err.message || 'Không thể in hóa đơn. Vui lòng thử lại sau.');
    } finally {
      setPrintingId(null);
    }
  };

  if (loading || bills.length === 0) return null;

  return (
    <Card title="Hóa đơn">
      <div className="space-y-3">
        {bills.map(b => (
          <div key={b.id} className="rounded-xl border border-line p-4">
            <div className="flex items-start justify-between">
              <div>
                <p className="font-mono text-sm font-medium text-ink">{b.billNumber}</p>
                <div className="flex items-center gap-1.5 mt-1">
                  <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                    b.billType === 'FINAL' ? 'bg-success/10 text-success' : 'bg-brand/10 text-brand'
                  }`}>
                    {b.billType === 'FINAL' ? 'Hóa đơn tổng' : 'Hóa đơn đặt cọc'}
                  </span>
                  {b.paymentMethod && (
                    <span className="text-[11px] font-medium text-slate-500 bg-slate-100 rounded-full px-2 py-0.5">
                      {b.paymentMethod === 'CASH' ? '💵 Tiền mặt' : '💳 VietQR'}
                    </span>
                  )}
                </div>
              </div>
              <button
                type="button"
                disabled={printingId === b.id}
                onClick={() => handlePrint(b.id)}
                className="inline-flex items-center gap-1 rounded-lg border border-line px-3 py-1.5 text-xs font-medium text-ink-soft hover:bg-surface-elevated hover:text-brand transition-colors disabled:opacity-50"
              >
                {printingId === b.id ? (
                  <>
                    <span className="inline-block h-3 w-3 animate-spin rounded-full border-2 border-brand border-t-transparent" />
                    Đang mở...
                  </>
                ) : (
                  <>
                    <span>🖨️</span>
                    In / Xuất PDF
                  </>
                )}
              </button>
            </div>

            <div className="mt-3 space-y-1 text-sm">
              <div className="flex justify-between">
                <span className="text-ink-soft">Giá thỏa thuận</span>
                <span className="font-medium text-ink">{formatCurrency(b.quotedPrice)}</span>
              </div>
              {b.billType === 'FINAL' && b.depositAmount > 0 && (
                <div className="flex justify-between">
                  <span className="text-ink-soft">Đã đặt cọc</span>
                  <span className="font-medium text-ink">-{formatCurrency(b.depositAmount)}</span>
                </div>
              )}
              {b.additionalCost > 0 && (
                <div className="flex justify-between">
                  <span className="text-ink-soft">Phát sinh</span>
                  <span className="font-medium text-amber-600">+{formatCurrency(b.additionalCost)}</span>
                </div>
              )}
              <div className="flex justify-between border-t border-line pt-1">
                <span className="font-bold text-ink">Tổng thanh toán</span>
                <span className="font-bold text-brand">{formatCurrency(b.totalAmount)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}
