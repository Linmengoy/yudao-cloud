import Link from "next/link";

const PACKAGES = [
  {
    name: "体验版",
    price: "¥9.9",
    credits: "100 积分",
    features: ["文生图基础模型", "标准分辨率", "每日 50 次"],
  },
  {
    name: "标准版",
    price: "¥49.9",
    credits: "600 积分",
    features: ["全部图片模型", "高清分辨率", "每日 200 次", "优先队列"],
    popular: true,
  },
  {
    name: "专业版",
    price: "¥199",
    credits: "3000 积分",
    features: ["全部模型", "最高分辨率", "无限次数", "优先队列", "API 访问"],
  },
];

export default function PricingPage() {
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

      <div className="mt-10 grid gap-6 md:grid-cols-3">
        {PACKAGES.map((pkg) => (
          <div
            key={pkg.name}
            className={`relative flex flex-col rounded-xl border bg-background p-6 ${
              pkg.popular
                ? "border-charcoal"
                : "border-border-warm"
            }`}
          >
            {pkg.popular && (
              <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-charcoal px-3 py-0.5 text-xs text-off-white">
                推荐
              </span>
            )}
            <h3 className="text-lg font-medium text-charcoal">{pkg.name}</h3>
            <p className="mt-3 text-3xl font-semibold text-charcoal">
              {pkg.price}
            </p>
            <p className="mt-1 text-sm text-muted-gray">{pkg.credits}</p>

            <ul className="mt-6 flex flex-col gap-2">
              {pkg.features.map((f) => (
                <li key={f} className="text-sm text-muted-gray">
                  · {f}
                </li>
              ))}
            </ul>

            <Link
              href="/wallet"
              className={`mt-auto inline-flex items-center justify-center rounded-md py-2.5 text-sm active:opacity-80 ${
                pkg.popular
                  ? "bg-charcoal text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]"
                  : "border border-[rgba(28,28,28,0.4)] text-charcoal"
              }`}
            >
              立即充值
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
