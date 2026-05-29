"use client";

import { useEffect, useState } from "react";
import { createAigcRechargeOrderByPackage, getEnabledAigcRechargePackages } from "@/features/wallet/wallet-api";
import type { AigcRechargePackage } from "@/features/wallet/wallet-types";

function formatMoney(value: number) {
  return `¥${(Number(value || 0) / 100).toFixed(2)}`;
}

function formatPoints(value: number) {
  return `${Number(value || 0).toLocaleString("zh-CN")} 积分`;
}

function parseFeatures(value?: string) {
  return (value ?? "")
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export default function PricingPage() {
  const [packages, setPackages] = useState<AigcRechargePackage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creatingPackageId, setCreatingPackageId] = useState<number | null>(null);

  useEffect(() => {
    let mounted = true;
    setError("");
    getEnabledAigcRechargePackages()
      .then((list) => {
        if (mounted) setPackages(list);
      })
      .catch((err) => {
        if (mounted) setError(err instanceof Error ? err.message : "价格方案加载失败");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, []);

  async function handleCreateOrder(packageId: number) {
    setCreatingPackageId(packageId);
    setError("");
    try {
      const orderId = await createAigcRechargeOrderByPackage(packageId);
      window.location.href = `/wallet?rechargeOrderId=${orderId}`;
    } catch (err) {
      setError(err instanceof Error ? err.message : "充值订单创建失败");
    } finally {
      setCreatingPackageId(null);
    }
  }

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-10">
      <div className="text-center">
        <h1 className="text-2xl font-semibold tracking-tight text-charcoal">
          价格方案
        </h1>
        <p className="mt-2 text-sm text-muted-gray">
          按需充值，灵活使用
        </p>
      </div>

      {loading ? (
        <div className="mt-10 rounded-xl border border-border-warm bg-background p-8 text-center text-sm text-muted-gray">
          正在加载价格方案...
        </div>
      ) : error ? (
        <div className="mt-10 rounded-xl border border-border-warm bg-background p-8 text-center text-sm text-charcoal">
          {error}
        </div>
      ) : packages.length === 0 ? (
        <div className="mt-10 rounded-xl border border-border-warm bg-background p-8 text-center text-sm text-muted-gray">
          暂无可用价格方案
        </div>
      ) : (
        <div className="mt-10 grid gap-6 md:grid-cols-3">
          {packages.map((pkg) => (
            <div
              key={pkg.id}
              className={`relative flex flex-col rounded-xl border bg-background p-6 ${
                pkg.recommendStatus ? "border-charcoal" : "border-border-warm"
              }`}
            >
              {pkg.recommendStatus && (
                <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-charcoal px-3 py-0.5 text-xs text-off-white">
                  推荐
                </span>
              )}
              <h3 className="text-lg font-medium text-charcoal">{pkg.name}</h3>
              <p className="mt-3 text-3xl font-semibold text-charcoal">
                {formatMoney(pkg.payAmount)}
              </p>
              <p className="mt-1 text-sm text-muted-gray">
                {formatPoints(pkg.totalPointAmount)}
              </p>
              {pkg.description && (
                <p className="mt-2 text-sm text-muted-gray">{pkg.description}</p>
              )}

              <ul className="mt-6 flex flex-col gap-2">
                {parseFeatures(pkg.features).map((f) => (
                  <li key={f} className="text-sm text-muted-gray">
                    · {f}
                  </li>
                ))}
              </ul>

              <button
                type="button"
                disabled={creatingPackageId === pkg.id}
                onClick={() => handleCreateOrder(pkg.id)}
                className={`mt-auto inline-flex items-center justify-center rounded-md py-2.5 text-sm active:opacity-80 ${
                  pkg.recommendStatus
                    ? "bg-charcoal text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]"
                    : "border border-[rgba(28,28,28,0.4)] text-charcoal"
                } disabled:opacity-50`}
              >
                {creatingPackageId === pkg.id ? "创建订单中..." : "立即充值"}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
