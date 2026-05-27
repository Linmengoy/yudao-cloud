"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FolderPlus, ImageIcon, MoreHorizontal, Pencil, Search, Trash2, Video } from "lucide-react";
import { clearCanvas } from "@/features/canvas/use-canvas-storage";
import {
  createProject,
  deleteProject,
  listProjects,
  renameProject,
  type ProjectMeta,
} from "@/features/projects/project-store";

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

function projectKindLabel(project: ProjectMeta) {
  if (project.kind === "video") return "视频项目";
  if (project.kind === "mixed") return "混合项目";
  return "图片项目";
}

function ProjectIcon({ project }: { project: ProjectMeta }) {
  if (project.kind === "video") return <Video className="size-5" />;
  return <ImageIcon className="size-5" />;
}

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<ProjectMeta[]>([]);
  const [query, setQuery] = useState("");
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  function refreshProjects() {
    setProjects(listProjects());
  }

  useEffect(() => {
    const timer = window.setTimeout(refreshProjects, 0);
    window.addEventListener("copse:projects-changed", refreshProjects);
    return () => {
      window.clearTimeout(timer);
      window.removeEventListener("copse:projects-changed", refreshProjects);
    };
  }, []);

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

  const filteredProjects = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return projects;
    return projects.filter((project) => project.name.toLowerCase().includes(keyword));
  }, [projects, query]);

  function handleCreateProject() {
    const project = createProject({ name: "未命名项目", kind: "image" });
    router.push(`/create/image?projectId=${encodeURIComponent(project.id)}`);
  }

  function handleRename(project: ProjectMeta) {
    const nextName = window.prompt("项目名称", project.name);
    if (!nextName) return;
    renameProject(project.id, nextName);
    refreshProjects();
  }

  function handleDelete(project: ProjectMeta) {
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
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-charcoal px-4 py-2 text-sm font-medium text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px]"
        >
          <FolderPlus className="size-4" />
          新建项目
        </button>
      </div>

      <div className="mt-8 flex max-w-[420px] items-center gap-2 rounded-lg border border-border-warm bg-background px-3 py-2">
        <Search className="size-4 text-muted-gray" />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="搜索项目"
          className="w-full bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
        />
      </div>

      {filteredProjects.length === 0 ? (
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
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filteredProjects.map((project) => (
            <div
              key={project.id}
              className="group relative overflow-hidden rounded-xl border border-border-warm bg-background transition-colors hover:border-[rgba(28,28,28,0.4)]"
            >
              <Link href={`/create/image?projectId=${encodeURIComponent(project.id)}`} className="block">
                <div className="flex aspect-[4/3] items-center justify-center bg-muted text-muted-gray">
                  <ProjectIcon project={project} />
                </div>
                <div className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-charcoal">{project.name}</p>
                      <p className="mt-1 text-xs text-muted-gray">{projectKindLabel(project)}</p>
                    </div>
                    <div className="shrink-0 text-right text-xs text-muted-gray">
                      <p>{project.nodeCount} 节点</p>
                      <p className="mt-1">{project.assetCount} 素材</p>
                    </div>
                  </div>
                  <p className="mt-4 text-xs text-muted-gray">最近打开 {formatDate(project.lastOpenedAt)}</p>
                </div>
              </Link>

              <div className="absolute right-2 top-2">
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
          ))}
        </div>
      )}
    </div>
  );
}
