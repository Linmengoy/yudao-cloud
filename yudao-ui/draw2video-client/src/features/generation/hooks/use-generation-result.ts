"use client";

import { useCallback, useEffect, useState } from "react";
import { generationApi } from "../generation-api";
import { parseGenerateResult } from "../generation-result";
import { shouldPollGeneration } from "../generation-status";
import type { ParsedGenerateResult } from "../generation-types";

export function useGenerationResult(taskId?: number | string | null) {
  const [result, setResult] = useState<ParsedGenerateResult | null>(null);
  const [loading, setLoading] = useState(Boolean(taskId));
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const loadResult = useCallback(async (refreshOnly = false) => {
    if (!taskId) return null;
    setError("");
    if (refreshOnly) setRefreshing(true);
    else setLoading(true);
    try {
      const data = parseGenerateResult(await generationApi.getResult(taskId));
      setResult(data);
      return data;
    } catch (err) {
      setError(err instanceof Error ? err.message : "生成结果加载失败");
      return null;
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [taskId]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadResult(), 0);
    return () => window.clearTimeout(timer);
  }, [loadResult]);

  const status = result?.status;

  useEffect(() => {
    if (!status || !shouldPollGeneration(status)) return;
    const timer = window.setInterval(() => loadResult(true), 3000);
    return () => window.clearInterval(timer);
  }, [loadResult, status]);

  return { result, loading, refreshing, error, reload: () => loadResult(false), setResult };
}
