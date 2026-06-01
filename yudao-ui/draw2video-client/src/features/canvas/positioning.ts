import type { AppNode } from "./types";

type Point = { x: number; y: number };
type Size = { width: number; height: number };
type Rect = Point & Size;

const NODE_SIZES: Record<string, Size> = {
  prompt: { width: 380, height: 430 },
  image: { width: 220, height: 260 },
  sketch: { width: 300, height: 240 },
  result: { width: 260, height: 320 },
  text: { width: 320, height: 260 },
  video: { width: 420, height: 448 },
};

function nodeSize(node: AppNode): Size {
  return NODE_SIZES[node.type ?? ""] ?? { width: 260, height: 260 };
}

function nodeRect(node: AppNode, padding: number): Rect {
  const size = nodeSize(node);
  return {
    x: node.position.x - padding,
    y: node.position.y - padding,
    width: size.width + padding * 2,
    height: size.height + padding * 2,
  };
}

function overlaps(a: Rect, b: Rect) {
  return (
    a.x < b.x + b.width &&
    a.x + a.width > b.x &&
    a.y < b.y + b.height &&
    a.y + a.height > b.y
  );
}

function candidateRect(position: Point, size: Size, padding: number): Rect {
  return {
    x: position.x - padding,
    y: position.y - padding,
    width: size.width + padding * 2,
    height: size.height + padding * 2,
  };
}

export function findOpenNodePosition(
  preferred: Point,
  size: Size,
  nodes: AppNode[],
  options: { padding?: number; stepX?: number; stepY?: number } = {}
): Point {
  const padding = options.padding ?? 32;
  const stepX = options.stepX ?? Math.max(180, Math.round(size.width * 0.55));
  const stepY = options.stepY ?? Math.max(140, Math.round(size.height * 0.45));
  const occupied = nodes.map((node) => nodeRect(node, padding));

  const isFree = (position: Point) => {
    const rect = candidateRect(position, size, padding);
    return !occupied.some((existing) => overlaps(rect, existing));
  };

  if (isFree(preferred)) return preferred;

  for (let ring = 1; ring <= 12; ring += 1) {
    const candidates: Point[] = [];
    for (let dx = -ring; dx <= ring; dx += 1) {
      candidates.push({ x: preferred.x + dx * stepX, y: preferred.y - ring * stepY });
      candidates.push({ x: preferred.x + dx * stepX, y: preferred.y + ring * stepY });
    }
    for (let dy = -ring + 1; dy <= ring - 1; dy += 1) {
      candidates.push({ x: preferred.x - ring * stepX, y: preferred.y + dy * stepY });
      candidates.push({ x: preferred.x + ring * stepX, y: preferred.y + dy * stepY });
    }

    candidates.sort((a, b) => {
      const da = Math.hypot(a.x - preferred.x, a.y - preferred.y);
      const db = Math.hypot(b.x - preferred.x, b.y - preferred.y);
      return da - db;
    });

    const open = candidates.find(isFree);
    if (open) return open;
  }

  return {
    x: preferred.x + stepX * 13,
    y: preferred.y + stepY * 13,
  };
}
