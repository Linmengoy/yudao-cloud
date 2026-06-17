export type MarkdownPreviewBlock =
  | { type: "heading"; level: 1 | 2 | 3; text: string }
  | { type: "paragraph"; text: string }
  | { type: "quote"; text: string }
  | { type: "list"; ordered: boolean; items: string[] }
  | { type: "code"; text: string };

function flushParagraph(blocks: MarkdownPreviewBlock[], lines: string[]) {
  if (lines.length === 0) return;
  blocks.push({ type: "paragraph", text: lines.join("\n") });
  lines.length = 0;
}

function flushQuote(blocks: MarkdownPreviewBlock[], lines: string[]) {
  if (lines.length === 0) return;
  blocks.push({ type: "quote", text: lines.join("\n") });
  lines.length = 0;
}

function flushList(blocks: MarkdownPreviewBlock[], list: { ordered: boolean; items: string[] } | null) {
  if (!list || list.items.length === 0) return null;
  blocks.push({ type: "list", ordered: list.ordered, items: list.items });
  return null;
}

export function parseMarkdownPreview(markdown: string): MarkdownPreviewBlock[] {
  const blocks: MarkdownPreviewBlock[] = [];
  const paragraphLines: string[] = [];
  const quoteLines: string[] = [];
  let list: { ordered: boolean; items: string[] } | null = null;
  let codeLines: string[] | null = null;

  for (const line of markdown.replace(/\r\n/g, "\n").split("\n")) {
    const trimmed = line.trim();

    if (codeLines) {
      if (trimmed.startsWith("```")) {
        blocks.push({ type: "code", text: codeLines.join("\n") });
        codeLines = null;
      } else {
        codeLines.push(line);
      }
      continue;
    }

    if (trimmed.startsWith("```")) {
      flushParagraph(blocks, paragraphLines);
      flushQuote(blocks, quoteLines);
      list = flushList(blocks, list);
      codeLines = [];
      continue;
    }

    if (!trimmed) {
      flushParagraph(blocks, paragraphLines);
      flushQuote(blocks, quoteLines);
      list = flushList(blocks, list);
      continue;
    }

    const heading = /^(#{1,3})\s+(.+)$/.exec(trimmed);
    if (heading) {
      flushParagraph(blocks, paragraphLines);
      flushQuote(blocks, quoteLines);
      list = flushList(blocks, list);
      blocks.push({ type: "heading", level: heading[1].length as 1 | 2 | 3, text: heading[2] });
      continue;
    }

    const quote = /^>\s?(.*)$/.exec(line);
    if (quote) {
      flushParagraph(blocks, paragraphLines);
      list = flushList(blocks, list);
      quoteLines.push(quote[1]);
      continue;
    }

    const item = /^\s*(?:([-*+])|(\d+)\.)\s+(.+)$/.exec(line);
    if (item) {
      flushParagraph(blocks, paragraphLines);
      flushQuote(blocks, quoteLines);
      const ordered = Boolean(item[2]);
      if (list && list.ordered !== ordered) list = flushList(blocks, list);
      list ??= { ordered, items: [] };
      list.items.push(item[3]);
      continue;
    }

    flushQuote(blocks, quoteLines);
    list = flushList(blocks, list);
    paragraphLines.push(line);
  }

  if (codeLines) blocks.push({ type: "code", text: codeLines.join("\n") });
  flushParagraph(blocks, paragraphLines);
  flushQuote(blocks, quoteLines);
  flushList(blocks, list);
  return blocks;
}
