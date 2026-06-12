"use client";

import { useEffect, useState } from "react";
import { getAigcModelParamList, type AigcModelParamTemplate } from "./model-api";

export type UseAigcModelTemplatesOptions = {
  selectedModelId: number | null;
  capability: string;
};

function getErrorMessage(err: unknown, fallback: string) {
  return err instanceof Error ? err.message : fallback;
}

function normalizeTemplates(templates: AigcModelParamTemplate[]) {
  return templates.filter((item) => item.status === 0).sort((a, b) => a.sort - b.sort);
}

export function useAigcModelTemplates({ selectedModelId, capability }: UseAigcModelTemplatesOptions) {
  const [templates, setTemplates] = useState<AigcModelParamTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (selectedModelId == null) {
      const timer = window.setTimeout(() => {
        setTemplates([]);
        setLoading(false);
        setError(null);
      }, 0);
      return () => window.clearTimeout(timer);
    }

    let ignore = false;
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError(null);
      getAigcModelParamList(selectedModelId, capability)
        .then((data) => {
          if (!ignore) setTemplates(normalizeTemplates(data));
        })
        .catch((err) => {
          if (!ignore) setError(getErrorMessage(err, "参数模板加载失败"));
        })
        .finally(() => {
          if (!ignore) setLoading(false);
        });
    }, 0);

    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [capability, selectedModelId]);

  return {
    templates,
    loading,
    error,
  };
}
