import { api } from "@/lib/api-client";
import type { GenerateResult, GenerateSubmitRequest, GenerateSubmitResponse, VerticalGenerateSubmitRequest } from "./generation-types";

export const generationApi = {
  submit: (data: GenerateSubmitRequest) => api.post<GenerateSubmitResponse>("/aigc/gen/submit", data),

  generateText: (data: VerticalGenerateSubmitRequest) => api.post<GenerateSubmitResponse>("/aigc/gen/text/generate", data),

  textToImage: (data: VerticalGenerateSubmitRequest) => api.post<GenerateSubmitResponse>("/aigc/gen/image/text-to-image", data),

  textToVideo: (data: VerticalGenerateSubmitRequest) => api.post<GenerateSubmitResponse>("/aigc/gen/video/text-to-video", data),

  getResult: (taskId: number | string) => api.get<GenerateResult>(`/aigc/gen/result?taskId=${encodeURIComponent(taskId)}`),
};
