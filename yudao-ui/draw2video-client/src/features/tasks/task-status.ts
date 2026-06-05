import type { AigcTaskStatus, AigcTaskType } from "./task-types";

export const taskStatusMeta: Record<string, { label: string; className: string; dotClassName: string; polling: boolean }> = {
  CREATED: { label: "已创建", className: "border-slate-200 bg-slate-50 text-slate-600", dotClassName: "bg-slate-500", polling: true },
  PRICE_CALCULATED: { label: "已计价", className: "border-slate-200 bg-slate-50 text-slate-600", dotClassName: "bg-slate-500", polling: true },
  FROZEN: { label: "已冻结积分", className: "border-amber-200 bg-amber-50 text-amber-700", dotClassName: "bg-amber-500", polling: true },
  QUEUED: { label: "排队中", className: "border-amber-200 bg-amber-50 text-amber-700", dotClassName: "bg-amber-500", polling: true },
  RUNNING: { label: "生成中", className: "border-blue-200 bg-blue-50 text-blue-700", dotClassName: "bg-blue-500", polling: true },
  SUBMITTED: { label: "已提交供应商", className: "border-amber-200 bg-amber-50 text-amber-700", dotClassName: "bg-amber-500", polling: true },
  CALLBACK_WAITING: { label: "等待结果", className: "border-amber-200 bg-amber-50 text-amber-700", dotClassName: "bg-amber-500", polling: true },
  DOWNLOADING: { label: "结果处理中", className: "border-blue-200 bg-blue-50 text-blue-700", dotClassName: "bg-blue-500", polling: true },
  ASSET_CREATING: { label: "生成资产中", className: "border-blue-200 bg-blue-50 text-blue-700", dotClassName: "bg-blue-500", polling: true },
  AUDITING: { label: "审核中", className: "border-violet-200 bg-violet-50 text-violet-700", dotClassName: "bg-violet-500", polling: true },
  SUCCESS: { label: "已完成", className: "border-emerald-200 bg-emerald-50 text-emerald-700", dotClassName: "bg-emerald-500", polling: false },
  FAILED: { label: "生成失败", className: "border-red-200 bg-red-50 text-red-700", dotClassName: "bg-red-500", polling: false },
  CANCELLED: { label: "已取消", className: "border-slate-200 bg-slate-50 text-slate-600", dotClassName: "bg-slate-500", polling: false },
  REFUNDING: { label: "退款处理中", className: "border-cyan-200 bg-cyan-50 text-cyan-700", dotClassName: "bg-cyan-500", polling: true },
  REFUNDED: { label: "已退款", className: "border-cyan-200 bg-cyan-50 text-cyan-700", dotClassName: "bg-cyan-500", polling: false },
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
  if (!status) return { label: "未知", className: "border-slate-200 bg-slate-50 text-slate-600", dotClassName: "bg-slate-500", polling: false };
  return taskStatusMeta[status] ?? { label: status, className: "border-slate-200 bg-slate-50 text-slate-600", dotClassName: "bg-slate-500", polling: false };
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
