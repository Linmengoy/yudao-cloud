import { api } from "@/lib/api-client";
import type {
  AppNotifyMessage,
  NotificationItem,
  NotificationPageParams,
  NotificationUnreadCount,
  PageResult,
} from "./notification-types";

function toQuery(params: object) {
  const search = new URLSearchParams();
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

export function getNotificationPage(params: NotificationPageParams) {
  return api.get<PageResult<AppNotifyMessage>>(`/system/notify-message/my-page${toQuery(toApiPageParams(params))}`)
    .then((data) => ({
      ...data,
      list: data.list.map(toNotificationItem),
    }));
}

export function getNotificationUnreadCount() {
  return api.get<number>("/system/notify-message/get-unread-count")
    .then((count) => ({ count } satisfies NotificationUnreadCount));
}

export function getNotification(id: number | string) {
  return api.get<AppNotifyMessage | null>(`/system/notify-message/get${toQuery({ id })}`).then((message) => {
    if (!message) throw new Error("通知不存在或无权访问");
    return toNotificationItem(message);
  });
}

export function markNotificationRead(id: number | string) {
  return markNotificationsRead([id]);
}

export function markAllNotificationsRead() {
  return api.put<boolean>("/system/notify-message/update-all-read");
}

export function markNotificationsRead(ids: Array<number | string>) {
  return api.put<boolean>(`/system/notify-message/update-read${toQuery({ ids: ids.join(",") })}`);
}

function toApiPageParams(params: NotificationPageParams) {
  return {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    readStatus: params.read,
    templateCode: toTemplateCodeQuery(params.type),
  };
}

function toNotificationItem(message: AppNotifyMessage): NotificationItem {
  const params = message.templateParams ?? {};
  return {
    id: message.id,
    title: message.templateNickname || message.templateCode || "系统通知",
    content: message.templateContent || "",
    type: toNotificationType(message.templateType, message.templateCode),
    read: Boolean(message.readStatus),
    important: params.important === true || params.important === "true",
    actionUrl: typeof params.actionUrl === "string" ? params.actionUrl : undefined,
    actionText: typeof params.actionText === "string" ? params.actionText : undefined,
    createdAt: message.createTime,
  };
}

function toNotificationType(templateType?: number, templateCode?: string) {
  const code = templateCode?.toLowerCase() ?? "";
  if (code.includes("task")) return "task";
  if (code.includes("asset")) return "asset";
  if (code.includes("wallet") || code.includes("billing") || code.includes("recharge")) return "wallet";
  if (code.includes("account") || code.includes("auth") || code.includes("login")) return "account";
  if (code.includes("activity") || code.includes("coupon")) return "activity";
  if (templateType === 2) return "task";
  return "system";
}

function toTemplateCodeQuery(type?: string) {
  if (!type || type === "all" || type === "unread") return undefined;
  if (type === "system") return undefined;
  return type;
}
