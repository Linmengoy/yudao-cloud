import { ShieldAlert, ShieldCheck, ShieldQuestion } from "lucide-react"

import { cn } from "@/lib/utils"
import type { SafetyState } from "./safety-status"

const toneClassName = {
  neutral: "border-[#eceae4] bg-[#f7f4ed] text-[rgba(28,28,28,0.82)]",
  warning: "border-[rgba(28,28,28,0.18)] bg-[#f7f4ed] text-[#1c1c1c]",
  danger: "border-[rgba(28,28,28,0.28)] bg-[#f7f4ed] text-[#1c1c1c]",
  success: "border-[#eceae4] bg-[#f7f4ed] text-[rgba(28,28,28,0.82)]",
}

function SafetyIcon({ tone }: Pick<SafetyState, "tone">) {
  if (tone === "success") return <ShieldCheck aria-hidden="true" className="h-3.5 w-3.5" />
  if (tone === "danger" || tone === "warning") return <ShieldAlert aria-hidden="true" className="h-3.5 w-3.5" />
  return <ShieldQuestion aria-hidden="true" className="h-3.5 w-3.5" />
}

export function SafetyStatusPill({ state, className }: { state: SafetyState; className?: string }) {
  if (state.status === "idle") return null

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs leading-none",
        toneClassName[state.tone],
        className,
      )}
    >
      <SafetyIcon tone={state.tone} />
      {state.title}
    </span>
  )
}

export function SafetyInlineNotice({ state, className }: { state: SafetyState; className?: string }) {
  if (state.status === "idle") return null

  return (
    <div className={cn("rounded-md border p-3 text-sm", toneClassName[state.tone], className)} role="status">
      <div className="flex items-center gap-2 font-medium">
        <SafetyIcon tone={state.tone} />
        <span>{state.title}</span>
      </div>
      <p className="mt-1 text-[rgba(28,28,28,0.62)]">{state.description}</p>
    </div>
  )
}
