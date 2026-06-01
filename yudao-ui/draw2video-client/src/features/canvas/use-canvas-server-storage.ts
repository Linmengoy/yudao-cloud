"use client";

import { useCallback, useState } from "react";
import { canvasApi, snapshotRecordToCanvasState, type SaveCanvasSnapshotInput } from "@/features/canvas/canvas-api";
import type { CanvasProject, CanvasSnapshotRecord, CanvasState } from "@/features/canvas/types";

export function useCanvasServerStorage() {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createProject = useCallback(async (name = "未命名项目", kind: CanvasProject["kind"] = "image") => {
    setError(null);
    return canvasApi.createProject({ name, kind });
  }, []);

  const loadProject = useCallback(async (projectId: string | number): Promise<{ project: CanvasProject; snapshot: CanvasSnapshotRecord | null; state: CanvasState | null }> => {
    setIsLoading(true);
    setError(null);
    try {
      const [project, snapshot] = await Promise.all([
        canvasApi.getProject(projectId),
        canvasApi.getSnapshot(projectId).catch(() => null),
      ]);
      return { project, snapshot, state: snapshotRecordToCanvasState(snapshot) };
    } catch (err) {
      const message = err instanceof Error ? err.message : "加载画布失败";
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const saveSnapshot = useCallback(async (projectId: string | number, input: SaveCanvasSnapshotInput) => {
    setError(null);
    return canvasApi.saveSnapshot(projectId, input);
  }, []);

  return { isLoading, error, createProject, loadProject, saveSnapshot };
}
