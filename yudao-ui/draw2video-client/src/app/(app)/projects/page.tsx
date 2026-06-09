"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Archive, Check, ChevronLeft, ChevronRight, FolderPlus, ImageIcon, MoreVertical, Pencil, Search, Trash2, X } from "lucide-react";
import { getMyAssetPage } from "@/features/assets/asset-api";
import { getAssetPreviewUrl } from "@/features/assets/asset-dictionaries";
import type { AigcAsset } from "@/features/assets/asset-types";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProject } from "@/features/canvas/types";
import { clearCanvas } from "@/features/canvas/use-canvas-storage";
import { useAuth } from "@/features/auth/auth-store";
import { mergeStableList, readPageCache, writePageCache } from "@/lib/page-cache";
import {
  createProject,
  deleteProject,
  listProjects,
  renameProject,
  type ProjectMeta,
} from "@/features/projects/project-store";

import type Muuri from "muuri";

const PAGE_SIZE = 12;
const COVER_ASSET_PAGE_SIZE = 12;

type ProjectListItem = {
  id: string;
  name: string;
  nodeCount: number;
  assetCount: number;
  lastOpenedAt: string;
  coverUrl?: string | null;
  role?: string | null;
  readonly?: boolean;
  source: "server" | "local";
};

type ProjectsPageCache = {
  projects: ProjectListItem[];
  total: number;
  statusMessage: string;
};

function projectsPageCacheKey(ownerKey: string | number | null | undefined, search: string, pageNo: number) {
  return `projects:${ownerKey ?? "current"}:${search.trim()}:${pageNo}`;
}

function getProjectKey(project: ProjectListItem) {
  return `${project.source}:${project.id}`;
}

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

function toProjectListItem(project: CanvasProject): ProjectListItem {
  return {
    id: String(project.id),
    name: project.name,
    nodeCount: project.nodeCount ?? 0,
    assetCount: project.assetCount ?? 0,
    lastOpenedAt: project.updateTime ?? project.createTime ?? "",
    coverUrl: project.coverUrl,
    role: project.role,
    readonly: project.readonly,
    source: "server",
  };
}

function localProjectToListItem(project: ProjectMeta): ProjectListItem {
  return {
    id: project.id,
    name: project.name,
    nodeCount: project.nodeCount,
    assetCount: project.assetCount,
    lastOpenedAt: project.lastOpenedAt,
    coverUrl: project.thumbnailUrl,
    source: "local",
  };
}

function projectRoleLabel(project: ProjectListItem) {
  if (project.source === "local" || project.role === "owner") return "可管理";
  if (project.role === "viewer") return "可浏览";
  return "可编辑";
}

function canDeleteProject(project: ProjectListItem) {
  return project.source === "local" || project.role === "owner";
}

function canRenameProject(project: ProjectListItem) {
  return project.source === "local" || (project.readonly !== true && project.role !== "viewer");
}

function canUpdateProjectCover(project: ProjectListItem) {
  return project.source === "server" && project.readonly !== true && project.role !== "viewer";
}

function ProjectCover({ project, onLoad }: { project: ProjectListItem; onLoad?: () => void }) {
  if (project.coverUrl) {
    return (
      <div className="bg-muted">
        <img
          src={project.coverUrl}
          alt={`${project.name} 封面`}
          onLoad={onLoad}
          onError={onLoad}
          draggable={false}
          className="block h-auto w-full"
        />
      </div>
    );
  }
  return (
    <div className="flex aspect-[4/3] items-center justify-center bg-muted text-muted-gray">
      <ImageIcon className="size-10" strokeWidth={1.6} />
    </div>
  );
}

function AssetChoiceCard({
  asset,
  selected,
  onSelect,
}: {
  asset: AigcAsset;
  selected: boolean;
  onSelect: () => void;
}) {
  const previewUrl = getAssetPreviewUrl(asset);
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`group/asset overflow-hidden rounded-lg border bg-background text-left transition-colors ${
        selected ? "border-charcoal" : "border-border-warm hover:border-[rgba(28,28,28,0.45)]"
      }`}
    >
      <div className="relative aspect-square bg-muted">
        {previewUrl ? (
          <img src={previewUrl} alt={asset.title || "项目封面候选图"} className="size-full object-cover" draggable={false} />
        ) : (
          <div className="flex size-full items-center justify-center text-muted-gray">
            <ImageIcon className="size-5" />
          </div>
        )}
        {selected && (
          <span className="absolute right-2 top-2 inline-flex size-5 items-center justify-center rounded-full bg-charcoal text-off-white">
            <Check className="size-3.5" />
          </span>
        )}
      </div>
      <div className="px-2 py-2">
        <p className="truncate text-xs font-medium text-charcoal">{asset.title || asset.assetNo || `资产 ${asset.id}`}</p>
      </div>
    </button>
  );
}

function CoverAssetPager({
  pageNo,
  pageCount,
  total,
  onPageChange,
}: {
  pageNo: number;
  pageCount: number;
  total: number;
  onPageChange: (page: number) => void;
}) {
  return (
    <div className="mt-3 flex items-center justify-between gap-2 text-xs text-muted-gray">
      <span>
        {total > 0 ? `第 ${pageNo} / ${pageCount} 页 · 共 ${total} 张` : "暂无图片"}
      </span>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => onPageChange(Math.max(1, pageNo - 1))}
          disabled={pageNo <= 1}
          className="inline-flex size-7 items-center justify-center rounded-md border border-border-warm transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="上一页"
        >
          <ChevronLeft className="size-4" />
        </button>
        <button
          type="button"
          onClick={() => onPageChange(Math.min(pageCount, pageNo + 1))}
          disabled={pageNo >= pageCount}
          className="inline-flex size-7 items-center justify-center rounded-md border border-border-warm transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="下一页"
        >
          <ChevronRight className="size-4" />
        </button>
      </div>
    </div>
  );
}

export default function ProjectsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const initialPageCache = useMemo(
    () => readPageCache<ProjectsPageCache>(projectsPageCacheKey(user?.id, "", 1)),
    [user?.id]
  );
  const [projects, setProjects] = useState<ProjectListItem[]>(() => initialPageCache?.projects ?? []);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(() => initialPageCache?.total ?? 0);
  const [deletingProjectId, setDeletingProjectId] = useState<string | null>(null);
  const [editingProjectId, setEditingProjectId] = useState<string | null>(null);
  const [editingProjectName, setEditingProjectName] = useState("");
  const [savingProjectId, setSavingProjectId] = useState<string | null>(null);
  const [coverProject, setCoverProject] = useState<ProjectListItem | null>(null);
  const [selectedCoverAssetId, setSelectedCoverAssetId] = useState<number | null>(null);
  const [savingCoverProjectId, setSavingCoverProjectId] = useState<string | null>(null);
  const [projectAssets, setProjectAssets] = useState<AigcAsset[]>([]);
  const [projectAssetsPageNo, setProjectAssetsPageNo] = useState(1);
  const [projectAssetsTotal, setProjectAssetsTotal] = useState(0);
  const [isLoadingProjectAssets, setIsLoadingProjectAssets] = useState(false);
  const [userAssets, setUserAssets] = useState<AigcAsset[]>([]);
  const [userAssetsPageNo, setUserAssetsPageNo] = useState(1);
  const [userAssetsTotal, setUserAssetsTotal] = useState(0);
  const [isLoadingUserAssets, setIsLoadingUserAssets] = useState(false);
  const [isLoading, setIsLoading] = useState(() => !initialPageCache);
  const [isCreating, setIsCreating] = useState(false);
  const [statusMessage, setStatusMessage] = useState(() => initialPageCache?.statusMessage ?? "");
  const [openMenuProjectId, setOpenMenuProjectId] = useState<string | null>(null);
  const gridElementRef = useRef<HTMLDivElement | null>(null);
  const muuriRef = useRef<Muuri | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const projectLayoutKey = useMemo(() => projects.map((project) => project.id).join("|"), [projects]);

  const refreshProjects = useCallback(async (search = debouncedQuery, nextPageNo = pageNo) => {
    const cacheKey = projectsPageCacheKey(user?.id, search, nextPageNo);
    const cached = readPageCache<ProjectsPageCache>(cacheKey);
    if (cached) {
      setProjects((items) => mergeStableList(items, cached.projects, getProjectKey));
      setTotal(cached.total);
      setStatusMessage(cached.statusMessage);
      setIsLoading(false);
    } else {
      setIsLoading(true);
    }
    try {
      const page = await canvasApi.listProjects({
        pageNo: nextPageNo,
        pageSize: PAGE_SIZE,
        name: search.trim() || undefined,
      });
      const nextPageCount = Math.max(1, Math.ceil(page.total / PAGE_SIZE));
      if (nextPageNo > nextPageCount) {
        setPageNo(nextPageCount);
        return;
      }
      const nextProjects = page.list.map(toProjectListItem);
      writePageCache<ProjectsPageCache>(cacheKey, {
        projects: nextProjects,
        total: page.total,
        statusMessage: "",
      });
      setProjects((items) => mergeStableList(items, nextProjects, getProjectKey));
      setTotal(page.total);
      setStatusMessage("");
    } catch {
      const keyword = search.trim().toLowerCase();
      const localProjects = listProjects(user?.id)
        .filter((project) => !keyword || project.name.toLowerCase().includes(keyword))
        .map(localProjectToListItem);
      const nextPageCount = Math.max(1, Math.ceil(localProjects.length / PAGE_SIZE));
      if (nextPageNo > nextPageCount) {
        setPageNo(nextPageCount);
        return;
      }
      const nextProjects = localProjects.slice((nextPageNo - 1) * PAGE_SIZE, nextPageNo * PAGE_SIZE);
      const nextStatusMessage = "Project service is temporarily unavailable. Showing local drafts.";
      writePageCache<ProjectsPageCache>(cacheKey, {
        projects: nextProjects,
        total: localProjects.length,
        statusMessage: nextStatusMessage,
      });
      setProjects((items) => mergeStableList(items, nextProjects, getProjectKey));
      setTotal(localProjects.length);
      setStatusMessage(nextStatusMessage);
    } finally {
      setIsLoading(false);
    }
  }, [debouncedQuery, pageNo, user?.id]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), 350);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const timer = window.setTimeout(() => refreshProjects(debouncedQuery, pageNo), 0);
    return () => window.clearTimeout(timer);
  }, [debouncedQuery, pageNo, refreshProjects]);

  useEffect(() => {
    if (!projectLayoutKey || !gridElementRef.current) return;
    let disposed = false;
    const element = gridElementRef.current;
    import("muuri").then(({ default: MuuriGrid }) => {
      if (disposed || !element) return;
      muuriRef.current?.destroy(false);
      muuriRef.current = new MuuriGrid(element, {
        items: ".project-grid-item",
        layoutDuration: 180,
        layoutEasing: "ease",
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
  }, [projectLayoutKey]);

  const refreshProjectWallLayout = useCallback(() => {
    muuriRef.current?.refreshItems().layout();
  }, []);

  useEffect(() => {
    if (!openMenuProjectId) return;
    function handlePointerDown(event: PointerEvent) {
      if (menuRef.current?.contains(event.target as Node)) return;
      setOpenMenuProjectId(null);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpenMenuProjectId(null);
    }
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [openMenuProjectId]);

  const pageCount = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);
  const projectAssetsPageCount = useMemo(() => Math.max(1, Math.ceil(projectAssetsTotal / COVER_ASSET_PAGE_SIZE)), [projectAssetsTotal]);
  const userAssetsPageCount = useMemo(() => Math.max(1, Math.ceil(userAssetsTotal / COVER_ASSET_PAGE_SIZE)), [userAssetsTotal]);

  const loadProjectCoverAssets = useCallback(async (projectId: string, nextPageNo: number) => {
    setIsLoadingProjectAssets(true);
    try {
      const page = await canvasApi.getProjectAssets(projectId, {
        pageNo: nextPageNo,
        pageSize: COVER_ASSET_PAGE_SIZE,
        assetType: "IMAGE",
      });
      setProjectAssets(page.list);
      setProjectAssetsTotal(page.total);
    } catch (error) {
      setProjectAssets([]);
      setProjectAssetsTotal(0);
      setStatusMessage(error instanceof Error ? error.message : "项目内图片加载失败。");
    } finally {
      setIsLoadingProjectAssets(false);
    }
  }, []);

  const loadUserCoverAssets = useCallback(async (nextPageNo: number) => {
    setIsLoadingUserAssets(true);
    try {
      const page = await getMyAssetPage({
        pageNo: nextPageNo,
        pageSize: COVER_ASSET_PAGE_SIZE,
        assetType: "IMAGE",
      });
      setUserAssets(page.list);
      setUserAssetsTotal(page.total);
    } catch (error) {
      setUserAssets([]);
      setUserAssetsTotal(0);
      setStatusMessage(error instanceof Error ? error.message : "用户图片资源加载失败。");
    } finally {
      setIsLoadingUserAssets(false);
    }
  }, []);

  useEffect(() => {
    if (!coverProject) return;
    const timer = window.setTimeout(() => loadProjectCoverAssets(coverProject.id, projectAssetsPageNo), 0);
    return () => window.clearTimeout(timer);
  }, [coverProject, loadProjectCoverAssets, projectAssetsPageNo]);

  useEffect(() => {
    if (!coverProject) return;
    const timer = window.setTimeout(() => loadUserCoverAssets(userAssetsPageNo), 0);
    return () => window.clearTimeout(timer);
  }, [coverProject, loadUserCoverAssets, userAssetsPageNo]);

  async function handleCreateProject() {
    if (isCreating) return;
    setIsCreating(true);
    try {
      const projectId = await canvasApi.createProject({ name: "未命名项目" });
      router.push(`/canvas?projectId=${encodeURIComponent(String(projectId))}`);
    } catch {
      const project = createProject({ name: "未命名项目" }, user?.id);
      setStatusMessage("项目服务暂不可用，已创建本机草稿。");
      router.push(`/canvas?projectId=${encodeURIComponent(project.id)}`);
    } finally {
      setIsCreating(false);
    }
  }

  async function handleDelete(project: ProjectListItem) {
    if (!canDeleteProject(project) || deletingProjectId) return;
    const confirmed = window.confirm(
      project.source === "server"
        ? `删除项目「${project.name}」？删除后可在回收站恢复。`
        : `删除本机草稿「${project.name}」？`
    );
    if (!confirmed) return;
    setDeletingProjectId(project.id);
    if (project.source === "local") {
      deleteProject(project.id, user?.id);
      clearCanvas(project.id, user?.id);
      await refreshProjects();
      setDeletingProjectId(null);
      return;
    }
    try {
      await canvasApi.deleteProject(project.id);
      setStatusMessage("项目已移入回收站。");
      await refreshProjects();
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : "删除失败，请稍后再试。");
    } finally {
      setDeletingProjectId(null);
    }
  }

  function startRename(project: ProjectListItem) {
    if (!canRenameProject(project) || savingProjectId) return;
    setOpenMenuProjectId(null);
    setEditingProjectId(project.id);
    setEditingProjectName(project.name);
  }

  function cancelRename() {
    setEditingProjectId(null);
    setEditingProjectName("");
  }

  async function submitRename(project: ProjectListItem) {
    if (savingProjectId) return;
    const nextName = editingProjectName.trim();
    if (!nextName || nextName === project.name) {
      cancelRename();
      return;
    }
    setSavingProjectId(project.id);
    try {
      if (project.source === "local") {
        renameProject(project.id, nextName);
      } else {
        await canvasApi.updateProject(project.id, { name: nextName });
      }
      setProjects((items) => items.map((item) => item.id === project.id ? { ...item, name: nextName } : item));
      setStatusMessage("");
      cancelRename();
      window.setTimeout(refreshProjectWallLayout, 0);
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : "项目名称修改失败，请稍后再试。");
    } finally {
      setSavingProjectId(null);
    }
  }

  function openCoverPicker(project: ProjectListItem) {
    if (!canUpdateProjectCover(project) || savingCoverProjectId) return;
    setOpenMenuProjectId(null);
    setCoverProject(project);
    setSelectedCoverAssetId(null);
    setProjectAssetsPageNo(1);
    setUserAssetsPageNo(1);
    setProjectAssets([]);
    setUserAssets([]);
    setProjectAssetsTotal(0);
    setUserAssetsTotal(0);
  }

  function closeCoverPicker() {
    if (savingCoverProjectId) return;
    setCoverProject(null);
    setSelectedCoverAssetId(null);
  }

  async function submitCoverAsset() {
    if (!coverProject || !selectedCoverAssetId || savingCoverProjectId) return;
    setSavingCoverProjectId(coverProject.id);
    try {
      await canvasApi.updateProject(coverProject.id, { coverAssetId: selectedCoverAssetId });
      const selectedAsset = [...projectAssets, ...userAssets].find((asset) => asset.id === selectedCoverAssetId);
      const coverUrl = selectedAsset ? getAssetPreviewUrl(selectedAsset) : coverProject.coverUrl;
      setProjects((items) => items.map((item) => item.id === coverProject.id
        ? { ...item, coverUrl }
        : item
      ));
      setStatusMessage("项目显示图已更新。");
      setCoverProject(null);
      setSelectedCoverAssetId(null);
      window.setTimeout(refreshProjectWallLayout, 0);
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : "项目显示图修改失败，请稍后再试。");
    } finally {
      setSavingCoverProjectId(null);
    }
  }

  return (
    <div className="mx-auto flex max-w-[1180px] flex-col px-6 py-10">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-charcoal">项目库</h1>
          <p className="mt-1 text-sm text-muted-gray">从项目进入画布，所有节点和素材都会按项目独立保存。</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link
            href="/projects/recycle-bin"
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-border-warm bg-background px-4 py-2 text-sm font-medium text-muted-gray transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal"
          >
            <Archive className="size-4" />
            回收站
          </Link>
          <button
            type="button"
            onClick={handleCreateProject}
            disabled={isCreating}
            className="inline-flex items-center justify-center gap-2 rounded-lg bg-charcoal px-4 py-2 text-sm font-medium text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]"
          >
            <FolderPlus className="size-4" />
            {isCreating ? "创建中" : "新建项目"}
          </button>
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
          placeholder="搜索项目"
          className="w-full bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
        />
      </div>

      {statusMessage && (
        <div className="mt-4 rounded-lg border border-border-warm bg-muted px-3 py-2 text-xs text-muted-gray">
          {statusMessage}
        </div>
      )}

      {isLoading && projects.length === 0 ? (
        <div className="mt-16 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
          <ImageIcon className="size-8 text-muted-gray" />
          <p className="mt-4 text-sm font-medium text-charcoal">正在加载项目</p>
          <p className="mt-1 text-xs text-muted-gray">从云端同步你的项目库。</p>
        </div>
      ) : projects.length === 0 ? (
        <div className="mt-16 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
          <ImageIcon className="size-8 text-muted-gray" />
          <p className="mt-4 text-sm font-medium text-charcoal">{query ? "没有匹配的项目" : "还没有项目"}</p>
          <p className="mt-1 text-xs text-muted-gray">新建一个项目后，就可以进入独立画布继续创作。</p>
          {!query && (
            <button
              type="button"
              onClick={handleCreateProject}
              className="mt-5 inline-flex items-center gap-2 rounded-lg bg-charcoal px-4 py-2 text-sm font-medium text-off-white"
            >
              <FolderPlus className="size-4" />
              新建项目
            </button>
          )}
        </div>
      ) : (
        <div ref={gridElementRef} className="relative mt-6 -m-2">
          {projects.map((project) => (
            <div
              key={project.id}
              className="project-grid-item absolute w-full p-2 sm:w-1/2 lg:w-1/4"
            >
              <article className="group relative overflow-hidden rounded-xl border border-border-warm bg-background transition-colors hover:border-[rgba(28,28,28,0.4)]">
                <div className="relative overflow-hidden rounded-t-xl bg-muted">
                  <Link
                    href={`/canvas?projectId=${encodeURIComponent(project.id)}`}
                    className="block"
                    aria-label={`打开项目 ${project.name}`}
                  >
                    <ProjectCover project={project} onLoad={refreshProjectWallLayout} />
                  </Link>

                  <div
                    ref={openMenuProjectId === project.id ? menuRef : null}
                    className="absolute right-2 top-2 opacity-0 transition-opacity group-hover:opacity-100 has-[:focus-visible]:opacity-100"
                  >
                    <button
                      type="button"
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        setOpenMenuProjectId((value) => value === project.id ? null : project.id);
                      }}
                      className="inline-flex size-8 items-center justify-center rounded-full bg-charcoal/75 text-off-white shadow-sm backdrop-blur transition-colors hover:bg-charcoal"
                      aria-label="项目详情菜单"
                      aria-expanded={openMenuProjectId === project.id}
                    >
                      <MoreVertical className="size-4" />
                    </button>

                    {openMenuProjectId === project.id && (
                      <div className="absolute right-0 top-10 z-20 w-36 overflow-hidden rounded-lg border border-border-warm bg-background py-1 text-sm text-charcoal shadow-[0_12px_32px_rgba(0,0,0,0.18)]">
                        <button
                          type="button"
                          onClick={() => startRename(project)}
                          disabled={!canRenameProject(project) || savingProjectId === project.id}
                          className="flex w-full items-center gap-2 px-3 py-2 text-left transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
                        >
                          <Pencil className="size-3.5" />
                          重命名项目
                        </button>
                        <button
                          type="button"
                          onClick={() => openCoverPicker(project)}
                          disabled={!canUpdateProjectCover(project) || savingCoverProjectId === project.id}
                          className="flex w-full items-center gap-2 px-3 py-2 text-left transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
                        >
                          <ImageIcon className="size-3.5" />
                          修改显示图
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setOpenMenuProjectId(null);
                            void handleDelete(project);
                          }}
                          disabled={!canDeleteProject(project) || deletingProjectId === project.id}
                          className="flex w-full items-center gap-2 px-3 py-2 text-left text-destructive transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-45"
                        >
                          <Trash2 className="size-3.5" />
                          删除
                        </button>
                      </div>
                    )}
                  </div>

                  {canUpdateProjectCover(project) && (
                    <button
                      type="button"
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        openCoverPicker(project);
                      }}
                      disabled={savingCoverProjectId === project.id}
                      className="absolute bottom-2 right-2 inline-flex size-8 items-center justify-center rounded-full bg-charcoal/75 text-off-white opacity-0 shadow-sm backdrop-blur transition-colors hover:bg-charcoal group-hover:opacity-100 focus-visible:opacity-100 disabled:cursor-not-allowed disabled:opacity-45"
                      aria-label="替换项目封面图"
                      title="替换封面图"
                    >
                      <ImageIcon className="size-4" />
                    </button>
                  )}
                </div>

                <div className="px-3 pb-4 pt-4">
                  {editingProjectId === project.id ? (
                    <input
                      value={editingProjectName}
                      autoFocus
                      disabled={savingProjectId === project.id}
                      onChange={(event) => setEditingProjectName(event.target.value)}
                      onBlur={() => submitRename(project)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") submitRename(project);
                        if (event.key === "Escape") cancelRename();
                      }}
                      className="h-6 w-full rounded-md border border-border-warm bg-background px-1.5 py-0 text-base font-semibold leading-6 text-charcoal outline-none focus:border-[rgba(28,28,28,0.45)] disabled:opacity-60"
                    />
                  ) : (
                    <div className="group/title flex min-w-0 items-center gap-1">
                      <Link
                        href={`/canvas?projectId=${encodeURIComponent(project.id)}`}
                        className="min-w-0 truncate text-base font-semibold text-charcoal outline-none hover:text-charcoal"
                        title={project.name}
                      >
                        {project.name}
                      </Link>
                      {canRenameProject(project) && (
                        <button
                          type="button"
                          onClick={() => startRename(project)}
                          disabled={savingProjectId === project.id}
                          className="inline-flex size-6 shrink-0 items-center justify-center rounded-md text-muted-gray opacity-0 transition-colors hover:bg-muted hover:text-charcoal group-hover/title:opacity-100 focus-visible:opacity-100 disabled:cursor-not-allowed disabled:opacity-45"
                          aria-label="修改项目名称"
                          title="修改项目名称"
                        >
                          <Pencil className="size-3.5" />
                        </button>
                      )}
                    </div>
                  )}
                  <p className="mt-1 truncate text-sm text-muted-gray">
                    修改于 {formatDate(project.lastOpenedAt)}
                    {project.source === "local" ? " · 本机草稿" : ""}
                    {project.source === "server" && project.role && project.role !== "owner" ? ` · ${projectRoleLabel(project)}` : ""}
                  </p>
                </div>
              </article>
            </div>
          ))}
        </div>
      )}

      {total > PAGE_SIZE && (
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

      {coverProject && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 px-4 py-6">
          <div className="flex max-h-[88vh] w-full max-w-[980px] flex-col rounded-xl border border-border-warm bg-background shadow-[0_18px_60px_rgba(0,0,0,0.18)]">
            <div className="flex items-center justify-between gap-3 border-b border-border-warm px-5 py-4">
              <div className="min-w-0">
                <h2 className="truncate text-base font-semibold text-charcoal">修改项目显示图</h2>
                <p className="mt-1 truncate text-xs text-muted-gray">{coverProject.name}</p>
              </div>
              <button
                type="button"
                onClick={closeCoverPicker}
                disabled={Boolean(savingCoverProjectId)}
                className="inline-flex size-8 items-center justify-center rounded-lg text-muted-gray transition-colors hover:bg-muted hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
                aria-label="关闭"
              >
                <X className="size-4" />
              </button>
            </div>

            <div className="grid min-h-0 flex-1 gap-4 overflow-y-auto p-5 lg:grid-cols-2">
              <section className="min-w-0 rounded-lg border border-border-warm p-3">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <h3 className="text-sm font-medium text-charcoal">项目内图片资源</h3>
                  {isLoadingProjectAssets && <span className="text-xs text-muted-gray">加载中</span>}
                </div>
                {projectAssets.length === 0 ? (
                  <div className="flex h-[360px] items-center justify-center rounded-lg bg-muted px-4 text-center text-xs text-muted-gray">
                    {isLoadingProjectAssets ? "正在加载项目资源" : "当前项目还没有可选图片"}
                  </div>
                ) : (
                  <div className="grid h-[360px] content-start gap-2 overflow-y-auto pr-1 sm:grid-cols-2 xl:grid-cols-3">
                    {projectAssets.map((asset) => (
                      <AssetChoiceCard
                        key={`project-${asset.id}`}
                        asset={asset}
                        selected={selectedCoverAssetId === asset.id}
                        onSelect={() => setSelectedCoverAssetId(asset.id)}
                      />
                    ))}
                  </div>
                )}
                <CoverAssetPager
                  pageNo={projectAssetsPageNo}
                  pageCount={projectAssetsPageCount}
                  total={projectAssetsTotal}
                  onPageChange={setProjectAssetsPageNo}
                />
              </section>

              <section className="min-w-0 rounded-lg border border-border-warm p-3">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <h3 className="text-sm font-medium text-charcoal">用户所有图片资源</h3>
                  {isLoadingUserAssets && <span className="text-xs text-muted-gray">加载中</span>}
                </div>
                {userAssets.length === 0 ? (
                  <div className="flex h-[360px] items-center justify-center rounded-lg bg-muted px-4 text-center text-xs text-muted-gray">
                    {isLoadingUserAssets ? "正在加载用户资源" : "没有可选图片资源"}
                  </div>
                ) : (
                  <div className="grid h-[360px] content-start gap-2 overflow-y-auto pr-1 sm:grid-cols-2 xl:grid-cols-3">
                    {userAssets.map((asset) => (
                      <AssetChoiceCard
                        key={`user-${asset.id}`}
                        asset={asset}
                        selected={selectedCoverAssetId === asset.id}
                        onSelect={() => setSelectedCoverAssetId(asset.id)}
                      />
                    ))}
                  </div>
                )}
                <CoverAssetPager
                  pageNo={userAssetsPageNo}
                  pageCount={userAssetsPageCount}
                  total={userAssetsTotal}
                  onPageChange={setUserAssetsPageNo}
                />
              </section>
            </div>

            <div className="flex items-center justify-end gap-2 border-t border-border-warm px-5 py-4">
              <button
                type="button"
                onClick={closeCoverPicker}
                disabled={Boolean(savingCoverProjectId)}
                className="rounded-lg border border-border-warm px-4 py-2 text-sm text-muted-gray transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
              >
                取消
              </button>
              <button
                type="button"
                onClick={submitCoverAsset}
                disabled={!selectedCoverAssetId || Boolean(savingCoverProjectId)}
                className="inline-flex items-center gap-2 rounded-lg bg-charcoal px-4 py-2 text-sm font-medium text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] disabled:cursor-not-allowed disabled:opacity-45"
              >
                <Check className="size-4" />
                {savingCoverProjectId ? "保存中" : "设为显示图"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
