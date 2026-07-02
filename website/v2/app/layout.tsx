import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";

const plusJakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-plus-jakarta",
});

export const metadata: Metadata = {
  metadataBase: new URL("http://localhost:3000"),
  title: "Melodify | Experience Music. Reimagined.",
  description: "A modern offline music player crafted for Android with beautiful design and premium performance.",
  openGraph: {
    title: "Melodify | Experience Music. Reimagined.",
    description: "The official website for the Melodify Android Music Player.",
    type: "website",
    images: ["/screenshots/home.png"],
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={plusJakarta.variable}>
      <head>
        <link rel="icon" href="/logo.png" />
      </head>
      <body className="antialiased">{children}</body>
    </html>
  );
}
