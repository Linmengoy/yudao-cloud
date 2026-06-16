import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/api-client", () => ({
  api: {
    get: vi.fn(),
  },
}));

describe("release note api", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    vi.useRealTimers();
    delete process.env.NEXT_PUBLIC_APP_VERSION;
  });

  it("clamps the published release note limit before requesting", async () => {
    const { api } = await import("@/lib/api-client");
    const { getPublishedReleaseNotes } = await import("./release-note-api");
    vi.mocked(api.get).mockResolvedValueOnce([]);

    await getPublishedReleaseNotes(100);

    expect(api.get).toHaveBeenCalledWith("/aigc/release-note/published?limit=50");
  });

  it("reuses the in-flight cached request inside the ttl", async () => {
    const { api } = await import("@/lib/api-client");
    const { getPublishedReleaseNotes } = await import("./release-note-api");
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-06-16T00:00:00.000Z"));
    vi.mocked(api.get).mockResolvedValueOnce([{ id: 1, version: "v1.0.0", releaseDate: "2026-06-16", title: "Today" }]);

    const first = getPublishedReleaseNotes(1);
    const second = getPublishedReleaseNotes(1);

    await expect(first).resolves.toHaveLength(1);
    await expect(second).resolves.toHaveLength(1);
    expect(api.get).toHaveBeenCalledTimes(1);
  });

  it("clears cache after a failed request so the next call can retry", async () => {
    const { api } = await import("@/lib/api-client");
    const { getPublishedReleaseNotes } = await import("./release-note-api");
    vi.mocked(api.get)
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValueOnce([]);

    await expect(getPublishedReleaseNotes(1)).rejects.toThrow("network");
    await getPublishedReleaseNotes(1);

    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it("uses the configured app version or a stable fallback label", async () => {
    const { getFallbackVersion } = await import("./release-note-api");
    expect(getFallbackVersion()).toBe("当前版本");
  });
});
