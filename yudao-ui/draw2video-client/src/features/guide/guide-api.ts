import { api } from "@/lib/api-client";
import { readPageCache, writePageCache } from "@/lib/page-cache";
import type { GuideContent } from "./guide-types";

const GUIDE_CACHE_PREFIX = "guide:";
const GUIDE_CACHE_MAX_AGE_MS = 5 * 60 * 1000;

function query(params: Record<string, string | number | undefined | null>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

async function cachedGet<T>(path: string) {
  const key = `${GUIDE_CACHE_PREFIX}${path}`;
  const cached = readPageCache<T>(key, GUIDE_CACHE_MAX_AGE_MS);
  if (cached) return cached;
  const data = await api.get<T>(path);
  writePageCache(key, data);
  return data;
}

export function getPublishedGuides() {
  return cachedGet<GuideContent[]>("/aigc/guide/content/list");
}

export function getPublishedGuide(slug: string) {
  return cachedGet<GuideContent>(`/aigc/guide/content/public-get${query({ slug })}`);
}

