import { useCallback, useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";

type EditableNodeTitleProps = {
  value?: string | null;
  fallback: string;
  onCommit: (value: string) => void;
  className?: string;
};

export function EditableNodeTitle({ value, fallback, onCommit, className }: EditableNodeTitleProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const title = value?.trim() || fallback;
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(title);

  useEffect(() => {
    if (!editing) return;
    const frame = window.requestAnimationFrame(() => {
      inputRef.current?.focus();
      inputRef.current?.select();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [editing]);

  const commit = useCallback(() => {
    const nextTitle = draft.trim() || fallback;
    setEditing(false);
    if (nextTitle !== title) onCommit(nextTitle);
  }, [draft, fallback, onCommit, title]);

  if (editing) {
    return (
      <input
        ref={inputRef}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={commit}
        onPointerDown={(event) => event.stopPropagation()}
        onDoubleClick={(event) => event.stopPropagation()}
        onKeyDown={(event) => {
          if (event.key === "Enter") commit();
          if (event.key === "Escape") {
            setDraft(title);
            setEditing(false);
          }
        }}
        className="nodrag nowheel pointer-events-auto min-w-0 flex-1 rounded bg-background/90 px-1 py-0 text-sm font-medium text-charcoal outline-none ring-1 ring-charcoal/25"
      />
    );
  }

  return (
    <span
      className={cn(
        "nodrag nowheel pointer-events-auto min-w-0 cursor-text rounded px-1 transition-colors hover:bg-muted hover:text-charcoal",
        className ?? "line-clamp-1"
      )}
      title={title}
      onPointerDown={(event) => event.stopPropagation()}
      onDoubleClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        setDraft(title);
        setEditing(true);
      }}
    >
      {title}
    </span>
  );
}
