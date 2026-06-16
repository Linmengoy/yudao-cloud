import { api } from "@/lib/api-client";
import type { ReleaseNote } from "./release-note-types";

const RELEASE_NOTES_CACHE_TTL_MS = 60_000;

let releaseNotesCache: {
  expiresAt: number;
  promise: Promise<ReleaseNote[]>;
} | null = null;

export function getPublishedReleaseNotes(limit = 20): Promise<ReleaseNote[]> {
  const now = Date.now();
  if (releaseNotesCache && releaseNotesCache.expiresAt > now) {
    return releaseNotesCache.promise;
  }
  const safeLimit = Math.max(1, Math.min(limit, 50));
  const promise = api
    .get<ReleaseNote[]>(`/aigc/release-note/published?limit=${safeLimit}`)
    .catch((error) => {
      releaseNotesCache = null;
      throw error;
    });
  releaseNotesCache = {
    expiresAt: now + RELEASE_NOTES_CACHE_TTL_MS,
    promise,
  };
  return promise;
}

export function getFallbackVersion() {
  return process.env.NEXT_PUBLIC_APP_VERSION || "当前版本";
}
