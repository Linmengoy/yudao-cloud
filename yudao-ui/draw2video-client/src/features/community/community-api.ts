import { api } from "@/lib/api-client";
import type { CommunityAuthor, CommunityComment, CommunityPost, CommunityShare, PageResult } from "./community-types";

function query(params: Record<string, string | number | undefined | null>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

export function getCommunityPosts(params: { pageNo?: number; pageSize?: number; sort?: string; assetType?: string; keyword?: string; tag?: string }) {
  return api.get<PageResult<CommunityPost>>(`/aigc/community/post/page${query(params)}`);
}

export function getCommunityPost(id: number) {
  return api.get<CommunityPost>(`/aigc/community/post/get${query({ id })}`);
}

export function likeCommunityPost(postId: number) {
  return api.put<boolean>("/aigc/community/post/like", { postId });
}

export function unlikeCommunityPost(postId: number) {
  return api.delete<boolean>(`/aigc/community/post/like${query({ postId })}`);
}

export function shareCommunityPost(postId: number, shareChannel = "COPY") {
  return api.post<CommunityShare>("/aigc/community/post/share", { postId, shareChannel });
}

export function getCommunityComments(params: { postId: number; pageNo?: number; pageSize?: number }) {
  return api.get<PageResult<CommunityComment>>(`/aigc/community/comment/page${query(params)}`);
}

export function createCommunityComment(postId: number, content: string) {
  return api.post<number>("/aigc/community/comment/create", { postId, content });
}

export function deleteCommunityComment(id: number) {
  return api.delete<boolean>(`/aigc/community/comment/delete${query({ id })}`);
}

export function getCommunityAuthor(authorUserId: number) {
  return api.get<CommunityAuthor>(`/aigc/community/author/get${query({ authorUserId })}`);
}

export function getCommunityAuthorPosts(params: { authorUserId: number; pageNo?: number; pageSize?: number; sort?: string }) {
  return api.get<PageResult<CommunityPost>>(`/aigc/community/author/post-page${query(params)}`);
}

export function followCommunityAuthor(authorUserId: number) {
  return api.put<boolean>("/aigc/community/author/follow", { authorUserId });
}

export function unfollowCommunityAuthor(authorUserId: number) {
  return api.delete<boolean>(`/aigc/community/author/follow${query({ authorUserId })}`);
}
