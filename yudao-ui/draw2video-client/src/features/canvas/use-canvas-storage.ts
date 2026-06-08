import type { CanvasState, AppNode } from "./types";
import { sanitizeCanvasStateForPersistence } from "@/features/canvas/canvas-syncable-data";

const STORAGE_KEY = "copse_canvas_draft";

type StoredCanvasState = CanvasState & {
  projectId?: string | null;
};

function storageKey(projectId?: string | null) {
  return projectId ? `${STORAGE_KEY}:${projectId}` : STORAGE_KEY;
}

function stripDataUrlFromNodes(nodes: AppNode[]): unknown[] {
  return nodes.map((n) => {
    if (n.type === "image") {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { dataUrl: _dataUrl, ...rest } = n.data as Record<string, unknown>;
      return { ...n, data: rest };
    }
    if (n.type === "sketch") {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { dataUrl: _dataUrl, ...rest } = n.data as Record<string, unknown>;
      return { ...n, data: rest };
    }
    if (n.type === "video") {
      const data = n.data as Record<string, unknown>;
      if (typeof data.videoUrl !== "string" || (!data.videoUrl.startsWith("data:") && !data.videoUrl.startsWith("blob:"))) return n;
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { videoUrl: _videoUrl, ...rest } = data;
      return { ...n, data: { ...rest, videoUrl: null } };
    }
    return n;
  });
}

export function loadCanvas(projectId?: string | null): CanvasState | null {
  try {
    const raw = localStorage.getItem(storageKey(projectId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredCanvasState;
    if (!parsed || !Array.isArray(parsed.nodes)) return null;
    if (projectId && parsed.projectId && parsed.projectId !== projectId) return null;
    return parsed as CanvasState;
  } catch {
    console.warn("Canvas draft data corrupted, starting fresh");
    return null;
  }
}

export function saveCanvas(state: CanvasState, projectId?: string | null): void {
  const sanitized = sanitizeCanvasStateForPersistence(state);
  const lightweight = {
    ...sanitized,
    projectId: projectId ?? null,
    nodes: stripDataUrlFromNodes(sanitized.nodes),
  };
  localStorage.setItem(storageKey(projectId), JSON.stringify(lightweight));
}

export function clearCanvas(projectId?: string | null): void {
  localStorage.removeItem(storageKey(projectId));
}
