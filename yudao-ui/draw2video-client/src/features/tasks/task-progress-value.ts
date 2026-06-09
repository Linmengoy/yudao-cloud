import { shouldPollTask } from "./task-status";
import type { AigcTask, AigcTaskStatus } from "./task-types";

const OPTIMISTIC_PROGRESS_CAP = 95;
const OPTIMISTIC_PROGRESS_STATUSES = new Set<AigcTaskStatus>([
  "CREATED",
  "PRICE_CALCULATED",
  "FROZEN",
  "QUEUED",
  "RUNNING",
  "SUBMITTED",
  "CALLBACK_WAITING",
  "DOWNLOADING",
  "ASSET_CREATING",
  "AUDITING",
]);

function clampProgress(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)));
}

function getTimeProgress(task: AigcTask, now: number) {
  const estimatedDurationMillis = Number(task.estimatedDurationMillis ?? 0);
  if (!estimatedDurationMillis || estimatedDurationMillis <= 0) {
    return 0;
  }
  const baseTime = task.submitTime || task.startTime || task.createTime;
  if (!baseTime) {
    return 0;
  }
  const startAt = new Date(baseTime).getTime();
  if (Number.isNaN(startAt)) {
    return 0;
  }
  return Math.min(OPTIMISTIC_PROGRESS_CAP, ((now - startAt) / estimatedDurationMillis) * OPTIMISTIC_PROGRESS_CAP);
}

export function getDisplayTaskProgress(task: AigcTask, now = Date.now()) {
  const serverProgress = clampProgress(Number(task.progress ?? 0));
  if (task.status === "SUCCESS") {
    return 100;
  }
  if (!shouldPollTask(task.status) || !OPTIMISTIC_PROGRESS_STATUSES.has(task.status as AigcTaskStatus)) {
    return serverProgress;
  }
  return clampProgress(Math.max(serverProgress, getTimeProgress(task, now)));
}
