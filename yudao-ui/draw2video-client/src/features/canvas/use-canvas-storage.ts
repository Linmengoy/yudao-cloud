import type { CanvasState, AppNode } from "./types";
import { sanitizeCanvasStateForPersistence } from "@/features/canvas/canvas-syncable-data";

const STORAGE_KEY = "copse_canvas_draft";

type StoredCanvasState = CanvasState & {
  projectId?: string | null;
  ownerKey?: string | null;
  savedAt?: number;
};

function normalizeScopeValue(value?: string | number | null) {
  return value == null || value === "" ? null : String(value);
}

function storageKey(projectId?: string | null, ownerKey?: string | number | null) {
  if (!projectId) return STORAGE_KEY;
  const normalizedOwnerKey = normalizeScopeValue(ownerKey);
  return normalizedOwnerKey ? `${STORAGE_KEY}:${normalizedOwnerKey}:${projectId}` : `${STORAGE_KEY}:${projectId}`;
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

export function loadCanvas(projectId?: string | null, ownerKey?: string | number | null): CanvasState | null {
  try {
    const raw = localStorage.getItem(storageKey(projectId, ownerKey));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredCanvasState;
    if (!parsed || !Array.isArray(parsed.nodes)) return null;
    if (projectId && parsed.projectId && parsed.projectId !== projectId) return null;
    const normalizedOwnerKey = normalizeScopeValue(ownerKey);
    if (normalizedOwnerKey && parsed.ownerKey && parsed.ownerKey !== normalizedOwnerKey) return null;
    return parsed as CanvasState;
  } catch {
    console.warn("Canvas draft data corrupted, starting fresh");
    return null;
  }
}

export function saveCanvas(state: CanvasState, projectId?: string | null, ownerKey?: string | number | null): void {
  const sanitized = sanitizeCanvasStateForPersistence(state);
  const lightweight = {
    ...sanitized,
    projectId: projectId ?? null,
    ownerKey: normalizeScopeValue(ownerKey),
    savedAt: Date.now(),
    nodes: stripDataUrlFromNodes(sanitized.nodes),
  };
  localStorage.setItem(storageKey(projectId, ownerKey), JSON.stringify(lightweight));
}

export function clearCanvas(projectId?: string | null, ownerKey?: string | number | null): void {
  localStorage.removeItem(storageKey(projectId, ownerKey));
}
