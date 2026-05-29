import { api } from "@/lib/api-client";
import type {
  AigcRechargeOrder,
  AigcRechargePackage,
  AigcWallet,
  AigcWalletFreeze,
  AigcWalletRecord,
  PageResult,
  WalletPageParams,
} from "./wallet-types";

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

export function getAigcWallet() {
  return api.get<AigcWallet>("/aigc/billing/wallet/get");
}

export function getAigcWalletRecordPage(params: WalletPageParams) {
  return api.get<PageResult<AigcWalletRecord>>(`/aigc/billing/wallet/record/page${toQuery(params)}`);
}

export function getAigcWalletFreezePage(params: WalletPageParams) {
  return api.get<PageResult<AigcWalletFreeze>>(`/aigc/billing/wallet/freeze/page${toQuery(params)}`);
}

export function getAigcWalletStatistics() {
  return api.get<Record<string, number>>("/aigc/billing/wallet/statistics");
}

export function createAigcRechargeOrder(data: {
  payAmount: number;
  rechargeType?: number;
  payChannelCode?: string;
}) {
  return api.post<AigcRechargeOrder>("/aigc/billing/recharge/create", data);
}

export function getEnabledAigcRechargePackages() {
  return api.get<AigcRechargePackage[]>("/aigc/billing/recharge-package/list-enabled");
}

export function createAigcRechargeOrderByPackage(packageId: number) {
  return api.post<number>(`/aigc/billing/recharge/create-by-package${toQuery({ packageId })}`);
}

export function getAigcRechargeOrder(id: number) {
  return api.get<AigcRechargeOrder>(`/aigc/billing/recharge/get${toQuery({ id })}`);
}

export function getAigcRechargeOrderPage(params: WalletPageParams) {
  return api.get<PageResult<AigcRechargeOrder>>(`/aigc/billing/recharge/page${toQuery(params)}`);
}

export function syncRechargePayStatus(id: number) {
  return api.post<AigcRechargeOrder>("/aigc/billing/recharge/sync-pay-status", { id });
}

export function formatPoints(value?: number | null) {
  return `${Number(value ?? 0).toLocaleString("zh-CN")} 积分`;
}

export function formatCompactPoints(value?: number | null) {
  const number = Number(value ?? 0);
  if (Math.abs(number) >= 10000) return `${(number / 10000).toFixed(1)}万`;
  return number.toLocaleString("zh-CN");
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
