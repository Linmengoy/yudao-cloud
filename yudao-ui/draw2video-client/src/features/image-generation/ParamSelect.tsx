"use client";

import { useState, useRef, useEffect } from "react";

export interface ParamSelectOption<T extends string> {
  label: string;
  value: T;
  description?: string;
}

interface ParamSelectProps<T extends string> {
  value: T;
  options: ParamSelectOption<T>[];
  onChange: (value: T) => void;
  label?: string;
  className?: string;
  disabled?: boolean;
}

export function ParamSelect<T extends string>({
  value,
  options,
  onChange,
  label,
  className,
  disabled = false,
}: ParamSelectProps<T>) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const current = options.find((o) => o.value === value);

  useEffect(() => {
    if (!open) return;
    function handleClick(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  return (
    <div ref={containerRef} className={className}>
      {label && (
        <label className="nodrag nowheel mb-1 block text-[11px] font-medium text-muted-gray">
          {label}
        </label>
      )}
      <button
        type="button"
        disabled={disabled}
        onClick={() => {
          if (!disabled) setOpen((v) => !v);
        }}
        className="nodrag nowheel w-full rounded-md border border-border-warm bg-background px-2 py-1 text-left text-[11px] text-charcoal transition-colors hover:border-[rgba(28,28,28,0.4)] disabled:cursor-not-allowed disabled:bg-muted disabled:text-muted-gray"
      >
        {current?.label ?? value}
      </button>

      {open && (
        <div className="absolute z-50 mt-1 min-w-[120px] rounded-lg border border-border-warm bg-background py-0.5 shadow-[0_4px_12px_rgba(0,0,0,0.08)]">
          {options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => {
                onChange(opt.value);
                setOpen(false);
              }}
              className={`nodrag nowheel flex w-full items-center px-3 py-1.5 text-left text-[11px] transition-colors ${
                opt.value === value
                  ? "bg-muted text-charcoal font-medium"
                  : "text-charcoal hover:bg-muted"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
