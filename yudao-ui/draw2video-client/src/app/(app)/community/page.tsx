"use client";

import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { getCommunityPosts } from "@/features/community/community-api";
import type { CommunityPost } from "@/features/community/community-types";
import { CommunityPostCard } from "@/features/community/CommunityPostCard";

export default function CommunityPage() {
  const [posts, setPosts] = useState<CommunityPost[]>([]);
  const [total, setTotal] = useState(0);
  const [sort, setSort] = useState<"latest" | "hot">("latest");
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;
    getCommunityPosts({ pageNo: 1, pageSize: 30, sort, keyword })
      .then((data) => {
        if (ignore) return;
        setPosts(data.list);
        setTotal(data.total);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [sort, keyword]);

  return (
    <main className="min-h-full bg-background">
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="mb-5 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-charcoal">Community</h1>
            <p className="mt-1 text-sm text-muted-gray">{total} public works</p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-gray" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Search works"
                className="h-9 w-full rounded-lg border border-border-warm bg-background pl-9 pr-3 text-sm outline-none focus:border-[rgba(28,28,28,0.45)] sm:w-64"
              />
            </div>
            <div className="inline-flex h-9 rounded-lg border border-border-warm p-1">
              {(["latest", "hot"] as const).map((item) => (
                <button
                  key={item}
                  onClick={() => setSort(item)}
                  className={`rounded-md px-3 text-sm ${sort === item ? "bg-charcoal text-off-white" : "text-muted-gray hover:text-charcoal"}`}
                >
                  {item}
                </button>
              ))}
            </div>
          </div>
        </div>

        {loading ? (
          <div className="py-20 text-center text-sm text-muted-gray">Loading...</div>
        ) : posts.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border-warm py-20 text-center text-sm text-muted-gray">No public works yet.</div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {posts.map((post) => <CommunityPostCard key={post.id} post={post} />)}
          </div>
        )}
      </div>
    </main>
  );
}
