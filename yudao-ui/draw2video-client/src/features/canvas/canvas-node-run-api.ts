import { api } from "@/lib/api-client";
import type { CanvasOperationRecord } from "@/features/canvas/types";
import type { GenerateMode, GenerateStatus, GenerateType } from "@/features/generation/generation-types";
import { isGenerationTerminal } from "@/features/generation/generation-status";

export type CanvasNodeRunRequest = {
  clientId: string;
  baseVersion: number;
  runId?: string;
  nodeType: string;
  generateType: GenerateType;
  generateMode: GenerateMode;
  modelId: number;
  prompt?: string;
  inputParams?: string;
  sync?: boolean;
};

export type CanvasNodeRunResponse = {
  taskId: number;
  generateRecordId: number;
  generateNo: string;
  status: GenerateStatus;
  operation?: CanvasOperationRecord;
};

export type CanvasNodeRunSyncRequest = {
  taskId: number;
  baseVersion: number;
  nodeType: string;
};

export const canvasNodeRunApi = {
  runNode: (projectId: string | number, nodeId: string, input: CanvasNodeRunRequest) =>
    api.post<CanvasNodeRunResponse>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/run`, input),

  syncNodeRun: (projectId: string | number, nodeId: string, input: CanvasNodeRunSyncRequest) =>
    api.post<CanvasNodeRunResponse>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/run/sync`, input),
};

export function isServerCanvasProjectId(projectId: string | number | null | undefined): projectId is string | number {
  return typeof projectId === "number" || (typeof projectId === "string" && /^\d+$/.test(projectId));
}

function sleep(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

export async function waitCanvasNodeRunResult(projectId: string | number, nodeId: string, input: CanvasNodeRunSyncRequest) {
  const startedAt = Date.now();
  let latest: CanvasNodeRunResponse | null = null;
  while (Date.now() - startedAt < 180_000) {
    latest = await canvasNodeRunApi.syncNodeRun(projectId, nodeId, input);
    if (isGenerationTerminal(latest.status)) return latest;
    await sleep(1_600);
  }
  if (latest) return latest;
  throw new Error("生成结果查询超时，请稍后到任务中心查看进度。");
}
