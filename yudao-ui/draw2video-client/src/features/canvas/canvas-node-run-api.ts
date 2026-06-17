import { api } from "@/lib/api-client";
import type { CanvasOperationRecord } from "@/features/canvas/types";
import type { GenerateMode, GenerateStatus, GenerateType } from "@/features/generation/generation-types";
import { getGenerationStatusLabel, isGenerationTerminal } from "@/features/generation/generation-status";

export type CanvasNodeRunRequest = {
  projectId?: string | number;
  nodeId?: string;
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
  projectId?: string | number;
  nodeId?: string;
  taskId: number;
  baseVersion: number;
  nodeType: string;
};

export const canvasNodeRunApi = {
  runNode: (projectId: string | number, nodeId: string, input: CanvasNodeRunRequest) =>
    api.post<CanvasNodeRunResponse>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/run`, {
      ...input,
      projectId,
      nodeId,
    }),

  syncNodeRun: (projectId: string | number, nodeId: string, input: CanvasNodeRunSyncRequest) =>
    api.post<CanvasNodeRunResponse>(`/canvas/projects/${projectId}/nodes/${encodeURIComponent(nodeId)}/run/sync`, {
      ...input,
      projectId,
      nodeId,
    }),
};

export function isServerCanvasProjectId(projectId: string | number | null | undefined): projectId is string | number {
  return typeof projectId === "number" || (typeof projectId === "string" && /^\d+$/.test(projectId));
}

function sleep(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

const nodeRunPolls = new Map<string, Promise<CanvasNodeRunResponse>>();
const NODE_RUN_POLL_TIMEOUT_MS = 10 * 60_000;

function getNodeRunPollKey(projectId: string | number, nodeId: string, input: CanvasNodeRunSyncRequest) {
  return `${projectId}:${nodeId}:${input.taskId}`;
}

function getNodeRunPollDelay(elapsedMs: number) {
  if (elapsedMs < 20_000) return 2_000;
  if (elapsedMs < 120_000) return 5_000;
  return 10_000;
}

function getNodeRunFailureMessage(result: CanvasNodeRunResponse) {
  const fallback = getGenerationStatusLabel(result.status) || "生成失败，请稍后重试。";
  const operationJson = result.operation?.operationJson;
  if (!operationJson) return fallback;
  try {
    const parsed = JSON.parse(operationJson) as { payload?: { patch?: { errorMessage?: unknown } } };
    const message = parsed.payload?.patch?.errorMessage;
    return typeof message === "string" && message.trim() ? message : fallback;
  } catch {
    return fallback;
  }
}

function assertSuccessfulNodeRun(result: CanvasNodeRunResponse) {
  if (result.status === "SUCCESS") return;
  throw new Error(getNodeRunFailureMessage(result));
}
/**
 * todo
 * 这里可能会成为性能瓶颈，考虑优化为批量查询多个节点的运行结果？
 * - 通过待完成队列实现？
 * @param projectId 
 * @param nodeId 
 * @param input 
 * @returns 
 */
export async function waitCanvasNodeRunResult(projectId: string | number, nodeId: string, input: CanvasNodeRunSyncRequest) {
  const pollKey = getNodeRunPollKey(projectId, nodeId, input);
  const existingPoll = nodeRunPolls.get(pollKey);
  if (existingPoll) return existingPoll;

  const poll = waitCanvasNodeRunResultOnce(projectId, nodeId, input).finally(() => {
    nodeRunPolls.delete(pollKey);
  });
  nodeRunPolls.set(pollKey, poll);
  return poll;
}

export function getCanvasNodeRunPatch(result: CanvasNodeRunResponse, nodeId: string) {
  const operationJson = result.operation?.operationJson;
  if (!operationJson) return null;
  try {
    const parsed = JSON.parse(operationJson) as { payload?: { nodeId?: unknown; patch?: unknown } };
    if (parsed.payload?.nodeId !== nodeId || !parsed.payload.patch || typeof parsed.payload.patch !== "object") {
      return null;
    }
    return parsed.payload.patch as Record<string, unknown>;
  } catch {
    return null;
  }
}

async function waitCanvasNodeRunResultOnce(projectId: string | number, nodeId: string, input: CanvasNodeRunSyncRequest) {
  const startedAt = Date.now();
  let latest: CanvasNodeRunResponse | null = null;
  while (Date.now() - startedAt < NODE_RUN_POLL_TIMEOUT_MS) {
    latest = await canvasNodeRunApi.syncNodeRun(projectId, nodeId, input);
    if (isGenerationTerminal(latest.status)) {
      assertSuccessfulNodeRun(latest);
      return latest;
    }
    await sleep(getNodeRunPollDelay(Date.now() - startedAt));
  }
  throw new Error("生成结果查询超时，请稍后到任务中心查看进度。");
}
