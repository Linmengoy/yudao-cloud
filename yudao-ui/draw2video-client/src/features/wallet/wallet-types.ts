export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface AigcWallet {
  id: number;
  userId: number;
  balance: number;
  frozenBalance: number;
  totalRecharge: number;
  totalGift: number;
  totalConsume: number;
  totalRefund: number;
  status: number;
  lastTransTime?: string;
}

export interface AigcWalletRecord {
  id: number;
  recordNo: string;
  recordType: number | string;
  recordTypeName?: string;
  amount: number;
  balanceAfter: number;
  frozenBalanceAfter: number;
  bizType: number | string;
  bizId: string;
  taskId?: number;
  taskNo?: string;
  modelId?: number;
  remark?: string;
  createTime: string;
}

export interface AigcWalletFreeze {
  id: number;
  freezeNo: string;
  amount: number;
  confirmedAmount: number;
  releasedAmount: number;
  status: number | string;
  statusName?: string;
  taskId?: number;
  taskNo?: string;
  expireTime?: string;
  createTime: string;
}

export interface AigcRechargeOrder {
  id: number;
  rechargeNo: string;
  payOrderId?: number;
  payOrderNo?: string;
  payAppId?: number;
  payChannelCode?: string;
  payAmount: number;
  pointAmount: number;
  giftAmount: number;
  totalPointAmount: number;
  status: number | string;
  statusName?: string;
  payTime?: string;
  createTime: string;
}

export interface AigcRechargeCreateResult {
  rechargeOrderId: number;
  rechargeNo: string;
  payOrderId: number;
  payOrderNo: string;
  payAppId: number;
  payAmount: number;
  pointAmount: number;
  giftAmount: number;
  totalPointAmount: number;
}

export interface PayOrder {
  id: number;
  appId?: number;
  channelCode?: string;
  merchantOrderId: string;
  subject: string;
  body?: string;
  price: number;
  status: number;
  expireTime?: string;
  successTime?: string;
  no?: string;
}

export interface PayOrderSubmitResult {
  status: number;
  displayMode: string;
  displayContent: string;
}

export interface AigcRechargePackage {
  id: number;
  name: string;
  payAmount: number;
  pointAmount: number;
  giftAmount: number;
  totalPointAmount: number;
  description?: string;
  features?: string;
  recommendStatus?: boolean;
  sort?: number;
  status?: number;
}

export interface WalletPageParams {
  pageNo: number;
  pageSize: number;
  recordType?: number | string;
  status?: number | string;
  taskNo?: string;
}
