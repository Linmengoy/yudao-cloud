"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FolderPlus, ImageIcon, MoreHorizontal, Pencil, Search, Trash2 } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProject } from "@/features/canvas/types";
import { clearCanvas } from "@/features/canvas/use-canvas-storage";
import {
  createProject,
  deleteProject,
  listProjects,
  renameProject,
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
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(0);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const gridElementRef = useRef<HTMLDivElement | null>(null);
  const muuriRef = useRef<Muuri | null>(null);

  const refreshProjects = useCallback(async (search = query, nextPageNo = pageNo) => {
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
  }, [pageNo, query]);

  useEffect(() => {
    const timer = window.setTimeout(() => refreshProjects(query, pageNo), 0);
    return () => window.clearTimeout(timer);
  }, [pageNo, query, refreshProjects]);

  useEffect(() => {
    if (!openMenuId) return;
    function close() {
      setOpenMenuId(null);
    }
    window.addEventListener("pointerdown", close);
    window.addEventListener("keydown", close);
    return () => {
      window.removeEventListener("pointerdown", close);
      window.removeEventListener("keydown", close);
    };
  }, [openMenuId]);

  useEffect(() => {
    if (isLoading || projects.length === 0 || !gridElementRef.current) return;
    let disposed = false;
    const element = gridElementRef.current;
    import("muuri").then(({ default: MuuriGrid }) => {
      if (disposed || !element) return;
      muuriRef.current?.destroy(true);
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
      muuriRef.current?.destroy(true);
      muuriRef.current = null;
    };
  }, [isLoading, projects]);

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

  async function handleRename(project: ProjectListItem) {
    const nextName = window.prompt("项目名称", project.name);
    if (!nextName) return;
    if (project.source === "local") {
      renameProject(project.id, nextName);
      refreshProjects();
      return;
    }
    try {
      await canvasApi.updateProject(project.id, { name: nextName.trim() });
      refreshProjects();
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : "重命名失败，请稍后再试。");
    }
  }

  function handleDelete(project: ProjectListItem) {
    if (project.source === "server") {
      setStatusMessage("服务端项目归档/删除接口尚未开放，这期先保留项目。");
      return;
    }
    const confirmed = window.confirm(`删除项目「${project.name}」？`);
    if (!confirmed) return;
    deleteProject(project.id);
    clearCanvas(project.id);
    refreshProjects();
  }

  return (
    <div className="mx-auto flex max-w-[1180px] flex-col px-6 py-10">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-charcoal">项目库</h1>
          <p className="mt-1 text-sm text-muted-gray">从项目进入画布，所有节点和素材都会按项目独立保存。</p>
        </div>
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
                    <p className="mt-4 text-xs text-muted-gray">最近打开 {formatDate(project.lastOpenedAt)}</p>
                  </div>
                </Link>

                <div className="absolute right-4 top-4">
                  <button
                    type="button"
                    onPointerDown={(event) => event.stopPropagation()}
                    onClick={(event) => {
                      event.preventDefault();
                      event.stopPropagation();
                      setOpenMenuId(openMenuId === project.id ? null : project.id);
                    }}
                    className="flex size-8 items-center justify-center rounded-lg bg-background/90 text-muted-gray shadow-sm hover:text-charcoal"
                    aria-label="项目操作"
                  >
                    <MoreHorizontal className="size-4" />
                  </button>
                  {openMenuId === project.id && (
                    <div
                      onPointerDown={(event) => event.stopPropagation()}
                      className="absolute right-0 top-9 z-10 w-36 rounded-xl border border-border-warm bg-background p-1 shadow-[0_8px_24px_rgba(28,28,28,0.12)]"
                    >
                      <button
                        type="button"
                        onClick={() => {
                          setOpenMenuId(null);
                          handleRename(project);
                        }}
                        className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-charcoal hover:bg-muted"
                      >
                        <Pencil className="size-4 text-muted-gray" />
                        重命名
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setOpenMenuId(null);
                          handleDelete(project);
                        }}
                        className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-destructive hover:bg-muted"
                      >
                        <Trash2 className="size-4" />
                        删除
                      </button>
                    </div>
                  )}
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
