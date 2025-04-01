/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
      "./server/src/main/kotlin/**/*.kt"
  ],
  theme: {
    extend: {
      keyframes: {
        blink: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0' }
        },
        damage: {
          '0%': { backgroundColor: 'rgba(239, 68, 68, 0.7)', transform: 'translateX(0)' },
          '25%': { transform: 'translateX(-5px)' },
          '50%': { backgroundColor: 'rgba(239, 68, 68, 0.7)', transform: 'translateX(5px)' },
          '75%': { transform: 'translateX(-5px)' },
          '100%': { backgroundColor: 'transparent', transform: 'translateX(0)' }
        }
      },
      animation: {
        'blink': 'blink 1s steps(1) infinite',
        'damage': 'damage 0.5s ease-in-out'
      }
    },
  },
  plugins: [],
  darkMode: 'media'
}
