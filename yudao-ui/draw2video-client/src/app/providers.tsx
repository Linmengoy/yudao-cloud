"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { AuthProvider } from "@/features/auth/auth-store";
import { AuthModal } from "@/features/auth/AuthModal";
import { ThemeProvider } from "@/features/theme/theme-store";

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            retry: 1,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          {children}
          <AuthModal />
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
