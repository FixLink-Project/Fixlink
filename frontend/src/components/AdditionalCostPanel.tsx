import { useEffect, useState } from 'react';
import {
  addAdditionalCost,
  approveAdditionalCost,
  fetchAdditionalCosts,
  formatCurrency,
  rejectAdditionalCost
} from '../lib/api';
import type { AdditionalCost } from '../lib/types';
import Button from './Button';
import Card from './Card';

interface AdditionalCostPanelProps {
  requestId: number;
  isTechnician: boolean;
  isCustomer: boolean;
  onCostChanged?: () => void;
}

export default function AdditionalCostPanel({
  requestId,
  isTechnician,
  isCustomer,
  onCostChanged
}: AdditionalCostPanelProps) {
  const [costs, setCosts] = useState<AdditionalCost[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [desc, setDesc] = useState('');
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [acting, setActing] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadCosts() {
    try {
      const res = await fetchAdditionalCosts(requestId);
      setCosts(res.data);
    } catch {} finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadCosts(); }, [requestId]);

  async function handleAdd() {
    const num = Number(amount);
    if (!desc.trim() || !num || num <= 0) return;
    setSubmitting(true);
    setError(null);
    try {
      await addAdditionalCost(requestId, desc.trim(), num);
      setDesc('');
      setAmount('');
      setShowForm(false);
      await loadCosts();
      onCostChanged?.();
    } catch (err: any) {
      setError(err.message || 'Lỗi');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAction(costId: number, action: 'approve' | 'reject') {
    setActing(costId);
    setError(null);
    try {
      if (action === 'approve') await approveAdditionalCost(costId);
      else await rejectAdditionalCost(costId);
      await loadCosts();
      onCostChanged?.();
    } catch (err: any) {
      setError(err.message || 'Lỗi');
    } finally {
      setActing(null);
    }
  }

  if (loading) return null;

  const total = costs.filter(c => c.status === 'APPROVED').reduce((s, c) => s + c.amount, 0);

  return (
    <Card
      title="Chi phí phát sinh"
      actions={
        isTechnician && !showForm ? (
          <Button variant="secondary" onClick={() => setShowForm(true)}>+ Thêm</Button>
        ) : undefined
      }
    >
      {error && (
        <p className="mb-3 rounded-lg bg-danger/10 px-3 py-2 text-sm text-danger">{error}</p>
      )}

      {showForm && (
        <div className="mb-4 space-y-3 rounded-xl border border-line p-4">
          <input
            className="w-full rounded-lg border border-line bg-surface-elevated px-3 py-2 text-sm text-ink outline-none focus:border-brand"
            placeholder="Mô tả chi phí"
            value={desc}
            onChange={e => setDesc(e.target.value)}
          />
          <input
            className="w-full rounded-lg border border-line bg-surface-elevated px-3 py-2 text-sm text-ink outline-none focus:border-brand"
            type="number"
            min={1}
            placeholder="Số tiền (VND)"
            value={amount}
            onChange={e => setAmount(e.target.value)}
          />
          <div className="flex gap-2">
            <Button loading={submitting} onClick={handleAdd}>Gửi</Button>
            <Button variant="secondary" onClick={() => setShowForm(false)} disabled={submitting}>Hủy</Button>
          </div>
        </div>
      )}

      {costs.length === 0 ? (
        <p className="text-sm text-ink-soft">Chưa có chi phí phát sinh.</p>
      ) : (
        <div className="space-y-2">
          {costs.map(c => (
            <div key={c.id} className="flex items-center justify-between rounded-xl border border-line p-3">
              <div>
                <p className="text-sm font-medium text-ink">{c.description}</p>
                <p className="text-sm font-bold text-brand">{formatCurrency(c.amount)}</p>
                <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                  c.status === 'APPROVED' ? 'bg-success/10 text-success'
                    : c.status === 'REJECTED' ? 'bg-danger/10 text-danger'
                    : 'bg-warning/10 text-warning'
                }`}>
                  {c.status === 'APPROVED' ? 'Đã duyệt' : c.status === 'REJECTED' ? 'Từ chối' : 'Chờ duyệt'}
                </span>
              </div>
              {isCustomer && c.status === 'PENDING' && (
                <div className="flex gap-2">
                  <Button
                    variant="primary"
                    loading={acting === c.id}
                    disabled={acting !== null}
                    onClick={() => handleAction(c.id, 'approve')}
                  >
                    Duyệt
                  </Button>
                  <Button
                    variant="danger"
                    loading={acting === c.id}
                    disabled={acting !== null}
                    onClick={() => handleAction(c.id, 'reject')}
                  >
                    Từ chối
                  </Button>
                </div>
              )}
            </div>
          ))}
          {total > 0 && (
            <p className="pt-2 text-right text-sm font-bold text-ink">
              Tổng phát sinh đã duyệt: {formatCurrency(total)}
            </p>
          )}
        </div>
      )}
    </Card>
  );
}
