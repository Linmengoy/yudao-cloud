"use client";

import { useCallback, useLayoutEffect, useMemo, useRef } from "react";
import type Muuri from "muuri";
import type { Item, LayoutFunctionCallback } from "muuri";

const DEFAULT_MIN_COLUMN_WIDTH = 220;

function getColumnCount(width: number, minColumnWidth: number) {
  return Math.max(1, Math.floor(width / minColumnWidth));
}

function wallLayout(minColumnWidth: number) {
  return (_grid: Muuri, id: number, items: Item[], width: number, _height: number, callback: LayoutFunctionCallback) => {
    const columnCount = getColumnCount(width, minColumnWidth);
    const columnWidth = width / columnCount;
    const columnHeights = Array.from({ length: columnCount }, () => 0);
    const slots: number[] = [];

    items.forEach((item) => {
      const element = item.getElement();
      const requestedSpan = Number(element?.dataset.span || 1);
      const span = Math.max(1, Math.min(columnCount, requestedSpan));
      const itemWidth = columnWidth * span;
      if (element) element.style.width = `${itemWidth}px`;

      let bestColumn = 0;
      let bestTop = Number.POSITIVE_INFINITY;
      for (let column = 0; column <= columnCount - span; column += 1) {
        const top = Math.max(...columnHeights.slice(column, column + span));
        if (top < bestTop) {
          bestTop = top;
          bestColumn = column;
        }
      }

      const left = bestColumn * columnWidth;
      const top = Number.isFinite(bestTop) ? bestTop : 0;
      const itemHeight = item.getHeight();
      for (let column = bestColumn; column < bestColumn + span; column += 1) {
        columnHeights[column] = top + itemHeight;
      }
      slots.push(left, top);
    });

    callback({
      id,
      items,
      slots,
      styles: {
        height: `${Math.max(0, ...columnHeights)}px`,
      },
    });
  };
}

type MasonryWallProps<T> = {
  items: T[];
  getKey: (item: T) => string;
  getSpan?: (item: T) => number;
  minColumnWidth?: number;
  itemClassName?: string;
  className?: string;
  renderItem: (item: T) => React.ReactNode;
};

export function MasonryWall<T>({
  items,
  getKey,
  getSpan,
  minColumnWidth = DEFAULT_MIN_COLUMN_WIDTH,
  itemClassName = "p-0.5",
  className = "relative -m-0.5",
  renderItem,
}: MasonryWallProps<T>) {
  const gridElementRef = useRef<HTMLDivElement | null>(null);
  const muuriRef = useRef<Muuri | null>(null);
  const layoutKey = useMemo(() => items.map(getKey).join("|"), [getKey, items]);
  const relayout = useCallback(() => {
    requestAnimationFrame(() => muuriRef.current?.refreshItems().layout());
  }, []);

  useLayoutEffect(() => {
    if (!layoutKey || !gridElementRef.current) return;
    let disposed = false;
    const element = gridElementRef.current;
    const preserveHeight = () => {
      const height = element.getBoundingClientRect().height;
      if (height > 0) element.style.minHeight = `${height}px`;
    };
    preserveHeight();
    import("muuri").then(({ default: MuuriGrid }) => {
      if (disposed || !element) return;
      muuriRef.current?.destroy(false);
      muuriRef.current = new MuuriGrid(element, {
        items: ".masonry-wall-item",
        layout: wallLayout(minColumnWidth),
        layoutDuration: 0,
      });
      requestAnimationFrame(() => {
        if (disposed) return;
        muuriRef.current?.refreshItems().layout();
        requestAnimationFrame(() => {
          if (!disposed) element.style.minHeight = "";
        });
      });
    });
    const onResize = () => muuriRef.current?.refreshItems().layout();
    window.addEventListener("resize", onResize);
    return () => {
      disposed = true;
      preserveHeight();
      window.removeEventListener("resize", onResize);
      muuriRef.current?.destroy(false);
      muuriRef.current = null;
    };
  }, [layoutKey, minColumnWidth]);

  return (
    <div ref={gridElementRef} className={className} onLoadCapture={relayout}>
      {items.map((item) => (
        <div key={getKey(item)} data-span={getSpan?.(item) ?? 1} className={`masonry-wall-item absolute ${itemClassName}`}>
          {renderItem(item)}
        </div>
      ))}
    </div>
  );
}
