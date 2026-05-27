"use client";

import { useState, useMemo } from "react";
import { X } from "lucide-react";
import { normalizeImageSize, calculateImageSize, type SizeTier } from "./size";
import { cn } from "@/lib/utils";

const TIERS: SizeTier[] = ["1K", "2K", "4K"];

const RATIOS = [
  { label: "1:1", value: "1:1" },
  { label: "3:2", value: "3:2" },
  { label: "2:3", value: "2:3" },
  { label: "16:9", value: "16:9" },
  { label: "9:16", value: "9:16" },
  { label: "4:3", value: "4:3" },
  { label: "3:4", value: "3:4" },
  { label: "21:9", value: "21:9" },
];

type Mode = "auto" | "ratio" | "resolution";

interface Props {
  currentSize: string;
  onSelect: (size: string) => void;
  onClose: () => void;
}

const SIZE_LIMIT_TEXT =
  "宽高均为 16 的倍数，最大边长 3840px，宽高比不超过 3:1，总像素限制为 655360-8294400。";

export function SizePickerModal({ currentSize, onSelect, onClose }: Props) {
  // Compute matching tier+ratio from current size
  let matchedTier: SizeTier | undefined;
  let matchedRatio: string | undefined;
  if (currentSize && currentSize !== "auto") {
    const normalized = normalizeImageSize(currentSize);
    for (const tier of TIERS) {
      for (const ratio of RATIOS) {
        if (calculateImageSize(tier, ratio.value) === normalized) {
          matchedTier = tier;
          matchedRatio = ratio.value;
          break;
        }
      }
      if (matchedTier) break;
    }
  }

  const match = currentSize.match(/^\s*(\d+)\s*[xX×]\s*(\d+)\s*$/);
  const [mode, setMode] = useState<Mode>(() => {
    if (!currentSize || currentSize === "auto") return "auto";
    if (matchedTier) return "ratio";
    return "resolution";
  });

  const [tier, setTier] = useState<SizeTier>(matchedTier ?? "1K");
  const [ratio, setRatio] = useState(matchedRatio ?? "1:1");
  const [customRatio, setCustomRatio] = useState("16:9");
  const [customW, setCustomW] = useState(match?.[1] ?? "1024");
  const [customH, setCustomH] = useState(match?.[2] ?? "1024");

  const activeRatio = ratio === "custom" ? customRatio : ratio;

  const previewSize = useMemo(() => {
    if (mode === "auto") return "auto";
    if (mode === "ratio") {
      const size = calculateImageSize(tier, activeRatio);
      return size ? normalizeImageSize(size) : "";
    }
    if (mode === "resolution") {
      const w = parseInt(customW, 10);
      const h = parseInt(customH, 10);
      if (Number.isFinite(w) && Number.isFinite(h) && w > 0 && h > 0) {
        return normalizeImageSize(`${w}x${h}`);
      }
      return "";
    }
    return "";
  }, [mode, tier, activeRatio, customW, customH]);

  const applySize = () => {
    if (!previewSize) return;
    onSelect(previewSize);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div className="absolute inset-0 bg-black/20" />
      <div
        className="relative z-10 w-full max-w-[380px] rounded-2xl border border-border-warm bg-background p-5 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h3 className="text-sm font-semibold text-charcoal">设置图像尺寸</h3>
            <p className="mt-0.5 text-[11px] text-muted-gray">
              当前：{currentSize || "auto"}
            </p>
          </div>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-muted-gray hover:bg-muted hover:text-charcoal"
          >
            <X className="size-4" />
          </button>
        </div>

        {/* Tabs */}
        <div className="mb-4 flex rounded-lg bg-muted p-1">
          {(["auto", "ratio", "resolution"] as const).map((m) => (
            <button
              key={m}
              onClick={() => setMode(m)}
              className={cn(
                "flex-1 rounded-md py-1.5 text-[11px] font-medium transition-colors",
                mode === m
                  ? "bg-background text-charcoal shadow-sm"
                  : "text-muted-gray hover:text-charcoal"
              )}
            >
              {m === "auto" ? "自动" : m === "ratio" ? "按比例" : "自定义宽高"}
            </button>
          ))}
        </div>

        {/* Content */}
        <div className="min-h-[200px]">
          {mode === "auto" && (
            <div className="flex h-full flex-col items-center justify-center py-8 text-center">
              <div className="mb-3 flex size-10 items-center justify-center rounded-full bg-muted text-charcoal">
                <svg className="size-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <h4 className="text-xs font-medium text-charcoal">自动尺寸</h4>
              <p className="mt-1 text-[11px] text-muted-gray">
                由模型自己决定生成尺寸
              </p>
            </div>
          )}

          {mode === "ratio" && (
            <div className="space-y-4">
              <div>
                <div className="mb-2 text-[11px] font-medium text-muted-gray">基准分辨率</div>
                <div className="grid grid-cols-3 gap-2">
                  {TIERS.map((t) => (
                    <button
                      key={t}
                      onClick={() => setTier(t)}
                      className={cn(
                        "rounded-lg border px-3 py-2 text-[11px] transition-colors",
                        tier === t
                          ? "border-charcoal bg-charcoal text-off-white"
                          : "border-border-warm text-charcoal hover:border-[rgba(28,28,28,0.4)]"
                      )}
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <div className="mb-2 text-[11px] font-medium text-muted-gray">图像比例</div>
                <div className="grid grid-cols-4 gap-1.5">
                  {RATIOS.map((r) => (
                    <button
                      key={r.value}
                      onClick={() => setRatio(r.value)}
                      className={cn(
                        "rounded-md border px-2 py-1.5 text-[11px] transition-colors",
                        ratio === r.value
                          ? "border-charcoal bg-charcoal text-off-white"
                          : "border-border-warm text-charcoal hover:border-[rgba(28,28,28,0.4)]"
                      )}
                    >
                      {r.label}
                    </button>
                  ))}
                  <button
                    onClick={() => setRatio("custom")}
                    className={cn(
                      "col-span-4 rounded-md border px-2 py-1.5 text-[11px] transition-colors",
                      ratio === "custom"
                        ? "border-charcoal bg-charcoal text-off-white"
                        : "border-border-warm text-charcoal hover:border-[rgba(28,28,28,0.4)]"
                    )}
                  >
                    自定义比例
                  </button>
                </div>
              </div>

              {ratio === "custom" && (
                <input
                  value={customRatio}
                  onChange={(e) => setCustomRatio(e.target.value)}
                  placeholder="例如 5:4 / 2.39:1"
                  className="w-full rounded-lg border border-border-warm bg-background px-3 py-2 text-[11px] text-charcoal placeholder:text-muted-gray focus:border-[rgba(28,28,28,0.4)] focus:outline-none"
                />
              )}
            </div>
          )}

          {mode === "resolution" && (
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="flex-1">
                  <span className="mb-1 block text-[11px] text-muted-gray">宽度</span>
                  <input
                    type="number"
                    value={customW}
                    onChange={(e) => setCustomW(e.target.value)}
                    placeholder="1024"
                    className="w-full rounded-lg border border-border-warm bg-background px-3 py-2 text-[11px] text-charcoal focus:border-[rgba(28,28,28,0.4)] focus:outline-none"
                  />
                </div>
                <div className="mt-4 text-border-warm">×</div>
                <div className="flex-1">
                  <span className="mb-1 block text-[11px] text-muted-gray">高度</span>
                  <input
                    type="number"
                    value={customH}
                    onChange={(e) => setCustomH(e.target.value)}
                    placeholder="1024"
                    className="w-full rounded-lg border border-border-warm bg-background px-3 py-2 text-[11px] text-charcoal focus:border-[rgba(28,28,28,0.4)] focus:outline-none"
                  />
                </div>
              </div>
              <div className="rounded-lg bg-muted px-3 py-2 text-[10px] leading-relaxed text-muted-gray">
                {SIZE_LIMIT_TEXT}
              </div>
            </div>
          )}
        </div>

        {/* Preview */}
        <div className="mt-4 rounded-xl bg-muted px-4 py-3">
          <div className="text-[10px] text-muted-gray">将使用</div>
          <div className="mt-0.5 font-mono text-sm font-semibold text-charcoal">
            {previewSize || "尺寸无效"}
          </div>
        </div>

        {/* Actions */}
        <div className="mt-4 flex gap-2">
          <button
            onClick={onClose}
            className="flex-1 rounded-lg bg-muted px-4 py-2 text-[11px] text-charcoal transition-colors hover:bg-border-warm"
          >
            取消
          </button>
          <button
            onClick={applySize}
            disabled={!previewSize}
            className="flex-1 rounded-lg bg-charcoal px-4 py-2 text-[11px] text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset] disabled:opacity-50"
          >
            确定
          </button>
        </div>
      </div>
    </div>
  );
}
