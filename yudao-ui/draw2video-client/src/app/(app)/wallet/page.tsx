"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ArrowDownLeft, ArrowUpRight, Coins, Loader2, RefreshCw, Snowflake } from "lucide-react";
import {
  formatDateTime,
  formatPoints,
  getAigcWallet,
  getAigcWalletFreezePage,
  getAigcWalletRecordPage,
} from "@/features/wallet/wallet-api";
import type { AigcWallet, AigcWalletFreeze, AigcWalletRecord } from "@/features/wallet/wallet-types";

const recordTypeOptions = [
  { label: "全部", value: "" },
  { label: "充值", value: "RECHARGE" },
  { label: "赠送", value: "GIFT" },
  { label: "冻结", value: "FREEZE" },
  { label: "消费", value: "CONSUME" },
  { label: "释放", value: "RELEASE" },
];

const freezeStatusOptions = [
  { label: "全部", value: "" },
  { label: "冻结中", value: "FROZEN" },
  { label: "已扣费", value: "CONFIRMED" },
  { label: "已释放", value: "RELEASED" },
];

const recordPageSize = 12;
const freezePageSize = 6;

const recordTypeMap: Record<string, string> = {
  RECHARGE: "充值",
  GIFT: "赠送",
  FREEZE: "冻结",
  CONSUME: "消费",
  RELEASE: "释放",
  REFUND: "退款",
  ADJUST_INCREASE: "调增",
  ADJUST_DECREASE: "调减",
  COMPENSATE: "补偿",
  "1": "充值",
  "2": "赠送",
  "3": "冻结",
  "4": "消费",
  "5": "释放",
  "6": "退款",
};

const freezeStatusMap: Record<string, string> = {
  FROZEN: "冻结中",
  CONFIRMED: "已扣费",
  RELEASED: "已释放",
  EXPIRED: "已过期",
  PART_CONFIRMED: "部分扣费",
  PART_RELEASED: "部分释放",
  "1": "冻结中",
  "2": "已扣费",
  "3": "已释放",
  "4": "已过期",
};

function recordTypeText(type: number | string, fallback?: string) {
  return fallback || recordTypeMap[String(type)] || String(type);
}

function freezeStatusText(status: number | string, fallback?: string) {
  return fallback || freezeStatusMap[String(status)] || String(status);
}

function isIncome(record: AigcWalletRecord) {
  return ["RECHARGE", "GIFT", "RELEASE", "REFUND", "ADJUST_INCREASE", "COMPENSATE", "1", "2", "5", "6"].includes(
    String(record.recordType)
  );
}

function getPageCount(total: number, pageSize: number) {
  return Math.max(1, Math.ceil(total / pageSize));
}

export default function WalletPage() {
  const searchParams = useSearchParams();
  const rechargeOrderId = searchParams.get("rechargeOrderId");
  const [wallet, setWallet] = useState<AigcWallet | null>(null);
  const [records, setRecords] = useState<AigcWalletRecord[]>([]);
  const [freezes, setFreezes] = useState<AigcWalletFreeze[]>([]);
  const [recordTotal, setRecordTotal] = useState(0);
  const [freezeTotal, setFreezeTotal] = useState(0);
  const [recordType, setRecordType] = useState("");
  const [freezeStatus, setFreezeStatus] = useState("");
  const [taskNo, setTaskNo] = useState("");
  const [recordPageNo, setRecordPageNo] = useState(1);
  const [freezePageNo, setFreezePageNo] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const recordPageCount = getPageCount(recordTotal, recordPageSize);
  const freezePageCount = getPageCount(freezeTotal, freezePageSize);

  const summary = useMemo(
    () => [
      { label: "可用积分", value: wallet?.balance, icon: Coins, hint: "可立即用于新的生成任务" },
      { label: "冻结积分", value: wallet?.frozenBalance, icon: Snowflake, hint: "生成中的任务暂时占用，失败后自动退回" },
      { label: "累计充值", value: wallet?.totalRecharge, icon: ArrowDownLeft, hint: "充值和支付成功后累计入账" },
      { label: "累计消费", value: wallet?.totalConsume, icon: ArrowUpRight, hint: "任务成功后累计扣费" },
    ],
    [wallet]
  );

  const loadWallet = useCallback(async (options?: {
    nextRecordType?: string;
    nextFreezeStatus?: string;
    nextTaskNo?: string;
    nextRecordPageNo?: number;
    nextFreezePageNo?: number;
  }) => {
    const currentRecordType = options?.nextRecordType ?? recordType;
    const currentFreezeStatus = options?.nextFreezeStatus ?? freezeStatus;
    const currentTaskNo = options?.nextTaskNo ?? taskNo;
    const currentRecordPageNo = options?.nextRecordPageNo ?? recordPageNo;
    const currentFreezePageNo = options?.nextFreezePageNo ?? freezePageNo;
    setLoading(true);
    setError("");
    try {
      const [walletData, recordData, freezeData] = await Promise.all([
        getAigcWallet(),
        getAigcWalletRecordPage({
          pageNo: currentRecordPageNo,
          pageSize: recordPageSize,
          recordType: currentRecordType || undefined,
          taskNo: currentTaskNo || undefined,
        }),
        getAigcWalletFreezePage({
          pageNo: currentFreezePageNo,
          pageSize: freezePageSize,
          status: currentFreezeStatus || undefined,
          taskNo: currentTaskNo || undefined,
        }),
      ]);
      setWallet(walletData);
      setRecords(recordData.list ?? []);
      setFreezes(freezeData.list ?? []);
      setRecordTotal(recordData.total ?? 0);
      setFreezeTotal(freezeData.total ?? 0);
    } catch (err) {
      setError(err instanceof Error ? err.message : "钱包数据加载失败");
    } finally {
      setLoading(false);
    }
  }, [freezePageNo, freezeStatus, recordPageNo, recordType, taskNo]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadWallet(), 0);
    return () => window.clearTimeout(timer);
  }, [loadWallet]);

  function handleRecordTypeChange(value: string) {
    setRecordType(value);
    setRecordPageNo(1);
    loadWallet({ nextRecordType: value, nextRecordPageNo: 1 });
  }

  function handleFreezeStatusChange(value: string) {
    setFreezeStatus(value);
    setFreezePageNo(1);
    loadWallet({ nextFreezeStatus: value, nextFreezePageNo: 1 });
  }

  function handleTaskNoSearch() {
    setRecordPageNo(1);
    setFreezePageNo(1);
    loadWallet({ nextRecordPageNo: 1, nextFreezePageNo: 1 });
  }

  function handleRecordPageChange(nextPageNo: number) {
    const pageNo = Math.min(Math.max(nextPageNo, 1), recordPageCount);
    setRecordPageNo(pageNo);
    loadWallet({ nextRecordPageNo: pageNo });
  }

  function handleFreezePageChange(nextPageNo: number) {
    const pageNo = Math.min(Math.max(nextPageNo, 1), freezePageCount);
    setFreezePageNo(pageNo);
    loadWallet({ nextFreezePageNo: pageNo });
  }

  return (
    <div className="mx-auto flex max-w-[1200px] flex-col gap-8 px-4 py-10">
      <section className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm text-muted-gray">Wallet</p>
          <h1 className="mt-1 text-3xl font-semibold tracking-[-0.9px] text-charcoal">钱包 / 用量</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-gray">
            积分用于 AIGC 生成任务。提交后先冻结，生成成功才扣费；任务失败、取消或超时会释放冻结积分。
          </p>
        </div>
        <button
          type="button"
          onClick={() => loadWallet()}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal transition-colors active:opacity-80 disabled:opacity-50"
        >
          <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
          刷新
        </button>
      </section>

      {error && (
        <div className="rounded-xl border border-border-warm bg-muted px-4 py-3 text-sm text-charcoal">
          {error}
        </div>
      )}

      {rechargeOrderId && !error && (
        <div className="rounded-xl border border-border-warm bg-muted px-4 py-3 text-sm text-charcoal">
          充值订单 {rechargeOrderId} 已提交处理，钱包余额和流水会在支付确认后刷新。
        </div>
      )}

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {summary.map((item) => (
          <div key={item.label} className="rounded-xl border border-border-warm bg-background p-4">
            <div className="flex items-center gap-2 text-sm text-muted-gray">
              <item.icon className="size-4" />
              {item.label}
            </div>
            <p className="mt-3 text-2xl font-semibold tracking-[-0.4px] text-charcoal">{formatPoints(item.value)}</p>
            <p className="mt-2 text-xs leading-5 text-muted-gray">{item.hint}</p>
          </div>
        ))}
      </section>

      <section className="grid gap-8 lg:grid-cols-[1fr_340px]">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-medium text-charcoal">计费流水</h2>
              <p className="mt-1 text-xs text-muted-gray">共 {recordTotal.toLocaleString("zh-CN")} 条积分变动记录</p>
            </div>
            <div className="flex flex-wrap gap-2">
              {recordTypeOptions.map((item) => (
                <button
                  key={item.label}
                  type="button"
                  onClick={() => handleRecordTypeChange(item.value)}
                  className={`rounded-full px-3 py-1.5 text-xs transition-colors active:opacity-80 ${
                    recordType === item.value
                      ? "bg-charcoal text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]"
                      : "border border-border-warm text-muted-gray hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal"
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <input
              value={taskNo}
              onChange={(event) => setTaskNo(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") handleTaskNoSearch();
              }}
              placeholder="按任务编号筛选"
              className="h-9 w-full rounded-md border border-border-warm bg-background px-3 text-sm text-charcoal outline-none placeholder:text-muted-gray focus:border-[rgba(28,28,28,0.45)] sm:w-56"
            />
            <button
              type="button"
              onClick={handleTaskNoSearch}
              disabled={loading}
              className="inline-flex h-9 items-center rounded-md border border-[rgba(28,28,28,0.4)] px-3 text-sm text-charcoal active:opacity-80 disabled:opacity-50"
            >
              筛选
            </button>
          </div>

          <div className="mt-4 overflow-hidden rounded-xl border border-border-warm">
            {loading && !records.length && (
              <div className="flex items-center justify-center bg-background py-16 text-sm text-muted-gray">
                <Loader2 className="mr-2 size-4 animate-spin" />
                加载钱包流水...
              </div>
            )}
            {!loading && !records.length && (
              <div className="bg-background py-16 text-center text-sm text-muted-gray">暂无计费流水</div>
            )}
            {records.map((record, index) => {
              const income = isIncome(record);
              return (
                <div
                  key={record.id}
                  className={`flex items-center justify-between gap-4 bg-background px-4 py-3 ${index > 0 ? "border-t border-border-warm" : ""}`}
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm text-charcoal">{recordTypeText(record.recordType, record.recordTypeName)}</p>
                    <p className="mt-1 truncate text-xs text-muted-gray">
                      {record.taskNo ? `任务 ${record.taskNo} · ` : ""}{formatDateTime(record.createTime)}
                    </p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-sm font-medium text-charcoal">
                      {income ? "+" : "-"}{formatPoints(Math.abs(record.amount))}
                    </p>
                    <p className="mt-1 text-xs text-muted-gray">余额 {formatPoints(record.balanceAfter)}</p>
                  </div>
                </div>
              );
            })}
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-muted-gray">
            <span>第 {recordPageNo} / {recordPageCount} 页</span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handleRecordPageChange(recordPageNo - 1)}
                disabled={loading || recordPageNo <= 1}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal disabled:opacity-40"
              >
                上一页
              </button>
              <button
                type="button"
                onClick={() => handleRecordPageChange(recordPageNo + 1)}
                disabled={loading || recordPageNo >= recordPageCount}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal disabled:opacity-40"
              >
                下一页
              </button>
            </div>
          </div>
        </div>

        <aside>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-base font-medium text-charcoal">冻结记录</h2>
              <p className="mt-1 text-xs text-muted-gray">共 {freezeTotal.toLocaleString("zh-CN")} 条临时占用记录</p>
            </div>
            <select
              value={freezeStatus}
              onChange={(event) => handleFreezeStatusChange(event.target.value)}
              className="h-9 rounded-md border border-border-warm bg-background px-3 text-sm text-charcoal outline-none focus:border-[rgba(28,28,28,0.45)]"
            >
              {freezeStatusOptions.map((item) => (
                <option key={item.label} value={item.value}>{item.label}</option>
              ))}
            </select>
          </div>
          <div className="mt-4 overflow-hidden rounded-xl border border-border-warm">
            {!loading && !freezes.length && (
              <div className="bg-background py-12 text-center text-sm text-muted-gray">暂无冻结记录</div>
            )}
            {freezes.map((freeze, index) => (
              <div key={freeze.id} className={`bg-background p-4 ${index > 0 ? "border-t border-border-warm" : ""}`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-charcoal">{formatPoints(freeze.amount)}</p>
                    <p className="mt-1 truncate text-xs text-muted-gray">{freeze.taskNo ? `任务 ${freeze.taskNo}` : freeze.freezeNo}</p>
                  </div>
                  <span className="rounded-full border border-border-warm px-2 py-1 text-xs text-muted-gray">
                    {freezeStatusText(freeze.status, freeze.statusName)}
                  </span>
                </div>
                <div className="mt-3 grid grid-cols-2 gap-2 text-xs leading-5 text-muted-gray">
                  <span>已扣费 {formatPoints(freeze.confirmedAmount)}</span>
                  <span>已释放 {formatPoints(freeze.releasedAmount)}</span>
                  <span className="col-span-2">过期时间 {formatDateTime(freeze.expireTime)}</span>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-3 flex items-center justify-between text-xs text-muted-gray">
            <span>第 {freezePageNo} / {freezePageCount} 页</span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handleFreezePageChange(freezePageNo - 1)}
                disabled={loading || freezePageNo <= 1}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal disabled:opacity-40"
              >
                上一页
              </button>
              <button
                type="button"
                onClick={() => handleFreezePageChange(freezePageNo + 1)}
                disabled={loading || freezePageNo >= freezePageCount}
                className="rounded-md border border-border-warm px-3 py-1.5 text-charcoal disabled:opacity-40"
              >
                下一页
              </button>
            </div>
          </div>
        </aside>
      </section>
    </div>
  );
}
