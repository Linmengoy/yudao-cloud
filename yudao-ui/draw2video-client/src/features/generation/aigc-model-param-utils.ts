import type { AigcModelParamTemplate } from "./model-api";

type FilterAigcModelParamsOptions = {
  extraKeys?: Iterable<string>;
  excludeKeys?: Iterable<string>;
};

export function filterAigcModelParams<TParams extends Record<string, unknown>>(
  params: TParams,
  templates: AigcModelParamTemplate[],
  options: FilterAigcModelParamsOptions = {},
) {
  const templateKeys = new Set(templates.map((template) => template.paramKey));
  const extraKeys = new Set(options.extraKeys ?? []);
  const excludeKeys = new Set(options.excludeKeys ?? []);
  return Object.fromEntries(
    Object.entries(params).filter(
      ([key, value]) =>
        !excludeKeys.has(key) &&
        (templateKeys.has(key) || extraKeys.has(key)) &&
        value !== undefined &&
        value !== null &&
        value !== "",
    ),
  ) as Partial<TParams>;
}
