import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead
} from '../lib/api';
import type { NotificationItem } from '../lib/types';

function formatRelativeTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }).format(date);
  } catch {
    return isoString;
  }
}

function getNotificationTypeBadge(type: string): { label: string; icon: string; className: string } {
  switch (type) {
    case 'QUOTATION_RECEIVED':
      return { label: 'Báo giá', icon: '🏷️', className: 'bg-blue-50 text-blue-700 border-blue-200' };
    case 'QUOTATION_ACCEPTED':
      return { label: 'Chốt thợ', icon: '🤝', className: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
    case 'APPOINTMENT_REQUESTED':
    case 'APPOINTMENT_CONFIRMED':
      return { label: 'Lịch hẹn', icon: '📅', className: 'bg-purple-50 text-purple-700 border-purple-200' };
    case 'DEPOSIT_PAID':
      return { label: 'Đặt cọc', icon: '💳', className: 'bg-amber-50 text-amber-700 border-amber-200' };
    case 'FINAL_PAYMENT_PAID':
    case 'REQUEST_COMPLETED':
      return { label: 'Nghiệm thu', icon: '✅', className: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
    default:
      return { label: 'Hệ thống', icon: '🔔', className: 'bg-slate-50 text-slate-700 border-slate-200' };
  }
}

export default function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Load unread count initially and periodically
  const refreshUnreadCount = async () => {
    try {
      const res = await fetchUnreadNotificationCount();
      if (res.data) {
        setUnreadCount(res.data.unreadCount || 0);
      }
    } catch {
      // Ignore background fetch errors
    }
  };

  useEffect(() => {
    void refreshUnreadCount();
    const interval = setInterval(() => {
      void refreshUnreadCount();
    }, 20000); // 20s
    return () => clearInterval(interval);
  }, []);

  // When opening dropdown, load latest notifications
  const handleToggle = async () => {
    const nextState = !isOpen;
    setIsOpen(nextState);

    if (nextState) {
      setLoading(true);
      try {
        const res = await fetchNotifications(25);
        if (res.data) {
          setNotifications(res.data);
        }
      } catch {
        setNotifications([]);
      } finally {
        setLoading(false);
      }
    }
  };

  // Close when clicking outside or pressing Escape
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setIsOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const handleItemClick = async (item: NotificationItem) => {
    if (!item.isRead) {
      try {
        await markNotificationRead(item.id);
        setNotifications((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n))
        );
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch {
        // Continue navigation regardless
      }
    }
    setIsOpen(false);
    if (item.referenceUrl) {
      navigate(item.referenceUrl);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch {
      // Ignore error
    }
  };

  return (
    <div className="relative inline-block text-left" ref={dropdownRef}>
      {/* Bell Trigger Button */}
      <button
        type="button"
        onClick={handleToggle}
        className="relative flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors focus:outline-hidden focus:ring-2 focus:ring-brand/40"
        title="Thông báo"
        aria-label="Thông báo hệ thống"
      >
        <svg
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={1.8}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
          />
        </svg>

        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white shadow-xs animate-in zoom-in-75">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Popover Dropdown */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-white shadow-2xl border border-slate-200/80 z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/80 px-4 py-3">
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-slate-900">Thông báo</span>
              {unreadCount > 0 && (
                <span className="rounded-full bg-blue-100 px-2 py-0.5 text-[11px] font-semibold text-brand">
                  {unreadCount} chưa đọc
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllRead}
                className="text-xs font-semibold text-brand hover:text-brand-strong transition-colors"
              >
                Đã đọc tất cả
              </button>
            )}
          </div>

          {/* List of Notifications */}
          <div className="max-h-[380px] overflow-y-auto divide-y divide-slate-100">
            {loading ? (
              <div className="py-8 text-center text-xs text-slate-400 animate-pulse">
                Đang tải thông báo...
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-10 text-center px-4">
                <span className="text-3xl block mb-2">🔔</span>
                <p className="text-xs font-medium text-slate-600">Bạn chưa có thông báo nào</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Các cập nhật về báo giá, lịch hẹn và thanh toán sẽ hiển thị tại đây.
                </p>
              </div>
            ) : (
              notifications.map((item) => {
                const badge = getNotificationTypeBadge(item.type);
                return (
                  <div
                    key={item.id}
                    onClick={() => void handleItemClick(item)}
                    className={`p-3.5 hover:bg-slate-50/80 cursor-pointer transition-colors relative flex items-start gap-3 ${
                      !item.isRead ? 'bg-blue-50/40' : ''
                    }`}
                  >
                    {!item.isRead && (
                      <span className="absolute top-4 left-1.5 h-1.5 w-1.5 rounded-full bg-brand" />
                    )}
                    <span className="text-lg shrink-0 mt-0.5">{badge.icon}</span>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <span
                          className={`inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-semibold border ${badge.className}`}
                        >
                          {badge.label}
                        </span>
                        <span className="text-[10px] text-slate-400 shrink-0">
                          {formatRelativeTime(item.createdAt)}
                        </span>
                      </div>
                      <h4
                        className={`text-xs ${
                          !item.isRead ? 'font-bold text-slate-900' : 'font-medium text-slate-700'
                        }`}
                      >
                        {item.title}
                      </h4>
                      <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-2 leading-relaxed">
                        {item.message}
                      </p>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
