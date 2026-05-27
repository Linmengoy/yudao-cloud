export type SafetyStatus = "idle" | "reviewing" | "blocked" | "rejected" | "available"

export type SafetyTarget = "generation" | "task" | "asset"

export type SafetyTone = "neutral" | "warning" | "danger" | "success"

export interface SafetyState {
  status: SafetyStatus
  target: SafetyTarget
  title: string
  description: string
  tone: SafetyTone
  canRetry: boolean
}

export function createSafetyState(status: SafetyStatus, target: SafetyTarget = "generation"): SafetyState {
  if (status === "reviewing") {
    return {
      status,
      target,
      title: "内容已提交审核",
      description: "审核通过后会继续处理或展示结果。",
      tone: "neutral",
      canRetry: false,
    }
  }

  if (status === "blocked") {
    return {
      status,
      target,
      title: "内容可能不符合平台规范",
      description: "请调整提示词后重试。",
      tone: "warning",
      canRetry: true,
    }
  }

  if (status === "rejected") {
    return {
      status,
      target,
      title: "内容未通过审核",
      description: "请修改后重新提交。",
      tone: "danger",
      canRetry: true,
    }
  }

  if (status === "available") {
    return {
      status,
      target,
      title: "内容已通过审核",
      description: "现在可以继续使用。",
      tone: "success",
      canRetry: false,
    }
  }

  return {
    status,
    target,
    title: "",
    description: "",
    tone: "neutral",
    canRetry: false,
  }
}

export function normalizeSafetyStatus(input?: string | null): SafetyStatus {
  if (!input) return "idle"
  const value = input.toLowerCase()
  if (["pending", "reviewing", "auditing", "audit_pending", "review_pending", "manual_review"].includes(value)) return "reviewing"
  if (["blocked", "safety_blocked", "auto_reject"].includes(value)) return "blocked"
  if (["reject", "rejected", "manual_reject"].includes(value)) return "rejected"
  if (["pass", "passed", "available", "manual_pass", "auto_pass"].includes(value)) return "available"
  return "idle"
}

export function normalizeSafetyStatusFromError(message?: string | null): SafetyStatus {
  if (!message) return "idle"
  if (["安全", "审核", "规范", "敏感", "违规", "不符合"].some((keyword) => message.includes(keyword))) {
    return "rejected"
  }
  return "idle"
}
