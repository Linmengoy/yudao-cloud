"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type TouchEvent } from "react";
import Link from "next/link";
import { Loader2, RefreshCw, Search } from "lucide-react";
import { deleteMyAsset, downloadMyAsset, getMyAssetCategoryCounts, getMyAssetPage, updateMyAsset, updateMyAssetVisibility } from "@/features/assets/asset-api";
import { getAccessToken } from "@/lib/api-client";
import {
  canDownloadAsset,
  canPublishAsset,
  formatDateTime,
  formatDuration,
  formatFileSize,
  getAssetPreviewUrl,
  getAssetTypeLabel,
  getAssetVisibilityLabel,
} from "@/features/assets/asset-dictionaries";
import type { AigcAsset } from "@/features/assets/asset-types";
import { listGeneratedAssets, type GeneratedAsset } from "@/features/assets/asset-library";
import { useAuth } from "@/features/auth/auth-store";
import { MediaPreviewDialog } from "@/features/media-preview/MediaPreviewDialog";
import { PreviewVideoPlayer } from "@/features/media-preview/PreviewVideoPlayer";
import type { MediaPreviewItem } from "@/features/media-preview/types";
import { mergeStableList, readPageCache, writePageCache } from "@/lib/page-cache";
import type Muuri from "muuri";
import type { Item, LayoutFunctionCallback } from "muuri";

type AssetTab = "ALL" | "GENERATED_IMAGE" | "UPLOADED_IMAGE" | "VIDEO" | "OTHER";
type AssetTabCounts = Record<AssetTab, number>;

const MIN_ASSET_COLUMN_WIDTH = 220;
const PAGE_SIZE = 30;
const EMPTY_COUNTS: AssetTabCounts = {
  ALL: 0,
  GENERATED_IMAGE: 0,
  UPLOADED_IMAGE: 0,
  VIDEO: 0,
  OTHER: 0,
};

type AssetsPageCache = {
  assets: AigcAsset[];
  total: number;
  counts: AssetTabCounts;
  hasMore: boolean;
  nextPageNo: number;
};

function assetsPageCacheKey(ownerKey: string | number | null | undefined, tab: AssetTab, query: string) {
  return `assets:${ownerKey ?? "current"}:${tab}:${query.trim()}`;
}

function writeAssetsPageCache(
  ownerKey: string | number | null | undefined,
  tab: AssetTab,
  query: string,
  assets: AigcAsset[],
  total: number,
  counts: AssetTabCounts,
  hasMore: boolean,
  nextPageNo: number
) {
  writePageCache<AssetsPageCache>(assetsPageCacheKey(ownerKey, tab, query), {
    assets,
    total,
    counts,
    hasMore,
    nextPageNo,
  });
}

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

function isGeneratedImage(asset: AigcAsset) {
  return asset.assetType === "IMAGE" && asset.sourceType === "GENERATE";
}

function isUploadedImage(asset: AigcAsset) {
  return asset.assetType === "IMAGE" && asset.sourceType === "UPLOAD";
}

function matchesTab(asset: AigcAsset, tab: AssetTab) {
  if (tab === "ALL") return true;
  if (tab === "GENERATED_IMAGE") return isGeneratedImage(asset);
  if (tab === "UPLOADED_IMAGE") return isUploadedImage(asset);
  if (tab === "OTHER") return asset.assetType !== "IMAGE" && asset.assetType !== "VIDEO";
  return asset.assetType === tab;
}

function buildLocalAssetCounts(assets: AigcAsset[], query: string): AssetTabCounts {
  const matchedAssets = assets.filter((asset) => matchesQuery(asset, query));
  return {
    ALL: matchedAssets.length,
    GENERATED_IMAGE: matchedAssets.filter(isGeneratedImage).length,
    UPLOADED_IMAGE: matchedAssets.filter(isUploadedImage).length,
    VIDEO: matchedAssets.filter((asset) => asset.assetType === "VIDEO").length,
    OTHER: matchedAssets.filter((asset) => asset.assetType !== "IMAGE" && asset.assetType !== "VIDEO").length,
  };
}

function decrementAssetCounts(counts: AssetTabCounts, asset: AigcAsset): AssetTabCounts {
  const nextCounts = { ...counts, ALL: Math.max(0, counts.ALL - 1) };
  if (isGeneratedImage(asset)) {
    nextCounts.GENERATED_IMAGE = Math.max(0, nextCounts.GENERATED_IMAGE - 1);
  } else if (isUploadedImage(asset)) {
    nextCounts.UPLOADED_IMAGE = Math.max(0, nextCounts.UPLOADED_IMAGE - 1);
  } else if (asset.assetType === "VIDEO") {
    nextCounts.VIDEO = Math.max(0, nextCounts.VIDEO - 1);
  } else {
    nextCounts.OTHER = Math.max(0, nextCounts.OTHER - 1);
  }
  return nextCounts;
}

function getAssetTabQuery(tab: AssetTab) {
  if (tab === "GENERATED_IMAGE") return { assetType: "IMAGE", sourceType: "GENERATE" };
  if (tab === "UPLOADED_IMAGE") return { assetType: "IMAGE", sourceType: "UPLOAD" };
  if (tab === "VIDEO") return { assetType: "VIDEO", sourceType: undefined, category: undefined };
  if (tab === "OTHER") return { assetType: undefined, sourceType: undefined, category: "OTHER" };
  return { assetType: undefined, sourceType: undefined, category: undefined };
}

function getAssetTabLabel(tab: AssetTab) {
  if (tab === "ALL") return "全部";
  if (tab === "GENERATED_IMAGE") return "生成图片";
  if (tab === "UPLOADED_IMAGE") return "上传图片";
  if (tab === "VIDEO") return "生成视频";
  return "其它";
}

function matchesQuery(asset: AigcAsset, query: string) {
  const keyword = query.trim().toLowerCase();
  if (!keyword) return true;
  return [asset.title, asset.description, asset.assetNo, asset.tags]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function getFullAssetPreviewUrl(asset: AigcAsset) {
  return asset.fileUrl || getAssetPreviewUrl(asset);
}

function getAssetKey(asset: AigcAsset) {
  return String(asset.assetNo || asset.id);
}

function isMediaAsset(asset: AigcAsset) {
  return Boolean(getFullAssetPreviewUrl(asset) && (asset.assetType === "IMAGE" || asset.assetType === "VIDEO"));
}

type AssetDraft = {
  id: number | string;
  assetKey: string;
  title: string;
  description: string;
  tags: string;
  saving: boolean;
};

function assetToMediaPreview(
  asset: AigcAsset,
  draft: AssetDraft | null
): MediaPreviewItem | null {
  const url = getFullAssetPreviewUrl(asset);
  if (!url) return null;
  if (asset.assetType !== "IMAGE" && asset.assetType !== "VIDEO") return null;
  const currentDraft = draft?.assetKey === getAssetKey(asset) ? draft : null;
  return {
    kind: asset.assetType === "VIDEO" ? "video" : "image",
    url,
    title: currentDraft?.title || asset.title || asset.assetNo || "Asset",
    fileName: currentDraft?.title || asset.title || asset.assetNo,
    createdAt: asset.createTime,
    information: [
      { label: "资产编号", value: asset.assetNo || "-" },
      { label: "文件大小", value: formatFileSize(asset.fileSize) },
      { label: "文件格式", value: asset.fileExt || asset.mimeType || "-" },
      { label: "尺寸", value: asset.width && asset.height ? `${asset.width} x ${asset.height}` : "-" },
      { label: "时长", value: formatDuration(asset.duration) },
      { label: "下载次数", value: String(asset.downloadCount ?? 0) },
      { label: "使用次数", value: String(asset.useCount ?? 0) },
      { label: "创建时间", value: formatDateTime(asset.createTime) },
    ],
    editableAsset: {
      title: currentDraft?.title ?? asset.title ?? "",
      description: currentDraft?.description ?? asset.description ?? "",
      tags: currentDraft?.tags ?? asset.tags ?? "",
      visibility: asset.visibility,
      auditStatus: asset.auditStatus,
      status: asset.status,
      auditReason: asset.auditReason,
      taskId: asset.taskId,
      saving: currentDraft?.saving ?? false,
      canEdit: Number.isFinite(asset.id),
      canDownload: canDownloadAsset(asset),
      canDelete: Number.isFinite(asset.id),
      canPublish: canPublishAsset(asset),
      onChange: () => {},
      onSave: () => {},
      onVisibilityChange: () => {},
      onDownload: () => {},
      onDelete: () => {},
    },
  };
}

function getAssetSpan(asset: AigcAsset, measuredRatio?: number) {
  const ratio = measuredRatio || (asset.width && asset.height ? asset.width / asset.height : 1);
  if (ratio >= 1.45) return 2;
  return 1;
}

function getColumnCount(width: number) {
  return Math.max(1, Math.floor(width / MIN_ASSET_COLUMN_WIDTH));
}

function assetWallLayout(_grid: Muuri, id: number, items: Item[], width: number, _height: number, callback: LayoutFunctionCallback) {
  const columnCount = getColumnCount(width);
  const columnWidth = width / columnCount;
  const columnHeights = Array.from({ length: columnCount }, () => 0);
  const slots: number[] = [];

  items.forEach((item) => {
    const element = item.getElement();
    const requestedSpan = Number(element?.dataset.span || 1);
    const span = Math.max(1, Math.min(columnCount, requestedSpan));
    const itemWidth = columnWidth * span;
    if (element) element.style.width = `${itemWidth}px`;

    let bestColumn = 0;
    let bestTop = Number.POSITIVE_INFINITY;
    for (let column = 0; column <= columnCount - span; column += 1) {
      const top = Math.max(...columnHeights.slice(column, column + span));
      if (top < bestTop) {
        bestTop = top;
        bestColumn = column;
      }
    }

    const left = bestColumn * columnWidth;
    const top = Number.isFinite(bestTop) ? bestTop : 0;
    const itemHeight = item.getHeight();
    for (let column = bestColumn; column < bestColumn + span; column += 1) {
      columnHeights[column] = top + itemHeight;
    }
    slots.push(left, top);
  });

  callback({
    id,
    items,
    slots,
    styles: {
      height: `${Math.max(0, ...columnHeights)}px`,
    },
  });
}

function AssetWallPreview({ asset, onLoad }: { asset: AigcAsset; onLoad: (ratio?: number) => void }) {
  const previewUrl = getFullAssetPreviewUrl(asset);
  if (asset.assetType === "IMAGE" && previewUrl) {
    return <img src={previewUrl} alt={asset.title || "图片资产预览"} className="block h-auto w-full" loading="lazy" onLoad={(event) => onLoad(event.currentTarget.naturalWidth / event.currentTarget.naturalHeight)} />;
  }
  if (asset.assetType === "VIDEO" && asset.fileUrl) {
    return (
      <PreviewVideoPlayer
        src={asset.fileUrl}
        poster={asset.coverUrl || asset.thumbnailUrl}
        className="w-full bg-charcoal"
        videoClassName="block h-auto w-full max-w-none"
        onLoadedMetadata={(video) => onLoad(video.videoWidth / video.videoHeight)}
        playOnHover={false}
        clickToToggle={false}
        controlsInteractive={false}
      />
    );
  }
  return (
    <div className="flex min-h-[180px] w-full items-center justify-center bg-muted text-sm text-muted-gray">
      {getAssetTypeLabel(asset.assetType)}
    </div>
  );
}

function EmptyState({ tab }: { tab: AssetTab }) {
  return (
    <div className="mt-8 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
      <p className="text-sm font-medium text-charcoal">暂时没有{tab === "ALL" ? "资产" : getAssetTabLabel(tab)}</p>
      <p className="mt-1 text-xs text-muted-gray">在画布中生成或上传完成后，会自动出现在这里。</p>
      <Link href="/projects" className="mt-5 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80">
        去项目库
      </Link>
    </div>
  );
}

export default function AssetsPage() {
  const { user } = useAuth();
  const initialPageCache = useMemo(
    () => readPageCache<AssetsPageCache>(assetsPageCacheKey(user?.id, "ALL", "")),
    [user?.id]
  );
  const [tab, setTab] = useState<AssetTab>("ALL");
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [assets, setAssets] = useState<AigcAsset[]>(() => initialPageCache?.assets ?? []);
  const [loading, setLoading] = useState(() => !initialPageCache);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [measuredRatios, setMeasuredRatios] = useState<Record<string, number>>({});
  const [pullDistance, setPullDistance] = useState(0);
  const [total, setTotal] = useState(() => initialPageCache?.total ?? 0);
  const [counts, setCounts] = useState<AssetTabCounts>(() => initialPageCache?.counts ?? EMPTY_COUNTS);
  const [previewAssetKey, setPreviewAssetKey] = useState<string | null>(null);
  const [assetDraft, setAssetDraft] = useState<AssetDraft | null>(null);
  const [hasMore, setHasMore] = useState(() => initialPageCache?.hasMore ?? true);
  const [pageCursor, setPageCursor] = useState(() => initialPageCache?.nextPageNo ?? 1);
  const gridElementRef = useRef<HTMLDivElement | null>(null);
  const loadMoreElementRef = useRef<HTMLDivElement | null>(null);
  const muuriRef = useRef<Muuri | null>(null);
  const touchStartYRef = useRef<number | null>(null);
  const loadingPageRef = useRef(false);
  const pageNoRef = useRef(initialPageCache?.nextPageNo ?? 1);
  const hasMoreRef = useRef(initialPageCache?.hasMore ?? true);
  const assetsLengthRef = useRef(initialPageCache?.assets.length ?? 0);
  const countsRef = useRef<AssetTabCounts>(initialPageCache?.counts ?? EMPTY_COUNTS);

  const loadAssets = useCallback(async (reset = false) => {
    if (loadingPageRef.current) return;
    if (!reset && !hasMoreRef.current) return;
    loadingPageRef.current = true;
    const nextPageNo = reset ? 1 : pageNoRef.current;
    const cacheKey = assetsPageCacheKey(user?.id, tab, debouncedQuery);
    const cached = reset ? readPageCache<AssetsPageCache>(cacheKey) : null;
    const requestedPageCount = reset && cached ? Math.max(1, cached.nextPageNo - 1) : 1;
    const requestPageSize = reset ? PAGE_SIZE * requestedPageCount : PAGE_SIZE;
    if (cached) {
      setAssets((items) => mergeStableList(items, cached.assets, getAssetKey));
      setTotal(cached.total);
      setCounts(cached.counts);
      countsRef.current = cached.counts;
      setHasMore(cached.hasMore);
      setPageCursor(cached.nextPageNo);
      pageNoRef.current = cached.nextPageNo;
      hasMoreRef.current = cached.hasMore;
      assetsLengthRef.current = cached.assets.length;
      setLoading(false);
    } else {
      setLoading(reset);
    }
    setLoadingMore(!reset);
    setError("");
    if (reset) {
      if (!cached) {
        pageNoRef.current = 1;
        hasMoreRef.current = true;
        assetsLengthRef.current = 0;
        setAssets([]);
        setMeasuredRatios({});
        setTotal(0);
        setCounts(EMPTY_COUNTS);
        countsRef.current = EMPTY_COUNTS;
        setHasMore(true);
        setPageCursor(1);
      }
    }
    try {
      const tabQuery = getAssetTabQuery(tab);
      const [data, categoryCounts] = await Promise.all([
        getMyAssetPage({
          pageNo: nextPageNo,
          pageSize: requestPageSize,
          assetType: tabQuery.assetType,
          category: tabQuery.category,
          sourceType: tabQuery.sourceType,
          title: debouncedQuery.trim() || undefined,
        }),
        reset
          ? getMyAssetCategoryCounts({ title: debouncedQuery.trim() || undefined })
          : Promise.resolve(null),
      ]);
      const nextList = data.list ?? [];
      const nextTotal = data.total ?? 0;
      const currentLength = reset ? 0 : assetsLengthRef.current;
      const mergedLength = currentLength + nextList.length;
      const nextPageCursor = reset ? requestedPageCount + 1 : nextPageNo + 1;
      let nextCounts = countsRef.current;
      if (categoryCounts) {
        nextCounts = {
          ALL: categoryCounts.allCount ?? 0,
          GENERATED_IMAGE: categoryCounts.generatedImageCount ?? 0,
          UPLOADED_IMAGE: categoryCounts.uploadedImageCount ?? 0,
          VIDEO: categoryCounts.videoCount ?? 0,
          OTHER: categoryCounts.otherCount ?? 0,
        };
        countsRef.current = nextCounts;
        setCounts(nextCounts);
      }
      const nextHasMore = nextList.length > 0 && mergedLength < nextTotal;
      setAssets((items) => {
        const mergedAssets = reset
          ? mergeStableList(items, nextList, getAssetKey)
          : mergeStableList(items, [...items, ...nextList], getAssetKey);
        writeAssetsPageCache(user?.id, tab, debouncedQuery, mergedAssets, nextTotal, nextCounts, nextHasMore, nextPageCursor);
        return mergedAssets;
      });
      setTotal(nextTotal);
      pageNoRef.current = nextPageCursor;
      setPageCursor(nextPageCursor);
      assetsLengthRef.current = mergedLength;
      hasMoreRef.current = nextHasMore;
      setHasMore(hasMoreRef.current);
    } catch (err) {
      if (!getAccessToken()) {
        setAssets([]);
        setTotal(0);
        setCounts(EMPTY_COUNTS);
        setHasMore(false);
        setPageCursor(1);
        hasMoreRef.current = false;
        assetsLengthRef.current = 0;
        setError("登录已过期，请重新登录后查看资产。");
        return;
      }
      const localAssets = (await listGeneratedAssets(user?.id)).map(localToAsset);
      const localCounts = buildLocalAssetCounts(localAssets, debouncedQuery);
      writeAssetsPageCache(user?.id, tab, debouncedQuery, localAssets, localAssets.length, localCounts, false, 2);
      setAssets((items) => mergeStableList(items, localAssets, getAssetKey));
      setTotal(localAssets.length);
      setCounts(localCounts);
      countsRef.current = localCounts;
      setHasMore(false);
      hasMoreRef.current = false;
      pageNoRef.current = 2;
      setPageCursor(2);
      assetsLengthRef.current = localAssets.length;
      setError(localAssets.length > 0 ? "真实资产接口暂不可用，当前展示本地项目生成记录。" : err instanceof Error ? err.message : "资产列表加载失败");
    } finally {
      setLoading(false);
      setLoadingMore(false);
      setPullDistance(0);
      loadingPageRef.current = false;
    }
  }, [debouncedQuery, tab, user?.id]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadAssets(true), 0);
    return () => window.clearTimeout(timer);
  }, [loadAssets]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), 400);
    return () => window.clearTimeout(timer);
  }, [query]);

  const filteredAssets = useMemo(() => assets.filter((asset) => matchesTab(asset, tab) && matchesQuery(asset, debouncedQuery)), [assets, debouncedQuery, tab]);
  const assetLayoutKey = useMemo(() => filteredAssets.map((asset) => getAssetKey(asset)).join("|"), [filteredAssets]);

  useEffect(() => {
    if (!assetLayoutKey || !gridElementRef.current) return;
    let disposed = false;
    const element = gridElementRef.current;
    import("muuri").then(({ default: MuuriGrid }) => {
      if (disposed || !element) return;
      muuriRef.current?.destroy(false);
      muuriRef.current = new MuuriGrid(element, {
        items: ".asset-grid-item",
        layout: assetWallLayout,
        layoutDuration: 0,
      });
      requestAnimationFrame(() => muuriRef.current?.refreshItems().layout());
    });
    const onResize = () => muuriRef.current?.refreshItems().layout();
    window.addEventListener("resize", onResize);
    return () => {
      disposed = true;
      window.removeEventListener("resize", onResize);
      muuriRef.current?.destroy(false);
      muuriRef.current = null;
    };
  }, [assetLayoutKey]);

  const handlePreviewLoad = useCallback((asset: AigcAsset, ratio?: number) => {
    if (ratio && Number.isFinite(ratio)) {
      const key = getAssetKey(asset);
      setMeasuredRatios((values) => values[key] === ratio ? values : { ...values, [key]: ratio });
    }
    requestAnimationFrame(() => muuriRef.current?.refreshItems().layout());
  }, []);

  const previewAsset = useMemo(() => assets.find((asset) => getAssetKey(asset) === previewAssetKey) ?? null, [assets, previewAssetKey]);

  const refreshPreviewAsset = useCallback((nextAsset: AigcAsset) => {
    setAssets((items) => {
      const nextAssets = items.map((asset) => getAssetKey(asset) === getAssetKey(nextAsset) ? nextAsset : asset);
      writeAssetsPageCache(user?.id, tab, debouncedQuery, nextAssets, total, counts, hasMore, pageCursor);
      return nextAssets;
    });
    setAssetDraft((draft) => draft?.assetKey === getAssetKey(nextAsset) ? {
      id: nextAsset.id,
      assetKey: getAssetKey(nextAsset),
      title: nextAsset.title || "",
      description: nextAsset.description || "",
      tags: nextAsset.tags || "",
      saving: false,
    } : draft);
  }, [counts, debouncedQuery, hasMore, pageCursor, tab, total, user?.id]);

  const openAssetPreview = useCallback((asset: AigcAsset) => {
    const assetKey = getAssetKey(asset);
    setPreviewAssetKey(assetKey);
    setAssetDraft({
      id: asset.id,
      assetKey,
      title: asset.title || "",
      description: asset.description || "",
      tags: asset.tags || "",
      saving: false,
    });
  }, []);

  const handleAssetDraftChange = useCallback((patch: Partial<Pick<AssetDraft, "title" | "description" | "tags">>) => {
    setAssetDraft((draft) => draft ? { ...draft, ...patch } : draft);
  }, []);

  const handleAssetSave = useCallback(async () => {
    if (!assetDraft || !Number.isFinite(Number(assetDraft.id))) return;
    setAssetDraft((draft) => draft ? { ...draft, saving: true } : draft);
    try {
      await updateMyAsset({
        id: assetDraft.id,
        title: assetDraft.title,
        description: assetDraft.description,
        tags: assetDraft.tags,
      });
      setAssets((items) => {
        const nextAssets = items.map((asset) => getAssetKey(asset) === assetDraft.assetKey ? {
          ...asset,
          title: assetDraft.title,
          description: assetDraft.description,
          tags: assetDraft.tags,
        } : asset);
        writeAssetsPageCache(user?.id, tab, debouncedQuery, nextAssets, total, counts, hasMore, pageCursor);
        return nextAssets;
      });
    } finally {
      setAssetDraft((draft) => draft ? { ...draft, saving: false } : draft);
    }
  }, [assetDraft, counts, debouncedQuery, hasMore, pageCursor, tab, total, user?.id]);

  const handleAssetVisibilityChange = useCallback(async (visibility: string) => {
    if (!previewAsset || !Number.isFinite(previewAsset.id)) return;
    const currentVisibility = previewAsset.visibility || "PRIVATE";
    if (visibility !== "PRIVATE" && !canPublishAsset(previewAsset)) return;
    const isExpanding = currentVisibility === "PRIVATE" && visibility !== "PRIVATE";
    if (isExpanding && !window.confirm(`确认将资产设置为「${getAssetVisibilityLabel(visibility)}」吗？`)) return;
    await updateMyAssetVisibility({ id: previewAsset.id, visibility });
    refreshPreviewAsset({ ...previewAsset, visibility });
  }, [previewAsset, refreshPreviewAsset]);

  const handleAssetDownload = useCallback(async () => {
    if (!previewAsset || !canDownloadAsset(previewAsset)) return;
    if (Number.isFinite(previewAsset.id)) {
      const url = await downloadMyAsset({ assetId: previewAsset.id });
      window.open(url || previewAsset.fileUrl, "_blank", "noopener,noreferrer");
      refreshPreviewAsset({ ...previewAsset, downloadCount: (previewAsset.downloadCount ?? 0) + 1 });
      return;
    }
    window.open(previewAsset.fileUrl, "_blank", "noopener,noreferrer");
  }, [previewAsset, refreshPreviewAsset]);

  const handleAssetDelete = useCallback(async () => {
    if (!previewAsset || !Number.isFinite(previewAsset.id)) return;
    if (!window.confirm("确认删除这个资产吗？")) return;
    await deleteMyAsset(previewAsset.id);
    const nextCounts = decrementAssetCounts(counts, previewAsset);
    countsRef.current = nextCounts;
    setCounts(nextCounts);
    setAssets((items) => {
      const nextAssets = items.filter((asset) => getAssetKey(asset) !== getAssetKey(previewAsset));
      const nextTotal = Math.max(0, total - 1);
      writeAssetsPageCache(user?.id, tab, debouncedQuery, nextAssets, nextTotal, nextCounts, hasMore, pageCursor);
      return nextAssets;
    });
    setTotal((value) => Math.max(0, value - 1));
    setPreviewAssetKey(null);
    setAssetDraft(null);
  }, [counts, debouncedQuery, hasMore, pageCursor, previewAsset, tab, total, user?.id]);

  const previewItem = useMemo(() => {
    if (!previewAsset) return null;
    const item = assetToMediaPreview(previewAsset, assetDraft);
    if (!item?.editableAsset) return item;
    return {
      ...item,
      editableAsset: {
        ...item.editableAsset,
        onChange: handleAssetDraftChange,
        onSave: handleAssetSave,
        onVisibilityChange: handleAssetVisibilityChange,
        onDownload: handleAssetDownload,
        onDelete: handleAssetDelete,
      },
    };
  }, [assetDraft, handleAssetDelete, handleAssetDownload, handleAssetDraftChange, handleAssetSave, handleAssetVisibilityChange, previewAsset]);

  useEffect(() => {
    const element = loadMoreElementRef.current;
    if (!element || loading || loadingMore || !hasMore) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) loadAssets(false);
    }, { rootMargin: "600px 0px" });
    observer.observe(element);
    return () => observer.disconnect();
  }, [hasMore, loadAssets, loading, loadingMore]);

  function handleTouchStart(event: TouchEvent<HTMLDivElement>) {
    if (window.scrollY > 0 || loading) return;
    touchStartYRef.current = event.touches[0]?.clientY ?? null;
  }

  function handleTouchMove(event: TouchEvent<HTMLDivElement>) {
    if (touchStartYRef.current === null || loading) return;
    const distance = (event.touches[0]?.clientY ?? 0) - touchStartYRef.current;
    setPullDistance(distance > 0 ? Math.min(96, distance * 0.5) : 0);
  }

  function handleTouchEnd() {
    if (pullDistance >= 64) {
      loadAssets(true);
    } else {
      setPullDistance(0);
    }
    touchStartYRef.current = null;
  }

  return (
    <div className="mx-auto flex max-w-[1180px] flex-col px-6 py-10" onTouchStart={handleTouchStart} onTouchMove={handleTouchMove} onTouchEnd={handleTouchEnd}>
      <div className="flex justify-center overflow-hidden text-xs text-muted-gray" style={{ height: pullDistance }}>
        <div className="flex items-center gap-2">
          <RefreshCw className="size-3.5" />
          {pullDistance >= 64 ? "松开刷新" : "下拉刷新"}
        </div>
      </div>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-charcoal">资产库</h1>
          <p className="mt-1 text-sm text-muted-gray">集中查看生成图片、上传图片、生成视频和其它文件型结果。</p>
        </div>
        <button type="button" onClick={() => loadAssets(true)} disabled={loading} className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50" aria-label="刷新资产列表" title="刷新资产列表">
          <RefreshCw className="size-4" />
          刷新
        </button>
      </div>

      <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="inline-flex w-fit rounded-full bg-muted p-1">
          {(["ALL", "GENERATED_IMAGE", "UPLOADED_IMAGE", "VIDEO", "OTHER"] as AssetTab[]).map((item) => (
            <button key={item} type="button" onClick={() => setTab(item)} className={`rounded-full px-4 py-2 text-sm ${tab === item ? "bg-background text-charcoal shadow-sm" : "text-muted-gray hover:text-charcoal"}`}>
              {getAssetTabLabel(item)}
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
          <Loader2 className="mr-2 size-4" />
          加载资产列表...
        </div>
      ) : filteredAssets.length === 0 ? (
        <EmptyState tab={tab} />
      ) : (
        <>
          <div ref={gridElementRef} className="relative mt-6 -m-0.5">
            {filteredAssets.map((asset) => {
              return (
              <div key={getAssetKey(asset)} data-span={getAssetSpan(asset, measuredRatios[getAssetKey(asset)])} className="asset-grid-item absolute p-0.5">
                {isMediaAsset(asset) ? (
                  <button
                    type="button"
                    onClick={() => openAssetPreview(asset)}
                    className="block w-full cursor-pointer overflow-hidden text-left"
                    aria-label={`预览资产 ${asset.title || asset.assetNo || asset.id}`}
                  >
                    <AssetWallPreview asset={asset} onLoad={(ratio) => handlePreviewLoad(asset, ratio)} />
                  </button>
                ) : (
                  <Link href={getAssetHref(asset)} className="block" aria-label={`查看资产 ${asset.title || asset.assetNo || asset.id}`}>
                    <AssetWallPreview asset={asset} onLoad={(ratio) => handlePreviewLoad(asset, ratio)} />
                  </Link>
                )}
              </div>
              );
            })}
          </div>
          <div ref={loadMoreElementRef} className="mt-8 flex justify-center py-4 text-xs text-muted-gray">
            {loadingMore ? (
              <span className="inline-flex items-center gap-2"><Loader2 className="size-3.5" />加载更多...</span>
            ) : hasMore ? (
              <span>继续下滑加载更多</span>
            ) : total > 0 ? (
              <span>已加载全部</span>
            ) : null}
          </div>
        </>
      )}
      <MediaPreviewDialog
        item={previewItem}
        open={previewAssetKey != null}
        onClose={() => {
          setPreviewAssetKey(null);
          setAssetDraft(null);
        }}
      />
    </div>
  );
}
