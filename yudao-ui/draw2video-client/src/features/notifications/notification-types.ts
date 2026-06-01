export type NotificationType =
  | "system"
  | "task"
  | "asset"
  | "wallet"
  | "account"
  | "activity";

export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface NotificationItem {
  id: number | string;
  title: string;
  content: string;
  type: NotificationType | string;
  read: boolean;
  important?: boolean;
  actionUrl?: string;
  actionText?: string;
  createdAt?: string;
}

export interface NotificationPageParams {
  pageNo: number;
  pageSize: number;
  type?: NotificationType | string;
  read?: boolean;
}

export interface NotificationUnreadCount {
  count: number;
}

export interface AppNotifyMessage {
  id: number | string;
  templateId?: number | string;
  templateCode?: string;
  templateNickname?: string;
  templateContent?: string;
  templateType?: number;
  templateParams?: Record<string, unknown>;
  readStatus?: boolean;
  readTime?: string;
  createTime?: string;
}
