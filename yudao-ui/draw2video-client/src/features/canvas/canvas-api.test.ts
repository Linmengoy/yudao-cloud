import { beforeEach, describe, expect, it, vi } from "vitest";
import { canvasApi } from "./canvas-api";
import { api } from "@/lib/api-client";
import type { AppNode } from "./types";

vi.mock("@/lib/api-client", () => ({
  api: {
    post: vi.fn(),
  },
}));

describe("canvasApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("includes the canvas node id in bind asset request body", async () => {
    vi.mocked(api.post).mockResolvedValueOnce(1);

    await canvasApi.bindNodeAsset("project-1", "node/with slash", {
      assetId: 101,
      assetVersionId: null,
      previewUrl: "https://cdn.example.com/image.png",
      usageType: "INPUT",
    });

    expect(api.post).toHaveBeenCalledWith(
      "/canvas/projects/project-1/nodes/node%2Fwith%20slash/assets",
      {
        assetId: 101,
        assetVersionId: null,
        previewUrl: "https://cdn.example.com/image.png",
        usageType: "INPUT",
        projectId: "project-1",
        nodeId: "node/with slash",
      },
    );
  });

  it("strips base64 and runtime preview urls from saved snapshots", async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ id: 1, version: 2 });
    const node: AppNode = {
      id: "image-node",
      type: "image",
      position: { x: 0, y: 0 },
      data: {
        imageId: "image-1",
        assetId: 101,
        fileName: "reference.png",
        dataUrl: "data:image/png;base64,abc",
        previewUrl: "https://signed.example.com/reference.png",
        outputPreviewUrl: "https://signed.example.com/reference.png",
        mimeType: "image/png",
        createdAt: "2026-06-16T00:00:00.000Z",
      },
    };

    await canvasApi.saveSnapshot("project-1", {
      nodes: [node],
      edges: [],
      baseVersion: 1,
    });

    const body = vi.mocked(api.post).mock.calls[0][1] as { nodesJson: string };
    expect(body.nodesJson).not.toContain("data:image");
    expect(body.nodesJson).not.toContain("signed.example.com");
    expect(JSON.parse(body.nodesJson)[0].data).toMatchObject({
      imageId: "image-1",
      assetId: 101,
    });
  });
});
