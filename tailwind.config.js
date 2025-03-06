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
        }
      },
      animation: {
        'blink': 'blink 1s steps(1) infinite'
      }
    },
  },
  plugins: [],
  darkMode: 'media'
}
