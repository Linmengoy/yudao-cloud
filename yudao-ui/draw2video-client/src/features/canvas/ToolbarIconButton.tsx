"use client";

import { useState, useRef, useEffect, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { positionBelow } from "./floating-position";

interface ToolbarIconButtonProps {
  label: string;
  icon: ReactNode;
  onClick: () => void;
  disabled?: boolean;
  variant?: "default" | "primary" | "danger";
  className?: string;
}

export function ToolbarIconButton({
  label,
  icon,
  onClick,
  disabled,
  variant = "default",
  className,
}: ToolbarIconButtonProps) {
  const [showTooltip, setShowTooltip] = useState(false);
  const [tooltipPos, setTooltipPos] = useState<{ x: number; y: number } | null>(null);
  const btnRef = useRef<HTMLButtonElement>(null);
  const tipRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!showTooltip || !btnRef.current || !tipRef.current) return;
    const rect = btnRef.current.getBoundingClientRect();
    const { width, height } = tipRef.current.getBoundingClientRect();
    setTooltipPos(positionBelow(rect, width, height));
  }, [showTooltip]);

  const baseClass =
    variant === "primary"
      ? "flex items-center gap-1.5 rounded-md bg-charcoal px-3 py-1.5 text-xs text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80 disabled:opacity-50"
      : variant === "danger"
        ? "flex items-center gap-1.5 rounded-md border border-destructive bg-destructive px-3 py-1.5 text-xs text-off-white active:opacity-80"
        : "flex items-center gap-1.5 rounded-md border border-border-warm px-3 py-1.5 text-xs text-muted-gray transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal disabled:opacity-50";

  return (
    <>
      <button
        ref={btnRef}
        onClick={onClick}
        disabled={disabled}
        onMouseEnter={() => setShowTooltip(true)}
        onMouseLeave={() => setShowTooltip(false)}
        className={`${baseClass} ${className ?? ""}`}
      >
        {icon}
        {label}
      </button>

      {showTooltip && createPortal(
        <div
          ref={tipRef}
          className="pointer-events-none fixed z-[200] whitespace-nowrap rounded-lg bg-charcoal px-2.5 py-1.5 text-off-white shadow-lg"
          style={tooltipPos ? { left: tooltipPos.x, top: tooltipPos.y } : { visibility: "hidden" }}
        >
          <span className="text-[11px]">{label}</span>
        </div>,
        document.body
      )}
    </>
  );
}
