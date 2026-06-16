import { describe, expect, it } from "vitest";

import { getPayStatusName, isPayWaiting } from "./pay-order-status";

describe("pay-order-status", () => {
  it("renders backend WAIT_PAY text as Chinese waiting status", () => {
    expect(getPayStatusName("WAIT_PAY")).toBe("待支付");
    expect(getPayStatusName("wait_pay")).toBe("待支付");
    expect(isPayWaiting("WAIT_PAY")).toBe(true);
  });
});
