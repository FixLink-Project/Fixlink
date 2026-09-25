import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import MultiImageUploadField from '../components/MultiImageUploadField';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, fetchAreas, fetchCategories, formatCurrency, toFieldErrors } from '../lib/api';
import type { ServiceArea, ServiceCategory } from '../lib/types';

const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Bảng điều khiển' },
  { to: '/yeu-cau-cua-toi', label: 'Yêu cầu của tôi' },
  { to: '/dang-yeu-cau', label: 'Đăng yêu cầu' }
];

type Step = 1 | 2 | 3;

function getCategoryIcon(name: string): string {
  const lower = name.toLowerCase();
  if (lower.includes('điện thoại') || lower.includes('smartphone') || lower.includes('phone')) return '📱';
  if (lower.includes('máy tính') || lower.includes('laptop') || lower.includes('pc')) return '💻';
  if (lower.includes('lạnh') || lower.includes('điều hòa') || lower.includes('tủ lạnh')) return '❄️';
  if (lower.includes('giặt') || lower.includes('máy giặt')) return '🧺';
  if (lower.includes('tivi') || lower.includes('ti vi') || lower.includes('tv') || lower.includes('màn hình')) return '📺';
  if (lower.includes('âm thanh') || lower.includes('loa') || lower.includes('amply')) return '🔊';
  if (lower.includes('bếp') || lower.includes('nồi') || lower.includes('lò vi sóng')) return '🍳';
  if (lower.includes('bo mạch') || lower.includes('mạch') || lower.includes('linh kiện')) return '⚡';
  return '🔧';
}

export default function CreateRepairRequestPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>(1);

  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [areas, setAreas] = useState<ServiceArea[]>([]);

  const [form, setForm] = useState({
    categoryId: '',
    areaId: '',
    title: '',
    description: '',
    deviceBrand: '',
    deviceModel: '',
    serialNumber: '',
    addressLine: '',
    budgetRef: '',
    biddingDeadlineDays: '3'
  });
  const [mediaUrls, setMediaUrls] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchCategories(), fetchAreas()]).then(([catRes, areaRes]) => {
      setCategories(catRes.data.filter((c) => c.isActive));
      setAreas(areaRes.data);
    });
  }, []);

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => {
      if (!e[field]) return e;
      const next = { ...e };
      delete next[field];
      return next;
    });
  }

  function validateStep1(): boolean {
    const errs: Record<string, string> = {};
    if (!form.categoryId) errs.categoryId = 'Vui lòng chọn loại thiết bị cần sửa chữa.';
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return false;
    }
    return true;
  }

  function validateStep2(): boolean {
    const errs: Record<string, string> = {};
    if (!form.title.trim()) errs.title = 'Tiêu đề không được để trống.';
    if (!form.description.trim()) errs.description = 'Mô tả chi tiết triệu chứng hỏng không được để trống.';
    if (!form.addressLine.trim()) errs.addressLine = 'Địa chỉ sửa chữa không được để trống.';
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return false;
    }
    return true;
  }

  function goNext() {
    if (step === 1 && validateStep1()) setStep(2);
    else if (step === 2 && validateStep2()) setStep(3);
  }

  async function submitRequest(saveAsDraft: boolean) {
    setSaveError(null);
    if (saveAsDraft) setSavingDraft(true);
    else setSaving(true);

    try {
      const body = {
        title: form.title.trim(),
        description: form.description.trim(),
        deviceBrand: form.deviceBrand.trim() || null,
        deviceModel: form.deviceModel.trim() || null,
        serialNumber: form.serialNumber.trim() || null,
        categoryId: Number(form.categoryId),
        areaId: form.areaId ? Number(form.areaId) : null,
        address: form.addressLine.trim(),
        addressLine: form.addressLine.trim(),
        budgetRef: form.budgetRef ? Number(form.budgetRef) : null,
        biddingDeadlineDays: Number(form.biddingDeadlineDays) || 3,
        saveAsDraft,
        mediaUrls
      };

      await api.post('/repair-requests', body);
      navigate('/yeu-cau-cua-toi');
    } catch (err) {
      const fieldErrors = toFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
        setStep(2);
      } else if (err instanceof ApiError) {
        setSaveError(err.message);
      } else {
        setSaveError('Không kết nối được máy chủ.');
      }
    } finally {
      setSaving(false);
      setSavingDraft(false);
    }
  }

  const selectedCategory = categories.find((c) => c.id === Number(form.categoryId));

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title="Đăng yêu cầu sửa chữa"
      description="Kết nối với đội ngũ thợ điện tử uy tín, nhận báo giá cạnh tranh chỉ trong vài phút."
    >
      {/* Stepper indicator */}
      <div className="mb-8 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm" aria-label="Tiến trình">
        <div className="flex items-center justify-between sm:justify-start sm:gap-6">
          {[
            { num: 1, label: 'Chọn thiết bị' },
            { num: 2, label: 'Mô tả sự cố & Ảnh' },
            { num: 3, label: 'Xác nhận & Gửi' }
          ].map(({ num, label }) => (
            <div key={num} className="flex items-center gap-2 sm:gap-3">
              <div
                className={`flex h-9 w-9 items-center justify-center rounded-xl text-sm font-semibold transition-all duration-300 ${
                  num === step
                    ? 'bg-brand text-white shadow-sm ring-4 ring-blue-100'
                    : num < step
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      : 'bg-slate-100 text-slate-500 border border-slate-200'
                }`}
              >
                {num < step ? '✓' : num}
              </div>
              <span className={`text-sm font-semibold hidden sm:inline ${num === step ? 'text-slate-900' : 'text-slate-500'}`}>
                {label}
              </span>
              {num < 3 && <span className="text-slate-300 hidden sm:inline">→</span>}
            </div>
          ))}
        </div>
      </div>

      {saveError && <div className="mb-6"><Alert tone="error">{saveError}</Alert></div>}

      {/* Step 1: Chọn loại thiết bị */}
      {step === 1 && (
        <Card
          title="Chọn loại thiết bị cần sửa chữa"
          description="FixLink chuyên trị mọi sự cố thiết bị điện tử, công nghệ và điện gia dụng."
        >
          <div className="space-y-6">
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 sm:gap-4">
              {categories.map((cat) => {
                const isSelected = form.categoryId === String(cat.id);
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => update('categoryId', String(cat.id))}
                    className={`flex flex-col items-center justify-center p-4 sm:p-5 rounded-2xl border text-center transition-all duration-200 ${
                      isSelected
                        ? 'border-brand bg-blue-50/60 ring-2 ring-brand/30 shadow-sm'
                        : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50 bg-white shadow-xs'
                    }`}
                  >
                    <span className="text-3xl sm:text-4xl mb-2">{getCategoryIcon(cat.name)}</span>
                    <span className={`text-sm font-semibold ${isSelected ? 'text-brand' : 'text-slate-800'}`}>
                      {cat.name}
                    </span>
                  </button>
                );
              })}
            </div>

            {errors.categoryId && (
              <p className="text-sm font-medium text-rose-600">{errors.categoryId}</p>
            )}

            <div className="flex justify-end pt-4">
              <Button onClick={goNext}>
                Tiếp tục: Mô tả sự cố →
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Step 2: Mô tả sự cố & Ảnh */}
      {step === 2 && (
        <Card
          title={`Mô tả sự cố thiết bị ${selectedCategory?.name ? `(${selectedCategory.name})` : ''}`}
          description="Cung cấp thông tin chi tiết giúp thợ chẩn đoán chính xác và đưa ra mức giá tốt nhất."
        >
          <div className="space-y-5">
            <TextField
              label="Tiêu đề yêu cầu"
              required
              placeholder="VD: Máy lạnh Daikin không mát, quạt kêu to"
              value={form.title}
              error={errors.title}
              onChange={(e) => update('title', e.target.value)}
            />

            <TextArea
              label="Mô tả chi tiết sự cố"
              required
              rows={4}
              placeholder="Mô tả hiện trạng: máy bị lỗi gì, xuất hiện từ khi nào, các dấu hiệu bất thường..."
              value={form.description}
              error={errors.description}
              onChange={(e) => update('description', e.target.value)}
            />

            <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 sm:p-5">
              <h3 className="text-sm font-bold text-slate-900">Thông tin thiết bị (không bắt buộc)</h3>
              <p className="mt-1 text-xs text-slate-500">Thông tin này giúp thợ chuẩn bị linh kiện và chẩn đoán chính xác hơn.</p>
              <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
                <TextField label="Thương hiệu" placeholder="Ví dụ: Samsung, Daikin" value={form.deviceBrand} onChange={(e) => update('deviceBrand', e.target.value)} />
                <TextField label="Model" placeholder="Ví dụ: Inverter 1.5 HP" value={form.deviceModel} onChange={(e) => update('deviceModel', e.target.value)} />
                <TextField label="Số serial" placeholder="Có trên tem thiết bị" value={form.serialNumber} onChange={(e) => update('serialNumber', e.target.value)} />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <TextField
                label="Địa chỉ cụ thể"
                required
                placeholder="Số nhà, tên đường, phường..."
                value={form.addressLine}
                error={errors.addressLine}
                onChange={(e) => update('addressLine', e.target.value)}
              />

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Khu vực (Quận/Huyện)</label>
                <select
                  value={form.areaId}
                  onChange={(e) => update('areaId', e.target.value)}
                  className="min-h-[44px] w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 shadow-xs focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
                >
                  <option value="">Toàn khu vực (Không chỉ định)</option>
                  {areas.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name} ({a.city})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <TextField
                label="Ngân sách dự kiến (VND)"
                type="number"
                min={0}
                placeholder="VD: 500000"
                hint="Để trống nếu bạn muốn thợ tự đề xuất báo giá"
                value={form.budgetRef}
                onChange={(e) => update('budgetRef', e.target.value)}
              />
              <TextField
                label="Thời hạn nhận báo giá (Ngày)"
                type="number"
                min={1}
                max={7}
                value={form.biddingDeadlineDays}
                hint="Thời gian tối đa để các thợ gửi báo giá (1-7 ngày)"
                onChange={(e) => update('biddingDeadlineDays', e.target.value)}
              />
            </div>

            {/* RC-30 & RC-8: Ảnh đính kèm Firebase Storage */}
            <div className="pt-2 border-t border-slate-100">
              <MultiImageUploadField
                label="Hình ảnh hiện trường thiết bị (Tối đa 6 ảnh)"
                value={mediaUrls}
                onChange={setMediaUrls}
                maxFiles={6}
                hint="Chụp ảnh ngoại quan, tem máy, mã lỗi màn hình hoặc hiện trường hỏng hóc để thợ chẩn đoán từ xa."
              />
            </div>

            <div className="flex justify-between items-center pt-4">
              <Button variant="secondary" onClick={() => setStep(1)}>
                ← Đổi loại thiết bị
              </Button>
              <div className="flex gap-2">
                <Button variant="secondary" loading={savingDraft} onClick={() => submitRequest(true)}>
                  💾 Lưu bản nháp
                </Button>
                <Button onClick={goNext}>
                  Kiểm tra thông tin →
                </Button>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Step 3: Xác nhận */}
      {step === 3 && (
        <Card
          title="Xác nhận thông tin yêu cầu"
          description="Kiểm tra lại lần cuối trước khi phát sóng tới cộng đồng thợ kỹ thuật."
        >
          <div className="space-y-6">
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xs">
              <dl className="divide-y divide-slate-100">
                <Row label="Loại thiết bị" value={selectedCategory?.name ?? '—'} highlight />
                <Row
                  label="Khu vực"
                  value={
                    form.areaId
                      ? areas.find((a) => a.id === Number(form.areaId))?.name ?? '—'
                      : 'Toàn khu vực (Không chỉ định)'
                  }
                />
                <Row label="Tiêu đề yêu cầu" value={form.title} highlight />
                <Row label="Mô tả sự cố" value={form.description} />
                <Row label="Địa chỉ" value={form.addressLine} />
                <Row
                  label="Ngân sách dự kiến"
                  value={form.budgetRef ? formatCurrency(Number(form.budgetRef)) : 'Thợ tự đề xuất báo giá'}
                  accent={Boolean(form.budgetRef)}
                />
                <Row label="Thời hạn nhận giá" value={`${form.biddingDeadlineDays} ngày`} />

                {mediaUrls.length > 0 && (
                  <div className="flex flex-col gap-2 p-4 sm:flex-row sm:gap-6">
                    <dt className="w-44 shrink-0 text-sm font-semibold text-slate-500">
                      Hình ảnh đính kèm ({mediaUrls.length})
                    </dt>
                    <dd className="flex flex-wrap gap-2">
                      {mediaUrls.map((url, idx) => (
                        <img
                          key={idx}
                          src={url}
                          alt={`Ảnh đính kèm ${idx + 1}`}
                          className="h-20 w-20 rounded-xl object-cover border border-slate-200 shadow-xs"
                        />
                      ))}
                    </dd>
                  </div>
                )}
              </dl>
            </div>

            <div className="rounded-2xl border border-blue-100 bg-blue-50/70 p-4 text-sm text-blue-900 shadow-xs">
              💡 <strong>Lưu ý:</strong> Sau khi đăng, yêu cầu của bạn sẽ được gửi tới các thợ phù hợp. Bạn có thể chọn <em>Lưu bản nháp</em> để tiếp tục hoàn thiện sau, hoặc <em>Phát sóng ngay</em> để nhận báo giá minh bạch.
            </div>

            <div className="flex flex-col sm:flex-row justify-between gap-3 pt-2">
              <Button variant="secondary" onClick={() => setStep(2)}>
                ← Quay lại chỉnh sửa
              </Button>
              <div className="flex gap-2">
                <Button variant="secondary" loading={savingDraft} onClick={() => submitRequest(true)}>
                  💾 Lưu bản nháp
                </Button>
                <Button loading={saving} onClick={() => submitRequest(false)}>
                  {saving ? 'Đang phát sóng yêu cầu...' : '🚀 Phát sóng yêu cầu sửa chữa'}
                </Button>
              </div>
            </div>
          </div>
        </Card>
      )}
    </DashboardLayout>
  );
}

function Row({
  label,
  value,
  highlight = false,
  accent = false
}: {
  label: string;
  value: string;
  highlight?: boolean;
  accent?: boolean;
}) {
  return (
    <div className="flex flex-col gap-1 p-4 sm:flex-row sm:gap-6">
      <dt className="w-44 shrink-0 text-sm font-semibold text-slate-500">{label}</dt>
      <dd
        className={`text-sm ${
          accent ? 'font-bold text-brand' : highlight ? 'font-bold text-slate-900' : 'text-slate-700'
        }`}
      >
        {value}
      </dd>
    </div>
  );
}
