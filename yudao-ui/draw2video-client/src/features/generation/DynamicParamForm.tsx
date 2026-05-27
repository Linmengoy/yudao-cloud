"use client";

import type { AigcModelParamTemplate } from "./model-api";

type DynamicParamFormProps = {
  templates: AigcModelParamTemplate[];
  values: Record<string, unknown>;
  disabled?: boolean;
  onChange: (patch: Record<string, unknown>) => void;
};

function parseOptions(options?: string[]) {
  return (options ?? []).map((item) => ({ label: item, value: item }));
}

export function DynamicParamForm({ templates, values, disabled = false, onChange }: DynamicParamFormProps) {
  if (templates.length === 0) return null;

  return (
    <div className="space-y-3 rounded-lg border border-border-warm bg-background p-3">
      {templates.map((item) => {
        const value = values[item.paramKey] ?? item.defaultValue ?? "";
        return (
          <label key={item.id} className="block text-[12px] text-muted-gray">
            <span className="mb-1.5 flex items-center gap-1">
              {item.paramName}
              {item.requiredStatus && <span className="text-destructive">*</span>}
            </span>
            {item.paramType === "NUMBER" ? (
              <input
                type="number"
                min={item.minValue}
                max={item.maxValue}
                value={Number(value || 0)}
                disabled={disabled}
                onChange={(e) => onChange({ [item.paramKey]: Number(e.target.value) })}
                className="nodrag nowheel w-full rounded-md border border-border-warm bg-background px-3 py-2 text-sm text-charcoal transition-colors focus:border-charcoal/40 focus:outline-none focus:shadow-[0_4px_12px_rgba(0,0,0,0.1)] disabled:cursor-not-allowed disabled:text-muted-gray"
              />
            ) : item.paramType === "BOOLEAN" ? (
              <button
                type="button"
                disabled={disabled}
                onClick={() => onChange({ [item.paramKey]: !Boolean(value) })}
                className="nodrag nowheel flex h-8 w-14 items-center rounded-full border border-border-warm bg-muted px-1 transition-colors disabled:cursor-not-allowed disabled:opacity-50"
                aria-label={`切换${item.paramName}`}
              >
                <span className={`size-5 rounded-full bg-charcoal transition-transform ${Boolean(value) ? "translate-x-6" : "translate-x-0 opacity-40"}`} />
              </button>
            ) : item.paramType === "SELECT" ? (
              <select
                value={String(value)}
                disabled={disabled}
                onChange={(e) => onChange({ [item.paramKey]: e.target.value })}
                className="nodrag nowheel w-full rounded-md border border-border-warm bg-background px-3 py-2 text-sm text-charcoal transition-colors focus:border-charcoal/40 focus:outline-none focus:shadow-[0_4px_12px_rgba(0,0,0,0.1)] disabled:cursor-not-allowed disabled:text-muted-gray"
              >
                <option value="">请选择</option>
                {parseOptions(item.options).map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            ) : (
              <input
                type="text"
                value={String(value)}
                disabled={disabled}
                onChange={(e) => onChange({ [item.paramKey]: e.target.value })}
                className="nodrag nowheel w-full rounded-md border border-border-warm bg-background px-3 py-2 text-sm text-charcoal transition-colors focus:border-charcoal/40 focus:outline-none focus:shadow-[0_4px_12px_rgba(0,0,0,0.1)] disabled:cursor-not-allowed disabled:text-muted-gray"
              />
            )}
          </label>
        );
      })}
    </div>
  );
}
