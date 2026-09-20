/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#16211d',
        'ink-soft': '#4b5a54',
        surface: '#f4f6f5',
        card: '#ffffff',
        line: '#dde4e1',
        brand: { DEFAULT: '#0d9488', strong: '#0f766e', ink: '#06403a' },
        amber: { DEFAULT: '#f59e0b', soft: '#fef3c7' },
        rose: { DEFAULT: '#e11d48', soft: '#ffe4e6' }
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'system-ui', 'sans-serif'],
        sans: ['Inter', 'system-ui', 'sans-serif']
      },
      borderRadius: { xl: '14px' },
      boxShadow: { card: '0 1px 2px rgba(22,33,29,.04), 0 8px 24px -16px rgba(22,33,29,.18)' }
    }
  },
  plugins: []
};
