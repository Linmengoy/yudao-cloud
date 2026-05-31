"use client";

import { Moon, Sun } from "lucide-react";
import { useTheme } from "./theme-store";
import { cn } from "@/lib/utils";

export function ThemeToggle({
  className,
  showLabel = false,
}: {
  className?: string;
  showLabel?: boolean;
}) {
  const { mode, toggleMode } = useTheme();
  const isDark = mode === "dark";
  const label = isDark ? "切换到日间模式" : "切换到夜间模式";

  return (
    <button
      type="button"
      onClick={toggleMode}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-md px-2.5 py-2 text-sm text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]",
        className
      )}
      aria-label={label}
      title={label}
    >
      {isDark ? <Sun className="size-4" /> : <Moon className="size-4" />}
      {showLabel && <span>{isDark ? "日间模式" : "夜间模式"}</span>}
    </button>
  );
}
