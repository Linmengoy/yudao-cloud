type ClipboardLike = {
  write?: (items: ClipboardItem[]) => Promise<void>;
  writeText?: (text: string) => Promise<void>;
};

export type ClipboardItemConstructor = new (items: Record<string, Blob>) => ClipboardItem;

type CopyImageOptions = {
  clipboard?: ClipboardLike | null;
  clipboardItem?: ClipboardItemConstructor;
  fetcher?: typeof fetch;
  imageSrc: string;
  mimeType?: string | null;
};

export type CopyImageResult = "image" | "text" | "failed" | "unavailable";

export async function imageSourceToClipboardBlob(
  imageSrc: string,
  mimeType?: string | null,
  fetcher: typeof fetch = fetch,
) {
  const response = await fetcher(imageSrc);
  if (!response.ok) {
    throw new Error(`Image fetch failed: ${response.status}`);
  }
  const blob = await response.blob();
  return blob.type ? blob : blob.slice(0, blob.size, mimeType || "image/png");
}

export async function copyImageSourceToClipboard({
  clipboard = globalThis.navigator?.clipboard,
  clipboardItem = globalThis.ClipboardItem,
  fetcher = fetch,
  imageSrc,
  mimeType,
}: CopyImageOptions): Promise<CopyImageResult> {
  if (!imageSrc) return "unavailable";

  try {
    if (clipboardItem && clipboard?.write) {
      const blob = await imageSourceToClipboardBlob(imageSrc, mimeType, fetcher);
      await clipboard.write([
        new clipboardItem({ [blob.type || mimeType || "image/png"]: blob }),
      ]);
      return "image";
    }

    if (clipboard?.writeText) {
      await clipboard.writeText(imageSrc);
      return "text";
    }
  } catch {
    try {
      if (clipboard?.writeText) {
        await clipboard.writeText(imageSrc);
        return "text";
      }
    } catch {
      return "failed";
    }
    return "failed";
  }

  return "unavailable";
}
