"use client";

import type { NotificationType } from "../notification-types";
import { cn } from "@/lib/utils";

export type NotificationFilterValue = "all" | "unread" | NotificationType;

const filters: Array<{ value: NotificationFilterValue; label: string }> = [
  { value: "all", label: "全部" },
  { value: "unread", label: "未读" },
  { value: "system", label: "系统" },
  { value: "task", label: "任务" },
  { value: "asset", label: "资产" },
  { value: "wallet", label: "钱包" },
  { value: "account", label: "账号" },
  { value: "activity", label: "活动" },
];

export function NotificationFilterTabs({
  value,
  onChange,
}: {
  value: NotificationFilterValue;
  onChange: (value: NotificationFilterValue) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {filters.map((filter) => (
        <button
          key={filter.value}
          type="button"
          onClick={() => onChange(filter.value)}
          className={cn(
            "rounded-md border px-3 py-1.5 text-sm transition-colors active:opacity-80",
            value === filter.value
              ? "border-charcoal bg-charcoal text-off-white"
              : "border-border-warm text-muted-gray hover:bg-muted hover:text-charcoal"
          )}
        >
          {filter.label}
        </button>
      ))}
    </div>
  );
}
