import { api } from "@/lib/api-client";
import { clearPageCache, readPageCache, writePageCache } from "@/lib/page-cache";
import type { CommunityAuthor, CommunityComment, CommunityPost, CommunityShare, PageResult } from "./community-types";

const COMMUNITY_CACHE_PREFIX = "community:";
const COMMUNITY_CACHE_MAX_AGE_MS = 60 * 1000;

function query(params: Record<string, string | number | undefined | null>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

async function cachedGet<T>(path: string) {
  const key = `${COMMUNITY_CACHE_PREFIX}${path}`;
  const cached = readPageCache<T>(key, COMMUNITY_CACHE_MAX_AGE_MS);
  if (cached) return cached;
  const data = await api.get<T>(path);
  writePageCache(key, data);
  return data;
}

function clearCommunityCache() {
  clearPageCache(COMMUNITY_CACHE_PREFIX);
}

export function getCommunityPosts(params: { pageNo?: number; pageSize?: number; sort?: string; assetType?: string; keyword?: string; tag?: string }) {
  return cachedGet<PageResult<CommunityPost>>(`/aigc/community/post/page${query(params)}`);
}

export function getCommunityPost(id: number | string) {
  return cachedGet<CommunityPost>(`/aigc/community/post/get${query({ id })}`);
}

export async function likeCommunityPost(postId: number) {
  const result = await api.put<boolean>("/aigc/community/post/like", { postId });
  clearCommunityCache();
  return result;
}

export async function unlikeCommunityPost(postId: number) {
  const result = await api.delete<boolean>(`/aigc/community/post/like${query({ postId })}`);
  clearCommunityCache();
  return result;
}

export async function shareCommunityPost(postId: number, shareChannel = "COPY") {
  const result = await api.post<CommunityShare>("/aigc/community/post/share", { postId, shareChannel });
  clearCommunityCache();
  return result;
}

export function getCommunityComments(params: { postId: number; pageNo?: number; pageSize?: number }) {
  return cachedGet<PageResult<CommunityComment>>(`/aigc/community/comment/page${query(params)}`);
}

export async function createCommunityComment(postId: number, content: string) {
  const result = await api.post<number>("/aigc/community/comment/create", { postId, content });
  clearCommunityCache();
  return result;
}

export async function deleteCommunityComment(id: number) {
  const result = await api.delete<boolean>(`/aigc/community/comment/delete${query({ id })}`);
  clearCommunityCache();
  return result;
}

export function getCommunityAuthor(authorUserId: number) {
  return cachedGet<CommunityAuthor>(`/aigc/community/author/get${query({ authorUserId })}`);
}

export function getCommunityAuthorPosts(params: { authorUserId: number; pageNo?: number; pageSize?: number; sort?: string }) {
  return cachedGet<PageResult<CommunityPost>>(`/aigc/community/author/post-page${query(params)}`);
}

export async function followCommunityAuthor(authorUserId: number) {
  const result = await api.put<boolean>("/aigc/community/author/follow", { authorUserId });
  clearCommunityCache();
  return result;
}

export async function unfollowCommunityAuthor(authorUserId: number) {
  const result = await api.delete<boolean>(`/aigc/community/author/follow${query({ authorUserId })}`);
  clearCommunityCache();
  return result;
}
