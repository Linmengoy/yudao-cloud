"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FolderPlus, ImageIcon, Paperclip, Plus, Send, Video } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProject } from "@/features/canvas/types";
import { createProject, listProjects, type ProjectMeta } from "@/features/projects/project-store";

const MODELS = [
  { id: "gpt-image", label: "GPT Image" },
  { id: "seedance", label: "Seedance 2.0" },
  { id: "nano", label: "Nano Banana Pro" },
  { id: "design", label: "Design" },
  { id: "branding", label: "Branding" },
  { id: "ecommerce", label: "E-Commerce" },
  { id: "video", label: "Video", disabled: true },
];

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

type ProjectListItem = {
  id: string;
  name: string;
  kind: "image" | "video" | "mixed";
  lastOpenedAt: string;
};

function toProjectListItem(project: CanvasProject): ProjectListItem {
  return {
    id: String(project.id),
    name: project.name,
    kind: project.kind,
    lastOpenedAt: project.updateTime ?? project.createTime ?? "",
  };
}

function localProjectToListItem(project: ProjectMeta): ProjectListItem {
  return {
    id: project.id,
    name: project.name,
    kind: project.kind,
    lastOpenedAt: project.lastOpenedAt,
  };
}

function ProjectIcon({ project }: { project: ProjectListItem }) {
  if (project.kind === "video") return <Video className="size-5" />;
  return <ImageIcon className="size-5" />;
}

export default function WorkspacePage() {
  const router = useRouter();
  const [prompt, setPrompt] = useState("");
  const [selectedModel, setSelectedModel] = useState("gpt-image");
  const [submitting, setSubmitting] = useState(false);
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const recentProjects = useMemo(() => projects.slice(0, 7), [projects]);

  async function refreshProjects() {
    try {
      const page = await canvasApi.listProjects({ pageNo: 1, pageSize: 7 });
      setProjects(page.list.map(toProjectListItem));
    } catch {
      setProjects(listProjects().map(localProjectToListItem));
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(refreshProjects, 0);
    return () => window.clearTimeout(timer);
  }, []);

  async function openNewProject(name?: string) {
    const projectName = name?.trim() || "未命名项目";
    try {
      const projectId = await canvasApi.createProject({ name: projectName, kind: "image" });
      router.push(`/canvas?projectId=${encodeURIComponent(String(projectId))}`);
    } catch {
      const project = createProject({ name: projectName, kind: "image" });
      router.push(`/canvas?projectId=${encodeURIComponent(project.id)}`);
    }
  }

  async function handleSubmit() {
    if (!prompt.trim() || submitting) return;
    setSubmitting(true);
    await openNewProject(prompt.trim().slice(0, 30));
    setPrompt("");
    setSubmitting(false);
  }

  return (
    <div className="flex flex-col items-center px-4 pt-16 pb-20">
      {/* Hero text */}
      <div className="flex items-center gap-2 text-lg font-semibold text-charcoal">
        <div className="flex size-8 items-center justify-center rounded-full bg-charcoal text-sm text-off-white">
          C
        </div>
        Copse 让创作更简单
      </div>
      <p className="mt-2 text-sm text-muted-gray">
        懂你的创意代理，帮你搞定一切
      </p>

      {/* Prompt input */}
      <div className="mt-8 w-full max-w-[840px]">
        <div className="rounded-[24px] border border-border-warm bg-background p-4 shadow-[0_1px_3px_rgba(0,0,0,0.04)]">
          <textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="让 Copse 创作一张..."
            rows={4}
            className="w-full resize-none bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
          />
          <div className="mt-2 flex items-center justify-between">
            <div className="flex items-center gap-1">
              <button
                type="button"
                className="flex size-8 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
                title="添加素材"
              >
                <Plus className="size-4" />
              </button>
              <button
                type="button"
                className="flex size-8 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
                title="参考图"
              >
                <Paperclip className="size-4" />
              </button>
            </div>
            <button
              onClick={handleSubmit}
              disabled={!prompt.trim() || submitting}
              className="flex size-8 items-center justify-center rounded-lg bg-charcoal text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80 disabled:opacity-50"
            >
              <Send className="size-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Model chips */}
      <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
        {MODELS.map((m) => (
          <button
            key={m.id}
            disabled={m.disabled}
            onClick={() => !m.disabled && setSelectedModel(m.id)}
            className={`rounded-full px-3.5 py-1.5 text-xs transition-colors ${
              m.disabled
                ? "cursor-not-allowed text-muted-gray/40 line-through"
                : selectedModel === m.id
                  ? "bg-charcoal text-off-white"
                  : "border border-border-warm text-muted-gray hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal"
            }`}
          >
            {m.label}
            {m.disabled && " (即将推出)"}
          </button>
        ))}
      </div>

      {/* Recent projects */}
      <div className="mt-16 w-full max-w-[840px]">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-medium text-charcoal">最近项目</h2>
          <Link
            href="/projects"
            className="text-xs text-muted-gray hover:text-charcoal"
          >
            查看全部
          </Link>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          <button
            type="button"
            onClick={() => { void openNewProject(); }}
            className="flex aspect-square flex-col items-center justify-center rounded-xl border-2 border-dashed border-border-warm text-muted-gray transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal"
          >
            <FolderPlus className="size-6" />
            <span className="mt-1.5 text-xs">新建项目</span>
          </button>

          {recentProjects.map((project) => (
            <Link
              key={project.id}
              href={`/canvas?projectId=${encodeURIComponent(project.id)}`}
              className="group overflow-hidden rounded-xl border border-border-warm bg-background transition-colors hover:border-[rgba(28,28,28,0.4)]"
            >
              <div className="flex aspect-square items-center justify-center bg-muted text-muted-gray">
                <ProjectIcon project={project} />
              </div>
              <div className="p-3">
                <p className="truncate text-xs font-medium text-charcoal">{project.name}</p>
                <p className="mt-1 text-[11px] text-muted-gray">{formatDate(project.lastOpenedAt)}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
