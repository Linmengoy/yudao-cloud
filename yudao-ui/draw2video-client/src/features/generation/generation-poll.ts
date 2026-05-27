import { generationApi } from "./generation-api";
import { parseGenerateResult } from "./generation-result";
import { isGenerationTerminal } from "./generation-status";
import type { ParsedGenerateResult } from "./generation-types";

const POLL_INTERVAL_MS = 1600;
const POLL_TIMEOUT_MS = 180000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

export async function waitGenerationResult(taskId: number | string): Promise<ParsedGenerateResult> {
  const startedAt = Date.now();
  let latest: ParsedGenerateResult | null = null;

  while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
    latest = parseGenerateResult(await generationApi.getResult(taskId));
    if (isGenerationTerminal(latest.status)) return latest;
    await sleep(POLL_INTERVAL_MS);
  }

  if (latest) return latest;
  throw new Error("生成结果查询超时，请稍后到任务中心查看进度。");
}
