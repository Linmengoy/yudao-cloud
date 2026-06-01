"use client";

import type { NotificationItem as NotificationItemType } from "../notification-types";
import { NotificationItem } from "./notification-item";

export function NotificationList({
  items,
  onMarkRead,
  markingId,
}: {
  items: NotificationItemType[];
  onMarkRead: (id: number | string) => void;
  markingId?: number | string | null;
}) {
  return (
    <div className="mt-5 grid gap-3">
      {items.map((item) => (
        <NotificationItem key={item.id} item={item} onMarkRead={onMarkRead} marking={markingId === item.id} />
      ))}
    </div>
  );
}
