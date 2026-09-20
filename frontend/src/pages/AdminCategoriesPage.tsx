import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import Pagination from '../components/Pagination';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, toFieldErrors } from '../lib/api';
import type { PageMeta } from '../lib/types';

interface Category {
  id: number;
  code: string;
  name: string;
  description: string | null;
  iconUrl: string | null;
  displayOrder: number;
  isActive: boolean;
}

interface ImpactAssessment {
  categoryId: number;
  categoryName: string;
  activeTechniciansCount: number;
  pendingRequestsCount: number;
  canDeleteDirectly: boolean;
}

const EMPTY_FORM = {
  name: '',
  code: '',
  description: '',
  iconUrl: '',
  displayOrder: '',
  isActive: true
};

const ADMIN_NAV = [
  { to: '/quan-tri', label: 'Người dùng' },
  { to: '/quan-tri/danh-muc', label: 'Danh mục dịch vụ' }
];

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const [page, setPage] = useState(1);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');

  const [editing, setEditing] = useState<Category | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);

  const [impact, setImpact] = useState<ImpactAssessment | null>(null);
  const [deleting, setDeleting] = useState(false);

  const query = useMemo(() => {
    const params = new URLSearchParams({ page: String(page), limit: '10' });
    if (search.trim()) params.set('search', search.trim());
    return params.toString();
  }, [page, search]);

  const load = useCallback(async () => {
    setRefreshing(true);
    try {
      const res = await api.get<Category[]>(`/admin/categories?${query}`);
      setCategories(res.data);
      if (res.meta) setMeta(res.meta);
      setStatus('ready');
      setLoadError(null);
    } catch (err) {
      setLoadError(
        err instanceof ApiError ? err.message : 'Không kết nối được máy chủ. Thử tải lại trang.'
      );
      setStatus('error');
    } finally {
      setRefreshing(false);
    }
  }, [query]);

  useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setErrors({});
    setFormError(null);
    setFormOpen(true);
  }

  function openEdit(category: Category) {
    setEditing(category);
    setForm({
      name: category.name,
      code: category.code,
      description: category.description ?? '',
      iconUrl: category.iconUrl ?? '',
      displayOrder: String(category.displayOrder ?? ''),
      isActive: category.isActive
    });
    setErrors({});
    setFormError(null);
    setFormOpen(true);
  }

  function update(field: keyof typeof EMPTY_FORM, value: string | boolean) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const clientErrors: Record<string, string> = {};
    if (!form.name.trim()) clientErrors.name = 'Tên danh mục không được để trống.';
    if (!form.code.trim()) clientErrors.code = 'Mã danh mục không được để trống.';
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    const payload = {
      name: form.name.trim(),
      code: form.code.trim().toUpperCase(),
      description: form.description.trim() || null,
      iconUrl: form.iconUrl.trim() || null,
      displayOrder: form.displayOrder.trim() ? Number(form.displayOrder) : null,
      isActive: form.isActive
    };

    setSaving(true);
    try {
      if (editing) {
        await api.put(`/admin/categories/${editing.id}`, payload);
        setNote(`Đã cập nhật danh mục ${payload.name}.`);
      } else {
        await api.post('/admin/categories', payload);
        setNote(`Đã thêm danh mục ${payload.name}.`);
      }
      setFormOpen(false);
      setEditing(null);
      await load();
    } catch (err) {
      const fieldErrors = toFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else if (err instanceof ApiError) {
        setFormError(err.message);
      } else {
        setFormError('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
      }
    } finally {
      setSaving(false);
    }
  }

  async function askDelete(category: Category) {
    setNote(null);
    setFormError(null);
    try {
      const res = await api.get<ImpactAssessment>(`/admin/categories/${category.id}/impact`);
      setImpact(res.data);
    } catch (err) {
      setFormError(
        err instanceof ApiError ? err.message : 'Không kiểm tra được mức độ ảnh hưởng.'
      );
    }
  }

  async function confirmDelete() {
    if (!impact) return;
    setDeleting(true);
    try {
      await api.delete(`/admin/categories/${impact.categoryId}`);
      setNote(`Đã gỡ danh mục ${impact.categoryName} khỏi danh sách đang dùng.`);
      setImpact(null);
      await load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Không gỡ được danh mục.');
    } finally {
      setDeleting(false);
    }
  }

  return (
    <DashboardLayout
      nav={ADMIN_NAV}
      title="Danh mục dịch vụ"
      description="Danh mục quyết định khách chọn được loại việc nào và thợ nào nhận được yêu cầu."
      actions={<Button onClick={openCreate}>Thêm danh mục</Button>}
    >
      <div className="space-y-6">
        {note && <Alert tone="success">{note}</Alert>}
        {formError && !formOpen && <Alert tone="error">{formError}</Alert>}

        {impact && (
          <Alert
            tone={impact.canDeleteDirectly ? 'warning' : 'error'}
            title={`Gỡ danh mục ${impact.categoryName}?`}
          >
            <p>
              Hiện có {impact.activeTechniciansCount} thợ đang nhận nhóm việc này và{' '}
              {impact.pendingRequestsCount} yêu cầu chưa xử lý.
              {impact.canDeleteDirectly
                ? ' Gỡ bây giờ sẽ không ảnh hưởng tới dữ liệu đang chạy.'
                : ' Gỡ bây giờ sẽ khiến các yêu cầu đó mất danh mục tham chiếu.'}
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button variant="danger" loading={deleting} onClick={confirmDelete}>
                Gỡ danh mục
              </Button>
              <Button variant="secondary" disabled={deleting} onClick={() => setImpact(null)}>
                Giữ lại
              </Button>
            </div>
          </Alert>
        )}

        {formOpen && (
          <Card title={editing ? `Sửa danh mục ${editing.name}` : 'Thêm danh mục mới'}>
            <form onSubmit={handleSubmit} noValidate className="space-y-5">
              {formError && <Alert tone="error">{formError}</Alert>}

              <div className="grid gap-5 sm:grid-cols-2">
                <TextField
                  label="Tên danh mục"
                  name="name"
                  required
                  value={form.name}
                  error={errors.name}
                  placeholder="Sửa Chữa Điện Lạnh"
                  onChange={(e) => update('name', e.target.value)}
                />
                <TextField
                  label="Mã danh mục"
                  name="code"
                  required
                  value={form.code}
                  error={errors.code}
                  placeholder="DIEN_LANH"
                  hint="Viết hoa không dấu, dùng gạch dưới."
                  onChange={(e) => update('code', e.target.value)}
                />
              </div>

              <TextArea
                label="Mô tả"
                name="description"
                rows={3}
                value={form.description}
                error={errors.description}
                placeholder="Khách đọc dòng này khi chọn loại việc."
                onChange={(e) => update('description', e.target.value)}
              />

              <div className="grid gap-5 sm:grid-cols-2">
                <TextField
                  label="Thứ tự hiển thị"
                  name="displayOrder"
                  type="number"
                  min={0}
                  value={form.displayOrder}
                  error={errors.displayOrder}
                  onChange={(e) => update('displayOrder', e.target.value)}
                />
                <TextField
                  label="Biểu tượng"
                  name="iconUrl"
                  value={form.iconUrl}
                  error={errors.iconUrl}
                  placeholder="snowflake"
                  onChange={(e) => update('iconUrl', e.target.value)}
                />
              </div>

              <label className="flex min-h-[44px] cursor-pointer items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.isActive}
                  onChange={(e) => update('isActive', e.target.checked)}
                  className="h-4 w-4 accent-brand"
                />
                Đang mở cho khách chọn
              </label>

              <div className="flex flex-wrap gap-2">
                <Button type="submit" loading={saving}>
                  {editing ? 'Lưu thay đổi' : 'Thêm danh mục'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={saving}
                  onClick={() => {
                    setFormOpen(false);
                    setEditing(null);
                  }}
                >
                  Huỷ
                </Button>
              </div>
            </form>
          </Card>
        )}

        <Card>
          <form
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
            onSubmit={(e) => {
              e.preventDefault();
              setPage(1);
              setSearch(searchInput);
            }}
          >
            <div className="flex-1">
              <label htmlFor="category-search" className="mb-1.5 block text-sm font-medium">
                Tìm theo tên danh mục
              </label>
              <input
                id="category-search"
                type="search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Ví dụ: điện lạnh"
                className="min-h-[44px] w-full rounded-lg border border-line bg-card px-3.5 text-ink placeholder:text-ink-soft/60"
              />
            </div>
            <Button type="submit" variant="secondary">
              Tìm
            </Button>
          </form>
        </Card>

        {status === 'loading' && (
          <div className="space-y-2">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-16 animate-pulse rounded-xl border border-line bg-card" />
            ))}
          </div>
        )}

        {status === 'error' && (
          <Alert tone="error" title="Chưa tải được danh mục">
            {loadError}
          </Alert>
        )}

        {status === 'ready' && (
          <Card>
            {categories.length === 0 ? (
              <div className="py-10 text-center">
                <p className="font-display font-semibold">Không có danh mục nào khớp</p>
                <p className="mt-1 text-sm text-ink-soft">
                  Thử từ khoá khác, hoặc thêm danh mục mới.
                </p>
              </div>
            ) : (
              <ul className={`divide-y divide-line ${refreshing ? 'opacity-60' : ''}`}>
                {categories.map((category) => (
                  <li key={category.id} className="py-4 first:pt-0 last:pb-0">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="font-display font-semibold">{category.name}</p>
                          <StatusChip
                            status={category.isActive ? 'ACTIVE' : 'INACTIVE'}
                            label={category.isActive ? 'Đang mở' : 'Đang tắt'}
                          />
                        </div>
                        <p className="mt-1 text-sm text-ink-soft">
                          {category.description || 'Chưa có mô tả.'}
                        </p>
                        <p className="mt-1 text-sm text-ink-soft">
                          Mã {category.code}, thứ tự {category.displayOrder}
                        </p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Button variant="quiet" onClick={() => openEdit(category)}>
                          Sửa
                        </Button>
                        <Button variant="secondary" onClick={() => askDelete(category)}>
                          Gỡ
                        </Button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {meta && (
              <div className="mt-5">
                <Pagination
                  meta={meta}
                  disabled={refreshing}
                  itemNoun="danh mục"
                  onPageChange={(next) => {
                    setPage(next);
                    setNote(null);
                  }}
                />
              </div>
            )}
          </Card>
        )}
      </div>
    </DashboardLayout>
  );
}
