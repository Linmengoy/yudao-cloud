"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Download, Loader2, RefreshCw, Search } from "lucide-react";
import { deleteMyAsset, downloadMyAsset, getMyAssetPage } from "@/features/assets/asset-api";
import { AssetPreview } from "@/features/assets/components/asset-preview";
import { AssetAuditBadge, AssetVisibilityBadge } from "@/features/assets/components/asset-status-badge";
import { getAccessToken } from "@/lib/api-client";
import {
  formatDateTime,
  formatFileSize,
  getAssetTypeLabel,
  canDownloadAsset,
} from "@/features/assets/asset-dictionaries";
import type { AigcAsset } from "@/features/assets/asset-types";
import { listGeneratedAssets, type GeneratedAsset } from "@/features/assets/asset-library";

type AssetTab = "ALL" | "IMAGE" | "VIDEO" | "OTHER";

function localToAsset(asset: GeneratedAsset): AigcAsset {
  return {
    id: Number.NaN,
    assetNo: asset.id,
    assetType: asset.kind === "image" ? "IMAGE" : "VIDEO",
    sourceType: "GENERATE",
    title: asset.title,
    description: asset.prompt,
    fileUrl: asset.url,
    thumbnailUrl: asset.kind === "image" ? asset.url : undefined,
    visibility: "PRIVATE",
    auditStatus: asset.auditStatus || "PASS",
    auditReason: asset.auditReason || undefined,
    status: "NORMAL",
    createTime: asset.createdAt,
    localProjectId: asset.projectId,
    localNodeId: asset.nodeId,
  };
}

function getAssetHref(asset: AigcAsset) {
  if (Number.isFinite(asset.id)) return `/assets/${asset.id}`;
  if (asset.localProjectId) return `/canvas?projectId=${encodeURIComponent(asset.localProjectId)}`;
  return "/projects";
}

function matchesTab(asset: AigcAsset, tab: AssetTab) {
  if (tab === "ALL") return true;
  if (tab === "OTHER") return asset.assetType !== "IMAGE" && asset.assetType !== "VIDEO";
  return asset.assetType === tab;
}

function matchesQuery(asset: AigcAsset, query: string) {
  const keyword = query.trim().toLowerCase();
  if (!keyword) return true;
  return [asset.title, asset.description, asset.assetNo, asset.tags]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function EmptyState({ tab }: { tab: AssetTab }) {
  return (
    <div className="mt-8 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
      <p className="text-sm font-medium text-charcoal">暂时没有{tab === "VIDEO" ? "生成视频" : tab === "IMAGE" ? "生成图片" : "资产"}</p>
      <p className="mt-1 text-xs text-muted-gray">在画布中生成完成后，会自动出现在这里。</p>
      <Link href="/projects" className="mt-5 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80">
        去项目库
      </Link>
    </div>
  );
}

export default function AssetsPage() {
  const [tab, setTab] = useState<AssetTab>("ALL");
  const [query, setQuery] = useState("");
  const [assets, setAssets] = useState<AigcAsset[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [usingLocalFallback, setUsingLocalFallback] = useState(false);

  const loadAssets = useCallback(async () => {
    setLoading(true);
    setError("");
    setUsingLocalFallback(false);
    try {
      const data = await getMyAssetPage({ pageNo: 1, pageSize: 60 });
      setAssets(data.list ?? []);
    } catch (err) {
      if (!getAccessToken()) {
        setAssets([]);
        setUsingLocalFallback(false);
        setError("登录已过期，请重新登录后查看资产。");
        return;
      }
      const localAssets = (await listGeneratedAssets()).map(localToAsset);
      setAssets(localAssets);
      setUsingLocalFallback(localAssets.length > 0);
      setError(localAssets.length > 0 ? "真实资产接口暂不可用，当前展示本地项目生成记录。" : err instanceof Error ? err.message : "资产列表加载失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(loadAssets, 0);
    return () => window.clearTimeout(timer);
  }, [loadAssets]);

  const filteredAssets = useMemo(() => assets.filter((asset) => matchesTab(asset, tab) && matchesQuery(asset, query)), [assets, query, tab]);
  const counts = useMemo(() => ({
    ALL: assets.length,
    IMAGE: assets.filter((asset) => asset.assetType === "IMAGE").length,
    VIDEO: assets.filter((asset) => asset.assetType === "VIDEO").length,
    OTHER: assets.filter((asset) => asset.assetType !== "IMAGE" && asset.assetType !== "VIDEO").length,
  }), [assets]);

  async function handleDownload(asset: AigcAsset) {
    if (!canDownloadAsset(asset)) return;
    if (!Number.isFinite(asset.id)) {
      if (asset.fileUrl) window.open(asset.fileUrl, "_blank", "noopener,noreferrer");
      return;
    }
    const url = await downloadMyAsset({ assetId: asset.id });
    window.open(url || asset.fileUrl, "_blank", "noopener,noreferrer");
    loadAssets();
  }

  async function handleDelete(asset: AigcAsset) {
    if (!Number.isFinite(asset.id)) return;
    if (!window.confirm("确认删除这个资产吗？")) return;
    await deleteMyAsset(asset.id);
    loadAssets();
  }

  return (
    <div className="mx-auto flex max-w-[1180px] flex-col px-6 py-10">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-charcoal">资产库</h1>
          <p className="mt-1 text-sm text-muted-gray">集中查看生成图片、生成视频和其它文件型结果。</p>
        </div>
        <button type="button" onClick={loadAssets} disabled={loading} className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50" aria-label="刷新资产列表" title="刷新资产列表">
          <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
          刷新
        </button>
      </div>

      <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="inline-flex w-fit rounded-full bg-muted p-1">
          {(["ALL", "IMAGE", "VIDEO", "OTHER"] as AssetTab[]).map((item) => (
            <button key={item} type="button" onClick={() => setTab(item)} className={`rounded-full px-4 py-2 text-sm transition-colors ${tab === item ? "bg-background text-charcoal shadow-sm" : "text-muted-gray hover:text-charcoal"}`}>
              {item === "ALL" ? "全部" : item === "IMAGE" ? "生成图片" : item === "VIDEO" ? "生成视频" : "其它"}
              <span className="ml-2 text-xs text-muted-gray">{counts[item]}</span>
            </button>
          ))}
        </div>

        <div className="flex w-full max-w-[360px] items-center gap-2 rounded-md border border-border-warm bg-background px-3 py-2">
          <Search className="size-4 text-muted-gray" />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索标题、编号或标签" className="w-full bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none" />
        </div>
      </div>

      {error && <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-muted-gray">{error}</div>}

      {loading && !assets.length ? (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载资产列表...
        </div>
      ) : filteredAssets.length === 0 ? (
        <EmptyState tab={tab} />
      ) : (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {filteredAssets.map((asset) => (
            <div key={asset.assetNo || asset.id} className="rounded-xl border border-border-warm bg-background p-3">
              <Link href={getAssetHref(asset)} className="block" aria-label={`查看资产 ${asset.title || asset.assetNo || asset.id}`}>
                <AssetPreview asset={asset} />
              </Link>
              <div className="mt-3 min-w-0">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-charcoal">{asset.title || asset.assetNo || "未命名资产"}</p>
                    <p className="mt-1 text-xs text-muted-gray">{getAssetTypeLabel(asset.assetType)} · {formatFileSize(asset.fileSize)}</p>
                  </div>
                  <button type="button" onClick={() => handleDownload(asset)} disabled={!canDownloadAsset(asset)} className="inline-flex size-8 shrink-0 items-center justify-center rounded-full border border-border-warm text-charcoal hover:bg-muted disabled:opacity-40" aria-label="下载资产" title={canDownloadAsset(asset) ? "下载资产" : "审核通过后可下载"}>
                    <Download className="size-4" />
                  </button>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  <AssetAuditBadge status={asset.auditStatus} />
                  <AssetVisibilityBadge visibility={asset.visibility} />
                </div>
                <div className="mt-3 flex items-center justify-between gap-3 text-xs text-muted-gray">
                  <span>{formatDateTime(asset.createTime)}</span>
                  {Number.isFinite(asset.id) ? <button type="button" onClick={() => handleDelete(asset)} className="text-destructive hover:underline">删除</button> : usingLocalFallback ? <Link href={getAssetHref(asset)} className="text-charcoal hover:underline">打开项目</Link> : null}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
