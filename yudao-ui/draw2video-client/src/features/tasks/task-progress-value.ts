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

export function getTaskTimingStartMs(
  task: Pick<AigcTask, "id" | "submitTime" | "startTime" | "createTime">,
) {
  const baseTime = task.submitTime || task.startTime || task.createTime;
  if (!baseTime) return null;
  const startAt = new Date(baseTime).getTime();
  return Number.isNaN(startAt) ? null : startAt;
}

function getTimeProgress(task: AigcTask, now: number) {
  const estimatedDurationMillis = Number(task.estimatedDurationMillis ?? 0);
  if (!estimatedDurationMillis || estimatedDurationMillis <= 0) {
    return 0;
  }
  const startAt = getTaskTimingStartMs(task);
  if (startAt == null) {
    return 0;
  }
  return Math.min(
    OPTIMISTIC_PROGRESS_CAP,
    ((now - startAt) / estimatedDurationMillis) * OPTIMISTIC_PROGRESS_CAP,
  );
}

function formatDuration(milliseconds: number) {
  const totalSeconds = Math.max(0, Math.ceil(milliseconds / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const restMinutes = minutes % 60;
    return restMinutes > 0 ? `${hours}小时${restMinutes}分钟` : `${hours}小时`;
  }
  if (minutes > 0) {
    return seconds > 0 ? `${minutes}分${seconds}秒` : `${minutes}分钟`;
  }
  return `${seconds}秒`;
}

export function getDisplayTaskProgress(task: AigcTask, now = Date.now()) {
  const serverProgress = clampProgress(Number(task.progress ?? 0));
  if (task.status === "SUCCESS") {
    return 100;
  }
  if (
    !shouldPollTask(task.status) ||
    !OPTIMISTIC_PROGRESS_STATUSES.has(task.status as AigcTaskStatus)
  ) {
    return serverProgress;
  }
  return clampProgress(Math.max(serverProgress, getTimeProgress(task, now)));
}

export function getTaskEstimatedTimeText(task: AigcTask, now = Date.now()) {
  if (!shouldPollTask(task.status)) return null;
  const estimatedDurationMillis = Number(task.estimatedDurationMillis ?? 0);
  if (!estimatedDurationMillis || estimatedDurationMillis <= 0) return null;
  const startAt = getTaskTimingStartMs(task);
  if (startAt == null) return `预计 ${formatDuration(estimatedDurationMillis)}`;
  const remainingMillis = estimatedDurationMillis - (now - startAt);
  if (remainingMillis <= 0)
    return `已超时 ${formatDuration(Math.abs(remainingMillis))}`;
  return `预计剩余 ${formatDuration(remainingMillis)}`;
}
