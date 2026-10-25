import { useEffect, useState } from 'react';
import { acceptPriceQuote, fetchPriceQuotes, formatCurrency } from '../lib/api';
import type { PriceQuote } from '../lib/types';
import Button from './Button';
import Card from './Card';

interface PriceQuoteListProps {
  requestId: number;
  isCustomer: boolean;
  requestStatus: string;
  onQuoteAccepted?: () => void;
}

export default function PriceQuoteList({ requestId, isCustomer, requestStatus, onQuoteAccepted }: PriceQuoteListProps) {
  const [quotes, setQuotes] = useState<PriceQuote[]>([]);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchPriceQuotes(requestId)
      .then(res => setQuotes(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [requestId]);

  async function handleAccept(quoteId: number) {
    setAccepting(quoteId);
    setError(null);
    try {
      await acceptPriceQuote(quoteId);
      onQuoteAccepted?.();
    } catch (err: any) {
      setError(err.message || 'Không thể chọn báo giá.');
    } finally {
      setAccepting(null);
    }
  }

  if (loading) return null;
  if (quotes.length === 0) return null;

  const canAccept = isCustomer && requestStatus === 'OPEN';

  return (
    <Card title="Báo giá từ kỹ thuật viên">
      {error && (
        <p className="mb-3 rounded-lg bg-danger/10 px-3 py-2 text-sm text-danger">{error}</p>
      )}

      <div className="space-y-3">
        {quotes.map(q => (
          <div
            key={q.id}
            className="flex items-center justify-between rounded-xl border border-line p-4"
          >
            <div>
              <p className="font-medium text-ink">KTV #{q.technicianId}</p>
              <p className="text-lg font-bold text-brand">{formatCurrency(q.quotedPrice)}</p>
              {q.note && <p className="mt-1 text-sm text-ink-soft">{q.note}</p>}
              <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
                q.status === 'ACCEPTED' ? 'bg-success/10 text-success'
                  : q.status === 'REJECTED' ? 'bg-danger/10 text-danger'
                  : 'bg-warning/10 text-warning'
              }`}>
                {q.status === 'ACCEPTED' ? 'Đã chọn' : q.status === 'REJECTED' ? 'Từ chối' : 'Chờ duyệt'}
              </span>
            </div>
            {canAccept && q.status === 'PENDING' && (
              <Button
                loading={accepting === q.id}
                disabled={accepting !== null}
                onClick={() => handleAccept(q.id)}
              >
                Chọn
              </Button>
            )}
          </div>
        ))}
      </div>
    </Card>
  );
}
