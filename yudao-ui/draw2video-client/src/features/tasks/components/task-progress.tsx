import { Loader2 } from "lucide-react";
import { canCancelTask, getTaskStatusMeta } from "../task-status";
import { getDisplayTaskProgress } from "../task-progress-value";
import type { AigcTask } from "../task-types";
import { TaskStatusBadge } from "./task-status-badge";

export function TaskProgress({
  task,
  refreshing,
  cancelling,
  onCancel,
}: {
  task: AigcTask;
  refreshing?: boolean;
  cancelling?: boolean;
  onCancel?: () => void;
}) {
  const progress = getDisplayTaskProgress(task);
  const meta = getTaskStatusMeta(task.status);

  return (
    <div className="rounded-lg border border-border-warm bg-background p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs text-muted-gray">任务状态</p>
          <div className="mt-2 flex items-center gap-2">
            <TaskStatusBadge status={task.status} />
            {refreshing && <Loader2 className="size-4 animate-spin text-muted-gray" />}
          </div>
        </div>
        {onCancel && canCancelTask(task.status) && (
          <button
            type="button"
            onClick={onCancel}
            disabled={cancelling}
            className="rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-1.5 text-xs text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
          >
            {cancelling ? "取消中..." : "取消任务"}
          </button>
        )}
      </div>
      <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-charcoal transition-all" style={{ width: `${progress}%` }} />
      </div>
      <div className="mt-2 flex items-center justify-between text-xs text-muted-gray">
        <span>{meta.polling ? "任务仍在处理中，页面会自动刷新" : "任务已进入终态"}</span>
        <span>{progress}%</span>
      </div>
    </div>
  );
}
