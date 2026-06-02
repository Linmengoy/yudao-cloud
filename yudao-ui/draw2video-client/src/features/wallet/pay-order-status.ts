export enum PayOrderStatusEnum {
  WAITING = 0,
  SUCCESS = 10,
  REFUND = 20,
  CLOSED = 30,
}

export function isPaySuccess(status?: number): boolean {
  return status === PayOrderStatusEnum.SUCCESS;
}

export function isPayWaiting(status?: number): boolean {
  return status === PayOrderStatusEnum.WAITING;
}

export function isPayRefund(status?: number): boolean {
  return status === PayOrderStatusEnum.REFUND;
}

export function isPayClosed(status?: number): boolean {
  return status === PayOrderStatusEnum.CLOSED;
}

export function getPayStatusName(status?: number): string {
  switch (status) {
    case PayOrderStatusEnum.WAITING:
      return "未支付";
    case PayOrderStatusEnum.SUCCESS:
      return "支付成功";
    case PayOrderStatusEnum.REFUND:
      return "已退款";
    case PayOrderStatusEnum.CLOSED:
      return "支付关闭";
    default:
      return "未知状态";
  }
}
