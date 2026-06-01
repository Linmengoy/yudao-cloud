"use client";

import type { CSSProperties } from "react";
import { motion } from "motion/react";
import { Download, Maximize2 } from "lucide-react";
import { cn } from "@/lib/utils";

type SelectedMediaToolbarProps = {
  canDownload: boolean;
  onDownload: () => void;
  onOpenPreview: () => void;
  className?: string;
  style?: CSSProperties;
  uiScale?: number;
};

export function SelectedMediaToolbar({
  canDownload,
  onDownload,
  onOpenPreview,
  className,
  style,
  uiScale = 1,
}: SelectedMediaToolbarProps) {
  return (
    <div
      className={cn(
        "nodrag nowheel absolute z-30 -translate-x-1/2",
        className
      )}
      style={style}
    >
      <motion.div
        initial={{ opacity: 0, y: 6, scale: 0.96 * uiScale }}
        animate={{ opacity: 1, y: 0, scale: uiScale }}
        exit={{ opacity: 0, y: 6, scale: 0.96 * uiScale }}
        transition={{ duration: 0.16, ease: "easeOut" }}
        className="flex items-center gap-1 rounded-full border border-border-warm bg-background p-1 shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
        style={{ transformOrigin: "bottom center" }}
      >
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onDownload();
          }}
          disabled={!canDownload}
          className="group/toolbar-button relative flex size-8 items-center justify-center rounded-full text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px] disabled:cursor-not-allowed disabled:opacity-35"
          aria-label="下载媒体"
        >
          <Download className="size-3.5" />
          <span className="pointer-events-none absolute bottom-full left-1/2 mb-2 -translate-x-1/2 whitespace-nowrap rounded-md border border-border-warm bg-background px-2 py-1 text-xs text-charcoal opacity-0 shadow-[rgba(0,0,0,0.1)_0px_4px_12px] transition-opacity group-hover/toolbar-button:opacity-100">
            下载
          </span>
        </button>
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onOpenPreview();
          }}
          className="group/toolbar-button relative flex size-8 items-center justify-center rounded-full text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          aria-label="放大查看"
        >
          <Maximize2 className="size-3.5" />
          <span className="pointer-events-none absolute bottom-full left-1/2 mb-2 -translate-x-1/2 whitespace-nowrap rounded-md border border-border-warm bg-background px-2 py-1 text-xs text-charcoal opacity-0 shadow-[rgba(0,0,0,0.1)_0px_4px_12px] transition-opacity group-hover/toolbar-button:opacity-100">
            放大
          </span>
        </button>
      </motion.div>
    </div>
  );
}
