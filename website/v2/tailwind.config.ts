import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#000000",
        foreground: "#ffffff",
        primary: "#6C5CE7",
        accent: "#00E5FF",
        muted: "#888888",
        glass: "rgba(255, 255, 255, 0.03)",
      },
      fontFamily: {
        sans: ["Plus Jakarta Sans", "Inter", "sans-serif"],
      },
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "hero-gradient": "radial-gradient(circle at 50% 50%, #1a1a2e 0%, #000000 100%)",
      },
      boxShadow: {
        "glow": "0 0 50px -10px rgba(108, 92, 231, 0.3)",
      }
    },
  },
  plugins: [],
};
export default config;
