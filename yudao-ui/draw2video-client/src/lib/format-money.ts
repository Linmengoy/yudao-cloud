export function formatBalance(balance: number): string {
  return (balance / 100).toFixed(2);
}

export function formatPrice(cents: number): string {
  return `¥${(cents / 100).toFixed(2)}`;
}
