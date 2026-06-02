"use client";

import { useEffect, useMemo, useState } from "react";
import { calculateAigcModelPrice, getAigcModelList, getAigcModelParamList, type AigcModel, type AigcModelParamTemplate, type AigcModelPrice } from "./model-api";

type UseAigcModelsOptions = {
  type: number;
  capability: string;
  params?: Record<string, unknown>;
};

export function useAigcModels({ type, capability, params }: UseAigcModelsOptions) {
  const [models, setModels] = useState<AigcModel[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<number | null>(null);
  const [templates, setTemplates] = useState<AigcModelParamTemplate[]>([]);
  const [price, setPrice] = useState<AigcModelPrice | null>(null);
  const [loading, setLoading] = useState(false);
  const [templateLoading, setTemplateLoading] = useState(false);
  const [priceLoading, setPriceLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError(null);
      getAigcModelList(type, capability)
        .then((data) => {
          if (ignore) return;
          setModels(data);
          setSelectedModelId((current) =>
            data.some((item) => item.id === current)
              ? current
              : data.find((item) => item.defaultModel)?.id ?? data[0]?.id ?? null
          );
        })
        .catch((err) => {
          if (!ignore) setError(err instanceof Error ? err.message : "模型列表加载失败");
        })
        .finally(() => {
          if (!ignore) setLoading(false);
        });
    }, 0);
    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [capability, type]);

  useEffect(() => {
    if (!selectedModelId) {
      const timer = window.setTimeout(() => setTemplates([]), 0);
      return () => window.clearTimeout(timer);
    }
    let ignore = false;
    const timer = window.setTimeout(() => {
      setTemplateLoading(true);
      getAigcModelParamList(selectedModelId, capability)
        .then((data) => {
          if (!ignore) setTemplates(data.filter((item) => item.status === 0).sort((a, b) => a.sort - b.sort));
        })
        .catch((err) => {
          if (!ignore) setError(err instanceof Error ? err.message : "参数模板加载失败");
        })
        .finally(() => {
          if (!ignore) setTemplateLoading(false);
        });
    }, 0);
    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [capability, selectedModelId]);

  const priceParams = useMemo(() => JSON.stringify(params ?? {}), [params]);

  useEffect(() => {
    if (!selectedModelId) {
      const timer = window.setTimeout(() => setPrice(null), 0);
      return () => window.clearTimeout(timer);
    }
    const parsedParams = JSON.parse(priceParams) as Record<string, unknown>;
    const timer = window.setTimeout(() => {
      setPriceLoading(true);
      calculateAigcModelPrice({ modelId: selectedModelId, capability, params: parsedParams })
        .then(setPrice)
        .catch(() => setPrice(null))
        .finally(() => setPriceLoading(false));
    }, 350);
    return () => window.clearTimeout(timer);
  }, [capability, priceParams, selectedModelId]);

  const selectedModel = models.find((item) => item.id === selectedModelId) ?? null;

  return {
    models,
    selectedModel,
    selectedModelId,
    setSelectedModelId,
    templates,
    price,
    loading,
    templateLoading,
    priceLoading,
    error,
  };
}
