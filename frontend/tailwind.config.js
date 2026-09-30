/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Design System FixLink — teal + off-white (tránh "AI look").
        // Primary — Teal (màu chủ đạo)
        brand: {
          DEFAULT: '#0d9488', // teal-600
          strong: '#0f766e', // teal-700 — hover/active
          glow: '#14b8a6', // teal-500 — dùng cho text nhấn nhẹ (không neon)
          light: '#f0fdfa', // teal-50 — nền chip/nhấn rất nhạt
          ink: '#06403a' // text trên nền brand
        },
        // Amber CHỈ dùng cho status/warning — không dùng làm accent trang trí.
        accent: {
          DEFAULT: '#f59e0b',
          strong: '#d97706',
          soft: '#fef3c7',
          orange: '#ea580c'
        },
        // Surfaces
        surface: {
          DEFAULT: '#f4f6f5', // nền chính — off-white hơi xanh
          card: '#ffffff', // card, modal
          elevated: '#ffffff',
          raised: '#ffffff',
          dark: '#16211d',
          navy: '#1f2a26'
        },
        // Text
        ink: {
          DEFAULT: '#16211d', // gần đen, không đen tuyệt đối
          soft: '#4b5a54', // text phụ
          muted: '#6b7a74' // caption
        },
        // Borders — dùng được cả border-line (cũ) lẫn border-border (spec mới)
        line: {
          DEFAULT: '#d4ddd8',
          soft: '#e4ebe7',
          strong: '#b9c6c0',
          focus: '#0d9488'
        },
        border: {
          DEFAULT: '#d4ddd8',
          focus: '#0d9488'
        },
        // Status
        success: { DEFAULT: '#10b981', soft: '#ecfdf5', strong: '#047857' },
        danger: { DEFAULT: '#e11d48', soft: '#fff1f2', strong: '#be123c' },
        warning: { DEFAULT: '#f59e0b', soft: '#fffbeb', strong: '#b45309' },
        info: { DEFAULT: '#0284c7', soft: '#f0f9ff', strong: '#0369a1' },
        // Compat
        card: '#ffffff',
        rose: { DEFAULT: '#e11d48', soft: '#fff1f2' },
        amber: { DEFAULT: '#f59e0b', soft: '#fffbeb' }
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'Inter', 'system-ui', 'sans-serif'],
        sans: ['Inter', '"Space Grotesk"', 'system-ui', 'sans-serif']
      },
      borderRadius: {
        lg: '10px',
        xl: '14px',
        '2xl': '18px'
      },
      boxShadow: {
        // Chỉ giữ shadow rất nhẹ; bỏ mọi glow/elevated "AI look".
        sm: '0 1px 2px 0 rgba(22, 33, 29, 0.05)',
        card: '0 1px 2px 0 rgba(22, 33, 29, 0.05)'
      },
      animation: {
        // Chỉ giữ hiệu ứng vào trang tinh tế; bỏ float/pulse/bounce.
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.35s ease-out'
      },
      keyframes: {
        fadeIn: {
          from: { opacity: '0' },
          to: { opacity: '1' }
        },
        slideUp: {
          from: { opacity: '0', transform: 'translateY(10px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        }
      }
    }
  },
  plugins: []
};
