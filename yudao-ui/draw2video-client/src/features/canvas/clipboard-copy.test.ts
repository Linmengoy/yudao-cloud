import { describe, expect, it, vi } from "vitest";
import { copyImageSourceToClipboard, imageSourceToClipboardBlob, type ClipboardItemConstructor } from "./clipboard-copy";

class TestClipboardItem {
  items: Record<string, Blob>;

  constructor(items: Record<string, Blob>) {
    this.items = items;
  }
}

describe("clipboard image copy helpers", () => {
  it("writes the image blob when ClipboardItem image writes are available", async () => {
    const blob = new Blob(["png"], { type: "image/png" });
    const write = vi.fn().mockResolvedValue(undefined);
    const writeText = vi.fn().mockResolvedValue(undefined);
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(blob),
    } as Response);

    const result = await copyImageSourceToClipboard({
      clipboard: { write, writeText },
      clipboardItem: TestClipboardItem as unknown as ClipboardItemConstructor,
      fetcher,
      imageSrc: "https://cdn.example.com/image.png",
      mimeType: "image/png",
    });

    expect(result).toBe("image");
    expect(fetcher).toHaveBeenCalledWith("https://cdn.example.com/image.png");
    expect(write).toHaveBeenCalledTimes(1);
    expect(writeText).not.toHaveBeenCalled();
    const [[items]] = write.mock.calls;
    expect(items[0]).toBeInstanceOf(TestClipboardItem);
    expect((items[0] as unknown as TestClipboardItem).items["image/png"]).toBe(blob);
  });

  it("falls back to writing the image source text when image clipboard writes are unavailable", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);

    const result = await copyImageSourceToClipboard({
      clipboard: { writeText },
      imageSrc: "data:image/png;base64,abc",
      mimeType: "image/png",
    });

    expect(result).toBe("text");
    expect(writeText).toHaveBeenCalledWith("data:image/png;base64,abc");
  });

  it("falls back to text when fetching the image blob fails", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    const fetcher = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
    } as Response);

    const result = await copyImageSourceToClipboard({
      clipboard: { write: vi.fn(), writeText },
      clipboardItem: TestClipboardItem as unknown as ClipboardItemConstructor,
      fetcher,
      imageSrc: "https://cdn.example.com/missing.png",
      mimeType: "image/png",
    });

    expect(result).toBe("text");
    expect(writeText).toHaveBeenCalledWith("https://cdn.example.com/missing.png");
  });

  it("preserves the requested mime type when the fetched blob has no type", async () => {
    const source = new Blob(["png"]);
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(source),
    } as Response);

    const blob = await imageSourceToClipboardBlob("https://cdn.example.com/image", "image/jpeg", fetcher);

    expect(blob.type).toBe("image/jpeg");
  });
});
