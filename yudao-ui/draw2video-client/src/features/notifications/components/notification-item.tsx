"use client";

import Link from "next/link";
import { ArrowUpRight, Check, Circle, Eye } from "lucide-react";
import type { NotificationItem as NotificationItemType } from "../notification-types";
import { formatNotificationTime, getNotificationTypeLabel, isSafeInternalUrl } from "../notification-utils";
import { cn } from "@/lib/utils";

export function NotificationItem({
  item,
  onMarkRead,
  marking,
}: {
  item: NotificationItemType;
  onMarkRead: (id: number | string) => void;
  marking?: boolean;
}) {
  const actionUrl = isSafeInternalUrl(item.actionUrl) ? item.actionUrl : undefined;

  return (
    <article
      className={cn(
        "rounded-lg border border-border-warm bg-background px-4 py-4 transition-colors",
        !item.read && "border-[rgba(28,28,28,0.24)] bg-muted"
      )}
    >
      <div className="flex items-start gap-3">
        <div className="mt-1 flex size-8 shrink-0 items-center justify-center rounded-lg border border-border-warm text-muted-gray">
          {item.read ? <Check className="size-4" /> : <Circle className="size-3 fill-current" />}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-gray">
              {getNotificationTypeLabel(item.type)}
            </span>
            {item.important && <span className="rounded bg-destructive/10 px-1.5 py-0.5 text-xs text-destructive">重要</span>}
            <span className="text-xs text-muted-gray">{formatNotificationTime(item.createdAt)}</span>
          </div>
          <h2 className="mt-2 text-sm font-medium text-charcoal">{item.title}</h2>
          <p className="mt-1 line-clamp-2 text-sm leading-6 text-muted-gray">{item.content}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            {!item.read && (
              <button
                type="button"
                onClick={() => onMarkRead(item.id)}
                disabled={marking}
                className="rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-1.5 text-xs text-charcoal transition-colors hover:bg-muted active:opacity-80 disabled:opacity-50"
              >
                标为已读
              </button>
            )}
            {actionUrl && (
              <Link
                href={actionUrl}
                onClick={() => {
                  if (!item.read) onMarkRead(item.id);
                }}
                className="inline-flex items-center gap-1 rounded-md px-3 py-1.5 text-xs text-charcoal transition-colors hover:bg-muted active:opacity-80"
              >
                {item.actionText ?? "去查看"}
                <ArrowUpRight className="size-3" />
              </Link>
            )}
            <Link
              href={`/notifications/${item.id}`}
              className="inline-flex items-center gap-1 rounded-md px-3 py-1.5 text-xs text-charcoal transition-colors hover:bg-muted active:opacity-80"
            >
              查看详情
              <Eye className="size-3" />
            </Link>
          </div>
        </div>
      </div>
    </article>
  );
}
