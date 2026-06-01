"use client";

import Link from "next/link";
import { Bell } from "lucide-react";
import { useNotificationUnreadCount } from "../notification-hooks";
import { cn } from "@/lib/utils";

export function NotificationBell({ className }: { className?: string }) {
  const { data } = useNotificationUnreadCount(true);
  const count = data?.count ?? 0;
  const label = count > 99 ? "99+" : String(count);

  return (
    <Link
      href="/notifications"
      title="通知中心"
      aria-label={count > 0 ? `通知中心，${label} 条未读` : "通知中心"}
      className={cn(
        "relative flex size-10 items-center justify-center rounded-lg text-muted-gray transition-colors hover:bg-muted hover:text-charcoal",
        className
      )}
    >
      <Bell className="size-5" />
      {count > 0 && (
        <span className="absolute -right-1 -top-1 min-w-4 rounded-full bg-destructive px-1 text-center text-[10px] font-medium leading-4 text-white">
          {label}
        </span>
      )}
    </Link>
  );
}
