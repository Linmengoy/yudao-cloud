"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { UserMinus, UserPlus } from "lucide-react";
import { CommunityPostCard } from "@/features/community/CommunityPostCard";
import { followCommunityAuthor, getCommunityAuthor, getCommunityAuthorPosts, unfollowCommunityAuthor } from "@/features/community/community-api";
import type { CommunityAuthor, CommunityPost } from "@/features/community/community-types";
import { useAuth } from "@/features/auth/auth-store";

export default function CreatorProfilePage() {
  const params = useParams<{ authorUserId: string }>();
  const authorUserId = Number(params.authorUserId);
  const { user } = useAuth();
  const [author, setAuthor] = useState<CommunityAuthor | null>(null);
  const [posts, setPosts] = useState<CommunityPost[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    const [authorData, postData] = await Promise.all([
      getCommunityAuthor(authorUserId),
      getCommunityAuthorPosts({ authorUserId, pageNo: 1, pageSize: 40, sort: "latest" }),
    ]);
    setAuthor(authorData);
    setPosts(postData.list);
  };

  useEffect(() => {
    let ignore = false;
    queueMicrotask(() => {
      if (!ignore) void load().finally(() => setLoading(false));
    });
    return () => {
      ignore = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authorUserId]);

  const toggleFollow = async () => {
    if (!author) return;
    if (author.followedByCurrentUser) await unfollowCommunityAuthor(author.authorUserId);
    else await followCommunityAuthor(author.authorUserId);
    await load();
  };

  if (loading) return <div className="flex min-h-full items-center justify-center text-sm text-muted-gray">Loading...</div>;
  if (!author) return <div className="flex min-h-full items-center justify-center text-sm text-muted-gray">Creator not found.</div>;

  const isMine = user?.id === author.authorUserId;

  return (
    <main className="min-h-full bg-background">
      <div className="mx-auto max-w-7xl px-4 py-6">
        <section className="mb-6 flex flex-col gap-4 border-b border-border-warm pb-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex size-16 items-center justify-center rounded-full bg-charcoal text-xl font-medium text-off-white">
              {author.nickname?.[0] || "U"}
            </div>
            <div>
              <h1 className="text-2xl font-semibold text-charcoal">{author.nickname || `User ${author.authorUserId}`}</h1>
              <div className="mt-2 flex flex-wrap gap-4 text-sm text-muted-gray">
                <span>{author.followerCount ?? 0} followers</span>
                <span>{author.publicPostCount ?? 0} works</span>
                <span>{author.likeReceivedCount ?? 0} likes</span>
              </div>
            </div>
          </div>
          {!isMine && (
            <button onClick={toggleFollow} className="inline-flex h-9 w-fit items-center gap-2 rounded-lg border border-border-warm px-3 text-sm text-charcoal hover:bg-muted">
              {author.followedByCurrentUser ? <UserMinus className="size-4" /> : <UserPlus className="size-4" />}
              {author.followedByCurrentUser ? "Following" : "Follow"}
            </button>
          )}
        </section>
        {posts.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border-warm py-20 text-center text-sm text-muted-gray">No public works.</div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {posts.map((post) => <CommunityPostCard key={post.id} post={post} />)}
          </div>
        )}
      </div>
    </main>
  );
}
