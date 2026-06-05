"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Loader2, RefreshCw } from "lucide-react";
import { getAigcTaskPage } from "@/features/tasks/task-api";
import type { AigcTask } from "@/features/tasks/task-types";
import { TaskCard } from "@/features/tasks/components/task-card";

export default function TasksPage() {
  const [tasks, setTasks] = useState<AigcTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadTasks = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getAigcTaskPage({ pageNo: 1, pageSize: 12 });
      setTasks(data.list ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "任务列表加载失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(loadTasks, 0);
    return () => window.clearTimeout(timer);
  }, [loadTasks]);

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold tracking-[-0.3px] text-charcoal">任务</h1>
          <p className="mt-1 text-sm text-muted-gray">查看生成进度、结果和退款状态。</p>
        </div>
        <button
          type="button"
          onClick={loadTasks}
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

      {!loading && tasks.length === 0 ? (
        <div className="mt-16 flex flex-col items-center text-center">
          <p className="text-muted-gray">还没有任务</p>
          <Link href="/canvas" className="mt-4 inline-flex items-center rounded-md bg-charcoal px-5 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80">
            去创作
          </Link>
        </div>
      ) : tasks.length > 0 ? (
        <div className="mt-5 grid gap-3 lg:grid-cols-2">
          {tasks.map((task) => <TaskCard key={task.id} task={task} />)}
        </div>
      ) : null}
    </div>
  );
}
