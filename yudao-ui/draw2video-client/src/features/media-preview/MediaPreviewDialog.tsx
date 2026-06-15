/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, Download, Save, Star, Trash2, X } from "lucide-react";
import {
  getAssetAuditStatusLabel,
  getAssetVisibilityLabel,
} from "@/features/assets/asset-dictionaries";
import { cn } from "@/lib/utils";
import type { MediaPreviewItem } from "./types";
import { compactInfo, downloadMedia } from "./media-preview-utils";
import { PreviewVideoPlayer } from "./PreviewVideoPlayer";

type MediaPreviewDialogProps = {
  item: MediaPreviewItem | null;
  items?: MediaPreviewItem[];
  currentIndex?: number;
  open: boolean;
  onClose: () => void;
  onNavigate?: (index: number) => void;
  onSetPrimary?: () => void;
};

export function MediaPreviewDialog({ item, items, currentIndex = 0, open, onClose, onNavigate, onSetPrimary }: MediaPreviewDialogProps) {
  const [copyState, setCopyState] = useState<"idle" | "copied">("idle");
  const information = useMemo(() => compactInfo(item?.information ?? []), [item?.information]);
  const editableAsset = item?.editableAsset;
  const galleryItems = items?.filter((galleryItem) => galleryItem.url) ?? [];
  const hasGallery = galleryItems.length > 1;
  const canSetPrimary = Boolean(onSetPrimary && hasGallery);
  const activeIndex = Math.min(Math.max(currentIndex, 0), Math.max(0, galleryItems.length - 1));

  const handleClose = useCallback(() => {
    setCopyState("idle");
    onClose();
  }, [onClose]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") handleClose();
      if (event.key === "ArrowLeft" && hasGallery) onNavigate?.((activeIndex - 1 + galleryItems.length) % galleryItems.length);
      if (event.key === "ArrowRight" && hasGallery) onNavigate?.((activeIndex + 1) % galleryItems.length);
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activeIndex, galleryItems.length, handleClose, hasGallery, onNavigate, open]);

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
              <div className="relative flex h-full min-h-0 items-center justify-center overflow-hidden rounded-xl bg-black/20 dark:bg-black">
                {item.kind === "video" ? (
                  <PreviewVideoPlayer src={item.url} />
                ) : (
                  <img src={item.url} alt={item.title ?? item.fileName ?? ""} className="max-h-full max-w-full object-contain" draggable={false} />
                )}
                {hasGallery && (
                  <>
                    <button
                      type="button"
                      onClick={() => onNavigate?.((activeIndex - 1 + galleryItems.length) % galleryItems.length)}
                      className="absolute left-4 top-1/2 flex size-11 -translate-y-1/2 items-center justify-center rounded-full bg-black/45 text-off-white shadow-[0_10px_30px_rgba(0,0,0,0.32)] backdrop-blur-md transition-colors hover:bg-black/65"
                      aria-label="上一张"
                    >
                      <ChevronLeft className="size-6" />
                    </button>
                    <button
                      type="button"
                      onClick={() => onNavigate?.((activeIndex + 1) % galleryItems.length)}
                      className="absolute right-4 top-1/2 flex size-11 -translate-y-1/2 items-center justify-center rounded-full bg-black/45 text-off-white shadow-[0_10px_30px_rgba(0,0,0,0.32)] backdrop-blur-md transition-colors hover:bg-black/65"
                      aria-label="下一张"
                    >
                      <ChevronRight className="size-6" />
                    </button>
                    <div className="absolute bottom-4 rounded-full bg-black/45 px-3 py-1 text-xs font-medium text-off-white shadow backdrop-blur-md">
                      {activeIndex + 1} / {galleryItems.length}
                    </div>
                  </>
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
                {editableAsset && (
                  <section>
                    <div className="grid gap-3">
                      <label className="text-sm font-medium text-charcoal dark:text-[#f4efe6]">
                        标题
                        <input
                          value={editableAsset.title}
                      onChange={(event) => editableAsset.onChange({ title: event.target.value })}
                      className="input-base mt-1"
                      disabled={!editableAsset.canEdit}
                    />
                      </label>
                      <label className="text-sm font-medium text-charcoal dark:text-[#f4efe6]">
                        标签
                        <input
                          value={editableAsset.tags}
                          onChange={(event) => editableAsset.onChange({ tags: event.target.value })}
                      className="input-base mt-1"
                      placeholder="用逗号分隔"
                      disabled={!editableAsset.canEdit}
                    />
                      </label>
                      <label className="text-sm font-medium text-charcoal dark:text-[#f4efe6]">
                        描述
                        <textarea
                          value={editableAsset.description}
                      onChange={(event) => editableAsset.onChange({ description: event.target.value })}
                      className="input-base mt-1 min-h-24 resize-y"
                      disabled={!editableAsset.canEdit}
                    />
                      </label>
                    </div>
                    <button
                      type="button"
                      onClick={() => editableAsset.onSave()}
                      disabled={editableAsset.saving || !editableAsset.canEdit}
                      className="mt-4 inline-flex items-center gap-2 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80 disabled:opacity-50 dark:bg-[#1c1c1c] dark:text-[#fcfbf8]"
                    >
                      <Save className="size-4" />
                      保存
                    </button>
                  </section>
                )}

                {editableAsset && (
                  <section className="mt-7">
                    <h2 className="mb-3 text-sm font-semibold text-muted-gray dark:text-[#a7a096]">状态</h2>
                    <div className="flex flex-wrap gap-2">
                      <span className="inline-flex rounded-full border border-border-warm bg-background px-2 py-1 text-xs text-muted-gray dark:border-white/10 dark:bg-white/6 dark:text-[#a7a096]">
                        {getAssetAuditStatusLabel(editableAsset.auditStatus)}
                      </span>
                      <span className="inline-flex rounded-full border border-border-warm bg-muted px-2 py-1 text-xs text-muted-gray dark:border-white/10 dark:bg-white/6 dark:text-[#a7a096]">
                        {getAssetVisibilityLabel(editableAsset.visibility)}
                      </span>
                    </div>
                    {editableAsset.auditReason && <p className="mt-2 text-xs text-destructive">{editableAsset.auditReason}</p>}
                    {editableAsset.onVisibilityChange && (
                      <label className="mt-4 block text-sm text-charcoal dark:text-[#f4efe6]">
                        可见性
                        <select
                          value={editableAsset.visibility || "PRIVATE"}
                          onChange={(event) => editableAsset.onVisibilityChange?.(event.target.value)}
                          className="input-base mt-1"
                          disabled={!editableAsset.canEdit}
                        >
                          {(["PRIVATE", "PUBLIC", "LINK", "TENANT"] as const).map((visibility) => (
                            <option key={visibility} value={visibility}>{getAssetVisibilityLabel(visibility)}</option>
                          ))}
                        </select>
                        {!editableAsset.canPublish && <span className="mt-1 block text-xs text-muted-gray">审核通过且状态正常后才能公开资产。</span>}
                      </label>
                    )}
                  </section>
                )}

                {item.prompt && !editableAsset && (
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

                {editableAsset?.taskId && (
                  <Link href={`/tasks/${editableAsset.taskId}`} className="mt-5 inline-flex text-sm text-charcoal underline underline-offset-4 dark:text-[#f4efe6]">
                    查看来源任务
                  </Link>
                )}
              </div>

              <div className="border-t border-border-warm p-5 dark:border-white/12">
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => editableAsset?.onDownload ? editableAsset.onDownload() : downloadMedia(item)}
                    disabled={editableAsset ? !editableAsset.canDownload : false}
                    className="flex flex-1 items-center justify-center gap-2 rounded-md bg-charcoal px-4 py-2.5 text-sm font-medium text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] transition-opacity active:opacity-80 disabled:opacity-50 dark:border dark:border-white/14 dark:bg-[#1f1d19] dark:text-[#f4efe6] dark:shadow-[rgba(255,255,255,0.08)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.45)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.24)_0px_1px_2px_0px] dark:hover:bg-[#27241f]"
                  >
                    <Download className="size-4" />
                    下载
                  </button>
                  {canSetPrimary && (
                    <button
                      type="button"
                      onClick={() => onSetPrimary?.()}
                      className="flex flex-1 items-center justify-center gap-2 rounded-md border border-border-warm bg-background px-4 py-2.5 text-sm font-medium text-charcoal shadow-[rgba(0,0,0,0.05)_0px_1px_2px_0px] transition-colors hover:bg-muted active:opacity-80 dark:border-white/12 dark:bg-white/6 dark:text-[#f4efe6] dark:hover:bg-white/10"
                    >
                      <Star className="size-4" />
                      设为首图
                    </button>
                  )}
                  {editableAsset?.onDelete && (
                    <button
                      type="button"
                      onClick={() => editableAsset.onDelete?.()}
                      disabled={!editableAsset.canDelete}
                      className="flex items-center justify-center gap-2 rounded-md border border-border-warm px-4 py-2.5 text-sm text-destructive hover:bg-muted active:opacity-80 disabled:opacity-50 dark:border-white/12 dark:hover:bg-white/8"
                    >
                      <Trash2 className="size-4" />
                      删除
                    </button>
                  )}
                </div>
              </div>
            </aside>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body
  );
}
