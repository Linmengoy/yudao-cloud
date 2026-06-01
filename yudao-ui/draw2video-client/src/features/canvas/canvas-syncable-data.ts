import { BLOCKED_NODE_DATA_KEYS, SYNCABLE_NODE_DATA_KEYS, type AppNode } from "@/features/canvas/types";

const syncableKeys = new Set<string>(SYNCABLE_NODE_DATA_KEYS);
const blockedKeys = new Set<string>(BLOCKED_NODE_DATA_KEYS);

function hasLocalMediaValue(value: unknown): boolean {
  if (typeof value === "string") {
    return value.startsWith("data:") || value.startsWith("blob:");
  }
  if (Array.isArray(value)) {
    return value.some(hasLocalMediaValue);
  }
  if (value && typeof value === "object") {
    return Object.values(value as Record<string, unknown>).some(hasLocalMediaValue);
  }
  return false;
}

export function filterSyncableNodeDataPatch(patch: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(patch).filter(([key, value]) => syncableKeys.has(key) && !blockedKeys.has(key) && !hasLocalMediaValue(value))
  );
}

export function sanitizeNodeForCanvasOperation(node: AppNode): AppNode {
  if (node.type !== "image" && node.type !== "video" && node.type !== "sketch") return node;
  const data = { ...(node.data as Record<string, unknown>) };
  for (const key of blockedKeys) {
    delete data[key];
  }
  if (node.type === "video" && typeof node.data.videoUrl === "string" && !hasLocalMediaValue(node.data.videoUrl)) {
    data.videoUrl = node.data.videoUrl;
  }
  return { ...node, data } as AppNode;
}
