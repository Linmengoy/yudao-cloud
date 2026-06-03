export type ImageQuality = "auto" | "low" | "medium" | "high";
export type ImageOutputFormat = "png" | "jpeg" | "webp";
export type ImageModeration = "auto" | "low";

export type ImageTaskParams = {
  size: string;
  quality: ImageQuality;
  output_format: ImageOutputFormat;
  output_compression: number | null;
  moderation: ImageModeration;
  n: number;
  [key: string]: unknown;
};

export const DEFAULT_IMAGE_PARAMS: ImageTaskParams = {
  size: "auto",
  quality: "auto",
  output_format: "png",
  output_compression: null,
  moderation: "auto",
  n: 1,
};
