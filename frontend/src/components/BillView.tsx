import { useEffect, useState } from 'react';
import { fetchBills, formatCurrency, getBillHtmlUrl } from '../lib/api';
import type { Bill } from '../lib/types';
import Card from './Card';

interface BillViewProps {
  requestId: number;
}

export default function BillView({ requestId }: BillViewProps) {
  const [bills, setBills] = useState<Bill[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBills(requestId)
      .then(res => setBills(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [requestId]);

  if (loading || bills.length === 0) return null;

  return (
    <Card title="Hóa đơn">
      <div className="space-y-3">
        {bills.map(b => (
          <div key={b.id} className="rounded-xl border border-line p-4">
            <div className="flex items-start justify-between">
              <div>
                <p className="font-mono text-sm font-medium text-ink">{b.billNumber}</p>
                <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                  b.billType === 'FINAL' ? 'bg-success/10 text-success' : 'bg-brand/10 text-brand'
                }`}>
                  {b.billType === 'FINAL' ? 'Hóa đơn tổng' : 'Hóa đơn đặt cọc'}
                </span>
              </div>
              <a
                href={getBillHtmlUrl(b.id)}
                target="_blank"
                rel="noopener noreferrer"
                className="rounded-lg border border-line px-3 py-1.5 text-xs font-medium text-ink-soft hover:bg-surface-elevated"
              >
                In / Xuất PDF
              </a>
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
