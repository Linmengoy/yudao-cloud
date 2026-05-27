"use client";

import { useEffect } from "react";
import { X } from "lucide-react";
import { useAuth } from "./auth-store";
import { AuthPanel } from "./AuthPanel";

export function AuthModal() {
  const { authMode, modalOpen, closeModal } = useAuth();

  useEffect(() => {
    if (!modalOpen) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") closeModal();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [closeModal, modalOpen]);

  if (!modalOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <button className="absolute inset-0 bg-black/50" onClick={closeModal} aria-label="关闭登录弹窗" />

      <div className="relative max-h-[calc(100vh-32px)] w-full max-w-[460px] overflow-auto rounded-2xl border border-border-warm bg-background p-6 shadow-[rgba(0,0,0,0.1)_0px_4px_12px] sm:p-8">
        <button
          onClick={closeModal}
          className="absolute right-4 top-4 rounded-md p-1 text-muted-gray hover:text-charcoal"
          aria-label="关闭"
        >
          <X className="size-5" />
        </button>

        <div className="flex flex-col items-center text-center">
          <div className="flex size-12 items-center justify-center rounded-full bg-charcoal text-lg font-semibold text-off-white">
            C
          </div>
          <h2 className="mt-3 text-xl font-semibold text-charcoal">欢迎来到 Copse</h2>
          <p className="mt-1 text-sm text-muted-gray">登录后即可保存作品、查看任务进度和资产</p>
        </div>

        <div className="mt-6">
          <AuthPanel key={authMode} initialMode={authMode} onSuccess={closeModal} />
        </div>
      </div>
    </div>
  );
}
