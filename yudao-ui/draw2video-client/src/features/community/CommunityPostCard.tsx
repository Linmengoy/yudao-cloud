"use client";

import Link from "next/link";
import { Heart, MessageCircle, Share2 } from "lucide-react";
import type { CommunityPost } from "./community-types";

export function CommunityPostCard({ post }: { post: CommunityPost }) {
  return (
    <Link href={`/community/${post.id}`} className="group block overflow-hidden rounded-lg border border-border-warm bg-background transition-colors hover:border-[rgba(28,28,28,0.35)]">
      <div className="aspect-[4/3] bg-muted">
        {post.coverUrl || post.fileUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={post.coverUrl || post.fileUrl} alt={post.title} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-sm text-muted-gray">No preview</div>
        )}
      </div>
      <div className="space-y-3 p-3">
        <div>
          <h3 className="line-clamp-1 text-sm font-medium text-charcoal">{post.title}</h3>
          <p className="mt-1 line-clamp-2 min-h-9 text-xs leading-5 text-muted-gray">{post.summary || "Community work"}</p>
        </div>
        <div className="flex items-center justify-between gap-3 text-xs text-muted-gray">
          <span className="truncate">{post.authorNickname || `User ${post.authorUserId}`}</span>
          <div className="flex shrink-0 items-center gap-2">
            <span className="inline-flex items-center gap-1"><Heart className="size-3.5" />{post.likeCount ?? 0}</span>
            <span className="inline-flex items-center gap-1"><MessageCircle className="size-3.5" />{post.commentCount ?? 0}</span>
            <span className="inline-flex items-center gap-1"><Share2 className="size-3.5" />{post.shareCount ?? 0}</span>
          </div>
        </div>
      </div>
    </Link>
  );
}
