import { describe, expect, it } from "vitest";
import { canApplyGenerationRunOperation, type CanvasGenerationRunEvent } from "./use-canvas-generation-run-events";
import type { CanvasOperationRecord } from "./types";

const operation: CanvasOperationRecord = {
  id: 1,
  projectId: 290,
  clientId: "server",
  opId: "op-1",
  actorUserId: 1,
  baseVersion: 7,
  nextVersion: 8,
  operationType: "TASK_STATUS_PATCH",
  operationJson: JSON.stringify({ type: "TASK_STATUS_PATCH", payload: { nodeId: "image-1", patch: {} } }),
};

function event(overrides: Partial<CanvasGenerationRunEvent>): CanvasGenerationRunEvent {
  return {
    projectId: 290,
    nodeId: "image-1",
    taskId: 1001,
    runId: "run-new",
    operation,
    ...overrides,
  };
}

describe("canApplyGenerationRunOperation", () => {
  it("rejects a late terminal operation for an old task on the same node", () => {
    const nodes = [
      {
        id: "image-1",
        type: "image",
        data: { status: "pending", taskId: "1002", runId: "run-new" },
      },
    ];

    expect(canApplyGenerationRunOperation(event({ taskId: 1001, runId: "run-old" }), nodes)).toBe(false);
  });

  it("allows a terminal operation when the current task id matches", () => {
    const nodes = [
      {
        id: "image-1",
        type: "image",
        data: { status: "pending", taskId: "1001", runId: "run-new" },
      },
    ];

    expect(canApplyGenerationRunOperation(event({ taskId: 1001, runId: "run-old" }), nodes)).toBe(true);
  });

  it("allows a terminal operation when the current run id matches", () => {
    const nodes = [
      {
        id: "image-1",
        type: "image",
        data: { status: "pending", taskId: "1002", runId: "run-new" },
      },
    ];

    expect(canApplyGenerationRunOperation(event({ taskId: 1001, runId: "run-new" }), nodes)).toBe(true);
  });

  it("rejects operations for missing or no-longer-pending nodes", () => {
    const nodes = [
      {
        id: "image-1",
        type: "image",
        data: { status: "idle", taskId: "1001", runId: "run-new" },
      },
    ];

    expect(canApplyGenerationRunOperation(event({}), nodes)).toBe(false);
    expect(canApplyGenerationRunOperation(event({ nodeId: "missing-node" }), nodes)).toBe(false);
  });

  it("rejects terminal operations that cannot be tied to the current task or run", () => {
    const nodes = [
      {
        id: "image-1",
        type: "image",
        data: { status: "pending", taskId: "1001", runId: "run-new" },
      },
    ];

    expect(canApplyGenerationRunOperation(event({ taskId: undefined, runId: undefined }), nodes)).toBe(false);
    const nullIdEvent = event({ taskId: null as unknown as number, runId: null as unknown as string });
    expect(canApplyGenerationRunOperation(nullIdEvent, nodes)).toBe(false);
  });

  it("does not overwrite a completed node with a late matching terminal operation", () => {
    const nodes = [
      {
        id: "image-1",
        type: "image",
        data: { status: "success", taskId: "1001", runId: "run-new" },
      },
    ];

    expect(canApplyGenerationRunOperation(event({ taskId: 1001, runId: "run-new" }), nodes)).toBe(false);
  });
});
