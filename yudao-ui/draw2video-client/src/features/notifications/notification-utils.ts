import type { NotificationType } from "./notification-types";

export const notificationTypeLabels: Record<NotificationType, string> = {
  system: "系统",
  task: "任务",
  asset: "资产",
  wallet: "钱包",
  account: "账号",
  activity: "活动",
};

export function getNotificationTypeLabel(type?: string) {
  if (!type) return "系统";
  return notificationTypeLabels[type as NotificationType] ?? "系统";
}

export function formatNotificationTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

export function isSafeInternalUrl(url?: string) {
  if (!url) return false;
  return url.startsWith("/") && !url.startsWith("//") && !url.includes("://");
}
