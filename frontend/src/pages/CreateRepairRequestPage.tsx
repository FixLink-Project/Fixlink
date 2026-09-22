import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import ImageUploadField from '../components/ImageUploadField';
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
    addressLine: '',
    budgetRef: '',
    biddingDeadlineDays: '3',
    mediaUrl: ''
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
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
    if (!form.categoryId) errs.categoryId = 'Chọn loại dịch vụ cần sửa.';
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return false;
    }
    return true;
  }

  function validateStep2(): boolean {
    const errs: Record<string, string> = {};
    if (!form.title.trim()) errs.title = 'Tiêu đề không được để trống.';
    if (!form.description.trim()) errs.description = 'Mô tả không được để trống.';
    if (!form.addressLine.trim()) errs.addressLine = 'Địa chỉ không được để trống.';
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

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaveError(null);
    setSaving(true);

    try {
      const mediaUrls = form.mediaUrl.trim() ? [form.mediaUrl.trim()] : [];
      const body = {
        title: form.title.trim(),
        description: form.description.trim(),
        categoryId: Number(form.categoryId),
        areaId: form.areaId ? Number(form.areaId) : null,
        addressLine: form.addressLine.trim(),
        budgetRef: form.budgetRef ? Number(form.budgetRef) : null,
        biddingDeadlineDays: Number(form.biddingDeadlineDays) || 3,
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
    }
  }

  const selectedCategory = categories.find((c) => c.id === Number(form.categoryId));

  return (
    <DashboardLayout nav={CUSTOMER_NAV} title="Đăng yêu cầu sửa chữa" description="Mô tả sự cố, chọn loại dịch vụ, chờ thợ báo giá.">
      {/* Stepper indicator */}
      <div className="mb-8 flex items-center gap-2" aria-label="Tiến trình">
        {[1, 2, 3].map((s) => (
          <div key={s} className="flex items-center gap-2">
            <div
              className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${
                s === step
                  ? 'bg-brand text-white'
                  : s < step
                    ? 'bg-brand/20 text-brand-ink'
                    : 'bg-surface text-ink-soft'
              }`}
            >
              {s < step ? '✓' : s}
            </div>
            <span className={`text-sm ${s === step ? 'font-medium text-ink' : 'text-ink-soft'}`}>
              {s === 1 ? 'Chọn dịch vụ' : s === 2 ? 'Mô tả sự cố' : 'Xác nhận'}
            </span>
            {s < 3 && <div className="mx-2 h-px w-8 bg-line sm:w-16" />}
          </div>
        ))}
      </div>

      {saveError && (
        <div className="mb-5">
          <Alert tone="error">{saveError}</Alert>
        </div>
      )}

      {/* Step 1: Chọn danh mục */}
      {step === 1 && (
        <Card title="Chọn loại dịch vụ" description="Chọn nhóm việc gần nhất với thứ đang hỏng.">
          <div className="space-y-4">
            {errors.categoryId && <Alert tone="error">{errors.categoryId}</Alert>}
            <div className="grid gap-3 sm:grid-cols-2">
              {categories.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => update('categoryId', String(cat.id))}
                  className={`flex flex-col gap-1 rounded-xl border-2 p-4 text-left transition-colors ${
                    form.categoryId === String(cat.id)
                      ? 'border-brand bg-brand/5'
                      : 'border-line hover:border-brand/50'
                  }`}
                >
                  <span className="font-medium">{cat.name}</span>
                  {cat.description && (
                    <span className="text-sm text-ink-soft">{cat.description}</span>
                  )}
                </button>
              ))}
            </div>

            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium">Khu vực (không bắt buộc)</label>
              <select
                value={form.areaId}
                onChange={(e) => update('areaId', e.target.value)}
                className="min-h-[44px] w-full rounded-lg border border-line bg-card px-3 text-sm"
              >
                <option value="">— Chọn khu vực —</option>
                {areas.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} — {a.city}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex justify-end pt-2">
              <Button onClick={goNext}>Tiếp theo</Button>
            </div>
          </div>
        </Card>
      )}

      {/* Step 2: Mô tả sự cố */}
      {step === 2 && (
        <Card
          title="Mô tả sự cố"
          description={
            selectedCategory
              ? `Danh mục: ${selectedCategory.name}`
              : undefined
          }
        >
          <div className="space-y-5">
            <TextField
              label="Tiêu đề"
              name="title"
              required
              placeholder="VD: Máy lạnh không lạnh"
              value={form.title}
              error={errors.title}
              onChange={(e) => update('title', e.target.value)}
            />
            <TextArea
              label="Mô tả chi tiết"
              name="description"
              rows={4}
              required
              placeholder="Mô tả biểu hiện hỏng, đã thử gì chưa..."
              value={form.description}
              error={errors.description}
              onChange={(e) => update('description', e.target.value)}
            />
            <TextField
              label="Địa chỉ"
              name="addressLine"
              required
              placeholder="123 Nguyễn Văn Trỗi, P.12, Q.Phú Nhuận"
              value={form.addressLine}
              error={errors.addressLine}
              onChange={(e) => update('addressLine', e.target.value)}
            />
            <div className="grid gap-5 sm:grid-cols-2">
              <TextField
                label="Ngân sách tham khảo (VNĐ)"
                name="budgetRef"
                type="number"
                min={0}
                placeholder="500000"
                value={form.budgetRef}
                onChange={(e) => update('budgetRef', e.target.value)}
              />
              <TextField
                label="Số ngày nhận báo giá"
                name="biddingDeadlineDays"
                type="number"
                min={1}
                max={7}
                value={form.biddingDeadlineDays}
                onChange={(e) => update('biddingDeadlineDays', e.target.value)}
              />
            </div>
            <ImageUploadField
              label="Hình ảnh sự cố"
              folder="repair-requests"
              value={form.mediaUrl}
              hint="Chụp rõ chỗ hỏng để thợ báo giá chính xác hơn. Tối đa 5 MB."
              onChange={(url) => update('mediaUrl', url)}
            />

            <div className="flex justify-between pt-2">
              <Button variant="secondary" onClick={() => setStep(1)}>
                Quay lại
              </Button>
              <Button onClick={goNext}>Tiếp theo</Button>
            </div>
          </div>
        </Card>
      )}

      {/* Step 3: Xác nhận */}
      {step === 3 && (
        <Card title="Xác nhận yêu cầu" description="Kiểm tra lại trước khi gửi.">
          <form onSubmit={handleSubmit} className="space-y-4">
            <dl className="divide-y divide-line">
              <Row label="Loại dịch vụ" value={selectedCategory?.name ?? '—'} />
              <Row
                label="Khu vực"
                value={
                  form.areaId
                    ? areas.find((a) => a.id === Number(form.areaId))?.name ?? '—'
                    : 'Không chọn'
                }
              />
              <Row label="Tiêu đề" value={form.title} />
              <Row label="Mô tả" value={form.description} />
              <Row label="Địa chỉ" value={form.addressLine} />
              <Row
                label="Ngân sách"
                value={form.budgetRef ? formatCurrency(Number(form.budgetRef)) : 'Không giới hạn'}
              />
              <Row label="Nhận báo giá trong" value={`${form.biddingDeadlineDays} ngày`} />
              {form.mediaUrl && <Row label="Ảnh đính kèm" value="Đã tải 1 ảnh" />}
            </dl>

            <div className="flex justify-between pt-4">
              <Button variant="secondary" onClick={() => setStep(2)}>
                Quay lại sửa
              </Button>
              <Button type="submit" loading={saving}>
                {saving ? 'Đang gửi...' : 'Gửi yêu cầu'}
              </Button>
            </div>
          </form>
        </Card>
      )}
    </DashboardLayout>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 py-3 sm:flex-row sm:gap-4">
      <dt className="w-40 shrink-0 text-sm text-ink-soft">{label}</dt>
      <dd className="text-sm">{value}</dd>
    </div>
  );
}
