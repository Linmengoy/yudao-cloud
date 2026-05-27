import type { GenerateStatus } from "./generation-types";

const pollingStatuses = new Set(["CREATED", "SUBMITTED", "RUNNING"]);
const terminalStatuses = new Set(["SUCCESS", "FAILED", "CANCELED", "CANCELLED"]);

export function shouldPollGeneration(status?: GenerateStatus | string) {
  return Boolean(status && pollingStatuses.has(status));
}

export function isGenerationTerminal(status?: GenerateStatus | string) {
  return Boolean(status && terminalStatuses.has(status));
}

export function getGenerationStatusLabel(status?: GenerateStatus | string) {
  switch (status) {
    case "CREATED":
      return "已创建";
    case "SUBMITTED":
      return "排队中";
    case "RUNNING":
      return "生成中";
    case "SUCCESS":
      return "已完成";
    case "FAILED":
      return "生成失败";
    case "CANCELED":
    case "CANCELLED":
      return "已取消";
    default:
      return status || "未知";
  }
}
