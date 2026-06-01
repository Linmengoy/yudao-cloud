"use client";

import { useMemo, useState } from "react";
import { Loader2, RefreshCw } from "lucide-react";
import { NotificationEmpty } from "@/features/notifications/components/notification-empty";
import {
  NotificationFilterTabs,
  type NotificationFilterValue,
} from "@/features/notifications/components/notification-filter-tabs";
import { NotificationList } from "@/features/notifications/components/notification-list";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotificationPage,
} from "@/features/notifications/notification-hooks";
import type { NotificationItem, NotificationPageParams } from "@/features/notifications/notification-types";

const pageSize = 20;

export default function NotificationsPage() {
  const [filter, setFilter] = useState<NotificationFilterValue>("all");
  const [pageNo, setPageNo] = useState(1);
  const [markingId, setMarkingId] = useState<number | string | null>(null);

  const params = useMemo<NotificationPageParams>(() => {
    return {
      pageNo,
      pageSize,
      type: filter !== "all" && filter !== "unread" ? filter : undefined,
      read: filter === "unread" ? false : undefined,
    };
  }, [filter, pageNo]);

  const { data, isLoading, isFetching, error, refetch } = useNotificationPage(params);
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();
  const items: NotificationItem[] = data?.list ?? [];
  const total = data?.total ?? 0;
  const hasNext = pageNo * pageSize < total;

  function handleFilterChange(next: NotificationFilterValue) {
    setFilter(next);
    setPageNo(1);
  }

  async function handleMarkRead(id: number | string) {
    setMarkingId(id);
    try {
      await markRead.mutateAsync(id);
    } finally {
      setMarkingId(null);
    }
  }

  return (
    <div className="mx-auto max-w-[960px] px-4 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold tracking-[-0.3px] text-charcoal">通知中心</h1>
          <p className="mt-1 text-sm text-muted-gray">查看系统通知、任务进展、资产和钱包消息。</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="刷新通知列表"
            title="刷新通知列表"
            className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
          >
            <RefreshCw className={`size-4 ${isFetching ? "animate-spin" : ""}`} />
            刷新
          </button>
          <button
            type="button"
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending || !items.some((item) => !item.read)}
            className="inline-flex items-center rounded-md bg-charcoal px-3 py-2 text-sm text-off-white active:opacity-80 disabled:opacity-50"
          >
            全部已读
          </button>
        </div>
      </div>

      <div className="mt-6">
        <NotificationFilterTabs value={filter} onChange={handleFilterChange} />
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-destructive">
          {error instanceof Error ? error.message : "通知列表加载失败"}
        </div>
      )}

      {isLoading && (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载通知列表...
        </div>
      )}

      {!isLoading && items.length === 0 ? (
        <NotificationEmpty title={filter === "unread" ? "暂无未读通知" : "暂无通知"} />
      ) : items.length > 0 ? (
        <>
          <NotificationList items={items} onMarkRead={handleMarkRead} markingId={markingId} />
          <div className="mt-6 flex items-center justify-between text-sm text-muted-gray">
            <span>共 {total} 条通知</span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setPageNo((current) => Math.max(1, current - 1))}
                disabled={pageNo <= 1 || isFetching}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
              >
                上一页
              </button>
              <span>第 {pageNo} 页</span>
              <button
                type="button"
                onClick={() => setPageNo((current) => current + 1)}
                disabled={!hasNext || isFetching}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
              >
                下一页
              </button>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
