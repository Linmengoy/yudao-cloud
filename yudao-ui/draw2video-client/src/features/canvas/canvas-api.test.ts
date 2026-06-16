import { describe, expect, it, vi } from "vitest";
import { canvasApi } from "./canvas-api";
import { api } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  api: {
    post: vi.fn(),
  },
}));

describe("canvasApi", () => {
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
});
