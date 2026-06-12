import type { AigcModelParamTemplate } from "./model-api";

export function filterAigcModelParams<TParams extends Record<string, unknown>>(
  params: TParams,
  templates: AigcModelParamTemplate[],
) {
  const keys = new Set(templates.map((template) => template.paramKey));
  return Object.fromEntries(
    Object.entries(params).filter(
      ([key, value]) =>
        keys.has(key) && value !== undefined && value !== null && value !== "",
    ),
  ) as Partial<TParams>;
}
