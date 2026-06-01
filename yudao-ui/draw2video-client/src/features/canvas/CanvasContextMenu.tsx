"use client";

import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { clampToViewport } from "./floating-position";

export interface ContextMenuState {
  x: number;
  y: number;
  visible: boolean;
}

interface MenuItem {
  label: string;
  shortcut: string;
  action: () => void;
  disabled?: boolean;
}

interface Props {
  state: ContextMenuState;
  onClose: () => void;
  onPaste: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitView: () => void;
  onZoom100: () => void;
}

export function CanvasContextMenu({
  state,
  onClose,
  onPaste,
  onZoomIn,
  onZoomOut,
  onFitView,
  onZoom100,
}: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState({ x: state.x, y: state.y });

  useEffect(() => {
    if (!state.visible) return;

    function handlePointerDown(e: PointerEvent) {
      if (ref.current && ref.current.contains(e.target as HTMLElement)) return;
      onClose();
    }
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKey);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKey);
    };
  }, [state.visible, onClose]);

  // Clamp position after mount so we know the menu dimensions
  useEffect(() => {
    if (!state.visible || !ref.current) return;
    const { width, height } = ref.current.getBoundingClientRect();
    const clamped = clampToViewport({ x: state.x, y: state.y, width, height });
    setPos(clamped);
  }, [state.visible, state.x, state.y]);

  const items: MenuItem[] = [
    { label: "粘贴", shortcut: "⌘V", action: onPaste },
    { label: "放大", shortcut: "⌘+", action: onZoomIn },
    { label: "缩小", shortcut: "⌘-", action: onZoomOut },
    { label: "显示画布所有元素", shortcut: "⇧1", action: onFitView },
    { label: "缩放至 100%", shortcut: "⌘0", action: onZoom100 },
  ];

  return (
    <AnimatePresence>
      {state.visible && (
        <motion.div
          ref={ref}
          initial={{ opacity: 0, scale: 0.98, y: -2 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: -2 }}
          transition={{ duration: 0.14, ease: "easeOut" }}
          className="fixed z-[260] min-w-[220px] origin-top-left rounded-xl border border-border-warm bg-background py-1 shadow-[0_4px_16px_rgba(0,0,0,0.08)]"
          style={{ left: pos.x, top: pos.y }}
        >
          {items.map((item) => (
            <button
              key={item.label}
              onClick={() => {
                item.action();
                onClose();
              }}
              disabled={item.disabled}
              className="flex w-full items-center justify-between px-4 py-2 text-left text-[13px] text-charcoal transition-colors hover:bg-muted disabled:opacity-40"
            >
              <span>{item.label}</span>
              <span className="ml-6 text-[11px] text-muted-gray">{item.shortcut}</span>
            </button>
          ))}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
