"use client";

import { useEffect, useMemo, useState } from "react";
import { getAigcModelList, type AigcModel } from "./model-api";

export type UseAigcModelListOptions = {
  type: number;
  capability: string;
  listCapability?: string | null;
  preferredModelId?: number | null;
};

function getErrorMessage(err: unknown, fallback: string) {
  return err instanceof Error ? err.message : fallback;
}

function resolveListCapability(capability: string, listCapability?: string | null) {
  return listCapability === null ? undefined : (listCapability ?? capability);
}

function resolveSelectedModelId(
  models: AigcModel[],
  currentModelId: number | null,
  preferredModelId?: number | null
) {
  const preferredAvailable = preferredModelId != null && models.some((item) => item.id === preferredModelId);
  if (preferredAvailable) return preferredModelId;

  const currentAvailable = currentModelId != null && models.some((item) => item.id === currentModelId);
  if (currentAvailable) return currentModelId;

  return models.find((item) => item.defaultModel)?.id ?? models[0]?.id ?? null;
}

export function useAigcModelList({
  type,
  capability,
  listCapability,
  preferredModelId,
}: UseAigcModelListOptions) {
  const [models, setModels] = useState<AigcModel[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<number | null>(preferredModelId ?? null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError(null);
      getAigcModelList(type, resolveListCapability(capability, listCapability))
        .then((data) => {
          if (ignore) return;
          setModels(data);
          setSelectedModelId((current) => resolveSelectedModelId(data, current, preferredModelId));
        })
        .catch((err) => {
          if (!ignore) setError(getErrorMessage(err, "模型列表加载失败"));
        })
        .finally(() => {
          if (!ignore) setLoading(false);
        });
    }, 0);

    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [capability, listCapability, preferredModelId, type]);

  const selectedModel = useMemo(
    () => models.find((item) => item.id === selectedModelId) ?? null,
    [models, selectedModelId]
  );

  return {
    models,
    selectedModel,
    selectedModelId,
    setSelectedModelId,
    loading,
    error,
  };
}
