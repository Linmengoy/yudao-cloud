export function formatVideoGenerationError(message: unknown, reason?: unknown) {
  const rawReason = typeof reason === "string" ? reason.trim().toUpperCase() : "";
  if (rawReason === "MEDIA_URL_INVALID") {
    return "生成参数未通过模型校验，请检查参考图是否可访问，并确认视频模式、比例、分辨率和时长后重试。";
  }
  if (rawReason === "PARAM_REJECTED") {
    return "生成参数未通过模型校验，请检查提示词、参考图和视频参数后重试。";
  }

  const rawMessage = typeof message === "string" ? message.trim() : "";
  const lowerMessage = rawMessage.toLowerCase();
  const isParameterRejected =
    lowerMessage.includes("request parameters were rejected") ||
    lowerMessage.includes("verify the prompt") ||
    lowerMessage.includes("media url") ||
    lowerMessage.includes("invalid parameter") ||
    lowerMessage.includes("invalid params") ||
    lowerMessage.includes("invalid request") ||
    lowerMessage.includes("parameter rejected") ||
    lowerMessage.includes("parameter error") ||
    lowerMessage.includes("bad request");
  const isReferenceUrlError =
    lowerMessage.includes("image_url") ||
    lowerMessage.includes("image url") ||
    lowerMessage.includes("media_url") ||
    lowerMessage.includes("media url") ||
    lowerMessage.includes("media") ||
    lowerMessage.includes("reference") ||
    lowerMessage.includes("url");

  if (isParameterRejected) {
    return isReferenceUrlError
      ? "生成参数未通过模型校验，请检查提示词、参考图是否可访问，以及视频模式、比例、分辨率和时长后重试。"
      : "生成参数未通过模型校验，请检查提示词和视频参数后重试。";
  }

  return rawMessage || "视频生成失败，请稍后重试。";
}
