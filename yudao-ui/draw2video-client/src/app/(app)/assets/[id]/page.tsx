"use client";

import { use, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Download, Loader2, RefreshCw, Save, Trash2 } from "lucide-react";
import { deleteMyAsset, downloadMyAsset, getMyAsset, updateMyAsset, updateMyAssetVisibility } from "@/features/assets/asset-api";
import { AssetPreview } from "@/features/assets/components/asset-preview";
import { AssetAuditBadge, AssetVisibilityBadge } from "@/features/assets/components/asset-status-badge";
import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyInlineNotice } from "@/features/safety/safety-ui";
import {
  formatDateTime,
  formatDuration,
  formatFileSize,
  getAssetTypeLabel,
  getAssetVisibilityLabel,
  canDownloadAsset,
  canPublishAsset,
} from "@/features/assets/asset-dictionaries";
import type { AigcAsset } from "@/features/assets/asset-types";

export default function AssetDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [asset, setAsset] = useState<AigcAsset | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [tags, setTags] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const loadAsset = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getMyAsset(id);
      setAsset(data);
      setTitle(data.title || "");
      setDescription(data.description || "");
      setTags(data.tags || "");
    } catch (err) {
      setError(err instanceof Error ? err.message : "资产详情加载失败");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    const timer = window.setTimeout(loadAsset, 0);
    return () => window.clearTimeout(timer);
  }, [loadAsset]);

  async function handleSave() {
    setSaving(true);
    try {
      await updateMyAsset({ id, title, description, tags });
      await loadAsset();
    } finally {
      setSaving(false);
    }
  }

  async function handleVisibilityChange(visibility: string) {
    const currentVisibility = asset?.visibility || "PRIVATE";
    if (asset && visibility !== "PRIVATE" && !canPublishAsset(asset)) return;
    const isExpanding = currentVisibility === "PRIVATE" && visibility !== "PRIVATE";
    if (isExpanding && !window.confirm(`确认将资产设置为「${getAssetVisibilityLabel(visibility)}」吗？`)) return;
    await updateMyAssetVisibility({ id, visibility });
    await loadAsset();
  }

  async function handleDownload() {
    if (!asset) return;
    if (!canDownloadAsset(asset)) return;
    const url = await downloadMyAsset({ assetId: id });
    window.open(url || asset.fileUrl, "_blank", "noopener,noreferrer");
    await loadAsset();
  }

  async function handleDelete() {
    if (!window.confirm("确认删除这个资产吗？")) return;
    await deleteMyAsset(id);
    window.location.href = "/assets";
  }

  return (
    <div className="mx-auto max-w-[1200px] px-4 py-8">
      <Link href="/assets" className="inline-flex items-center gap-1 text-sm text-muted-gray hover:text-charcoal">
        <ArrowLeft className="size-4" />
        返回资产库
      </Link>

      {loading && (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载资产详情...
        </div>
      )}

      {error && <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-destructive">{error}</div>}

      {asset && (
        <div className="mt-5 grid gap-6 lg:grid-cols-[1fr_340px]">
          <div className="flex flex-col gap-4">
            <AssetPreview asset={asset} large />
            <AssetSafetyNotice asset={asset} />
            <div className="rounded-xl border border-border-warm bg-background p-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="text-sm text-charcoal">
                  标题
                  <input value={title} onChange={(event) => setTitle(event.target.value)} className="input-base mt-1" />
                </label>
                <label className="text-sm text-charcoal">
                  标签
                  <input value={tags} onChange={(event) => setTags(event.target.value)} className="input-base mt-1" placeholder="用逗号分隔" />
                </label>
              </div>
              <label className="mt-3 block text-sm text-charcoal">
                描述
                <textarea value={description} onChange={(event) => setDescription(event.target.value)} className="input-base mt-1 min-h-24 resize-y" />
              </label>
              <button type="button" onClick={handleSave} disabled={saving} className="mt-4 inline-flex items-center gap-2 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80 disabled:opacity-50">
                <Save className="size-4" />
                保存
              </button>
            </div>
          </div>

          <div className="h-fit rounded-xl border border-border-warm bg-background p-4">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <h1 className="truncate text-base font-semibold tracking-[-0.2px] text-charcoal">{asset.title || asset.assetNo || `资产 #${asset.id}`}</h1>
                <p className="mt-1 text-xs text-muted-gray">{getAssetTypeLabel(asset.assetType)}</p>
              </div>
              <button type="button" onClick={loadAsset} aria-label="刷新资产详情" title="刷新资产详情" className="inline-flex size-8 items-center justify-center rounded-full border border-border-warm text-charcoal hover:bg-muted">
                <RefreshCw className="size-4" />
              </button>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              <AssetAuditBadge status={asset.auditStatus} />
              <AssetVisibilityBadge visibility={asset.visibility} />
            </div>
            <div className="mt-4 grid gap-3 text-sm">
              <Meta label="资产编号" value={asset.assetNo || "-"} />
              <Meta label="文件大小" value={formatFileSize(asset.fileSize)} />
              <Meta label="文件格式" value={asset.fileExt || asset.mimeType || "-"} />
              <Meta label="尺寸" value={asset.width && asset.height ? `${asset.width} × ${asset.height}` : "-"} />
              <Meta label="时长" value={formatDuration(asset.duration)} />
              <Meta label="下载次数" value={String(asset.downloadCount ?? 0)} />
              <Meta label="使用次数" value={String(asset.useCount ?? 0)} />
              <Meta label="创建时间" value={formatDateTime(asset.createTime)} />
            </div>
            <label className="mt-4 block text-sm text-charcoal">
              可见性
              <select value={asset.visibility || "PRIVATE"} onChange={(event) => handleVisibilityChange(event.target.value)} className="input-base mt-1">
                {(["PRIVATE", "PUBLIC", "LINK", "TENANT"] as const).map((item) => <option key={item} value={item}>{getAssetVisibilityLabel(item)}</option>)}
              </select>
              {!canPublishAsset(asset) && <span className="mt-1 block text-xs text-muted-gray">审核通过且状态正常后才能公开资产。</span>}
            </label>
            <div className="mt-4 flex flex-wrap gap-2">
              <button type="button" onClick={handleDownload} disabled={!canDownloadAsset(asset)} className="inline-flex items-center gap-2 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80 disabled:opacity-50">
                <Download className="size-4" />
                下载
              </button>
              <button type="button" onClick={handleDelete} className="inline-flex items-center gap-2 rounded-md border border-border-warm px-4 py-2 text-sm text-destructive hover:bg-muted active:opacity-80">
                <Trash2 className="size-4" />
                删除
              </button>
            </div>
            {asset.taskId && <Link href={`/tasks/${asset.taskId}`} className="mt-4 inline-flex text-sm text-charcoal underline underline-offset-4">查看来源任务</Link>}
          </div>
        </div>
      )}
    </div>
  );
}

function AssetSafetyNotice({ asset }: { asset: AigcAsset }) {
  const safety = getSafetyCopy(asset.auditStatus, "asset");
  if (safety.status === "idle" || safety.status === "available") return null;
  return <SafetyInlineNotice state={{ ...safety, description: asset.auditReason || safety.description }} />;
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-xs text-muted-gray">{label}</span>
      <span className="truncate text-sm text-charcoal">{value}</span>
    </div>
  );
}
