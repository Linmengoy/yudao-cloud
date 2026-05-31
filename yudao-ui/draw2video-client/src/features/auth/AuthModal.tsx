"use client";

import { useEffect } from "react";
import { X } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
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

  return (
    <AnimatePresence>
      {modalOpen && (
        <motion.div
          className="fixed inset-0 z-[100] flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18, ease: "easeOut" }}
        >
          <motion.button
            className="absolute inset-0 bg-charcoal/45"
            onClick={closeModal}
            aria-label="关闭登录弹窗"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
          />

          <motion.div
            className="relative max-h-[calc(100vh-32px)] w-full max-w-[460px] overflow-auto rounded-2xl border border-border-warm bg-background p-6 shadow-[rgba(0,0,0,0.1)_0px_4px_12px] sm:p-8"
            initial={{ opacity: 0, y: 14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.985 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          >
            <button
              onClick={closeModal}
              className="absolute right-4 top-4 rounded-md p-1 text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
              aria-label="关闭"
            >
              <X className="size-5" />
            </button>

            <div className="flex flex-col items-center text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-charcoal text-lg font-semibold text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]">
                C
              </div>
              <h2 className="mt-4 text-xl font-normal leading-tight text-charcoal">欢迎来到 Copse</h2>
              <p className="mt-2 max-w-[320px] text-sm leading-relaxed text-muted-gray">
                登录后保存项目画布、查看任务进度，并把生成结果沉淀到资产库。
              </p>
            </div>

            <div className="mt-6">
              <AuthPanel key={authMode} initialMode={authMode} onSuccess={closeModal} />
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
