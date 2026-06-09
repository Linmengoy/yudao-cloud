import Link from "next/link";
import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyInlineNotice, SafetyStatusPill } from "@/features/safety/safety-ui";
import { normalizeSafetyStatus, normalizeSafetyStatusFromError } from "@/features/safety/safety-status";
import { getDisplayTaskProgress } from "../task-progress-value";
import { formatDateTime, formatPoints, getTaskTypeLabel } from "../task-status";
import type { AigcTask } from "../task-types";
import { TaskStatusBadge } from "./task-status-badge";

export function TaskCard({ task, now }: { task: AigcTask; now?: number }) {
  const progress = getDisplayTaskProgress(task, now);
  const safetyStatus = normalizeSafetyStatus(task.safetyStatus ?? task.auditStatus ?? (task.status === "AUDITING" ? "reviewing" : null));
  const failedSafetyStatus = task.status === "FAILED" ? normalizeSafetyStatusFromError(task.auditReason ?? task.failReason) : "idle";
  const safety = getSafetyCopy(safetyStatus !== "idle" ? safetyStatus : failedSafetyStatus, "task");

  return (
    <Link
      href={`/tasks/${task.id}`}
      className="group block rounded-lg border border-border-warm bg-background px-4 py-3 transition-colors hover:border-[rgba(28,28,28,0.4)]"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-charcoal">{task.taskNo || `任务 #${task.id}`}</p>
          <p className="mt-1 text-xs text-muted-gray">{getTaskTypeLabel(task.taskType)}</p>
        </div>
        <div className="flex shrink-0 flex-wrap justify-end gap-2">
          <TaskStatusBadge status={task.status} />
          <SafetyStatusPill state={safety} />
        </div>
      </div>
      <div className="mt-3 flex items-center gap-3">
        <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
          <div className="h-full rounded-full bg-charcoal" style={{ width: `${progress}%` }} />
        </div>
        <span className="w-9 text-right text-xs text-muted-gray">{progress}%</span>
      </div>
      <div className="mt-3 flex items-center justify-between gap-3 text-xs text-muted-gray">
        <span>{formatDateTime(task.createTime)}</span>
        <span>{formatPoints(task.salePrice, task.currencyType)}</span>
      </div>
      {safety.status !== "idle" ? <SafetyInlineNotice state={safety} className="mt-3" /> : task.failReason && <p className="mt-3 line-clamp-2 text-xs text-destructive">{task.failReason}</p>}
      {task.outputSummary && <p className="mt-3 line-clamp-2 text-xs text-muted-gray">{task.outputSummary}</p>}
    </Link>
  );
}
