import type { AigcModelPrice } from "./model-api";

type PriceEstimateProps = {
  price: AigcModelPrice | null;
  loading?: boolean;
};

const BILLING_UNIT_LABELS: Record<string, string> = {
  PER_TASK: "按任务",
  PER_IMAGE: "按张",
  PER_SECOND: "按秒",
  PER_5_SECONDS: "每 5 秒",
  PER_BATCH: "按批次",
};

export function PriceEstimate({ price, loading = false }: PriceEstimateProps) {
  return (
    <div className="rounded-lg border border-border-warm bg-background px-3 py-2 text-xs text-muted-gray">
      {loading ? (
        "价格预估中..."
      ) : price ? (
        <div className="flex items-center justify-between gap-3">
          <span>预计消耗</span>
          <span className="text-sm font-medium text-charcoal">
            {price.salePrice} {price.currencyType || "积分"}
            {price.billingUnit && <span className="ml-1 font-normal text-muted-gray">{BILLING_UNIT_LABELS[price.billingUnit] ?? price.billingUnit}</span>}
          </span>
        </div>
      ) : (
        "暂无价格预估"
      )}
    </div>
  );
}
