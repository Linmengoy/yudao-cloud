"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Archive, FolderPlus, ImageIcon, Search, Trash2 } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProject } from "@/features/canvas/types";
import { clearCanvas } from "@/features/canvas/use-canvas-storage";
import {
  createProject,
  deleteProject,
  listProjects,
  type ProjectMeta,
} from "@/features/projects/project-store";

import type Muuri from "muuri";

const PAGE_SIZE = 12;

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

function ProjectIcon() {
  return <ImageIcon className="size-5" />;
}

function projectRoleLabel(project: ProjectListItem) {
  if (project.source === "local" || project.role === "owner") return "可管理";
  if (project.role === "viewer") return "可浏览";
  return "可编辑";
}

function canDeleteProject(project: ProjectListItem) {
  return project.source === "local" || project.role === "owner";
}

function ProjectCover({ project, onLoad }: { project: ProjectListItem; onLoad?: () => void }) {
  useEffect(() => {
    if (!project.coverUrl || !onLoad) return;
    const image = new window.Image();
    image.onload = onLoad;
    image.onerror = onLoad;
    image.src = project.coverUrl;
    return () => {
      image.onload = null;
      image.onerror = null;
    };
  }, [onLoad, project.coverUrl]);

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
    <div className="flex aspect-[4/3] flex-col items-center justify-center bg-muted text-muted-gray">
      <ProjectIcon />
      <span className="mt-2 text-xs">空白项目</span>
    </div>
  );
}

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(0);
  const [deletingProjectId, setDeletingProjectId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const gridElementRef = useRef<HTMLDivElement | null>(null);
  const muuriRef = useRef<Muuri | null>(null);
  const projectLayoutKey = useMemo(() => projects.map((project) => project.id).join("|"), [projects]);

  const refreshProjects = useCallback(async (search = debouncedQuery, nextPageNo = pageNo) => {
    setIsLoading(true);
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
      setProjects(page.list.map(toProjectListItem));
      setTotal(page.total);
      setStatusMessage("");
    } catch {
      const keyword = search.trim().toLowerCase();
      const localProjects = listProjects()
        .filter((project) => !keyword || project.name.toLowerCase().includes(keyword))
        .map(localProjectToListItem);
      const nextPageCount = Math.max(1, Math.ceil(localProjects.length / PAGE_SIZE));
      if (nextPageNo > nextPageCount) {
        setPageNo(nextPageCount);
        return;
      }
      setProjects(localProjects.slice((nextPageNo - 1) * PAGE_SIZE, nextPageNo * PAGE_SIZE));
      setTotal(localProjects.length);
      setStatusMessage("暂时无法连接项目服务，已显示本机草稿。");
    } finally {
      setIsLoading(false);
    }
  }, [debouncedQuery, pageNo]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), 350);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const timer = window.setTimeout(() => refreshProjects(debouncedQuery, pageNo), 0);
    return () => window.clearTimeout(timer);
  }, [debouncedQuery, pageNo, refreshProjects]);

  useEffect(() => {
    if (isLoading || !projectLayoutKey || !gridElementRef.current) return;
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
  }, [isLoading, projectLayoutKey]);

  const refreshProjectWallLayout = useCallback(() => {
    muuriRef.current?.refreshItems().layout();
  }, []);

  const pageCount = useMemo(() => Math.max(1, Math.ceil(total / PAGE_SIZE)), [total]);

  async function handleCreateProject() {
    if (isCreating) return;
    setIsCreating(true);
    try {
      const projectId = await canvasApi.createProject({ name: "未命名项目" });
      router.push(`/canvas?projectId=${encodeURIComponent(String(projectId))}`);
    } catch {
      const project = createProject({ name: "未命名项目" });
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
      deleteProject(project.id);
      clearCanvas(project.id);
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

      {isLoading ? (
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
              className="project-grid-item absolute w-full p-2 sm:w-1/2 lg:w-1/3 xl:w-1/4"
            >
              <div className="group relative overflow-hidden rounded-xl border border-border-warm bg-background transition-colors hover:border-[rgba(28,28,28,0.4)]">
                <Link href={`/canvas?projectId=${encodeURIComponent(project.id)}`} className="block">
                  <ProjectCover project={project} onLoad={refreshProjectWallLayout} />
                </Link>
                <div className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-charcoal">{project.name}</p>
                        <p className="mt-1 text-xs text-muted-gray">
                          {projectRoleLabel(project)}
                          {project.source === "local" ? " · 本机草稿" : ""}
                        </p>
                    </div>
                    <div className="shrink-0 text-right text-xs text-muted-gray">
                      <p>{project.nodeCount} 节点</p>
                      <p className="mt-1">{project.assetCount} 素材</p>
                    </div>
                  </div>
                  <div className="mt-4 flex items-center justify-between gap-3">
                    <p className="min-w-0 text-xs text-muted-gray">最近打开 {formatDate(project.lastOpenedAt)}</p>
                    {canDeleteProject(project) && (
                      <button
                        type="button"
                        onClick={() => handleDelete(project)}
                        disabled={deletingProjectId === project.id}
                        className="inline-flex shrink-0 items-center gap-1.5 rounded-lg px-2 py-1 text-xs text-destructive opacity-0 transition-colors hover:bg-muted group-hover:opacity-100 disabled:cursor-not-allowed disabled:opacity-40"
                        aria-label="删除项目"
                      >
                        <Trash2 className="size-3.5" />
                        {deletingProjectId === project.id ? "删除中" : ""}
                      </button>
                    )}
                  </div>
                </div>
              </div>
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
