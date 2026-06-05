"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Archive, RotateCcw, Search } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProjectRecycleBinRecord } from "@/features/canvas/types";

const PAGE_SIZE = 12;

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ProjectRecycleBinPage() {
  const [records, setRecords] = useState<CanvasProjectRecycleBinRecord[]>([]);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [restoringProjectId, setRestoringProjectId] = useState<number | null>(null);
  const [statusMessage, setStatusMessage] = useState("");

  const pageCount = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);

  const refreshRecycleBin = useCallback(async (search = debouncedQuery, nextPageNo = pageNo) => {
    setIsLoading(true);
    try {
      const page = await canvasApi.listProjectRecycleBin({
        pageNo: nextPageNo,
        pageSize: PAGE_SIZE,
        name: search.trim() || undefined,
      });
      const nextPageCount = Math.max(1, Math.ceil(page.total / PAGE_SIZE));
      if (nextPageNo > nextPageCount) {
        setPageNo(nextPageCount);
        return;
      }
      setRecords(page.list);
      setTotal(page.total);
      setStatusMessage("");
    } catch (error) {
      setRecords([]);
      setTotal(0);
      setStatusMessage(error instanceof Error ? error.message : "回收站加载失败，请稍后再试。");
    } finally {
      setIsLoading(false);
    }
  }, [debouncedQuery, pageNo]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), 350);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const timer = window.setTimeout(() => refreshRecycleBin(debouncedQuery, pageNo), 0);
    return () => window.clearTimeout(timer);
  }, [debouncedQuery, pageNo, refreshRecycleBin]);

  async function handleRestore(record: CanvasProjectRecycleBinRecord) {
    if (restoringProjectId) return;
    const confirmed = window.confirm(`恢复项目「${record.projectName}」？`);
    if (!confirmed) return;
    setRestoringProjectId(record.projectId);
    try {
      await canvasApi.restoreProject(record.projectId);
      setStatusMessage("项目已恢复，可在项目库中继续打开。");
      await refreshRecycleBin();
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : "恢复失败，请稍后再试。");
    } finally {
      setRestoringProjectId(null);
    }
  }

  return (
    <div className="mx-auto flex max-w-[1180px] flex-col px-6 py-10">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <Link href="/projects" className="inline-flex items-center gap-2 text-sm text-muted-gray hover:text-charcoal">
            <ArrowLeft className="size-4" />
            返回项目库
          </Link>
          <h1 className="mt-4 text-2xl font-semibold tracking-tight text-charcoal">项目回收站</h1>
          <p className="mt-1 text-sm text-muted-gray">删除后的服务端项目会暂存在这里，恢复后可重新进入画布。</p>
        </div>
      </div>

      <div className="mt-8 flex max-w-[420px] items-center gap-2 rounded-lg border border-border-warm bg-background px-3 py-2">
        <Search className="size-4 text-muted-gray" />
        <input
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setPageNo(1);
          }}
          placeholder="搜索回收站项目"
          className="w-full bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
        />
      </div>

      {statusMessage && (
        <div className="mt-4 rounded-lg border border-border-warm bg-muted px-3 py-2 text-xs text-muted-gray">
          {statusMessage}
        </div>
      )}

      {isLoading ? (
        <div className="mt-16 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
          <Archive className="size-8 text-muted-gray" />
          <p className="mt-4 text-sm font-medium text-charcoal">正在加载回收站</p>
          <p className="mt-1 text-xs text-muted-gray">从云端同步已删除项目。</p>
        </div>
      ) : records.length === 0 ? (
        <div className="mt-16 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
          <Archive className="size-8 text-muted-gray" />
          <p className="mt-4 text-sm font-medium text-charcoal">{query ? "没有匹配的回收站项目" : "回收站为空"}</p>
          <p className="mt-1 text-xs text-muted-gray">删除服务端项目后，可以在这里恢复。</p>
        </div>
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {records.map((record) => (
            <div key={record.id} className="rounded-xl border border-border-warm bg-background p-4 transition-colors hover:border-[rgba(28,28,28,0.4)]">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-charcoal">{record.projectName}</p>
                  <p className="mt-1 text-xs text-muted-gray">删除于 {formatDate(record.deletedTime)}</p>
                </div>
                <Archive className="size-5 shrink-0 text-muted-gray" />
              </div>
              <div className="mt-5 grid grid-cols-2 gap-2 text-xs text-muted-gray">
                <div className="rounded-lg bg-muted px-3 py-2">
                  <p>{record.nodeCount} 节点</p>
                </div>
                <div className="rounded-lg bg-muted px-3 py-2">
                  <p>{record.assetCount} 素材</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => handleRestore(record)}
                disabled={restoringProjectId === record.projectId}
                className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-charcoal px-4 py-2 text-sm font-medium text-off-white disabled:cursor-not-allowed disabled:opacity-60"
              >
                <RotateCcw className="size-4" />
                {restoringProjectId === record.projectId ? "恢复中" : "恢复项目"}
              </button>
            </div>
          ))}
        </div>
      )}

      {!isLoading && total > PAGE_SIZE && (
        <div className="mt-8 flex items-center justify-between border-t border-border-warm pt-4 text-sm text-muted-gray">
          <span>
            第 {pageNo} / {pageCount} 页 · 共 {total} 个项目
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={pageNo <= 1}
              onClick={() => setPageNo((value) => Math.max(1, value - 1))}
              className="rounded-lg border border-border-warm px-3 py-1.5 transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
            >
              上一页
            </button>
            <button
              type="button"
              disabled={pageNo >= pageCount}
              onClick={() => setPageNo((value) => Math.min(pageCount, value + 1))}
              className="rounded-lg border border-border-warm px-3 py-1.5 transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
            >
              下一页
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
