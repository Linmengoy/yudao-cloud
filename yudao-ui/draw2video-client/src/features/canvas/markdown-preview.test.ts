import { describe, expect, it } from "vitest";
import { parseMarkdownPreview } from "./markdown-preview";

describe("parseMarkdownPreview", () => {
  it("parses common markdown blocks", () => {
    expect(parseMarkdownPreview("# 标题\n\n- A\n- B\n\n```js\nx()\n```")).toEqual([
      { type: "heading", level: 1, text: "标题" },
      { type: "list", ordered: false, items: ["A", "B"] },
      { type: "code", text: "x()" },
    ]);
  });
});
