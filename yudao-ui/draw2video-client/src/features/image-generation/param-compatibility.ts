import type { ImageTaskParams } from "./types";
import { DEFAULT_IMAGE_PARAMS } from "./types";
import { normalizeImageSize } from "./size";

export const MAX_OUTPUT_IMAGES = 10;

export function normalizeImageParams(params: ImageTaskParams): ImageTaskParams {
  return {
    ...params,
    size: normalizeImageSize(params.size) || DEFAULT_IMAGE_PARAMS.size,
    n: Math.min(MAX_OUTPUT_IMAGES, Math.max(1, params.n || 1)),
    output_compression:
      params.output_format === "png" ? null : params.output_compression,
  };
}
