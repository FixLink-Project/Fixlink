/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Primary — Electric Sapphire Blue (Trust & Precision)
        brand: {
          DEFAULT: '#2563EB',
          strong: '#1D4ED8',
          glow: '#3B82F6',
          light: '#EFF6FF',
          ink: '#1E40AF'
        },
        // Accent — Energy Amber / Warm Orange (Action & Stars)
        accent: {
          DEFAULT: '#F59E0B',
          strong: '#D97706',
          soft: '#FEF3C7',
          orange: '#EA580C'
        },
        // Surfaces — Clean Modern Consumer Marketplace
        surface: {
          DEFAULT: '#F8FAFC',    // Soft clean canvas
          card: '#FFFFFF',       // Pure crisp white card
          elevated: '#F1F5F9',   // Light gray highlight / input background
          dark: '#0F172A',       // Rich slate for dark hero / footer accents
          navy: '#1E293B'
        },
        // Text
        ink: {
          DEFAULT: '#0F172A',    // Dark slate primary text
          soft: '#475569',       // Slate-600 friendly secondary
          muted: '#94A3B8'       // Slate-400 caption text
        },
        // Borders
        line: {
          DEFAULT: '#E2E8F0',    // Slate-200 clean border
          soft: '#F1F5F9',
          strong: '#CBD5E1'
        },
        // Status
        success: { DEFAULT: '#10B981', soft: '#ECFDF5', strong: '#047857' },
        danger: { DEFAULT: '#EF4444', soft: '#FEF2F2', strong: '#B91C1C' },
        warning: { DEFAULT: '#F59E0B', soft: '#FFFBEB', strong: '#B45309' },
        info: { DEFAULT: '#0284C7', soft: '#F0F9FF', strong: '#0369A1' },
        // Compat
        card: '#FFFFFF',
        rose: { DEFAULT: '#EF4444', soft: '#FEF2F2' },
        amber: { DEFAULT: '#F59E0B', soft: '#FFFBEB' }
      },
      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'Inter', 'system-ui', 'sans-serif'],
        sans: ['Inter', '"Plus Jakarta Sans"', 'system-ui', 'sans-serif']
      },
      borderRadius: {
        xl: '14px',
        '2xl': '18px',
        '3xl': '24px'
      },
      boxShadow: {
        sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        card: '0 1px 3px 0 rgba(0, 0, 0, 0.06), 0 1px 2px -1px rgba(0, 0, 0, 0.04)',
        elevated: '0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04)',
        hover: '0 20px 25px -5px rgba(37, 99, 235, 0.1), 0 8px 10px -6px rgba(37, 99, 235, 0.05)',
        glow: '0 4px 14px 0 rgba(37, 99, 235, 0.25)',
        'glow-lg': '0 10px 30px 0 rgba(37, 99, 235, 0.35)',
        'glow-amber': '0 4px 14px 0 rgba(245, 158, 11, 0.3)'
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'hero-mesh':
          'radial-gradient(at 10% 20%, rgba(37, 99, 235, 0.06) 0, transparent 40%), radial-gradient(at 90% 80%, rgba(245, 158, 11, 0.06) 0, transparent 40%), radial-gradient(at 50% 50%, rgba(2, 132, 199, 0.04) 0, transparent 50%)'
      },
      animation: {
        'fade-in': 'fadeIn 0.35s ease-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        float: 'float 5s ease-in-out infinite',
        'pulse-subtle': 'pulseSubtle 2.5s ease-in-out infinite'
      },
      keyframes: {
        fadeIn: {
          from: { opacity: '0' },
          to: { opacity: '1' }
        },
        slideUp: {
          from: { opacity: '0', transform: 'translateY(14px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        },
        slideDown: {
          from: { opacity: '0', transform: 'translateY(-10px)' },
          to: { opacity: '1', transform: 'translateY(0)' }
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-8px)' }
        },
        pulseSubtle: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.85' }
        }
      }
    }
  },
  plugins: []
};
