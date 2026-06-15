import { BLOCKED_NODE_DATA_KEYS, SYNCABLE_NODE_DATA_KEYS, type AppEdge, type AppNode, type CanvasState } from "@/features/canvas/types";

const syncableKeys = new Set<string>(SYNCABLE_NODE_DATA_KEYS);
const blockedKeys = new Set<string>(BLOCKED_NODE_DATA_KEYS);
const runtimeAssetUrlKeys = new Set(["previewUrl", "outputPreviewUrl", "videoUrl", "assetUrlExpireTime"]);

function stripRuntimeAssetUrlsFromValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(stripRuntimeAssetUrlsFromValue);
  }
  if (!value || typeof value !== "object") {
    return value;
  }
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .filter(([key]) => !runtimeAssetUrlKeys.has(key))
      .map(([key, item]) => [key, stripRuntimeAssetUrlsFromValue(item)])
  );
}

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
    Object.entries(patch).flatMap(([key, value]) => {
      if (!syncableKeys.has(key) || blockedKeys.has(key) || runtimeAssetUrlKeys.has(key)) return [];
      const sanitizedValue = stripRuntimeAssetUrlsFromValue(value);
      if (hasLocalMediaValue(sanitizedValue)) return [];
      return [[key, sanitizedValue]];
    })
  );
}

export function stripRuntimeAssetUrlsFromPatch(patch: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(patch)
      .filter(([key]) => !runtimeAssetUrlKeys.has(key))
      .map(([key, value]) => [key, stripRuntimeAssetUrlsFromValue(value)])
  );
}

export function sanitizeNodeForCanvasOperation(node: AppNode): AppNode {
  if (node.type !== "image" && node.type !== "video" && node.type !== "sketch") return node;
  const data = stripRuntimeAssetUrlsFromValue(node.data) as Record<string, unknown>;
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

function isUnsyncedLocalMediaNode(node: AppNode): boolean {
  if (node.type !== "image" && node.type !== "video" && node.type !== "sketch") return false;
  const data = node.data as Record<string, unknown>;
  if (data.assetId || data.outputAssetId || data.previewAssetId) return false;
  return hasLocalMediaValue(data);
}

export function isCanvasNodeSyncable(node: AppNode): boolean {
  return !isUnsyncedLocalMediaNode(node);
}

export function filterSyncableCanvasNodes(nodes: AppNode[]): AppNode[] {
  return nodes.filter(isCanvasNodeSyncable);
}

export function sanitizeNodesForCanvasSnapshot(nodes: AppNode[]): AppNode[] {
  return filterSyncableCanvasNodes(nodes).map(sanitizeNodeForCanvasOperation);
}

export function sanitizeCanvasStateForPersistence(state: CanvasState): CanvasState {
  return {
    ...state,
    nodes: sanitizeNodesForCanvasSnapshot(state.nodes),
    edges: state.edges as AppEdge[],
  };
}
