"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type CanvasNodeTitleProps = {
  children: ReactNode;
  fixedUiScale: number;
  left?: number;
  maxWidth?: number;
  className?: string;
};

export function CanvasNodeTitle({
  children,
  fixedUiScale,
  left = 0,
  maxWidth,
  className,
}: CanvasNodeTitleProps) {
  return (
    <div
      className={cn(
        "nodrag nowheel pointer-events-auto absolute flex origin-bottom-left items-center gap-1.5 bg-transparent px-1 text-sm font-medium text-muted-gray",
        className
      )}
      style={{
        left,
        top: -8 * fixedUiScale,
        maxWidth,
        transform: `translateY(-100%) scale(${fixedUiScale})`,
      }}
    >
      {children}
    </div>
  );
}
