export enum PayOrderStatusEnum {
  WAITING = 0,
  SUCCESS = 10,
  REFUND = 20,
  CLOSED = 30,
}

export function normalizePayStatus(status?: number | string | null): number | string | undefined {
  if (typeof status === "number") return status;
  if (!status) return undefined;
  const text = status.toUpperCase();
  const names: Record<string, number> = {
    WAIT_PAY: PayOrderStatusEnum.WAITING,
    WAITING: PayOrderStatusEnum.WAITING,
    SUCCESS: PayOrderStatusEnum.SUCCESS,
    PAY_SUCCESS: PayOrderStatusEnum.SUCCESS,
    REFUND: PayOrderStatusEnum.REFUND,
    CLOSED: PayOrderStatusEnum.CLOSED,
  };
  return names[text] ?? status;
}

export function isPaySuccess(status?: number | string | null): boolean {
  return normalizePayStatus(status) === PayOrderStatusEnum.SUCCESS;
}

export function isPayWaiting(status?: number | string | null): boolean {
  return normalizePayStatus(status) === PayOrderStatusEnum.WAITING;
}

export function isPayRefund(status?: number | string | null): boolean {
  return normalizePayStatus(status) === PayOrderStatusEnum.REFUND;
}

export function isPayClosed(status?: number | string | null): boolean {
  return normalizePayStatus(status) === PayOrderStatusEnum.CLOSED;
}

export function getPayStatusName(status?: number | string | null): string {
  if (typeof status === "string") {
    const text = status.toUpperCase();
    const names: Record<string, string> = {
      WAIT_PAY: "待支付",
      WAITING: "待支付",
      SUCCESS: "支付成功",
      PAY_SUCCESS: "支付成功",
      REFUND: "已退款",
      CLOSED: "支付关闭",
    };
    if (names[text]) return names[text];
  }
  switch (normalizePayStatus(status)) {
    case PayOrderStatusEnum.WAITING:
      return "待支付";
    case PayOrderStatusEnum.SUCCESS:
      return "支付成功";
    case PayOrderStatusEnum.REFUND:
      return "已退款";
    case PayOrderStatusEnum.CLOSED:
      return "支付关闭";
    default:
      return status ? String(status) : "未知状态";
  }
}
