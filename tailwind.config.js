/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/sst/ui/**/*.cljc"],
  theme: {
    extend: {},
  },
  plugins: [require('@tailwindcss/typography')],
}