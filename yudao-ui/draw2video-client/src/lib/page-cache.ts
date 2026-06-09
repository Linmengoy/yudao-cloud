type PageCacheEntry<T> = {
  value: T;
  updatedAt: number;
};

const DEFAULT_MAX_AGE_MS = 5 * 60 * 1000;
const pageCache = new Map<string, PageCacheEntry<unknown>>();

export function readPageCache<T>(key: string, maxAgeMs = DEFAULT_MAX_AGE_MS): T | null {
  const entry = pageCache.get(key) as PageCacheEntry<T> | undefined;
  if (!entry) return null;
  if (maxAgeMs > 0 && Date.now() - entry.updatedAt > maxAgeMs) {
    pageCache.delete(key);
    return null;
  }
  return entry.value;
}

export function writePageCache<T>(key: string, value: T) {
  pageCache.set(key, { value, updatedAt: Date.now() });
}

export function isSamePageCacheValue(left: unknown, right: unknown) {
  if (Object.is(left, right)) return true;
  try {
    return JSON.stringify(left) === JSON.stringify(right);
  } catch {
    return false;
  }
}

export function mergeStableList<T>(
  current: T[],
  next: T[],
  getKey: (item: T) => string | number,
  isEqual: (left: T, right: T) => boolean = isSamePageCacheValue
) {
  let changed = current.length !== next.length;
  const currentByKey = new Map(current.map((item) => [String(getKey(item)), item]));
  const merged = next.map((item, index) => {
    const previous = currentByKey.get(String(getKey(item)));
    if (previous && isEqual(previous, item)) {
      if (current[index] !== previous) changed = true;
      return previous;
    }
    changed = true;
    return item;
  });
  return changed ? merged : current;
}
