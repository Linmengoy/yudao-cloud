import type { GenerateStatus } from "./generation-types";

const terminalStatuses = new Set(["SUCCESS", "FAILED", "CANCELED", "CANCELLED", "REFUNDED"]);

export function shouldPollGeneration(status?: GenerateStatus | string) {
  return Boolean(status && !terminalStatuses.has(status));
}

export function isGenerationTerminal(status?: GenerateStatus | string) {
  return Boolean(status && terminalStatuses.has(status));
}

export function getGenerationStatusLabel(status?: GenerateStatus | string) {
  switch (status) {
    case "CREATED":
      return "已创建";
    case "SUBMITTING":
      return "提交中";
    case "SUBMITTED":
      return "已提交";
    case "RUNNING":
      return "生成中";
    case "CALLBACK_WAITING":
      return "等待回调";
    case "SYNCING":
      return "同步中";
    case "DOWNLOADING":
      return "下载中";
    case "ASSET_CREATING":
      return "资产创建中";
    case "SUCCESS":
      return "已完成";
    case "FAILED":
      return "生成失败";
    case "CANCELED":
    case "CANCELLED":
      return "已取消";
    case "REFUNDED":
      return "已退款";
    default:
      return status || "未知";
  }
}
