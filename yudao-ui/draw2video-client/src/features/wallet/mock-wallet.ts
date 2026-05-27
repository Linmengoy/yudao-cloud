export interface Wallet {
  balance: number;
  frozenBalance: number;
  totalRecharge: number;
  totalExpense: number;
}

// TODO: replace with GET /app-api/pay/wallet/get
export const MOCK_WALLET: Wallet = {
  balance: 72900, // ¥729.00 in cents — adjust display as needed
  frozenBalance: 0,
  totalRecharge: 0,
  totalExpense: 0,
};

export function formatBalance(cents: number): string {
  return `¥${(cents / 100).toFixed(2)}`;
}
