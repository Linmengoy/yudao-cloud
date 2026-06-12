"use client";

import { useEffect, useMemo, useState } from "react";
import { calculateAigcModelPrice, type AigcModelParamTemplate, type AigcModelPrice } from "./model-api";
import { filterAigcModelParams } from "./aigc-model-param-utils";

export type UseAigcModelPriceOptions = {
  selectedModelId: number | null;
  capability: string;
  templates: AigcModelParamTemplate[];
  templateLoading: boolean;
  params?: Record<string, unknown>;
};

export function useAigcModelPrice({
  selectedModelId,
  capability,
  templates,
  templateLoading,
  params,
}: UseAigcModelPriceOptions) {
  const [price, setPrice] = useState<AigcModelPrice | null>(null);
  const [loading, setLoading] = useState(false);

  const priceParams = useMemo(() => JSON.stringify(filterAigcModelParams(params ?? {}, templates)), [params, templates]);

  useEffect(() => {
    if (selectedModelId == null || templateLoading) {
      const timer = window.setTimeout(() => {
        setPrice(null);
        setLoading(false);
      }, 0);
      return () => window.clearTimeout(timer);
    }

    let ignore = false;
    const parsedParams = JSON.parse(priceParams) as Record<string, unknown>;
    const timer = window.setTimeout(() => {
      setLoading(true);
      calculateAigcModelPrice({ modelId: selectedModelId, capability, params: parsedParams })
        .then((data) => {
          if (!ignore) setPrice(data);
        })
        .catch(() => {
          if (!ignore) setPrice(null);
        })
        .finally(() => {
          if (!ignore) setLoading(false);
        });
    }, 350);

    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [capability, priceParams, selectedModelId, templateLoading]);

  return {
    price,
    loading,
  };
}
