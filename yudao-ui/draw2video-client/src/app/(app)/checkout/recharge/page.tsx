"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2, Loader2, QrCode, RefreshCw } from "lucide-react";
import {
  formatDateTime,
  formatPoints,
  getAigcRechargeOrder,
  getEnablePayChannelCodeList,
  getPayOrder,
  submitPayOrder,
  syncRechargePayStatus,
} from "@/features/wallet/wallet-api";
import type { AigcRechargeOrder, PayOrder, PayOrderSubmitResult } from "@/features/wallet/wallet-types";
import { useAuth } from "@/features/auth/auth-store";

function formatMoney(value?: number | null) {
  return `¥${(Number(value ?? 0) / 100).toFixed(2)}`;
}

function isPaySuccess(status?: number) {
  return status === 10;
}

function channelLabel(code: string) {
  const labels: Record<string, string> = {
    alipay_pc: "支付宝电脑支付",
    alipay_qr: "支付宝扫码支付",
    wx_pub: "微信支付",
    wx_lite: "微信小程序",
    wallet: "余额支付",
  };
  return labels[code] ?? code;
}

export default function RechargeCheckoutPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshWallet } = useAuth();
  const rechargeOrderId = Number(searchParams.get("rechargeOrderId") || 0);
  const payOrderId = Number(searchParams.get("payOrderId") || 0);
  const payAppId = Number(searchParams.get("payAppId") || 0);
  const [rechargeOrder, setRechargeOrder] = useState<AigcRechargeOrder | null>(null);
  const [payOrder, setPayOrder] = useState<PayOrder | null>(null);
  const [channels, setChannels] = useState<string[]>([]);
  const [selectedChannel, setSelectedChannel] = useState("");
  const [submitResult, setSubmitResult] = useState<PayOrderSubmitResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");

  const returnUrl = useMemo(() => {
    if (typeof window === "undefined") return undefined;
    return `${window.location.origin}/wallet?rechargeOrderId=${rechargeOrderId}`;
  }, [rechargeOrderId]);

  const handleSyncPaid = useCallback(async (showLoading = true) => {
    if (!rechargeOrderId) return false;
    if (showLoading) setSyncing(true);
    try {
      const paid = await syncRechargePayStatus(rechargeOrderId);
      if (paid) {
        await refreshWallet();
        router.push(`/wallet?rechargeOrderId=${rechargeOrderId}`);
      }
      return paid;
    } catch (err) {
      setError(err instanceof Error ? err.message : "充值状态同步失败");
      return false;
    } finally {
      if (showLoading) setSyncing(false);
    }
  }, [rechargeOrderId, refreshWallet, router]);

  const loadCheckout = useCallback(async () => {
    if (!rechargeOrderId || !payOrderId || !payAppId) {
      setError("充值订单参数不完整，请返回价格页重新下单");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const [rechargeData, payData, channelData] = await Promise.all([
        getAigcRechargeOrder(rechargeOrderId),
        getPayOrder({ id: payOrderId, sync: true }),
        getEnablePayChannelCodeList(payAppId),
      ]);
      setRechargeOrder(rechargeData);
      setPayOrder(payData);
      setChannels(channelData ?? []);
      setSelectedChannel((current) => current || channelData?.[0] || "");
      if (isPaySuccess(payData?.status)) await handleSyncPaid(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "收银台加载失败");
    } finally {
      setLoading(false);
    }
  }, [handleSyncPaid, payAppId, payOrderId, rechargeOrderId]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadCheckout(), 0);
    return () => window.clearTimeout(timer);
  }, [loadCheckout]);

  useEffect(() => {
    if (!submitResult || isPaySuccess(payOrder?.status)) return;
    const timer = window.setInterval(async () => {
      try {
        const latest = await getPayOrder({ id: payOrderId, sync: true });
        setPayOrder(latest);
        if (isPaySuccess(latest?.status)) {
          window.clearInterval(timer);
          await handleSyncPaid(false);
        }
      } catch {
        window.clearInterval(timer);
      }
    }, 3000);
    return () => window.clearInterval(timer);
  }, [handleSyncPaid, payOrder?.status, payOrderId, submitResult]);

  async function handleSubmitPay() {
    if (!selectedChannel) {
      setError("请选择支付方式");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const result = await submitPayOrder({ id: payOrderId, channelCode: selectedChannel, returnUrl });
      setSubmitResult(result);
      if (result.displayMode === "url" && result.displayContent) {
        window.open(result.displayContent, "_blank", "noopener,noreferrer");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "支付提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-[920px] flex-col gap-6 px-4 py-10">
      <section>
        <p className="text-sm text-muted-gray">Checkout</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-[-0.9px] text-charcoal">充值收银台</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-gray">
          确认充值订单后选择支付方式。支付成功后积分会自动入账到你的 AIGC 钱包。
        </p>
      </section>

      {error && <div className="rounded-xl border border-border-warm bg-muted px-4 py-3 text-sm text-charcoal">{error}</div>}

      {loading ? (
        <div className="flex items-center justify-center rounded-xl border border-border-warm bg-background py-16 text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          正在加载收银台...
        </div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
          <section className="rounded-xl border border-border-warm bg-background p-5">
            <h2 className="text-base font-medium text-charcoal">支付方式</h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {channels.map((channel) => (
                <button
                  key={channel}
                  type="button"
                  onClick={() => setSelectedChannel(channel)}
                  className={`rounded-lg border px-4 py-3 text-left text-sm transition-colors active:opacity-80 ${
                    selectedChannel === channel ? "border-charcoal bg-muted text-charcoal" : "border-border-warm text-muted-gray"
                  }`}
                >
                  {channelLabel(channel)}
                </button>
              ))}
            </div>
            {!channels.length && <p className="mt-4 text-sm text-muted-gray">暂无可用支付方式，请稍后再试。</p>}

            {submitResult && (
              <div className="mt-5 rounded-xl border border-border-warm bg-muted p-4 text-sm text-charcoal">
                <div className="flex items-center gap-2 font-medium">
                  <QrCode className="size-4" />
                  支付信息已生成
                </div>
                {submitResult.displayMode === "qr_code" || submitResult.displayMode === "qr_code_url" ? (
                  <div className="mt-3 break-all rounded-lg bg-background p-3 text-xs text-muted-gray">{submitResult.displayContent}</div>
                ) : submitResult.displayMode === "form" ? (
                  <div className="mt-3 break-all rounded-lg bg-background p-3 text-xs text-muted-gray">请在新打开的支付页面完成支付。</div>
                ) : (
                  <p className="mt-3 text-xs text-muted-gray">请在支付页面完成支付，完成后本页面会自动同步状态。</p>
                )}
              </div>
            )}

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={handleSubmitPay}
                disabled={submitting || !selectedChannel || !channels.length}
                className="inline-flex items-center justify-center rounded-md bg-charcoal px-4 py-2.5 text-sm text-off-white active:opacity-80 disabled:opacity-50"
              >
                {submitting ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
                提交支付
              </button>
              <button
                type="button"
                onClick={() => handleSyncPaid()}
                disabled={syncing}
                className="inline-flex items-center justify-center rounded-md border border-[rgba(28,28,28,0.4)] px-4 py-2.5 text-sm text-charcoal active:opacity-80 disabled:opacity-50"
              >
                <RefreshCw className={`mr-2 size-4 ${syncing ? "animate-spin" : ""}`} />
                我已完成支付
              </button>
            </div>
          </section>

          <aside className="rounded-xl border border-border-warm bg-background p-5">
            <h2 className="text-base font-medium text-charcoal">订单摘要</h2>
            <div className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between gap-4"><span className="text-muted-gray">支付金额</span><span className="text-charcoal">{formatMoney(rechargeOrder?.payAmount)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">到账积分</span><span className="text-charcoal">{formatPoints(rechargeOrder?.totalPointAmount)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">充值单号</span><span className="break-all text-right text-charcoal">{rechargeOrder?.rechargeNo}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">创建时间</span><span className="text-right text-charcoal">{formatDateTime(rechargeOrder?.createTime)}</span></div>
            </div>
            {isPaySuccess(payOrder?.status) && (
              <div className="mt-5 flex items-center gap-2 rounded-lg border border-border-warm bg-muted px-3 py-2 text-sm text-charcoal">
                <CheckCircle2 className="size-4" />
                支付已成功，正在同步入账
              </div>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}
