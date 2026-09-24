/**
 * FixLink - Thành phần dùng chung: thanh phân trang đánh số (Numbered Pagination) + thanh chia tab (Tab Bar).
 *
 * Module ESM thuần dùng cho mọi trang HTML tĩnh của FixLink, không phụ thuộc framework.
 * Số trang hiển thị được tính bằng thuật toán cửa sổ trượt (sliding window) kèm dấu "..." rút gọn.
 */

export const ELLIPSIS = '...';

/**
 * Tính danh sách số trang cần render (kèm dấu "..." khi tổng số trang lớn).
 * Ví dụ: buildPageNumbers(5, 20) -> [1, '...', 4, 5, 6, '...', 20]
 *
 * @param {number} currentPage Trang hiện tại (1-based)
 * @param {number} totalPages Tổng số trang
 * @param {number} windowSize Số trang liền kề hiển thị mỗi bên của trang hiện tại
 * @returns {Array<number|string>}
 */
export function buildPageNumbers(currentPage, totalPages, windowSize = 1) {
  const total = Math.max(1, Math.floor(Number(totalPages) || 1));
  const current = Math.min(Math.max(1, Math.floor(Number(currentPage) || 1)), total);
  const windowRange = Math.max(0, Math.floor(windowSize));

  if (total <= windowRange * 2 + 3) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }

  const pages = [1];
  const start = Math.max(2, current - windowRange);
  const end = Math.min(total - 1, current + windowRange);

  if (start > 2) {
    pages.push(ELLIPSIS);
  }
  for (let page = start; page <= end; page++) {
    pages.push(page);
  }
  if (end < total - 1) {
    pages.push(ELLIPSIS);
  }
  pages.push(total);

  return pages;
}

/**
 * Render thanh phân trang đánh số vào một element container.
 *
 * @param {HTMLElement} container
 * @param {object} options
 * @param {number} options.currentPage Trang hiện tại
 * @param {number} options.totalPages Tổng số trang
 * @param {number} [options.totalItems] Tổng bản ghi (hiển thị thông tin)
 * @param {number} [options.limit] Số bản ghi/trang (hiển thị thông tin)
 * @param {Function} options.onPageChange Callback khi người dùng chọn trang: (page) => void
 * @param {number} [options.windowSize] Số trang liền kề mỗi bên
 */
export function renderPagination(container, options) {
  const {
    currentPage,
    totalPages,
    totalItems = 0,
    limit = 0,
    onPageChange,
    windowSize = 1
  } = options || {};

  if (!container) return;

  container.innerHTML = '';
  const total = Math.max(1, Math.floor(Number(totalPages) || 1));
  const current = Math.min(Math.max(1, Math.floor(Number(currentPage) || 1)), total);

  const goTo = page => {
    const target = Math.min(Math.max(1, page), total);
    if (target !== current && typeof onPageChange === 'function') {
      onPageChange(target);
    }
  };

  const createButton = ({ label, page, disabled = false, active = false, title = '' }) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'page-btn';
    button.textContent = label;
    if (title) button.title = title;
    if (active) button.classList.add('active');
    button.disabled = Boolean(disabled);
    if (!disabled && !active && page) {
      button.addEventListener('click', () => goTo(page));
    }
    return button;
  };

  const info = document.createElement('span');
  info.className = 'page-info';
  const shownItems = limit > 0 ? Math.min(limit, Math.max(totalItems - (current - 1) * limit, 0)) : 0;
  info.textContent = limit > 0
    ? `Hiển thị ${shownItems}/${totalItems} bản ghi`
    : `Trang ${current}/${total}`;
  container.appendChild(info);

  const nav = document.createElement('div');
  nav.className = 'page-nav';

  nav.appendChild(createButton({ label: '«', page: current - 1, disabled: current <= 1, title: 'Trang đầu' }));
  nav.appendChild(createButton({ label: '‹', page: current - 1, disabled: current <= 1, title: 'Trang trước' }));

  buildPageNumbers(current, total, windowSize).forEach(item => {
    if (item === ELLIPSIS) {
      const dots = document.createElement('span');
      dots.className = 'page-ellipsis';
      dots.textContent = ELLIPSIS;
      nav.appendChild(dots);
      return;
    }
    nav.appendChild(createButton({ label: String(item), page: item, active: item === current }));
  });

  nav.appendChild(createButton({ label: '›', page: current + 1, disabled: current >= total, title: 'Trang sau' }));
  nav.appendChild(createButton({ label: '»', page: current + 1, disabled: current >= total, title: 'Trang cuối' }));

  container.appendChild(nav);
}

/**
 * Render thanh chia tab (Tab Bar) kèm số lượng bản ghi của từng tab.
 *
 * @param {HTMLElement} container
 * @param {object} options
 * @param {Array<{code: string, label: string}>} options.tabs
 * @param {string} options.activeTab
 * @param {Function} options.onTabChange Callback khi đổi tab: (tabCode) => void
 * @param {Object<string, number>} [options.counts] Số bản ghi theo mã tab (tuỳ chọn)
 */
export function renderTabs(container, options) {
  const { tabs = [], activeTab, onTabChange, counts = {} } = options || {};
  if (!container) return;

  container.innerHTML = '';

  tabs.forEach(tab => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'tab-btn';
    if (tab.code === activeTab) button.classList.add('active');
    button.dataset.tab = tab.code;

    const label = document.createElement('span');
    label.textContent = tab.label;
    button.appendChild(label);

    if (typeof counts[tab.code] === 'number') {
      const badge = document.createElement('span');
      badge.className = 'tab-count';
      badge.textContent = counts[tab.code];
      button.appendChild(badge);
    }

    button.addEventListener('click', () => {
      if (tab.code !== activeTab && typeof onTabChange === 'function') {
        onTabChange(tab.code);
      }
    });

    container.appendChild(button);
  });
}

/** Rút gọn nội dung dài (mô tả sự cố) trên bảng danh sách. */
export function truncate(text, maxLength = 80) {
  const value = (text || '').trim();
  if (value.length <= maxLength) return value;
  return `${value.slice(0, maxLength - 1)}…`;
}

/** Định dạng thời gian hiển thị theo chuẩn Việt Nam. */
export function formatDateTime(value) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '--';
  return date.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

/** Định dạng tiền tệ VND. */
export function formatCurrency(value) {
  const amount = Number(value) || 0;
  return `${amount.toLocaleString('vi-VN')} ₫`;
}

/** Thoát ký tự HTML để render an toàn dữ liệu người dùng nhập. */
export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
