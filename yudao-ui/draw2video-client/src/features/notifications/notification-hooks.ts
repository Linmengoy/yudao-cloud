"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getNotification,
  getNotificationPage,
  getNotificationUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
  markNotificationsRead,
} from "./notification-api";
import type { NotificationItem, NotificationPageParams, PageResult } from "./notification-types";

export const notificationKeys = {
  all: ["notifications"] as const,
  unreadCount: () => [...notificationKeys.all, "unread-count"] as const,
  page: (params: NotificationPageParams) => [...notificationKeys.all, "page", params] as const,
  detail: (id: number | string) => [...notificationKeys.all, "detail", id] as const,
};

export function useNotificationUnreadCount(enabled = true) {
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: getNotificationUnreadCount,
    enabled,
    refetchInterval: 45_000,
    refetchOnWindowFocus: true,
    retry: 1,
  });
}

export function useNotificationPage(params: NotificationPageParams, enabled = true) {
  return useQuery({
    queryKey: notificationKeys.page(params),
    queryFn: () => getNotificationPage(params),
    enabled,
    retry: 1,
  });
}

export function useNotificationDetail(id: number | string, enabled = true) {
  return useQuery({
    queryKey: notificationKeys.detail(id),
    queryFn: () => getNotification(id),
    enabled,
    retry: 1,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markNotificationRead,
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.all });
      queryClient.setQueriesData<PageResult<NotificationItem>>(
        { queryKey: notificationKeys.all },
        (current: PageResult<NotificationItem> | undefined) => {
          if (!current?.list) return current;
          return {
            ...current,
            list: current.list.map((item) => item.id === id ? { ...item, read: true } : item),
          };
        }
      );
      queryClient.setQueryData<NotificationItem>(notificationKeys.detail(id), (current: NotificationItem | undefined) => {
        if (!current) return current;
        return { ...current, read: true };
      });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useMarkNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
