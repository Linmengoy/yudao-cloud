"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowUpRight, Loader2, RefreshCw } from "lucide-react";
import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyStatusPill } from "@/features/safety/safety-ui";
import { normalizeSafetyStatus, normalizeSafetyStatusFromError } from "@/features/safety/safety-status";
import { getAigcTaskPage } from "@/features/tasks/task-api";
import { getDisplayTaskProgress } from "@/features/tasks/task-progress-value";
import { formatDateTime, formatPoints, getTaskTypeLabel, shouldPollTask } from "@/features/tasks/task-status";
import type { AigcTask } from "@/features/tasks/task-types";
import { TaskStatusBadge } from "@/features/tasks/components/task-status-badge";
import { mergeStableList, readPageCache, writePageCache } from "@/lib/page-cache";

const taskPageSize = 12;

type TasksPageCache = {
  tasks: AigcTask[];
  total: number;
};

function tasksPageCacheKey(pageNo: number) {
  return `tasks:${pageNo}`;
}

function getTaskKey(task: AigcTask) {
  return task.id ?? task.taskNo ?? "";
}

function getTaskProgress(task: AigcTask, now: number) {
  return getDisplayTaskProgress(task, now);
}

function getTaskSafety(task: AigcTask) {
  const safetyStatus = normalizeSafetyStatus(task.safetyStatus ?? task.auditStatus ?? (task.status === "AUDITING" ? "reviewing" : null));
  const failedSafetyStatus = task.status === "FAILED" ? normalizeSafetyStatusFromError(task.auditReason ?? task.failReason) : "idle";
  return getSafetyCopy(safetyStatus !== "idle" ? safetyStatus : failedSafetyStatus, "task");
}

function getTaskNote(task: AigcTask) {
  return task.failReason || task.auditReason || task.outputSummary || "-";
}

function getPageCount(total: number) {
  return Math.max(1, Math.ceil(total / taskPageSize));
}

export default function TasksPage() {
  const initialPageCache = readPageCache<TasksPageCache>(tasksPageCacheKey(1));
  const [tasks, setTasks] = useState<AigcTask[]>(() => initialPageCache?.tasks ?? []);
  const [total, setTotal] = useState(() => initialPageCache?.total ?? 0);
  const [pageNo, setPageNo] = useState(1);
  const [loading, setLoading] = useState(() => !initialPageCache);
  const [error, setError] = useState("");
  const [now, setNow] = useState(() => Date.now());
  const pageCount = getPageCount(total);

  const loadTasks = useCallback(async (nextPageNo: number, options?: { silent?: boolean }) => {
    const cacheKey = tasksPageCacheKey(nextPageNo);
    const cached = readPageCache<TasksPageCache>(cacheKey);
    if (cached) {
      setTasks((items) => mergeStableList(items, cached.tasks, getTaskKey));
      setTotal(cached.total);
      setLoading(false);
    } else if (!options?.silent) {
      setLoading(true);
    }
    if (!options?.silent) {
      setError("");
    }
    try {
      const data = await getAigcTaskPage({ pageNo: nextPageNo, pageSize: taskPageSize });
      const nextTasks = data.list ?? [];
      const nextTotal = data.total ?? 0;
      writePageCache<TasksPageCache>(cacheKey, {
        tasks: nextTasks,
        total: nextTotal,
      });
      setTasks((items) => mergeStableList(items, nextTasks, getTaskKey));
      setTotal(nextTotal);
    } catch (err) {
      if (!options?.silent) {
        setError(err instanceof Error ? err.message : "任务列表加载失败");
      }
    } finally {
      if (!options?.silent) {
        setLoading(false);
      }
    }
  }, []);

  function handlePageChange(nextPageNo: number) {
    const safePageNo = Math.min(Math.max(nextPageNo, 1), pageCount);
    setPageNo(safePageNo);
    loadTasks(safePageNo);
  }

  useEffect(() => {
    const timer = window.setTimeout(() => loadTasks(1), 0);
    return () => window.clearTimeout(timer);
  }, [loadTasks]);

  useEffect(() => {
    if (!tasks.some((task) => shouldPollTask(task.status))) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [tasks]);

  useEffect(() => {
    if (!tasks.some((task) => shouldPollTask(task.status))) return;
    const timer = window.setInterval(() => loadTasks(pageNo, { silent: true }), 3000);
    return () => window.clearInterval(timer);
  }, [loadTasks, pageNo, tasks]);

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold tracking-[-0.3px] text-charcoal">任务</h1>
          <p className="mt-1 text-sm text-muted-gray">查看生成进度、结果和退款状态。</p>
        </div>
        <button
          type="button"
          onClick={() => loadTasks(pageNo)}
          disabled={loading}
          aria-label="刷新任务列表"
          title="刷新任务列表"
          className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
        >
          <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
          刷新
        </button>
      </div>

      {error && <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-destructive">{error}</div>}

      {loading && !tasks.length && (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载任务列表...
        </div>
      )}

      {!loading && total === 0 ? (
        <div className="mt-16 flex flex-col items-center text-center">
          <p className="text-muted-gray">还没有任务</p>
          <Link href="/canvas" className="mt-4 inline-flex items-center rounded-md bg-charcoal px-5 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80">
            去创作
          </Link>
        </div>
      ) : tasks.length > 0 ? (
        <>
          <div className="mt-5 overflow-x-auto rounded-lg border border-border-warm bg-background">
            <table className="w-full min-w-[1060px] border-collapse text-left">
              <thead className="bg-muted text-xs text-muted-gray">
                <tr>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">操作</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">任务</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">类型</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">状态</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">安全</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">进度</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">消耗</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">创建时间</th>
                  <th className="whitespace-nowrap px-4 py-3 font-medium">说明</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-warm">
                {tasks.map((task) => {
                  const progress = getTaskProgress(task, now);
                  const safety = getTaskSafety(task);
                  const note = getTaskNote(task);

                  return (
                    <tr key={task.id} className="transition-colors hover:bg-muted/60">
                      <td className="whitespace-nowrap px-4 py-3 align-middle">
                        <Link
                          href={`/tasks/${task.id}`}
                          className="inline-flex items-center gap-1 whitespace-nowrap rounded-md border border-border-warm px-2.5 py-1.5 text-xs text-charcoal transition-colors hover:border-[rgba(28,28,28,0.4)] active:opacity-80"
                        >
                          <ArrowUpRight className="size-3.5" />
                          查看
                        </Link>
                      </td>
                      <td className="px-4 py-3 align-middle">
                        <Link href={`/tasks/${task.id}`} className="block max-w-[220px] truncate whitespace-nowrap text-sm font-medium text-charcoal">
                          {task.taskNo || `任务 #${task.id}`}
                        </Link>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-middle text-sm text-muted-gray">
                        {getTaskTypeLabel(task.taskType)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-middle">
                        <TaskStatusBadge status={task.status} />
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-middle">
                        {safety.status !== "idle" ? <SafetyStatusPill state={safety} className="whitespace-nowrap" /> : <span className="text-xs text-muted-gray">-</span>}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-middle">
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 w-28 overflow-hidden rounded-full bg-muted">
                            <div className="h-full rounded-full bg-charcoal" style={{ width: `${progress}%` }} />
                          </div>
                          <span className="w-9 text-right text-xs text-muted-gray">{progress}%</span>
                        </div>
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-middle text-sm text-charcoal">
                        {formatPoints(task.salePrice, task.currencyType)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 align-middle text-sm text-muted-gray">
                        {formatDateTime(task.createTime)}
                      </td>
                      <td className="px-4 py-3 align-middle">
                        <p className={`max-w-[260px] truncate whitespace-nowrap text-xs ${task.failReason || task.auditReason ? "text-destructive" : "text-muted-gray"}`}>
                          {note}
                        </p>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-muted-gray">
            <span>
              共 {total.toLocaleString("zh-CN")} 条，当前第 {pageNo} / {pageCount} 页
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handlePageChange(pageNo - 1)}
                disabled={loading || pageNo <= 1}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal transition-colors hover:border-[rgba(28,28,28,0.4)] disabled:opacity-40"
              >
                上一页
              </button>
              <button
                type="button"
                onClick={() => handlePageChange(pageNo + 1)}
                disabled={loading || pageNo >= pageCount}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal transition-colors hover:border-[rgba(28,28,28,0.4)] disabled:opacity-40"
              >
                下一页
              </button>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
