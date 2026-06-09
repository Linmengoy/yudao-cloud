import { BLOCKED_NODE_DATA_KEYS, SYNCABLE_NODE_DATA_KEYS, type AppEdge, type AppNode, type CanvasState } from "@/features/canvas/types";

const syncableKeys = new Set<string>(SYNCABLE_NODE_DATA_KEYS);
const blockedKeys = new Set<string>(BLOCKED_NODE_DATA_KEYS);
const runtimeAssetUrlKeys = new Set(["previewUrl", "outputPreviewUrl", "videoUrl", "assetUrlExpireTime"]);

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
    Object.entries(patch).filter(([key, value]) =>
      syncableKeys.has(key) && !blockedKeys.has(key) && !runtimeAssetUrlKeys.has(key) && !hasLocalMediaValue(value)
    )
  );
}

export function stripRuntimeAssetUrlsFromPatch(patch: Record<string, unknown>) {
  return Object.fromEntries(Object.entries(patch).filter(([key]) => !runtimeAssetUrlKeys.has(key)));
}

export function sanitizeNodeForCanvasOperation(node: AppNode): AppNode {
  if (node.type !== "image" && node.type !== "video" && node.type !== "sketch") return node;
  const data = { ...(node.data as Record<string, unknown>) };
  for (const key of blockedKeys) {
    delete data[key];
  }
  if (node.type === "image" || node.type === "video") {
    for (const key of runtimeAssetUrlKeys) {
      delete data[key];
    }
  }
  return { ...node, data } as AppNode;
}

export function sanitizeNodesForCanvasSnapshot(nodes: AppNode[]): AppNode[] {
  return nodes.map(sanitizeNodeForCanvasOperation);
}

export function sanitizeCanvasStateForPersistence(state: CanvasState): CanvasState {
  return {
    ...state,
    nodes: sanitizeNodesForCanvasSnapshot(state.nodes),
    edges: state.edges as AppEdge[],
  };
}
