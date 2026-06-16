import { describe, expect, it } from "vitest";
import { collectNodeAssetIds, withFreshAssetUrl } from "./canvas-asset-runtime";
import { sanitizeCanvasStateForPersistence, sanitizeNodeForCanvasOperation, stripRuntimeAssetUrlsFromPatch } from "./canvas-syncable-data";
import type { AppEdge, AppNode, ImageNodeData } from "./types";

function imageNode(data: Partial<ImageNodeData>): AppNode {
  return {
    id: "image-node",
    type: "image",
    position: { x: 0, y: 0 },
    data: {
      imageId: "image-1",
      fileName: "result.png",
      dataUrl: "",
      mimeType: "image/png",
      createdAt: "2026-06-16T00:00:00.000Z",
      ...data,
    },
  };
}

describe("canvas asset runtime helpers", () => {
  it("collects main and output asset ids without duplicates", () => {
    const node = imageNode({
      assetId: 11,
      outputAssetId: 22,
      outputs: [
        { id: "a", assetId: 22, previewUrl: "" },
        { id: "b", assetId: 33, previewUrl: "" },
      ],
    });

    expect(collectNodeAssetIds(node)).toEqual([11, 22, 33]);
  });

  it("hydrates the matching image output without replacing the primary preview", () => {
    const node = imageNode({
      assetId: 11,
      previewUrl: "https://cdn.example.com/main.png",
      outputPreviewUrl: "https://cdn.example.com/main.png",
      outputs: [
        { id: "main", assetId: 11, previewUrl: "https://cdn.example.com/main.png" },
        { id: "alt", assetId: 33, previewUrl: "" },
      ],
    });

    const hydrated = withFreshAssetUrl(
      node,
      "https://cdn.example.com/alt.png",
      "2026-06-16T01:00:00.000Z",
      33,
    );

    expect(hydrated.data.previewUrl).toBe("https://cdn.example.com/main.png");
    expect((hydrated.data as ImageNodeData).outputs).toEqual([
      { id: "main", assetId: 11, previewUrl: "https://cdn.example.com/main.png" },
      { id: "alt", assetId: 33, previewUrl: "https://cdn.example.com/alt.png" },
    ]);
    expect(hydrated.data.assetUrlExpireTime).toBe("2026-06-16T01:00:00.000Z");
  });

  it("strips runtime urls from persisted image output patches", () => {
    const patch = stripRuntimeAssetUrlsFromPatch({
      outputs: [
        {
          id: "asset-33",
          assetId: 33,
          previewUrl: "https://signed.example.com/result.png",
        },
      ],
      outputPreviewUrl: "https://signed.example.com/result.png",
      assetUrlExpireTime: "2026-06-16T01:00:00.000Z",
    });

    expect(patch).toEqual({
      outputs: [{ id: "asset-33", assetId: 33 }],
    });
  });

  it("removes runtime urls from canvas operations while keeping asset identity", () => {
    const sanitized = sanitizeNodeForCanvasOperation(imageNode({
      assetId: 44,
      previewUrl: "https://signed.example.com/main.png",
      outputPreviewUrl: "https://signed.example.com/main.png",
      outputs: [
        {
          id: "asset-44",
          assetId: 44,
          previewUrl: "https://signed.example.com/main.png",
        },
      ],
    }));

    expect(sanitized.data).toMatchObject({
      assetId: 44,
      outputs: [{ id: "asset-44", assetId: 44 }],
    });
    expect(sanitized.data.previewUrl).toBeUndefined();
    expect(sanitized.data.outputPreviewUrl).toBeUndefined();
  });

  it("keeps local media nodes and edges in persisted canvas snapshots without data urls", () => {
    const referenceNode = imageNode({
      imageId: "reference-image",
      dataUrl: "data:image/png;base64,abc",
      prompt: "reference prompt",
    });
    const resultNode = imageNode({
      imageId: "generated-image",
      prompt: "combine two references",
      outputAssetId: 88,
      outputs: [{ id: "asset-88", assetId: 88, previewUrl: "https://signed.example.com/result.png" }],
    });
    resultNode.id = "result-image";
    const edge: AppEdge = {
      id: "edge-reference-result",
      source: referenceNode.id,
      target: resultNode.id,
      type: "signal",
    };

    const sanitized = sanitizeCanvasStateForPersistence({
      nodes: [referenceNode, resultNode],
      edges: [edge],
    });

    expect(sanitized.nodes).toHaveLength(2);
    expect(sanitized.edges).toEqual([edge]);
    expect(sanitized.nodes[0].data.prompt).toBe("reference prompt");
    expect(sanitized.nodes[0].data.dataUrl).toBeUndefined();
    expect(sanitized.nodes[1].data.prompt).toBe("combine two references");
    expect((sanitized.nodes[1].data as ImageNodeData).outputs).toEqual([{ id: "asset-88", assetId: 88 }]);
  });
});
