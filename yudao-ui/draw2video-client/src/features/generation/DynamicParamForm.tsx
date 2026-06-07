"use client";

import type { AigcModelParamTemplate } from "./model-api";
import { cn } from "@/lib/utils";

type DynamicParamFormProps = {
  templates: AigcModelParamTemplate[];
  values: Record<string, unknown>;
  disabled?: boolean;
  onChange: (patch: Record<string, unknown>) => void;
};

function parseOptions(options?: string[]) {
  return (options ?? []).map((item) => ({ label: item, value: item }));
}

function coerceBoolean(value: unknown) {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") return value.toLowerCase() === "true";
  return Boolean(value);
}

function coerceMultiValue(value: unknown) {
  if (Array.isArray(value)) return value.map(String);
  if (typeof value === "string" && value.trim()) {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) return parsed.map(String);
    } catch {
      return value.split(",").map((item) => item.trim()).filter(Boolean);
    }
  }
  return [];
}

export function DynamicParamForm({ templates, values, disabled = false, onChange }: DynamicParamFormProps) {
  if (templates.length === 0) return null;

  return (
    <div className="space-y-5">
      {templates.map((item) => {
        const value = values[item.paramKey] ?? item.defaultValue ?? "";
        return (
          <label key={item.id} className="block text-xs text-muted-gray">
            <span className="mb-2 flex items-center gap-1 font-medium">
              {item.paramName}
              {item.requiredStatus && <span className="text-destructive">*</span>}
            </span>
            {item.paramType === "NUMBER" ? (
              <div className="flex items-center gap-2 rounded-xl bg-muted p-1">
                <input
                  type="number"
                  min={item.minValue}
                  max={item.maxValue}
                  value={Number(value || 0)}
                  disabled={disabled}
                  onChange={(e) => onChange({ [item.paramKey]: Number(e.target.value) })}
                  className="nodrag nowheel h-9 w-full rounded-lg border border-transparent bg-background px-3 text-sm font-medium text-charcoal shadow-sm transition-colors focus:border-charcoal/40 focus:outline-none disabled:cursor-not-allowed disabled:text-muted-gray"
                />
                {(item.minValue != null || item.maxValue != null) && (
                  <span className="shrink-0 px-2 text-[11px] text-muted-gray">
                    {item.minValue ?? "-"}-{item.maxValue ?? "-"}
                  </span>
                )}
              </div>
            ) : item.paramType === "BOOLEAN" ? (
              <div className="grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                {[
                  { label: "开", value: true },
                  { label: "关", value: false },
                ].map((option) => {
                  const selected = coerceBoolean(value) === option.value;
                  return (
                    <button
                      key={String(option.value)}
                      type="button"
                      disabled={disabled}
                      onClick={() => onChange({ [item.paramKey]: option.value })}
                      className={cn(
                        "nodrag nowheel rounded-lg px-3 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
                        selected ? "bg-background text-charcoal shadow-sm" : "text-muted-gray hover:text-charcoal"
                      )}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            ) : item.paramType === "SELECT" ? (
              <div className="grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                {parseOptions(item.options).map((option) => {
                  const selected = String(value) === option.value;
                  return (
                    <button
                      key={option.value}
                      type="button"
                      disabled={disabled}
                      onClick={() => onChange({ [item.paramKey]: option.value })}
                      className={cn(
                        "nodrag nowheel rounded-lg px-3 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
                        selected ? "bg-background text-charcoal shadow-sm" : "text-muted-gray hover:text-charcoal"
                      )}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            ) : item.paramType === "MULTI_SELECT" ? (
              <div className="grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                {parseOptions(item.options).map((option) => {
                  const selected = coerceMultiValue(value).includes(option.value);
                  return (
                    <button
                      key={option.value}
                      type="button"
                      disabled={disabled}
                      onClick={() => {
                        const current = coerceMultiValue(value);
                        onChange({
                          [item.paramKey]: selected
                            ? current.filter((item) => item !== option.value)
                            : [...current, option.value],
                        });
                      }}
                      className={cn(
                        "nodrag nowheel rounded-lg px-3 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
                        selected ? "bg-background text-charcoal shadow-sm" : "text-muted-gray hover:text-charcoal"
                      )}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            ) : item.paramType === "JSON" ? (
              <textarea
                value={typeof value === "string" ? value : JSON.stringify(value ?? {}, null, 2)}
                disabled={disabled}
                onChange={(e) => onChange({ [item.paramKey]: e.target.value })}
                className="nodrag nowheel min-h-24 w-full resize-y rounded-xl border border-border-warm bg-background px-3 py-2 font-mono text-sm text-charcoal transition-colors focus:border-charcoal/40 focus:outline-none focus:shadow-[0_4px_12px_rgba(0,0,0,0.1)] disabled:cursor-not-allowed disabled:text-muted-gray"
              />
            ) : (
              <input
                type="text"
                value={String(value)}
                disabled={disabled}
                onChange={(e) => onChange({ [item.paramKey]: e.target.value })}
                className="nodrag nowheel w-full rounded-xl border border-border-warm bg-background px-3 py-2 text-sm text-charcoal transition-colors focus:border-charcoal/40 focus:outline-none focus:shadow-[0_4px_12px_rgba(0,0,0,0.1)] disabled:cursor-not-allowed disabled:text-muted-gray"
              />
            )}
          </label>
        );
      })}
    </div>
  );
}
