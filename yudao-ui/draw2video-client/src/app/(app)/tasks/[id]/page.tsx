"use client";

import { use, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Loader2, RefreshCw } from "lucide-react";
import { cancelAigcTask } from "@/features/tasks/task-api";
import { TaskProgress } from "@/features/tasks/components/task-progress";
import { TaskResult } from "@/features/tasks/components/task-result";
import { useTaskProgress } from "@/features/tasks/hooks/use-task-progress";
import { formatDateTime, formatPoints, getTaskTypeLabel } from "@/features/tasks/task-status";

export default function TaskDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { task, loading, refreshing, error, reload } = useTaskProgress(id);
  const [cancelling, setCancelling] = useState(false);

  async function handleCancel() {
    if (!window.confirm("确认取消该任务吗？")) return;
    setCancelling(true);
    try {
      await cancelAigcTask(id);
      await reload();
    } finally {
      setCancelling(false);
    }
  }

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-8">
      <Link
        href="/tasks"
        className="inline-flex items-center gap-1 text-sm text-muted-gray hover:text-charcoal"
      >
        <ArrowLeft className="size-4" />
        返回列表
      </Link>

      {loading && (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载任务详情...
        </div>
      )}

      {error && <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-destructive">{error}</div>}

      {task && (
        <div className="mt-5 grid gap-6 lg:grid-cols-[1fr_340px]">
          <div className="flex flex-col gap-4">
            <TaskProgress task={task} refreshing={refreshing} cancelling={cancelling} onCancel={handleCancel} />
            <TaskResult task={task} />
          </div>

          <div className="rounded-lg border border-border-warm bg-background p-4">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="text-base font-semibold tracking-[-0.2px] text-charcoal">{task.taskNo || `任务 #${task.id}`}</h1>
                <p className="mt-1 text-xs text-muted-gray">{getTaskTypeLabel(task.taskType)}</p>
              </div>
              <button
                type="button"
                onClick={reload}
                aria-label="刷新任务详情"
                title="刷新任务详情"
                className="inline-flex items-center gap-1 rounded-md border border-[rgba(28,28,28,0.4)] px-2.5 py-1.5 text-xs text-charcoal hover:bg-muted active:opacity-80"
              >
                <RefreshCw className="size-3.5" />
                刷新
              </button>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-muted-gray">模型 ID</span>
              <span className="text-sm text-charcoal">{task.modelId ?? "-"}</span>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-muted-gray">消耗</span>
              <span className="text-sm text-charcoal">{formatPoints(task.salePrice, task.currencyType)}</span>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-muted-gray">创建时间</span>
              <span className="text-sm text-charcoal">{formatDateTime(task.createTime)}</span>
            </div>
            {task.finishTime && (
              <div className="mt-3 flex items-center justify-between">
                <span className="text-xs text-muted-gray">完成时间</span>
                <span className="text-sm text-charcoal">{formatDateTime(task.finishTime)}</span>
              </div>
            )}
            {task.clientRequestId && (
              <div className="mt-3 flex items-center justify-between gap-3">
                <span className="text-xs text-muted-gray">请求号</span>
                <span className="truncate text-sm text-charcoal">{task.clientRequestId}</span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
