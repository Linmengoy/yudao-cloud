"use client";

import { useCallback, useEffect, useRef } from "react";
import { canvasNodeRunApi, isServerCanvasProjectId, type CanvasNodeRunBatchSyncResponse } from "@/features/canvas/canvas-node-run-api";
import type { AppNode } from "@/features/canvas/types";
import type { CanvasOperationRecord } from "@/features/canvas/types";
import { API_BASE_URL, API_TENANT_ID, API_TERMINAL, getAccessToken } from "@/lib/api-client";

export type CanvasGenerationRunEvent = {
  type?: string;
  eventId?: string;
  projectId?: number;
  nodeId?: string;
  runId?: string;
  taskId?: number;
  status?: string;
  progress?: number;
  operation?: CanvasOperationRecord;
  version?: number;
};

type PendingRunNode = Pick<AppNode, "id" | "type" | "data">;

export function isCanvasGenerationRunSseEnabled() {
  return process.env.NEXT_PUBLIC_CANVAS_GENERATION_SSE_ENABLED !== "false";
}

function getPendingRunNodeType(node: PendingRunNode) {
  if (node.type === "image" || node.type === "video" || node.type === "text") return node.type;
  return null;
}

function getPendingTaskId(node: PendingRunNode) {
  const data = node.data as { status?: unknown; taskId?: unknown };
  if (data.status !== "pending") return null;
  const taskId = Number(data.taskId);
  return Number.isFinite(taskId) ? taskId : null;
}

async function readEventStream(response: Response, onEvent: (event: CanvasGenerationRunEvent) => void) {
  const reader = response.body?.getReader();
  if (!reader) return;
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split(/\r?\n\r?\n/);
    buffer = chunks.pop() ?? "";
    for (const chunk of chunks) {
      const eventType = chunk
        .split(/\r?\n/)
        .find((line) => line.startsWith("event:"))
        ?.slice(6)
        .trim();
      const dataLines = chunk
        .split(/\r?\n/)
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.slice(5).trimStart());
      if (dataLines.length === 0) continue;
      try {
        onEvent({ ...(JSON.parse(dataLines.join("\n")) as CanvasGenerationRunEvent), type: eventType });
      } catch {
      }
    }
  }
}

export function useCanvasGenerationRunEvents(
  projectId: string | null,
  enabled: boolean,
  lastAppliedVersion: number,
  getNodes: () => PendingRunNode[],
  onOperation: (operation: CanvasOperationRecord) => void,
  onBatchSync?: (response: CanvasNodeRunBatchSyncResponse) => void
) {
  const getNodesRef = useRef(getNodes);
  const lastAppliedVersionRef = useRef(lastAppliedVersion);
  const onOperationRef = useRef(onOperation);
  const onBatchSyncRef = useRef(onBatchSync);
  const batchSyncInFlightRef = useRef(false);

  useEffect(() => {
    getNodesRef.current = getNodes;
    lastAppliedVersionRef.current = lastAppliedVersion;
    onOperationRef.current = onOperation;
    onBatchSyncRef.current = onBatchSync;
  }, [getNodes, lastAppliedVersion, onBatchSync, onOperation]);

  const syncProjectGenerationRuns = useCallback(async () => {
    if (!isServerCanvasProjectId(projectId) || batchSyncInFlightRef.current) return;
    const nodes = getNodesRef.current().flatMap((node) => {
      const taskId = getPendingTaskId(node);
      const nodeType = getPendingRunNodeType(node);
      if (!taskId || !nodeType) return [];
      return [{ projectId, nodeId: node.id, taskId, baseVersion: lastAppliedVersionRef.current, nodeType }];
    }).slice(0, 20);
    if (nodes.length === 0) return;
    batchSyncInFlightRef.current = true;
    try {
      const response = await canvasNodeRunApi.syncProjectNodeRuns(projectId, {
        projectId,
        baseVersion: lastAppliedVersionRef.current,
        nodes,
      });
      onBatchSyncRef.current?.(response);
      for (const result of response.results ?? []) {
        if (result.success === false || !result.operation) continue;
        onOperationRef.current(result.operation);
      }
    } finally {
      batchSyncInFlightRef.current = false;
    }
  }, [projectId]);

  useEffect(() => {
    if (!isServerCanvasProjectId(projectId)) return;
    if (!enabled) {
      void syncProjectGenerationRuns();
      return;
    }
    const abortController = new AbortController();
    let reconnectTimer: number | null = null;
    let closed = false;

    const connect = async () => {
      const token = getAccessToken();
      try {
        const response = await fetch(`${API_BASE_URL}/canvas/projects/${projectId}/generation-runs/events`, {
          headers: {
            Accept: "text/event-stream",
            "tenant-id": API_TENANT_ID,
            terminal: API_TERMINAL,
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          signal: abortController.signal,
        });
        if (!response.ok) throw new Error(`generation run stream failed: ${response.status}`);
        await readEventStream(response, (event) => {
          if (event.projectId != null && String(event.projectId) !== String(projectId)) return;
          if (event.type === "resync-required" || event.type === "generation-run-connection-limit") {
            void syncProjectGenerationRuns();
            return;
          }
          if (event.operation) onOperationRef.current(event.operation);
        });
      } catch {
        if (closed || abortController.signal.aborted) return;
        reconnectTimer = window.setTimeout(connect, 3_000);
        void syncProjectGenerationRuns();
      }
    };

    void connect();
    void syncProjectGenerationRuns();

    return () => {
      closed = true;
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      abortController.abort();
    };
  }, [enabled, projectId, syncProjectGenerationRuns]);

  useEffect(() => {
    if (!isServerCanvasProjectId(projectId)) return;
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") void syncProjectGenerationRuns();
    }, 10_000);
    return () => window.clearInterval(timer);
  }, [projectId, syncProjectGenerationRuns]);

  return { syncProjectGenerationRuns };
}
