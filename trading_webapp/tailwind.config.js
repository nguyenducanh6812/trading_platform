/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{html,js,svelte,ts}'],
  theme: {
    extend: {
      colors: {
        background: '#0a0b0d',
        card: '#16181c',
        border: '#2d2f36',
        'border-hover': '#3d3f47',
        accent: '#00ffbd',
        'accent-hover': '#00e6aa',
        'accent-dim': '#00ffbd15',
        secondary: '#00b8ff'
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif']
      },
      animation: {
        'spin-slow': 'spin 4s linear infinite'
      }
    }
  },
  plugins: []
}

