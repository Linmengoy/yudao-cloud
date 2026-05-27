const ACCEPTED_TYPES = new Set(["image/png", "image/jpeg", "image/webp", "image/gif"]);

export function isEditableElement(el: EventTarget | null): boolean {
  if (!(el instanceof HTMLElement)) return false;
  const tag = el.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true;
  if (el.isContentEditable) return true;
  return false;
}

export function getImageFilesFromPasteEvent(e: ClipboardEvent): File[] {
  const items = e.clipboardData?.items;
  if (!items) return [];
  const files: File[] = [];
  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (item.kind === "file" && ACCEPTED_TYPES.has(item.type)) {
      const file = item.getAsFile();
      if (file) files.push(file);
    }
  }
  return files;
}

export async function getImageFilesFromClipboardAPI(): Promise<File[] | null> {
  try {
    if (!navigator.clipboard?.read) return null;
    const items = await navigator.clipboard.read();
    const files: File[] = [];
    for (const item of items) {
      for (const type of item.types) {
        if (ACCEPTED_TYPES.has(type)) {
          const blob = await item.getType(type);
          files.push(new File([blob], `clipboard.${type.split("/")[1]}`, { type }));
        }
      }
    }
    return files.length > 0 ? files : null;
  } catch {
    return null;
  }
}
