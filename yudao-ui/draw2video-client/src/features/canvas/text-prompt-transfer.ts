import type { AppNode, TextNodeData } from "./types";

const PROMPT_TARGET_TYPES = new Set<AppNode["type"]>(["image", "text", "video"]);

export function getTextPromptTransferPatch(source: AppNode | undefined, target: AppNode | undefined) {
  if (source?.type !== "text" || !target || !PROMPT_TARGET_TYPES.has(target.type)) return null;

  const sourceText = ((source.data as TextNodeData).content ?? "").trim();
  if (!sourceText) return null;

  const currentPrompt = typeof target.data.prompt === "string" ? target.data.prompt.trim() : "";
  if (!currentPrompt) return { prompt: sourceText };
  if (currentPrompt === sourceText || currentPrompt.includes(sourceText)) return null;

  return { prompt: `${sourceText}\n\n${currentPrompt}` };
}
