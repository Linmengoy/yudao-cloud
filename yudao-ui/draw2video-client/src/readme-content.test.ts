import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

const readme = readFileSync(new URL("../../../README.md", import.meta.url), "utf8");

describe("project README", () => {
  it("describes the second-developed Manman AIGC platform instead of the upstream scaffold", () => {
    expect(readme).toContain("Manman AIGC 多模态创作平台");
    expect(readme).toContain("基于芋道 Spring Cloud 脚手架二次开发");
    expect(readme).toContain("Draw2Video 创作工作台");
    expect(readme).toContain("yudao-module-aigc-gen");
    expect(readme).toContain("yudao-ui/draw2video-client");
    expect(readme).not.toContain("芋道源码");
    expect(readme).not.toContain("ruoyi-vue-pro");
  });
});
