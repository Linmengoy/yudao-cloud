"use client";

import { useAigcModelList, type UseAigcModelListOptions } from "./use-aigc-model-list";
import { useAigcModelPrice } from "./use-aigc-model-price";
import { useAigcModelTemplates } from "./use-aigc-model-templates";

type UseAigcModelsOptions = UseAigcModelListOptions & {
  params?: Record<string, unknown>;
};

export function useAigcModels({ type, capability, listCapability, preferredModelId, params }: UseAigcModelsOptions) {
  const modelList = useAigcModelList({ type, capability, listCapability, preferredModelId });
  const templateState = useAigcModelTemplates({
    selectedModelId: modelList.selectedModelId,
    capability,
  });
  const priceState = useAigcModelPrice({
    selectedModelId: modelList.selectedModelId,
    capability,
    templates: templateState.templates,
    templateLoading: templateState.loading,
    params,
  });

  return {
    models: modelList.models,
    selectedModel: modelList.selectedModel,
    selectedModelId: modelList.selectedModelId,
    setSelectedModelId: modelList.setSelectedModelId,
    templates: templateState.templates,
    price: priceState.price,
    loading: modelList.loading,
    templateLoading: templateState.loading,
    priceLoading: priceState.loading,
    error: modelList.error ?? templateState.error,
  };
}

export { useAigcModelList } from "./use-aigc-model-list";
export { useAigcModelPrice } from "./use-aigc-model-price";
export { useAigcModelTemplates } from "./use-aigc-model-templates";
export type { UseAigcModelListOptions } from "./use-aigc-model-list";
export type { UseAigcModelPriceOptions } from "./use-aigc-model-price";
export type { UseAigcModelTemplatesOptions } from "./use-aigc-model-templates";
