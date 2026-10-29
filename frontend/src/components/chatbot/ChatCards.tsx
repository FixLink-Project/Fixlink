import { Link } from 'react-router-dom';
import type { ChatCardItem } from '../../lib/chatbotApi';

const TONE_CLASSES: Record<string, string> = {
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  warning: 'bg-amber-50 text-amber-800 border-amber-200',
  danger: 'bg-red-50 text-red-700 border-red-200',
  neutral: 'bg-slate-100 text-slate-600 border-slate-200',
  info: 'bg-blue-50 text-blue-700 border-blue-200',
};

const TYPE_ICONS: Record<string, string> = {
  request: '🧾',
  appointment: '📅',
  price_list: '💰',
  deposit: '🧮',
  technician: '🪪',
  job: '🛠️',
  draft: '📝',
  quotes: '⚖️',
};

/** Thanh tiến độ của đơn sửa chữa, giống thẻ theo dõi đơn hàng trên các sàn thương mại điện tử. */
function ProgressSteps({ steps, current }: { steps: string[]; current: number }) {
  return (
    <div className="mt-2.5 mb-1" aria-label={`Tiến độ: ${steps[current]}`}>
      <div className="flex items-center">
        {steps.map((step, idx) => (
          <div key={step} className="flex items-center flex-1 last:flex-none">
            <div
              className={`w-3 h-3 rounded-full border-2 flex-shrink-0 ${
                idx < current
                  ? 'bg-blue-600 border-blue-600'
                  : idx === current
                    ? 'bg-white border-blue-600 ring-2 ring-blue-200'
                    : 'bg-white border-slate-300'
              }`}
              title={step}
            />
            {idx < steps.length - 1 && (
              <div className={`h-0.5 flex-1 mx-0.5 ${idx < current ? 'bg-blue-600' : 'bg-slate-200'}`} />
            )}
          </div>
        ))}
      </div>
      <div className="mt-1 flex justify-between text-[9px] text-slate-500 leading-tight">
        {steps.map((step, idx) => (
          <span
            key={step}
            className={`w-0 flex-1 last:flex-none last:w-auto text-center first:text-left last:text-right ${
              idx === current ? 'text-blue-700 font-semibold' : ''
            }`}
          >
            {idx === 0 || idx === current || idx === steps.length - 1 ? step : ''}
          </span>
        ))}
      </div>
    </div>
  );
}

export default function ChatCards({ cards }: { cards: ChatCardItem[] }) {
  if (!cards || cards.length === 0) return null;
  return (
    <div className="space-y-2 mt-2">
      {cards.map((card, idx) => (
        <div key={idx} className="rounded-xl border border-slate-200 bg-slate-50/80 p-3 text-xs shadow-2xs">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <div className="font-semibold text-slate-900 text-[13px] leading-snug flex items-start gap-1.5">
                <span aria-hidden>{TYPE_ICONS[card.type] ?? '📌'}</span>
                <span className="break-words">{card.title}</span>
              </div>
              {card.subtitle && <div className="text-[11px] text-slate-500 mt-0.5">{card.subtitle}</div>}
            </div>
            {card.badge && (
              <span
                className={`flex-shrink-0 px-2 py-0.5 rounded-full border text-[10px] font-semibold ${
                  TONE_CLASSES[card.badgeTone ?? 'info'] ?? TONE_CLASSES.info
                }`}
              >
                {card.badge}
              </span>
            )}
          </div>

          {card.steps && card.currentStep !== undefined && card.currentStep !== null && (
            <ProgressSteps steps={card.steps} current={card.currentStep} />
          )}

          {card.fields && card.fields.length > 0 && (
            <dl className="mt-2 space-y-1.5">
              {card.fields.map((f, i) => (
                // Nhãn dài (tên gói dịch vụ) được xuống dòng; giá trị ngắn (giá, ngày giờ) giữ gọn bên phải
                <div key={i} className="flex items-start justify-between gap-3">
                  <dt className="min-w-0 flex-1 text-slate-500 leading-snug break-words">{f.label}</dt>
                  <dd className="max-w-[60%] text-slate-800 font-medium text-right leading-snug break-words">{f.value}</dd>
                </div>
              ))}
            </dl>
          )}

          {card.note && (
            <div className="mt-2 p-2 rounded-lg bg-white border border-slate-200 text-[11px] text-slate-600 leading-relaxed">
              💡 {card.note}
            </div>
          )}

          {card.url && (
            <Link
              to={card.url}
              className="mt-2.5 inline-flex items-center gap-1 text-[11px] font-semibold text-blue-700 hover:text-blue-900"
            >
              {card.urlLabel ?? 'Xem chi tiết'} →
            </Link>
          )}
        </div>
      ))}
    </div>
  );
}
