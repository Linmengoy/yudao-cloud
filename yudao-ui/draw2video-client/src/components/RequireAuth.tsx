"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-store";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { loggedIn, loading, openModal } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!loggedIn) {
      router.push("/");
      // Small delay so the push starts before modal opens
      setTimeout(() => openModal(), 100);
    }
  }, [loggedIn, loading, router, openModal]);

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="text-sm text-muted-gray">加载中...</div>
      </div>
    );
  }

  if (!loggedIn) return null;

  return <>{children}</>;
}
