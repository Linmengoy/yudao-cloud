const VP_PAD = 12;

export function clampToViewport(params: {
  x: number;
  y: number;
  width: number;
  height: number;
  padding?: number;
}): { x: number; y: number } {
  const pad = params.padding ?? VP_PAD;
  return {
    x: Math.min(Math.max(params.x, pad), window.innerWidth - params.width - pad),
    y: Math.min(Math.max(params.y, pad), window.innerHeight - params.height - pad),
  };
}

export function positionAbove(rect: DOMRect, popWidth: number, popHeight: number, padding?: number) {
  return clampToViewport({
    x: rect.left + rect.width / 2 - popWidth / 2,
    y: rect.top - popHeight - 8,
    width: popWidth,
    height: popHeight,
    padding,
  });
}

export function positionBelow(rect: DOMRect, popWidth: number, popHeight: number, padding?: number) {
  return clampToViewport({
    x: rect.left + rect.width / 2 - popWidth / 2,
    y: rect.bottom + 8,
    width: popWidth,
    height: popHeight,
    padding,
  });
}

export function fitsAbove(rect: DOMRect, popHeight: number) {
  return rect.top - popHeight - 8 >= 0;
}
