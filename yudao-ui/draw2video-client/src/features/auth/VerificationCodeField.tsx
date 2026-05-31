"use client";

import { Clock, Mail, MessageSquare, Send } from "lucide-react";
import { cn } from "@/lib/utils";

type VerificationCodeFieldProps = {
  channel: "email" | "sms";
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  disabled: boolean;
  sending: boolean;
  countdown: number;
  placeholder: string;
  label?: string;
  recipient?: string;
  className?: string;
};

export function VerificationCodeField({
  channel,
  value,
  onChange,
  onSend,
  disabled,
  sending,
  countdown,
  placeholder,
  label = "验证码",
  recipient,
  className,
}: VerificationCodeFieldProps) {
  const Icon = channel === "email" ? Mail : MessageSquare;
  const targetLabel = channel === "email" ? "邮箱" : "手机";
  const canSend = !disabled && !sending && countdown <= 0;

  return (
    <div className={cn("rounded-xl border border-border-warm bg-muted p-3", className)}>
      <div className="mb-3 flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-background text-charcoal shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.08)_0px_0px_0px_0.5px_inset]">
            <Icon className="size-4" />
          </span>
          <div className="min-w-0">
            <p className="text-sm font-medium text-charcoal">{label}</p>
            <p className="truncate text-xs text-muted-gray">
              {recipient ? `发送到 ${recipient}` : `填写${targetLabel}后获取验证码`}
            </p>
          </div>
        </div>
        {countdown > 0 && (
          <span className="inline-flex shrink-0 items-center gap-1 rounded-full border border-border-warm bg-background px-2.5 py-1 text-xs text-muted-gray">
            <Clock className="size-3" />
            {countdown}s
          </span>
        )}
      </div>

      <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
        <input
          inputMode="numeric"
          value={value}
          onChange={(event) => onChange(event.target.value.replace(/\D/g, "").slice(0, 6))}
          placeholder={placeholder}
          className="input-base bg-background"
        />
        <button
          type="button"
          disabled={!canSend}
          onClick={onSend}
          className={cn(
            "inline-flex min-h-10 items-center justify-center gap-2 rounded-md px-3 py-2.5 text-sm transition-[box-shadow,opacity,background-color]",
            canSend
              ? "bg-charcoal text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80"
              : "cursor-not-allowed border border-border-warm bg-background text-muted-gray opacity-70"
          )}
        >
          <Send className="size-4" />
          {sending ? "发送中" : countdown > 0 ? "稍后重试" : "发送验证码"}
        </button>
      </div>
    </div>
  );
}
