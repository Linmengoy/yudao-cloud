import type { GenerateResult, ParsedGenerateResult } from "./generation-types";

function parseJson(value?: string): unknown {
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function parseStringArray(value?: string): string[] {
  const parsed = parseJson(value);
  if (Array.isArray(parsed)) return parsed.map(String);
  if (typeof parsed === "string" && parsed) return [parsed];
  return [];
}

function parseNumberArray(value?: string): number[] {
  return parseStringArray(value)
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item));
}

export function parseGenerateResult(result: GenerateResult): ParsedGenerateResult {
  return {
    ...result,
    outputUrlList: parseStringArray(result.outputUrls),
    assetIdList: parseNumberArray(result.assetIds),
    outputDataValue: parseJson(result.outputData),
  };
}
