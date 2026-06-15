import { describe, expect, it } from "vitest";
import type { ImageNodeData, SketchNodeData } from "./types";
import {
  appendReferenceSubmitParams,
  getReferenceAssetId,
} from "./video-reference-submit";

function imageData(overrides: Partial<ImageNodeData>): ImageNodeData {
  return {
    imageId: "image-1",
    fileName: "image.png",
    dataUrl: "data:image/png;base64,abc",
    mimeType: "image/png",
    createdAt: "2026-06-15T00:00:00.000Z",
    ...overrides,
  };
}

function sketchData(overrides: Partial<SketchNodeData>): SketchNodeData {
  return {
    sketchId: "sketch-1",
    fileName: "sketch.png",
    dataUrl: "data:image/png;base64,abc",
    mimeType: "image/png",
    createdAt: "2026-06-15T00:00:00.000Z",
    ...overrides,
  };
}

describe("video reference submit helpers", () => {
  it("uses the first valid sketch asset id from assetIds", () => {
    expect(getReferenceAssetId(sketchData({ assetIds: [0, -1, 42, 43] }))).toBe(42);
  });

  it("prefers direct image asset ids and generated output asset ids", () => {
    expect(getReferenceAssetId(imageData({ assetId: 7, outputAssetId: 8 }))).toBe(7);
    expect(getReferenceAssetId(imageData({ outputAssetId: 8 }))).toBe(8);
  });

  it("adds reference asset ids and node ids to submitted video params", () => {
    const params = appendReferenceSubmitParams(
      { providerModel: "seedance-1" },
      [
        { nodeId: "sketch-node", data: sketchData({ assetIds: [101] }) },
        { nodeId: "image-node", data: imageData({ outputAssetId: 202 }) },
      ],
      ["https://cdn.example.com/sketch.png"],
    );

    expect(params).toEqual({
      providerModel: "seedance-1",
      referenceAssetIds: [101, 202],
      referenceImages: ["https://cdn.example.com/sketch.png"],
      referenceImageIds: ["sketch-node", "image-node"],
    });
  });
});
