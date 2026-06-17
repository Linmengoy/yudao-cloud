import { describe, expect, it } from "vitest";
import { getTextPromptTransferPatch } from "./text-prompt-transfer";
import type { AppNode } from "./types";

const textNode = (content: string): AppNode => ({
  id: "text_1",
  type: "text",
  position: { x: 0, y: 0 },
  data: {
    fileName: "Text",
    content,
    prompt: "",
    modelId: "model",
    status: "idle",
    errorMessage: null,
    width: 320,
    height: 260,
    createdAt: "now",
  },
});

const targetNode = (type: AppNode["type"], prompt?: string): AppNode => ({
  id: `${type}_1`,
  type,
  position: { x: 0, y: 0 },
  data: { prompt },
} as AppNode);

describe("getTextPromptTransferPatch", () => {
  it("copies text content into empty downstream prompts", () => {
    expect(getTextPromptTransferPatch(textNode("base prompt"), targetNode("image", ""))).toEqual({
      prompt: "base prompt",
    });
  });

  it("prepends text content to existing downstream prompts", () => {
    expect(getTextPromptTransferPatch(textNode("base prompt"), targetNode("video", "make it cinematic"))).toEqual({
      prompt: "base prompt\n\nmake it cinematic",
    });
  });

  it("does not duplicate text content", () => {
    expect(getTextPromptTransferPatch(textNode("base prompt"), targetNode("image", "base prompt\n\nmake it cinematic"))).toBeNull();
  });

  it("skips nodes without prompt composers", () => {
    expect(getTextPromptTransferPatch(textNode("base prompt"), targetNode("sketch"))).toBeNull();
  });
});
