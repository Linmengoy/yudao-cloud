"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import QRCode from "qrcode";
import { AlertCircle, CheckCircle2, Clock, Loader2, QrCode, RefreshCw } from "lucide-react";
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
import { getPayStatusName, isPayClosed, isPayRefund, isPaySuccess } from "@/features/wallet/pay-order-status";

const PAY_POLL_INTERVAL_MS = 3000;
const PAY_POLL_MAX_DURATION_MS = 5 * 60 * 1000;
const PAY_POLL_ERROR_NOTICE_THRESHOLD = 3;
const PAY_DEFAULT_COUNTDOWN_MS = 5 * 60 * 1000;
const QR_CODE_SIZE = 192;
const QR_CODE_LOGO_SIZE = 44;
const QR_CODE_LOGO_BACKGROUND_SIZE = 56;
const ALIPAY_LOGO_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><rect width="1024" height="1024" rx="224" fill="#1677ff"/><path fill="#fff" d="M773.6 625.7c-57.8-18.9-135.6-46.9-214.4-73.7 27.1-48.3 49.6-101.8 65.3-159.6H406.2v-55.1h267.5v-31.8H514.9v-76.1h-76.2v76.1H280.1v31.8h267.7v55.1H329.4v31.8h198.4c-12.5 40.5-29.4 78.1-50.2 112.3-127.9-40.2-236.6-63.8-266.2-21.5-19.2 27.5-3.8 73.8 41.6 111.8 79.9 66.8 185.7 50.2 266.5-24.5 58.4 27.9 141.2 69.3 253.9 124.5 19.8 9.7 35.7-91.5.2-101.1ZM290.8 597.4c-29.3-23-45.9-51.8-35.6-67.3 17.7-26.5 93.1-10.9 191.3 29.8-51.3 55.1-109.3 73.8-155.7 37.5Z"/></svg>`;
const ALIPAY_LOGO_DATA_URL = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(ALIPAY_LOGO_SVG)}`;

function formatMoney(value?: number | null) {
  return `¥${(Number(value ?? 0) / 100).toFixed(2)}`;
}

function channelLabel(code: string) {
  const labels: Record<string, string> = {
    alipay_pc: "支付宝电脑支付",
    alipay_qr: "支付宝扫码支付",
    wx_pub: "微信支付",
    wx_lite: "微信小程序",
    wallet: "余额支付",
    easypay_cashier: "支付宝支付",
  };
  return labels[code] ?? code;
}

function normalizeDisplayMode(mode?: string) {
  return mode?.replace(/([a-z])([A-Z])/g, "$1_$2").replace(/-/g, "_").toLowerCase() ?? "";
}

function isHttpUrl(value?: string) {
  return !!value && /^https?:\/\//i.test(value);
}

function isValidId(value: number) {
  return Number.isSafeInteger(value) && value > 0;
}

function parseNumberParam(value: string | null) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function parseTime(value?: string | number | null) {
  if (!value) return 0;
  if (typeof value === "number") {
    if (!Number.isFinite(value)) return 0;
    return value < 1_000_000_000_000 ? value * 1000 : value;
  }
  const normalized = value.includes("T") ? value : value.replace(" ", "T");
  const timestamp = new Date(normalized).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatCountdown(totalSeconds: number) {
  const safeSeconds = Math.max(0, totalSeconds);
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function getOrderPayAppId(rechargeOrder: AigcRechargeOrder | null, payOrder: PayOrder | null, fallbackPayAppId: number) {
  return Number(rechargeOrder?.payAppId || payOrder?.appId || fallbackPayAppId || 0);
}

function getRechargeOrderStatusName(rechargeOrder: AigcRechargeOrder | null, payOrder: PayOrder | null) {
  if (rechargeOrder?.statusName) return rechargeOrder.statusName;
  if (rechargeOrder?.status !== undefined && rechargeOrder.status !== null) return getPayStatusName(rechargeOrder.status);
  if (payOrder?.status !== undefined && payOrder.status !== null) return getPayStatusName(payOrder.status);
  return "待支付";
}

function isAlipayChannel(channelCode?: string) {
  return normalizeDisplayMode(channelCode).startsWith("alipay");
}

function loadImage(src: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = src;
  });
}

function drawRoundRect(context: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) {
  context.beginPath();
  context.moveTo(x + radius, y);
  context.arcTo(x + width, y, x + width, y + height, radius);
  context.arcTo(x + width, y + height, x, y + height, radius);
  context.arcTo(x, y + height, x, y, radius);
  context.arcTo(x, y, x + width, y, radius);
  context.closePath();
}

async function createQrCodeDataUrl(content: string, channelCode?: string) {
  const qrCodeDataUrl = await QRCode.toDataURL(content, { width: QR_CODE_SIZE, margin: 1, errorCorrectionLevel: "H" });
  if (!isAlipayChannel(channelCode) && channelCode !== "easypay_cashier") {
    return qrCodeDataUrl;
  }

  const canvas = document.createElement("canvas");
  canvas.width = QR_CODE_SIZE;
  canvas.height = QR_CODE_SIZE;
  const context = canvas.getContext("2d");
  if (!context) return qrCodeDataUrl;

  const [qrCodeImage, alipayLogoImage] = await Promise.all([loadImage(qrCodeDataUrl), loadImage(ALIPAY_LOGO_DATA_URL)]);
  context.drawImage(qrCodeImage, 0, 0, QR_CODE_SIZE, QR_CODE_SIZE);

  const backgroundX = (QR_CODE_SIZE - QR_CODE_LOGO_BACKGROUND_SIZE) / 2;
  const backgroundY = (QR_CODE_SIZE - QR_CODE_LOGO_BACKGROUND_SIZE) / 2;
  drawRoundRect(context, backgroundX, backgroundY, QR_CODE_LOGO_BACKGROUND_SIZE, QR_CODE_LOGO_BACKGROUND_SIZE, 12);
  context.fillStyle = "#fff";
  context.fill();

  const logoX = (QR_CODE_SIZE - QR_CODE_LOGO_SIZE) / 2;
  const logoY = (QR_CODE_SIZE - QR_CODE_LOGO_SIZE) / 2;
  context.drawImage(alipayLogoImage, logoX, logoY, QR_CODE_LOGO_SIZE, QR_CODE_LOGO_SIZE);

  return canvas.toDataURL("image/png");
}

function openPayContent(displayMode?: string, displayContent?: string) {
  if (!displayContent) return;
  const mode = normalizeDisplayMode(displayMode);
  if (["qr_code", "qr_code_url", "qrcode_url"].includes(mode)) {
    return;
  }
  if (["url", "iframe", "app"].includes(mode) || isHttpUrl(displayContent)) {
    window.open(displayContent, "_blank", "noopener,noreferrer");
    return;
  }
  if (mode === "form") {
    const payWindow = window.open("", "_blank", "noopener,noreferrer");
    payWindow?.document.write(displayContent);
    payWindow?.document.close();
  }
}

export default function RechargeCheckoutPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshWallet } = useAuth();
  const rechargeOrderId = Number(searchParams.get("rechargeOrderId") || 0);
  const payOrderId = Number(searchParams.get("payOrderId") || 0);
  const payAppId = Number(searchParams.get("payAppId") || 0);
  const fallbackSummary = useMemo(() => ({
    rechargeNo: searchParams.get("rechargeNo") || undefined,
    payAmount: parseNumberParam(searchParams.get("payAmount")),
    pointAmount: parseNumberParam(searchParams.get("pointAmount")),
    giftAmount: parseNumberParam(searchParams.get("giftAmount")),
    totalPointAmount: parseNumberParam(searchParams.get("totalPointAmount")),
  }), [searchParams]);
  const [rechargeOrder, setRechargeOrder] = useState<AigcRechargeOrder | null>(null);
  const [payOrder, setPayOrder] = useState<PayOrder | null>(null);
  const [channels, setChannels] = useState<string[]>([]);
  const [selectedChannel, setSelectedChannel] = useState("");
  const [submitResult, setSubmitResult] = useState<PayOrderSubmitResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState("");
  const [pollingMessage, setPollingMessage] = useState("");
  const [countdownNow, setCountdownNow] = useState(() => Date.now());
  const [fallbackPayExpireTimestamp, setFallbackPayExpireTimestamp] = useState(0);
  const pollingStartedAtRef = useRef(0);
  const pollingErrorCountRef = useRef(0);
  const countdownRedirectedRef = useRef(false);

  const payExpireTime = submitResult?.displayExpireTime ?? submitResult?.expireTime ?? payOrder?.expireTime;
  const payExpireTimestamp = parseTime(payExpireTime) || fallbackPayExpireTimestamp;
  const payRemainingSeconds = payExpireTimestamp ? Math.max(0, Math.ceil((payExpireTimestamp - countdownNow) / 1000)) : null;

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

  const handleCountdownExpired = useCallback(async () => {
    if (countdownRedirectedRef.current) return;
    countdownRedirectedRef.current = true;
    try {
      const paid = await syncRechargePayStatus(rechargeOrderId);
      if (paid) await refreshWallet();
    } finally {
      router.push(`/wallet?rechargeOrderId=${rechargeOrderId}`);
    }
  }, [rechargeOrderId, refreshWallet, router]);

  const loadCheckout = useCallback(async () => {
    if (!isValidId(rechargeOrderId) || !isValidId(payOrderId)) {
      setError("充值订单参数不完整，请返回价格页重新下单");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const [rechargeData, payData] = await Promise.all([
        getAigcRechargeOrder(rechargeOrderId),
        getPayOrder({ id: payOrderId, sync: true }),
      ]);
      if (!rechargeData) throw new Error("充值订单不存在，请返回价格页重新下单");
      if (!payData) throw new Error("支付订单不存在，请返回价格页重新下单");
      if (Number(rechargeData.payOrderId || 0) !== payOrderId) {
        throw new Error("充值订单与支付订单不匹配，请返回价格页重新下单");
      }
      if (Number(payData.price ?? 0) !== Number(rechargeData.payAmount ?? 0)) {
        throw new Error("充值订单金额与支付订单金额不一致，请返回价格页重新下单");
      }
      const effectivePayAppId = getOrderPayAppId(rechargeData, payData, payAppId);
      if (!isValidId(effectivePayAppId)) {
        throw new Error("支付应用参数缺失，请联系管理员检查 AIGC 支付应用配置");
      }
      const channelData = await getEnablePayChannelCodeList(effectivePayAppId);
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
    if (!submitResult || !payExpireTimestamp || isPaySuccess(payOrder?.status) || isPayClosed(payOrder?.status) || isPayRefund(payOrder?.status)) return;
    const timer = window.setInterval(() => setCountdownNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [payExpireTimestamp, payOrder?.status, submitResult]);

  useEffect(() => {
    if (!submitResult || payRemainingSeconds !== 0 || isPaySuccess(payOrder?.status)) return;
    handleCountdownExpired();
  }, [handleCountdownExpired, payOrder?.status, payRemainingSeconds, submitResult]);

  useEffect(() => {
    if (!submitResult || isPaySuccess(payOrder?.status)) return;
    pollingStartedAtRef.current = Date.now();
    pollingErrorCountRef.current = 0;
    let stopped = false;
    const timer = window.setInterval(async () => {
      if (document.visibilityState === "hidden") return;
      if (Date.now() - pollingStartedAtRef.current >= PAY_POLL_MAX_DURATION_MS) {
        window.clearInterval(timer);
        stopped = true;
        setPollingMessage("支付结果确认中，可稍后在钱包页查看，或点击“我已完成支付”手动刷新");
        return;
      }
      try {
        const latest = await getPayOrder({ id: payOrderId, sync: true });
        pollingErrorCountRef.current = 0;
        setPayOrder(latest);
        if (isPaySuccess(latest?.status)) {
          window.clearInterval(timer);
          stopped = true;
          setPollingMessage("支付已成功，正在同步入账...");
          await handleSyncPaid(false);
        } else if (isPayClosed(latest?.status) || isPayRefund(latest?.status)) {
          window.clearInterval(timer);
          stopped = true;
          setPollingMessage("");
          setError(`支付订单${getPayStatusName(latest?.status)}，请返回重新下单或联系客服处理`);
        } else {
          setPollingMessage("正在等待支付结果，请完成扫码支付后保持页面打开");
        }
      } catch {
        pollingErrorCountRef.current += 1;
        if (pollingErrorCountRef.current >= PAY_POLL_ERROR_NOTICE_THRESHOLD) {
          setPollingMessage("网络波动，正在继续尝试同步支付结果");
        }
      }
    }, PAY_POLL_INTERVAL_MS);
    return () => {
      if (!stopped) window.clearInterval(timer);
    };
  }, [handleSyncPaid, payOrder?.status, payOrderId, submitResult]);

  useEffect(() => {
    const mode = normalizeDisplayMode(submitResult?.displayMode);
    const content = submitResult?.displayContent;
    let cancelled = false;
    if (mode !== "qr_code" || !content) {
      Promise.resolve().then(() => {
        if (!cancelled) setQrCodeDataUrl("");
      });
      return () => {
        cancelled = true;
      };
    }
    createQrCodeDataUrl(content, selectedChannel)
      .then((dataUrl) => {
        if (!cancelled) setQrCodeDataUrl(dataUrl);
      })
      .catch(() => {
        if (!cancelled) setQrCodeDataUrl("");
      });
    return () => {
      cancelled = true;
    };
  }, [selectedChannel, submitResult?.displayContent, submitResult?.displayMode]);

  async function handleSubmitPay() {
    if (!selectedChannel) {
      setError("请选择支付方式");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const result = await submitPayOrder({ id: payOrderId, channelCode: selectedChannel, returnUrl });
      countdownRedirectedRef.current = false;
      setCountdownNow(Date.now());
      setFallbackPayExpireTimestamp(Date.now() + PAY_DEFAULT_COUNTDOWN_MS);
      setSubmitResult(result);
      setPollingMessage("正在等待支付结果，请完成扫码支付后保持页面打开");
      openPayContent(result.displayMode, result.displayContent);
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
                {payRemainingSeconds !== null && (
                  <div className="mt-3 flex items-center justify-center gap-2 rounded-lg bg-background px-3 py-2 text-sm text-charcoal">
                    <Clock className="size-4" />
                    <span>支付倒计时</span>
                    <span className="font-semibold tabular-nums">{formatCountdown(payRemainingSeconds)}</span>
                  </div>
                )}
                {["qr_code_url", "qrcode_url"].includes(normalizeDisplayMode(submitResult.displayMode)) ? (
                  <div className="mt-3 rounded-lg bg-background p-3 text-center">
                    <img src={submitResult.displayContent} alt="支付二维码" className="mx-auto size-48 rounded-lg bg-white object-contain p-2" />
                    <p className="mt-2 break-all text-xs text-muted-gray">请使用对应支付 App 扫码完成支付</p>
                  </div>
                ) : normalizeDisplayMode(submitResult.displayMode) === "qr_code" ? (
                  <div className="mt-3 rounded-lg bg-background p-3 text-center text-xs text-muted-gray">
                    { (
                      <img src={qrCodeDataUrl} alt="支付二维码" className="mx-auto size-48 rounded-lg bg-white object-contain p-2" />
                    ) }
                    <p className="mt-2 break-all text-xs text-muted-gray">请使用对应支付 App 扫码完成支付</p>
                  </div>
                ) : normalizeDisplayMode(submitResult.displayMode) === "form" ? (
                  <div className="mt-3 break-all rounded-lg bg-background p-3 text-xs text-muted-gray">已打开第三方支付页面，请在新页面完成支付。</div>
                ) : (
                  <p className="mt-3 text-xs text-muted-gray">请在支付页面完成支付，完成后本页面会自动同步状态。</p>
                )}
                {pollingMessage && (
                  <div className="mt-3 flex items-center gap-2 rounded-lg bg-background px-3 py-2 text-xs text-muted-gray">
                    <RefreshCw className="size-3 animate-spin" />
                    {pollingMessage}
                  </div>
                )}
              </div>
            )}

            <div className="mt-5 flex flex-wrap gap-3">
              {!submitResult && (
                <button
                  type="button"
                  onClick={handleSubmitPay}
                  disabled={submitting || !selectedChannel || !channels.length}
                  className="inline-flex items-center justify-center rounded-md bg-charcoal px-4 py-2.5 text-sm text-off-white active:opacity-80 disabled:opacity-50"
                >
                  {submitting ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
                  提交支付
                </button>
              )}
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
              <div className="flex justify-between gap-4"><span className="text-muted-gray">支付金额</span><span className="text-charcoal">{formatMoney(rechargeOrder?.payAmount ?? fallbackSummary.payAmount)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">充值积分</span><span className="text-charcoal">{formatPoints(rechargeOrder?.pointAmount ?? fallbackSummary.pointAmount)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">赠送积分</span><span className="text-charcoal">{formatPoints(rechargeOrder?.giftAmount ?? fallbackSummary.giftAmount)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">到账积分</span><span className="text-charcoal">{formatPoints(rechargeOrder?.totalPointAmount ?? fallbackSummary.totalPointAmount)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">充值单号</span><span className="break-all text-right text-charcoal">{rechargeOrder?.rechargeNo ?? fallbackSummary.rechargeNo ?? "-"}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">支付单号</span><span className="break-all text-right text-charcoal">{rechargeOrder?.payOrderNo ?? payOrder?.no ?? "-"}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">订单状态</span><span className="text-right text-charcoal">{getRechargeOrderStatusName(rechargeOrder, payOrder)}</span></div>
              <div className="flex justify-between gap-4"><span className="text-muted-gray">创建时间</span><span className="text-right text-charcoal">{formatDateTime(rechargeOrder?.createTime)}</span></div>
            </div>
            {isPaySuccess(payOrder?.status) && (
              <div className="mt-5 flex items-center gap-2 rounded-lg border border-border-warm bg-muted px-3 py-2 text-sm text-charcoal">
                <CheckCircle2 className="size-4" />
                支付已成功，正在同步入账
              </div>
            )}
            {(isPayClosed(payOrder?.status) || isPayRefund(payOrder?.status)) && (
              <div className="mt-5 flex items-center gap-2 rounded-lg border border-border-warm bg-muted px-3 py-2 text-sm text-charcoal">
                <AlertCircle className="size-4" />
                支付订单{getPayStatusName(payOrder?.status)}
              </div>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}
