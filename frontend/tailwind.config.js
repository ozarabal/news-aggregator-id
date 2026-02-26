/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#f5f0e8',
        ink: '#0d0d0d',
        ink2: '#1a1a1a',
        ink3: '#2a2a2a',
        accent: '#c41e3a',
        'accent-dark': '#8b1226',
        muted: '#8a8070',
      },
      fontFamily: {
        display: ['"Playfair Display"', 'Georgia', 'serif'],
        body: ['Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        'display-xl': ['3.5rem', { lineHeight: '1.05', letterSpacing: '-0.02em' }],
        'display-lg': ['2.25rem', { lineHeight: '1.1', letterSpacing: '-0.015em' }],
        'display-md': ['1.625rem', { lineHeight: '1.2' }],
        'display-sm': ['1.25rem', { lineHeight: '1.3' }],
      },
    },
  },
  plugins: [require('@tailwindcss/typography')],
}
