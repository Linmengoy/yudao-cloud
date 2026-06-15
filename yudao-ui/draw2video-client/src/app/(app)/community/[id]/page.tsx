"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Heart, MessageCircle, Share2, UserMinus, UserPlus } from "lucide-react";
import {
  createCommunityComment,
  deleteCommunityComment,
  followCommunityAuthor,
  getCommunityComments,
  getCommunityPost,
  likeCommunityPost,
  shareCommunityPost,
  unfollowCommunityAuthor,
  unlikeCommunityPost,
} from "@/features/community/community-api";
import type { CommunityComment, CommunityPost } from "@/features/community/community-types";
import { useAuth } from "@/features/auth/auth-store";

export default function CommunityDetailPage() {
  const params = useParams<{ id: string }>();
  const postKey = params.id;
  const { user } = useAuth();
  const [post, setPost] = useState<CommunityPost | null>(null);
  const [comments, setComments] = useState<CommunityComment[]>([]);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);

  const load = async () => {
    const postData = await getCommunityPost(postKey);
    const commentData = await getCommunityComments({ postId: postData.id, pageNo: 1, pageSize: 50 });
    setPost(postData);
    setComments(commentData.list);
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
  }, [postKey]);

  const toggleLike = async () => {
    if (!post) return;
    if (post.likedByCurrentUser) await unlikeCommunityPost(post.id);
    else await likeCommunityPost(post.id);
    await load();
  };

  const toggleFollow = async () => {
    if (!post) return;
    if (post.followedAuthor) await unfollowCommunityAuthor(post.authorUserId);
    else await followCommunityAuthor(post.authorUserId);
    await load();
  };

  const share = async () => {
    if (!post) return;
    const result = await shareCommunityPost(post.id);
    await navigator.clipboard.writeText(`${window.location.origin}${result.shareUrl}`);
    await load();
  };

  const submitComment = async () => {
    const text = content.trim();
    if (!text || !post) return;
    await createCommunityComment(post.id, text);
    setContent("");
    await load();
  };

  if (loading) return <div className="flex min-h-full items-center justify-center text-sm text-muted-gray">Loading...</div>;
  if (!post) return <div className="flex min-h-full items-center justify-center text-sm text-muted-gray">Work not found.</div>;

  const isMine = user?.id === post.authorUserId;

  return (
    <main className="min-h-full bg-background">
      <div className="mx-auto grid max-w-6xl gap-6 px-4 py-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <section className="overflow-hidden rounded-lg border border-border-warm bg-muted">
          {post.fileUrl || post.coverUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={post.fileUrl || post.coverUrl} alt={post.title} className="max-h-[720px] w-full object-contain" />
          ) : (
            <div className="flex aspect-video items-center justify-center text-sm text-muted-gray">No preview</div>
          )}
        </section>
        <aside className="space-y-5">
          <div>
            <h1 className="text-2xl font-semibold text-charcoal">{post.title}</h1>
            {post.summary && <p className="mt-2 text-sm leading-6 text-muted-gray">{post.summary}</p>}
          </div>
          <div className="flex items-center justify-between rounded-lg border border-border-warm p-3">
            <Link href={`/creators/${post.authorUserId}`} className="min-w-0">
              <p className="truncate text-sm font-medium text-charcoal">{post.authorNickname || `User ${post.authorUserId}`}</p>
              <p className="text-xs text-muted-gray">Creator profile</p>
            </Link>
            {!isMine && (
              <button onClick={toggleFollow} className="inline-flex size-9 items-center justify-center rounded-lg border border-border-warm text-charcoal hover:bg-muted" title={post.followedAuthor ? "Unfollow" : "Follow"}>
                {post.followedAuthor ? <UserMinus className="size-4" /> : <UserPlus className="size-4" />}
              </button>
            )}
          </div>
          <div className="grid grid-cols-3 gap-2">
            <button onClick={toggleLike} className="flex h-10 items-center justify-center gap-2 rounded-lg border border-border-warm text-sm hover:bg-muted">
              <Heart className={`size-4 ${post.likedByCurrentUser ? "fill-current" : ""}`} />{post.likeCount ?? 0}
            </button>
            <button onClick={share} className="flex h-10 items-center justify-center gap-2 rounded-lg border border-border-warm text-sm hover:bg-muted">
              <Share2 className="size-4" />{post.shareCount ?? 0}
            </button>
            <div className="flex h-10 items-center justify-center gap-2 rounded-lg border border-border-warm text-sm">
              <MessageCircle className="size-4" />{post.commentCount ?? 0}
            </div>
          </div>
          {post.tags && <div className="flex flex-wrap gap-2">{post.tags.split(",").map((tag) => <span key={tag} className="rounded-md bg-muted px-2 py-1 text-xs text-muted-gray">{tag.trim()}</span>)}</div>}
          <section className="space-y-3">
            <h2 className="text-sm font-medium text-charcoal">Comments</h2>
            <div className="flex gap-2">
              <input value={content} onChange={(event) => setContent(event.target.value)} placeholder="Add a comment" className="h-9 min-w-0 flex-1 rounded-lg border border-border-warm bg-background px-3 text-sm outline-none focus:border-[rgba(28,28,28,0.45)]" />
              <button onClick={submitComment} className="h-9 rounded-lg bg-charcoal px-3 text-sm text-off-white">Post</button>
            </div>
            <div className="space-y-3">
              {comments.map((comment) => (
                <div key={comment.id} className="rounded-lg border border-border-warm p-3">
                  <div className="mb-1 flex items-center justify-between gap-3 text-xs text-muted-gray">
                    <span>{comment.userNickname || `User ${comment.userId}`}</span>
                    {comment.mine && <button onClick={() => deleteCommunityComment(comment.id).then(load)} className="text-destructive">Delete</button>}
                  </div>
                  <p className="text-sm leading-6 text-charcoal">{comment.content}</p>
                </div>
              ))}
              {comments.length === 0 && <p className="text-sm text-muted-gray">No comments yet.</p>}
            </div>
          </section>
        </aside>
      </div>
    </main>
  );
}
