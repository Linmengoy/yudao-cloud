const SIZE_PATTERN = /^\s*(\d+)\s*[xX×]\s*(\d+)\s*$/;
const RATIO_PATTERN = /^\s*(\d+(?:\.\d+)?)\s*[:xX×]\s*(\d+(?:\.\d+)?)\s*$/;
const SIZE_MULTIPLE = 16;
const MAX_EDGE = 3840;
const MAX_ASPECT_RATIO = 3;
const MIN_PIXELS = 655_360;
const MAX_PIXELS = 8_294_400;

export type SizeTier = "1K" | "2K" | "4K";

function roundToMultiple(value: number, multiple: number) {
  return Math.max(multiple, Math.round(value / multiple) * multiple);
}

function floorToMultiple(value: number, multiple: number) {
  return Math.max(multiple, Math.floor(value / multiple) * multiple);
}

function ceilToMultiple(value: number, multiple: number) {
  return Math.max(multiple, Math.ceil(value / multiple) * multiple);
}

function normalizeDimensions(width: number, height: number) {
  let nw = roundToMultiple(width, SIZE_MULTIPLE);
  let nh = roundToMultiple(height, SIZE_MULTIPLE);

  for (let i = 0; i < 4; i++) {
    const maxEdge = Math.max(nw, nh);
    if (maxEdge > MAX_EDGE) {
      nw = floorToMultiple(nw * (MAX_EDGE / maxEdge), SIZE_MULTIPLE);
      nh = floorToMultiple(nh * (MAX_EDGE / maxEdge), SIZE_MULTIPLE);
    }

    if (nw / nh > MAX_ASPECT_RATIO) {
      nw = floorToMultiple(nh * MAX_ASPECT_RATIO, SIZE_MULTIPLE);
    } else if (nh / nw > MAX_ASPECT_RATIO) {
      nh = floorToMultiple(nw * MAX_ASPECT_RATIO, SIZE_MULTIPLE);
    }

    const pixels = nw * nh;
    if (pixels > MAX_PIXELS) {
      const scale = Math.sqrt(MAX_PIXELS / pixels);
      nw = floorToMultiple(nw * scale, SIZE_MULTIPLE);
      nh = floorToMultiple(nh * scale, SIZE_MULTIPLE);
    } else if (pixels < MIN_PIXELS) {
      const scale = Math.sqrt(MIN_PIXELS / pixels);
      nw = ceilToMultiple(nw * scale, SIZE_MULTIPLE);
      nh = ceilToMultiple(nh * scale, SIZE_MULTIPLE);
    }
  }

  return { width: nw, height: nh };
}

export function normalizeImageSize(size: string) {
  const trimmed = size.trim();
  const match = trimmed.match(SIZE_PATTERN);
  if (!match) return trimmed;

  const { width, height } = normalizeDimensions(Number(match[1]), Number(match[2]));
  return `${width}x${height}`;
}

export function parseRatio(ratio: string) {
  const match = ratio.match(RATIO_PATTERN);
  if (!match) return null;

  const width = Number(match[1]);
  const height = Number(match[2]);
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    return null;
  }

  return { width, height };
}

export function calculateImageSize(tier: SizeTier, ratio: string) {
  const parsed = parseRatio(ratio);
  if (!parsed) return null;

  const { width: rw, height: rh } = parsed;

  if (rw === rh) {
    const side = tier === "1K" ? 1024 : tier === "2K" ? 2048 : 3840;
    return normalizeImageSize(`${side}x${side}`);
  }

  if (tier === "1K") {
    const shortSide = 1024;
    const w = rw > rh ? roundToMultiple(shortSide * rw / rh, SIZE_MULTIPLE) : shortSide;
    const h = rw > rh ? shortSide : roundToMultiple(shortSide * rh / rw, SIZE_MULTIPLE);
    return normalizeImageSize(`${w}x${h}`);
  }

  const longSide = tier === "2K" ? 2048 : 3840;
  const w = rw > rh ? longSide : roundToMultiple(longSide * rw / rh, SIZE_MULTIPLE);
  const h = rw > rh ? roundToMultiple(longSide * rh / rw, SIZE_MULTIPLE) : longSide;
  return normalizeImageSize(`${w}x${h}`);
}
