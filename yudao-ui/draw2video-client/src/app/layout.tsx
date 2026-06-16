import type { Metadata } from "next";
import { getLocale, getMessages } from "next-intl/server";
import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "Copse — AI 创意平台",
  description: "AI 图片生成，释放你的创意",
};

const themeInitScript = `
(() => {
  try {
    const stored = window.localStorage.getItem("copse-theme");
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    const mode = stored === "dark" || stored === "light" ? stored : prefersDark ? "dark" : "light";
    document.documentElement.classList.toggle("dark", mode === "dark");
  } catch {
  }
})();
`;

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale} className="h-full antialiased" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className="min-h-full flex flex-col">
        <Providers locale={locale} messages={messages}>{children}</Providers>
      </body>
    </html>
  );
}
