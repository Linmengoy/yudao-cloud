"use client";

import { useCallback, useEffect, useState } from "react";
import { getAigcTask, getAigcTaskProgress } from "../task-api";
import { shouldPollTask } from "../task-status";
import type { AigcTask } from "../task-types";

export function useTaskProgress(id: string) {
  const [task, setTask] = useState<AigcTask | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [, setTick] = useState(0);

  const loadTask = useCallback(async (progressOnly = false) => {
    if (!id) return;
    setError("");
    if (progressOnly) setRefreshing(true);
    else setLoading(true);
    try {
      const data = progressOnly ? await getAigcTaskProgress(id) : await getAigcTask(id);
      setTask(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "任务加载失败");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [id]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadTask(), 0);
    return () => window.clearTimeout(timer);
  }, [loadTask]);

  const taskStatus = task?.status;

  useEffect(() => {
    if (!taskStatus || !shouldPollTask(taskStatus)) return;
    const timer = window.setInterval(() => loadTask(true), 3000);
    return () => window.clearInterval(timer);
  }, [loadTask, taskStatus]);

  useEffect(() => {
    if (!taskStatus || !shouldPollTask(taskStatus)) return;
    const timer = window.setInterval(() => setTick((value) => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [taskStatus]);

  return { task, loading, refreshing, error, reload: () => loadTask(false), setTask };
}
