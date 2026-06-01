/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import { Download, X } from "lucide-react";
import { cn } from "@/lib/utils";
import type { MediaPreviewItem } from "./types";
import { compactInfo, downloadMedia } from "./media-preview-utils";

type MediaPreviewDialogProps = {
  item: MediaPreviewItem | null;
  open: boolean;
  onClose: () => void;
};

export function MediaPreviewDialog({ item, open, onClose }: MediaPreviewDialogProps) {
  const [copyState, setCopyState] = useState<"idle" | "copied">("idle");
  const information = useMemo(() => compactInfo(item?.information ?? []), [item?.information]);

  const handleClose = useCallback(() => {
    setCopyState("idle");
    onClose();
  }, [onClose]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") handleClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleClose, open]);

  async function copyPrompt() {
    if (!item?.prompt) return;
    try {
      await navigator.clipboard.writeText(item.prompt);
      setCopyState("copied");
      window.setTimeout(() => setCopyState("idle"), 1200);
    } catch {
      setCopyState("idle");
    }
  }

  if (typeof document === "undefined") return null;

  return createPortal(
    <AnimatePresence>
      {open && item && (
        <motion.div
          className="fixed inset-0 z-[400] flex bg-charcoal/85 p-3 text-charcoal sm:p-4 dark:bg-black/88 dark:text-[#f4efe6]"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.16, ease: "easeOut" }}
          onMouseDown={handleClose}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label={item.title ?? "媒体预览"}
            className="grid min-h-0 w-full overflow-hidden rounded-2xl border border-border-warm bg-background shadow-[0_20px_80px_rgba(0,0,0,0.28)] dark:border-white/18 dark:bg-[#11100e] dark:shadow-[0_20px_80px_rgba(0,0,0,0.38)] lg:grid-cols-[minmax(0,1fr)_390px]"
            initial={{ scale: 0.985, y: 8 }}
            animate={{ scale: 1, y: 0 }}
            exit={{ scale: 0.985, y: 8 }}
            transition={{ duration: 0.18, ease: "easeOut" }}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="min-h-[46vh] bg-charcoal p-3 dark:bg-[#11100e] sm:p-4 lg:min-h-0">
              <div className="flex h-full min-h-0 items-center justify-center overflow-hidden rounded-xl bg-black/20 dark:bg-black">
                {item.kind === "video" ? (
                  <video src={item.url} className="max-h-full max-w-full object-contain" controls />
                ) : (
                  <img src={item.url} alt={item.title ?? item.fileName ?? ""} className="max-h-full max-w-full object-contain" draggable={false} />
                )}
              </div>
            </div>

            <aside className="flex min-h-0 flex-col border-t border-border-warm bg-background dark:border-white/12 dark:bg-[#14120f] lg:border-l lg:border-t-0">
              <div className="flex items-center justify-between gap-3 px-5 py-4">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-charcoal dark:text-[#f4efe6]">{item.title ?? item.fileName ?? "Preview"}</p>
                  {item.projectName && <p className="mt-0.5 truncate text-xs text-muted-gray dark:text-[#a7a096]">{item.projectName}</p>}
                </div>
                <button
                  type="button"
                  onClick={handleClose}
                  className="flex size-9 shrink-0 items-center justify-center rounded-full text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px] dark:text-[#a7a096] dark:hover:bg-white/8 dark:hover:text-[#f4efe6] dark:focus:shadow-[rgba(244,239,230,0.08)_0px_4px_14px]"
                  aria-label="关闭预览"
                >
                  <X className="size-5" />
                </button>
              </div>

              <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-5">
                {item.prompt && (
                  <section>
                    <div className="mb-2 flex items-center gap-2">
                      <h2 className="text-sm font-semibold text-muted-gray dark:text-[#a7a096]">Prompt</h2>
                      <span
                        className={cn(
                          "rounded-full border border-border-warm bg-muted px-3 py-1 text-xs text-muted-gray transition-colors dark:border-white/10 dark:bg-white/6 dark:text-[#a7a096]",
                          copyState === "copied" && "border-[rgba(28,28,28,0.4)] text-charcoal dark:border-white/28 dark:text-[#f4efe6]"
                        )}
                      >
                        {copyState === "copied" ? "Copied" : "Click content to copy"}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={copyPrompt}
                      className="max-h-56 w-full overflow-y-auto rounded-2xl border border-border-warm bg-muted px-4 py-3 text-left text-sm leading-6 text-charcoal/85 transition-colors hover:border-[rgba(28,28,28,0.4)] focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px] dark:border-white/10 dark:bg-white/6 dark:text-[#f4efe6]/85 dark:hover:border-white/28 dark:focus:shadow-[rgba(244,239,230,0.08)_0px_4px_14px]"
                    >
                      {item.prompt}
                    </button>
                  </section>
                )}

                {information.length > 0 && (
                  <section className="mt-7">
                    <h2 className="mb-3 text-sm font-semibold text-muted-gray dark:text-[#a7a096]">Information</h2>
                    <dl className="rounded-2xl border border-border-warm bg-muted px-4 py-3 dark:border-white/10 dark:bg-white/6">
                      {information.map((info) => (
                        <div key={`${info.label}-${String(info.value)}`} className="grid grid-cols-[110px_minmax(0,1fr)] gap-3 py-1.5 text-sm">
                          <dt className="text-muted-gray dark:text-[#a7a096]">{info.label}</dt>
                          <dd className="min-w-0 break-words font-medium text-charcoal dark:text-[#f4efe6]">{String(info.value)}</dd>
                        </div>
                      ))}
                    </dl>
                  </section>
                )}
              </div>

              <div className="border-t border-border-warm p-5 dark:border-white/12">
                <button
                  type="button"
                  onClick={() => downloadMedia(item)}
                  className="flex w-full items-center justify-center gap-2 rounded-md bg-charcoal px-4 py-2.5 text-sm font-medium text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] transition-opacity active:opacity-80 dark:border dark:border-white/14 dark:bg-[#1f1d19] dark:text-[#f4efe6] dark:shadow-[rgba(255,255,255,0.08)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.45)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.24)_0px_1px_2px_0px] dark:hover:bg-[#27241f]"
                >
                  <Download className="size-4" />
                  Download
                </button>
              </div>
            </aside>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body
  );
}
