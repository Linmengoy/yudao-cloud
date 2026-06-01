"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, ArrowUpRight, Loader2 } from "lucide-react";
import {
  useMarkNotificationRead,
  useNotificationDetail,
} from "@/features/notifications/notification-hooks";
import {
  formatNotificationTime,
  getNotificationTypeLabel,
  isSafeInternalUrl,
} from "@/features/notifications/notification-utils";

export default function NotificationDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { data, isLoading, error } = useNotificationDetail(id, Boolean(id));
  const markRead = useMarkNotificationRead();
  const actionUrl = isSafeInternalUrl(data?.actionUrl) ? data?.actionUrl : undefined;

  useEffect(() => {
    if (!data || data.read || markRead.isPending) return;
    markRead.mutate(data.id);
  }, [data, markRead]);

  return (
    <div className="mx-auto max-w-[860px] px-4 py-8">
      <Link
        href="/notifications"
        className="inline-flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
      >
        <ArrowLeft className="size-4" />
        返回通知中心
      </Link>

      {isLoading && (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载通知详情...
        </div>
      )}

      {error && (
        <div className="mt-6 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-destructive">
          {error instanceof Error ? error.message : "通知详情加载失败"}
        </div>
      )}

      {data && (
        <article className="mt-6 rounded-xl border border-border-warm bg-background px-6 py-6">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-gray">
              {getNotificationTypeLabel(data.type)}
            </span>
            {data.important && <span className="rounded bg-destructive/10 px-1.5 py-0.5 text-xs text-destructive">重要</span>}
            <span className="text-xs text-muted-gray">{formatNotificationTime(data.createdAt)}</span>
          </div>

          <h1 className="mt-4 text-2xl font-semibold tracking-[-0.5px] text-charcoal">{data.title}</h1>
          <div className="mt-5 whitespace-pre-wrap text-sm leading-7 text-charcoal">{data.content}</div>

          {actionUrl && (
            <div className="mt-6 border-t border-border-warm pt-4">
              <Link
                href={actionUrl}
                className="inline-flex items-center gap-2 rounded-md bg-charcoal px-4 py-2.5 text-sm text-off-white active:opacity-80"
              >
                {data.actionText ?? "去查看"}
                <ArrowUpRight className="size-4" />
              </Link>
            </div>
          )}
        </article>
      )}
    </div>
  );
}
