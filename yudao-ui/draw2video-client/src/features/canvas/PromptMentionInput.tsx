/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { ImageIcon } from "lucide-react";
import { cn } from "@/lib/utils";

export type PromptMentionOption = {
  id: string;
  label: string;
  token: string;
  thumbnailUrl?: string | null;
};

type PromptMentionInputProps = {
  value: string;
  onChange: (value: string) => void;
  mentions: PromptMentionOption[];
  disabled?: boolean;
  placeholder?: string;
  minHeightClassName?: string;
  onSubmit?: () => void;
};

const LEGACY_TOKEN_PATTERN = /^\{\{Image\s+(\d+)}}$/;
const ANY_TOKEN_PATTERN = /\{\{[^{}]+}}/g;

export function createPromptMentionToken(id: string) {
  return `{{ImageRef ${encodeURIComponent(id)}}}`;
}

export function promptValueToSubmitPrompt(value: string, mentions: PromptMentionOption[]) {
  return mentions.reduce(
    (prompt, mention, index) => prompt.split(mention.token).join(`{{Image ${index + 1}}}`),
    value
  );
}

export function handleComposerWheelPan(
  event: WheelEvent,
  getViewport: () => { x: number; y: number; zoom: number },
  setViewport: (viewport: { x: number; y: number; zoom: number }) => void
) {
  const target = event.target;
  // 提示词滚动区域
  const isPromptScrollArea = target instanceof Element && Boolean(target.closest("[data-prompt-scroll-area='true']"));
  const isLocalWheelArea = target instanceof Element && Boolean(target.closest("[data-composer-local-wheel='true']"));
  // 在提示词滚动区域，如果是垂直滚动（deltaY更大），则允许默认行为，不阻止滚动
  if (isPromptScrollArea && Math.abs(event.deltaY) >= Math.abs(event.deltaX)) return;
  if (isLocalWheelArea) {
    if (Math.abs(event.deltaY) >= Math.abs(event.deltaX)) return;
    event.preventDefault();
    event.stopPropagation();
    return;
  }
  // 非以上情况，画布就可以移动了
  event.preventDefault();
  event.stopPropagation();
  const viewport = getViewport();
  setViewport({
    ...viewport,
    x: viewport.x - event.deltaX,
    y: viewport.y - event.deltaY,
  });
}

export function useComposerWheelPan<T extends HTMLElement>(
  getViewport: () => { x: number; y: number; zoom: number },
  setViewport: (viewport: { x: number; y: number; zoom: number }) => void
) {
  const [element, setElement] = useState<T | null>(null);
  // 只会触发一次
  const ref = useCallback((nextElement: T | null) => {
    setElement(nextElement);
  }, []);

  useEffect(() => {
    // element 为空时，不添加事件监听器
    if (!element) return;
    // 监听 鼠标滚轮 事件
    const handleWheel = (event: WheelEvent) => handleComposerWheelPan(event, getViewport, setViewport);
    element.addEventListener("wheel", handleWheel, { capture: true, passive: false });
    return () => {
      element.removeEventListener("wheel", handleWheel, { capture: true });
    };
  }, [element, getViewport, setViewport]);

  return ref;
}

function removeMissingMentionTokens(value: string, mentions: PromptMentionOption[]) {
  const knownTokens = new Set(mentions.map((mention) => mention.token));
  return value
    .replace(ANY_TOKEN_PATTERN, (token) => {
      if (knownTokens.has(token)) return token;
      const legacyMatch = token.match(LEGACY_TOKEN_PATTERN);
      if (legacyMatch) {
        const index = Number(legacyMatch[1]);
        return index >= 1 && index <= mentions.length ? token : "";
      }
      return token.startsWith("{{ImageRef ") ? "" : token;
    })
    .replace(/[ \t]{2,}/g, " ");
}

function isInternalMentionToken(token: string) {
  return token.startsWith("{{ImageRef ");
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function renderPromptHtml(value: string, mentions: PromptMentionOption[]) {
  const byToken = new Map(mentions.map((mention) => [mention.token, mention]));
  let html = "";
  let lastIndex = 0;
  for (const match of value.matchAll(ANY_TOKEN_PATTERN)) {
    const token = match[0];
    const index = match.index ?? 0;
    html += escapeHtml(value.slice(lastIndex, index));
    const legacyMatch = token.match(LEGACY_TOKEN_PATTERN);
    const mention = byToken.get(token) ?? (legacyMatch ? mentions[Number(legacyMatch[1]) - 1] : undefined);
    if (mention) {
      const img = mention.thumbnailUrl
        ? `<img src="${escapeHtml(mention.thumbnailUrl)}" alt="" draggable="false" class="h-5 w-5 rounded object-cover" />`
        : `<span class="flex h-5 w-5 items-center justify-center rounded bg-muted"><svg viewBox="0 0 24 24" class="h-3.5 w-3.5" aria-hidden="true"><path fill="currentColor" d="M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2Zm0 14.6-3.1-3.1a2 2 0 0 0-2.8 0L12 15.6l-3.1-3.1a2 2 0 0 0-2.8 0L5 13.6V5h14v12.6ZM8.5 9.5A1.5 1.5 0 1 0 8.5 6a1.5 1.5 0 0 0 0 3.5Z"/></svg></span>`;
      html += `<span contenteditable="false" data-mention-token="${escapeHtml(token)}" class="mx-0.5 inline-flex items-center gap-1.5 rounded-md border border-border-warm bg-muted px-1.5 py-0.5 align-middle text-sm text-charcoal">${img}<span>${escapeHtml(mention.label)}</span></span>`;
    } else if (isInternalMentionToken(token)) {
      html += "";
    } else {
      html += escapeHtml(token);
    }
    lastIndex = index + token.length;
  }
  html += escapeHtml(value.slice(lastIndex));
  return html.replace(/\n/g, "<br>");
}

function serializePrompt(root: HTMLElement) {
  let text = "";
  root.childNodes.forEach((node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      text += node.textContent ?? "";
      return;
    }
    if (!(node instanceof HTMLElement)) return;
    const token = node.dataset.mentionToken;
    if (token) {
      text += token;
      return;
    }
    if (node.tagName === "BR") {
      text += "\n";
      return;
    }
    text += node.innerText;
  });
  return text.replace(/\u00a0/g, " ");
}

function placeCaretAtEnd(element: HTMLElement) {
  const range = document.createRange();
  range.selectNodeContents(element);
  range.collapse(false);
  const selection = window.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(range);
}

export function PromptMentionInput({
  value,
  onChange,
  mentions,
  disabled,
  placeholder = "Describe anything you want to generate",
  minHeightClassName = "min-h-[130px]",
  onSubmit,
}: PromptMentionInputProps) {
  const editorRef = useRef<HTMLDivElement>(null);
  const [focused, setFocused] = useState(false);
  const [query, setQuery] = useState<string | null>(null);

  const html = useMemo(() => renderPromptHtml(value, mentions), [mentions, value]);
  const filteredMentions = useMemo(() => {
    if (query == null) return [];
    const normalized = query.trim().toLowerCase();
    return mentions.filter((mention) => mention.label.toLowerCase().includes(normalized) || mention.token.toLowerCase().includes(normalized));
  }, [mentions, query]);

  useLayoutEffect(() => {
    const nextValue = removeMissingMentionTokens(value, mentions);
    if (nextValue === value) return;

    onChange(nextValue);
    const editor = editorRef.current;
    if (!editor) return;
    editor.innerHTML = renderPromptHtml(nextValue, mentions);
    if (focused) placeCaretAtEnd(editor);
  }, [focused, mentions, onChange, value]);

  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || focused) return;
    if (editor.innerHTML !== html) editor.innerHTML = html;
  }, [focused, html]);

  const syncValue = useCallback(() => {
    const editor = editorRef.current;
    if (!editor) return;
    const nextValue = serializePrompt(editor);
    onChange(nextValue);
    const tail = nextValue.match(/(?:^|\s)@([^\s{}]*)$/);
    setQuery(tail ? tail[1] : null);
  }, [onChange]);

  const insertMention = useCallback((mention: PromptMentionOption) => {
    const editor = editorRef.current;
    if (!editor) return;
    const current = serializePrompt(editor);
    const next = current.replace(/(?:^|\s)@[^\s{}]*$/, (match) => (match.startsWith(" ") ? " " : "") + `${mention.token} `);
    editor.innerHTML = renderPromptHtml(next, mentions);
    onChange(next);
    setQuery(null);
    requestAnimationFrame(() => {
      if (!editorRef.current) return;
      placeCaretAtEnd(editorRef.current);
    });
  }, [mentions, onChange]);

  const handleKeyDown = useCallback((event: KeyboardEvent<HTMLDivElement>) => {
    if (disabled) return;
    if (event.key === "Escape") {
      setQuery(null);
      return;
    }
    if (event.key === "Enter" && query != null && filteredMentions[0]) {
      event.preventDefault();
      insertMention(filteredMentions[0]);
      return;
    }
    if (event.key === "Enter" && query != null) {
      event.preventDefault();
      return;
    }
    if (event.key === "Enter" && !event.shiftKey && onSubmit) {
      event.preventDefault();
      syncValue();
      onSubmit();
      return;
    }
    if (event.key === "@" && mentions.length > 0) {
      setQuery("");
    }
  }, [disabled, filteredMentions, insertMention, mentions.length, onSubmit, query, syncValue]);

  return (
    <div className="relative">
      {!value && !focused && (
        <div className="pointer-events-none absolute left-0 top-0 text-base leading-7 text-muted-gray">{placeholder}</div>
      )}
      <div
        ref={editorRef}
        contentEditable={!disabled}
        suppressContentEditableWarning
        onFocus={() => setFocused(true)}
        onBlur={() => {
          setFocused(false);
          setQuery(null);
        }}
        onInput={syncValue}
        onKeyDown={handleKeyDown}
        data-prompt-scroll-area="true"
        className={cn(
          minHeightClassName,
          "max-h-[240px] w-full cursor-text overflow-y-auto overscroll-contain whitespace-pre-wrap break-words bg-transparent pr-1 text-base leading-7 text-charcoal focus:outline-none",
          disabled && "cursor-not-allowed text-muted-gray"
        )}
        role="textbox"
        aria-multiline="true"
        aria-label="Prompt"
      />
      {query != null && (
        <div
          className="absolute left-0 top-9 z-[280] w-[252px] rounded-lg border border-border-warm bg-background p-2 shadow-lg"
          onMouseDown={(event) => event.preventDefault()}
        >
          {filteredMentions.length > 0 ? (
            filteredMentions.map((mention) => (
              <button
                key={mention.id}
                type="button"
                onClick={() => insertMention(mention)}
                className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left text-sm text-charcoal hover:bg-muted"
              >
                <span className="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-md border border-border-warm bg-muted">
                  {mention.thumbnailUrl ? (
                    <img src={mention.thumbnailUrl} alt="" className="size-full object-cover" draggable={false} />
                  ) : (
                    <ImageIcon className="size-4 text-muted-gray" />
                  )}
                </span>
                <span className="min-w-0 flex-1 truncate">{mention.label}</span>
              </button>
            ))
          ) : (
            <div className="px-2 py-3 text-sm text-muted-gray">没有可引用的连线素材</div>
          )}
        </div>
      )}
    </div>
  );
}
