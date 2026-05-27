import type { AigcTaskStatus, AigcTaskType } from "./task-types";

export const taskStatusMeta: Record<string, { label: string; className: string; dotClassName: string; polling: boolean }> = {
  CREATED: { label: "已创建", className: "border-border-warm text-muted-gray", dotClassName: "bg-muted-gray", polling: true },
  PRICE_CALCULATED: { label: "已计价", className: "border-border-warm text-muted-gray", dotClassName: "bg-muted-gray", polling: true },
  FROZEN: { label: "已冻结积分", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  QUEUED: { label: "排队中", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  RUNNING: { label: "生成中", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  SUBMITTED: { label: "已提交供应商", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  CALLBACK_WAITING: { label: "等待结果", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  DOWNLOADING: { label: "结果处理中", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  ASSET_CREATING: { label: "生成资产中", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  AUDITING: { label: "审核中", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  SUCCESS: { label: "已完成", className: "border-border-warm text-charcoal", dotClassName: "bg-charcoal", polling: false },
  FAILED: { label: "生成失败", className: "border-border-warm text-destructive", dotClassName: "bg-destructive", polling: false },
  CANCELLED: { label: "已取消", className: "border-border-warm text-muted-gray", dotClassName: "bg-muted-gray", polling: false },
  REFUNDING: { label: "退款处理中", className: "border-[rgba(28,28,28,0.4)] text-charcoal", dotClassName: "bg-charcoal", polling: true },
  REFUNDED: { label: "已退款", className: "border-border-warm text-charcoal", dotClassName: "bg-charcoal", polling: false },
};

export const taskTypeLabels: Record<string, string> = {
  TEXT_GENERATE: "文本生成",
  TEXT_CHAT: "文本对话",
  IMAGE_TEXT_TO_IMAGE: "文生图",
  IMAGE_TO_IMAGE: "图生图",
  VIDEO_TEXT_TO_VIDEO: "文生视频",
  VIDEO_IMAGE_TO_VIDEO: "图生视频",
  AUDIO_TEXT_TO_SPEECH: "文本转语音",
  CODE_GENERATE: "代码生成",
  DOCUMENT_GENERATE: "文档生成",
};

export function getTaskStatusMeta(status?: AigcTaskStatus | string) {
  if (!status) return { label: "未知", className: "border-border-warm text-muted-gray", dotClassName: "bg-muted-gray", polling: false };
  return taskStatusMeta[status] ?? { label: status, className: "border-border-warm text-muted-gray", dotClassName: "bg-muted-gray", polling: false };
}

export function getTaskTypeLabel(type?: AigcTaskType | string) {
  if (!type) return "未知任务";
  return taskTypeLabels[type] ?? type;
}

export function shouldPollTask(status?: AigcTaskStatus | string) {
  return getTaskStatusMeta(status).polling;
}

export function canCancelTask(status?: AigcTaskStatus | string) {
  return ["CREATED", "PRICE_CALCULATED", "FROZEN", "QUEUED"].includes(String(status));
}

export function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatPoints(value?: number | null, currencyType?: string) {
  const unit = currencyType || "积分";
  return `${Number(value ?? 0).toLocaleString("zh-CN")} ${unit}`;
}
