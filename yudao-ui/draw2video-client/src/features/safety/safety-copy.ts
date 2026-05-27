import { createSafetyState, normalizeSafetyStatus, type SafetyTarget } from "./safety-status"

export function getSafetyCopy(status?: string | null, target: SafetyTarget = "generation") {
  return createSafetyState(normalizeSafetyStatus(status), target)
}

export const safetyActionCopy = {
  retry: "修改后重试",
  backToCanvas: "回到画布修改",
  unavailable: "暂不可用",
} as const
