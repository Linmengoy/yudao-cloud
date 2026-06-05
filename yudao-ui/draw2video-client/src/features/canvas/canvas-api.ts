import { api } from "@/lib/api-client";
import type {
  AppEdge,
  AppNode,
  CanvasOperationRecord,
  CanvasOperationSyncResult,
  InviteCanvasMemberRequest,
  CanvasMember,
  CanvasProject,
  CanvasProjectRecycleBinRecord,
  CanvasSnapshotRecord,
  CanvasState,
  UpdateCanvasMemberRoleRequest,
} from "@/features/canvas/types";

export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface CreateCanvasProjectInput {
  name: string;
}

export interface UpdateCanvasProjectInput {
  name?: string;
  coverAssetId?: number | null;
  nodeCount?: number;
  assetCount?: number;
}

export interface SaveCanvasSnapshotInput extends CanvasState {
  baseVersion: number;
  clientId?: string;
  nodeCount?: number;
  assetCount?: number;
}

export interface SubmitCanvasOperationInput {
  clientId: string;
  opId: string;
  baseVersion: number;
  operationType: string;
  operationJson: string;
  inverseOperationJson?: string | null;
}

export interface SubmitCanvasOperationPayloadInput {
  clientId: string;
  baseVersion: number;
  operationType: string;
  payload: Record<string, unknown>;
  inversePayload?: Record<string, unknown> | null;
}

export interface BindCanvasAssetInput {
  assetId: number;
  assetVersionId?: number | null;
  previewUrl?: string | null;
  usageType?: string;
  sourceTaskId?: number | null;
}

export interface CanvasSketchRecord {
  id: number;
  projectId: number;
  nodeId: string;
  sceneJson: string;
  previewUrl?: string | null;
  previewDataUrl?: string | null;
  previewAssetId?: number | null;
  previewAssetVersionId?: number | null;
  mimeType: string;
  width?: number | null;
  height?: number | null;
  background?: string | null;
  createTime?: string;
  updateTime?: string;
}

export interface SaveCanvasSketchInput {
  projectId?: string | number;
  nodeId?: string;
  sceneJson: string;
  previewUrl?: string | null;
  previewDataUrl?: string | null;
  previewAssetId?: number | null;
  previewAssetVersionId?: number | null;
  mimeType?: string;
  width?: number | null;
  height?: number | null;
  background?: string | null;
}

function parseJsonArray<T>(value: string | null | undefined): T[] {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function parseViewport(value: string | null | undefined): CanvasState["viewport"] {
  if (!value) return undefined;
  try {
    const parsed = JSON.parse(value);
    if (typeof parsed?.x === "number" && typeof parsed?.y === "number" && typeof parsed?.zoom === "number") {
      return parsed;
    }
  } catch {
    return undefined;
  }
  return undefined;
}

export function snapshotRecordToCanvasState(snapshot: CanvasSnapshotRecord | null | undefined): CanvasState | null {
  if (!snapshot) return null;
  return {
    nodes: parseJsonArray<AppNode>(snapshot.nodesJson),
    edges: parseJsonArray<AppEdge>(snapshot.edgesJson),
    viewport: parseViewport(snapshot.viewportJson),
  };
}

export const canvasApi = {
  listProjects: (params: { pageNo?: number; pageSize?: number; name?: string } = {}) => {
    const query = new URLSearchParams();
    query.set("pageNo", String(params.pageNo ?? 1));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.name) query.set("name", params.name);
    return api.get<PageResult<CanvasProject>>(`/canvas/projects?${query.toString()}`);
  },

  listProjectRecycleBin: (params: { pageNo?: number; pageSize?: number; name?: string } = {}) => {
    const query = new URLSearchParams();
    query.set("pageNo", String(params.pageNo ?? 1));
    query.set("pageSize", String(params.pageSize ?? 20));
    if (params.name) query.set("name", params.name);
    return api.get<PageResult<CanvasProjectRecycleBinRecord>>(`/canvas/projects/recycle-bin?${query.toString()}`);
  },

  createProject: (input: CreateCanvasProjectInput) => api.post<number>("/canvas/projects", input),

  getProject: (projectId: string | number) => api.get<CanvasProject>(`/canvas/projects/${projectId}`),

  deleteProject: (projectId: string | number) => api.delete<boolean>(`/canvas/projects/${projectId}`),

  restoreProject: (projectId: string | number) => api.put<boolean>(`/canvas/projects/${projectId}/restore`),

  getProjectMembers: (projectId: string | number) => api.get<CanvasMember[]>(`/canvas/projects/${projectId}/members`),

  inviteProjectMember: (projectId: string | number, input: InviteCanvasMemberRequest) =>
    api.post<boolean>(`/canvas/projects/${projectId}/members`, input),

  updateProjectMemberRole: (projectId: string | number, memberId: string | number, input: UpdateCanvasMemberRoleRequest) =>
    api.put<boolean>(`/canvas/projects/${projectId}/members/${memberId}`, input),

  removeProjectMember: (projectId: string | number, memberId: string | number) =>
    api.delete<boolean>(`/canvas/projects/${projectId}/members/${memberId}`),

  updateProject: (projectId: string | number, input: UpdateCanvasProjectInput) =>
    api.put<boolean>(`/canvas/projects/${projectId}`, {
      ...input,
      id: projectId,
    }),

  getSnapshot: (projectId: string | number) => api.get<CanvasSnapshotRecord | null>(`/canvas/projects/${projectId}/snapshot`),

  saveSnapshot: (projectId: string | number, input: SaveCanvasSnapshotInput) =>
    api.post<CanvasSnapshotRecord>(`/canvas/projects/${projectId}/snapshot`, {
      projectId,
      baseVersion: input.baseVersion,
      clientId: input.clientId,
      nodesJson: JSON.stringify(input.nodes),
      edgesJson: JSON.stringify(input.edges),
      viewportJson: input.viewport ? JSON.stringify(input.viewport) : undefined,
      nodeCount: input.nodeCount ?? input.nodes.length,
      assetCount: input.assetCount ?? 0,
    }),

  getOperations: (projectId: string | number, afterVersion = 0) =>
    api.get<CanvasOperationRecord[]>(`/canvas/projects/${projectId}/operations?afterVersion=${afterVersion}`),

  syncOperations: (projectId: string | number, afterVersion = 0) =>
    api.get<CanvasOperationSyncResult>(`/canvas/projects/${projectId}/operations/sync?afterVersion=${afterVersion}`),

  submitOperation: (projectId: string | number, input: SubmitCanvasOperationInput) =>
    api.post<CanvasOperationRecord>(`/canvas/projects/${projectId}/operations`, {
      ...input,
      projectId,
    }),

  submitOperationPayload: (projectId: string | number, input: SubmitCanvasOperationPayloadInput) =>
    api.post<CanvasOperationRecord>(`/canvas/projects/${projectId}/operations`, {
      projectId,
      clientId: input.clientId,
      opId: `op_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      baseVersion: input.baseVersion,
      operationType: input.operationType,
      operationJson: JSON.stringify({ type: input.operationType, payload: input.payload }),
      inverseOperationJson: input.inversePayload
        ? JSON.stringify({ type: input.operationType, payload: input.inversePayload })
        : undefined,
    }),

  bindNodeAsset: (projectId: string | number, nodeId: string, input: BindCanvasAssetInput) =>
    api.post<number>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/assets`, input),

  getSketch: (projectId: string | number, nodeId: string) =>
    api.get<CanvasSketchRecord | null>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/sketch`),

  saveSketch: (projectId: string | number, nodeId: string, input: SaveCanvasSketchInput) =>
    api.put<CanvasSketchRecord>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/sketch`, {
      ...input,
      projectId,
      nodeId,
    }),
};
