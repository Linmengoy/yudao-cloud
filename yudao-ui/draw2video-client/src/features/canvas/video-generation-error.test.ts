import { describe, expect, it } from "vitest";
import { formatVideoGenerationError } from "./video-generation-error";

describe("formatVideoGenerationError", () => {
  it("uses a reference-media message for classified media URL failures", () => {
    const message = formatVideoGenerationError(
      "The request parameters were rejected. Please verify the prompt and media URLs, then try again.",
      "MEDIA_URL_INVALID",
    );

    expect(message).toBe(
      "生成参数未通过模型校验，请检查参考图是否可访问，并确认视频模式、比例、分辨率和时长后重试。",
    );
    expect(message).not.toContain("request parameters were rejected");
  });

  it("uses a general parameter message for classified parameter failures", () => {
    expect(formatVideoGenerationError("invalid parameter: duration", "PARAM_REJECTED")).toBe(
      "生成参数未通过模型校验，请检查提示词、参考图和视频参数后重试。",
    );
  });

  it("translates provider parameter rejection text even without a fail reason", () => {
    const message = formatVideoGenerationError(
      "The request parameters were rejected. Please verify the prompt and media URLs, then try again.",
    );

    expect(message).toBe(
      "生成参数未通过模型校验，请检查提示词、参考图是否可访问，以及视频模式、比例、分辨率和时长后重试。",
    );
    expect(message).not.toContain("Please verify the prompt");
  });

  it("keeps unrelated failures visible for troubleshooting", () => {
    expect(formatVideoGenerationError("provider temporarily unavailable")).toBe(
      "provider temporarily unavailable",
    );
  });
});
