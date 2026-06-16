import { describe, expect, it } from "vitest";
import {
  getDisplayTaskProgress,
  getTaskEstimatedTimeText,
  getTaskTimingStartMs,
} from "./task-progress-value";
import type { AigcTask } from "./task-types";

describe("task progress timing", () => {
  it("uses the current task submit time as the countdown baseline", () => {
    const previousTask: AigcTask = {
      id: 100,
      status: "RUNNING",
      progress: 0,
      estimatedDurationMillis: 60_000,
      submitTime: "2026-06-16T10:00:00.000Z",
    };
    const currentTask: AigcTask = {
      ...previousTask,
      id: 101,
      submitTime: "2026-06-16T10:01:00.000Z",
    };

    expect(getTaskTimingStartMs(currentTask)).toBe(
      new Date("2026-06-16T10:01:00.000Z").getTime(),
    );
    expect(
      getDisplayTaskProgress(
        currentTask,
        new Date("2026-06-16T10:01:10.000Z").getTime(),
      ),
    ).toBe(16);
    expect(
      getTaskEstimatedTimeText(
        currentTask,
        new Date("2026-06-16T10:01:10.000Z").getTime(),
      ),
    ).toBe("预计剩余 50秒");
  });

  it("prefers submitTime over startTime and createTime for active tasks", () => {
    const task: AigcTask = {
      id: 102,
      status: "RUNNING",
      progress: 0,
      estimatedDurationMillis: 60_000,
      createTime: "2026-06-16T09:59:00.000Z",
      startTime: "2026-06-16T10:00:00.000Z",
      submitTime: "2026-06-16T10:01:00.000Z",
    };

    expect(getTaskTimingStartMs(task)).toBe(
      new Date("2026-06-16T10:01:00.000Z").getTime(),
    );
  });
});
