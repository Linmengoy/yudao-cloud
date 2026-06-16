"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, CalendarDays, History, RefreshCw } from "lucide-react";
import { getFallbackVersion, getPublishedReleaseNotes } from "@/features/release-notes/release-note-api";
import type { ReleaseNote } from "@/features/release-notes/release-note-types";
import { cn } from "@/lib/utils";

function formatReleaseDate(value?: string | null) {
  if (!value) return "";
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

function splitContent(content?: string | null) {
  return (content ?? "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

export default function ReleaseNotesPage() {
  const [notes, setNotes] = useState<ReleaseNote[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getPublishedReleaseNotes(30)
      .then((data) => {
        if (cancelled) return;
        setNotes(data);
        setSelectedId(data[0]?.id ?? null);
        setError(null);
      })
      .catch(() => {
        if (!cancelled) setError("版本记录暂时不可用");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const selectedNote = useMemo(() => {
    if (notes.length === 0) return null;
    return notes.find((note) => note.id === selectedId) ?? notes[0];
  }, [notes, selectedId]);

  return (
    <main className="min-h-full bg-background px-5 py-6 text-charcoal md:px-8">
      <div className="mx-auto flex max-w-6xl flex-col gap-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="min-w-0">
            <Link href="/app" className="inline-flex items-center gap-2 text-sm text-muted-gray hover:text-charcoal">
              <ArrowLeft className="size-4" />
              返回工作台
            </Link>
            <h1 className="mt-3 text-2xl font-semibold">版本更新</h1>
          </div>
          <div className="inline-flex items-center gap-2 rounded-lg border border-border-warm px-3 py-2 text-sm text-muted-gray">
            <RefreshCw className="size-4" />
            {selectedNote?.version ?? getFallbackVersion()}
          </div>
        </div>

        {loading ? (
          <div className="rounded-lg border border-border-warm bg-muted/40 p-8 text-sm text-muted-gray">正在加载版本更新记录...</div>
        ) : notes.length === 0 ? (
          <div className="rounded-lg border border-border-warm bg-muted/40 p-8">
            <p className="text-base font-medium">暂无更新记录</p>
            <p className="mt-2 text-sm text-muted-gray">{error ?? "发布记录上线后会显示在这里。"}</p>
          </div>
        ) : (
          <div className="grid gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">
            <aside className="rounded-lg border border-border-warm bg-background p-2">
              {notes.map((note) => (
                <button
                  key={note.id}
                  type="button"
                  onClick={() => setSelectedId(note.id)}
                  className={cn(
                    "flex w-full flex-col gap-1 rounded-md px-3 py-3 text-left transition-colors",
                    selectedNote?.id === note.id ? "bg-muted text-charcoal" : "text-muted-gray hover:bg-muted/70 hover:text-charcoal"
                  )}
                >
                  <span className="text-sm font-medium">{note.version}</span>
                  <span className="text-xs">{formatReleaseDate(note.releaseDate)}</span>
                </button>
              ))}
            </aside>

            {selectedNote && (
              <article className="rounded-lg border border-border-warm bg-background p-5 md:p-7">
                <div className="flex flex-wrap items-center gap-3 text-sm text-muted-gray">
                  <span className="inline-flex items-center gap-1.5">
                    <History className="size-4" />
                    {selectedNote.version}
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    <CalendarDays className="size-4" />
                    {formatReleaseDate(selectedNote.releaseDate)}
                  </span>
                </div>
                <h2 className="mt-4 text-xl font-semibold">{selectedNote.title}</h2>
                {selectedNote.summary && (
                  <p className="mt-3 text-sm leading-6 text-muted-gray">{selectedNote.summary}</p>
                )}
                <div className="mt-6 space-y-2">
                  {splitContent(selectedNote.content).length > 0 ? (
                    splitContent(selectedNote.content).map((line, index) => (
                      <p key={`${line}-${index}`} className="rounded-md bg-muted/50 px-3 py-2 text-sm leading-6">
                        {line}
                      </p>
                    ))
                  ) : (
                    <p className="text-sm text-muted-gray">本次更新暂无详细内容。</p>
                  )}
                </div>
              </article>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
