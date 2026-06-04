"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FolderPlus, ImageIcon, Paperclip, Plus, Send } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProject } from "@/features/canvas/types";
import { getAigcModelList, type AigcModel } from "@/features/generation/model-api";
import { createProject, listProjects, type ProjectMeta } from "@/features/projects/project-store";

const MODEL_TYPE_LABELS: Record<number, string> = {
  1: "文本",
  2: "图片",
  3: "视频",
  4: "音频",
  5: "审核",
};

const MODEL_TYPE_ORDER = [2, 3, 4, 1, 5];

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
  lastOpenedAt: string;
  coverUrl?: string | null;
  role?: string | null;
  source: "server" | "local";
};

function toProjectListItem(project: CanvasProject): ProjectListItem {
  return {
    id: String(project.id),
    name: project.name,
    lastOpenedAt: project.updateTime ?? project.createTime ?? "",
    coverUrl: project.coverUrl,
    role: project.role,
    source: "server",
  };
}

function localProjectToListItem(project: ProjectMeta): ProjectListItem {
  return {
    id: project.id,
    name: project.name,
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

function ProjectIcon() {
  return <ImageIcon className="size-5" />;
}

function ProjectCover({ project }: { project: ProjectListItem }) {
  if (project.coverUrl) {
    return (
      <div className="bg-muted">
        <img
          src={project.coverUrl}
          alt={`${project.name} 封面`}
          draggable={false}
          className="block h-auto max-w-full"
        />
      </div>
    );
  }
  return (
    <div className="flex aspect-square items-center justify-center bg-muted text-muted-gray">
      <ProjectIcon />
    </div>
  );
}

export default function WorkspacePage() {
  const router = useRouter();
  const [prompt, setPrompt] = useState("");
  const [models, setModels] = useState<AigcModel[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<number | null>(null);
  const [activeModelType, setActiveModelType] = useState<number | null>(null);
  const [modelsLoading, setModelsLoading] = useState(false);
  const [modelsError, setModelsError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [projects, setProjects] = useState<ProjectListItem[]>([]);
  const recentProjects = useMemo(() => projects.slice(0, 7), [projects]);
  const modelGroups = useMemo(() => {
    const groups = new Map<number, AigcModel[]>();
    for (const model of models) {
      groups.set(model.type, [...(groups.get(model.type) ?? []), model]);
    }
    return [...groups.entries()].sort(([a], [b]) => {
      const aIndex = MODEL_TYPE_ORDER.indexOf(a);
      const bIndex = MODEL_TYPE_ORDER.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return a - b;
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [models]);
  const selectedModel = useMemo(
    () => models.find((model) => model.id === selectedModelId) ?? null,
    [models, selectedModelId]
  );

  async function refreshProjects() {
    try {
      const page = await canvasApi.listProjects({ pageNo: 1, pageSize: 12 });
      setProjects(page.list.map(toProjectListItem));
    } catch {
      setProjects(listProjects().map(localProjectToListItem));
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(refreshProjects, 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    let ignore = false;
    const timer = window.setTimeout(() => {
      setModelsLoading(true);
      setModelsError(null);
      getAigcModelList()
        .then((data) => {
          if (ignore) return;
          setModels(data);
          setSelectedModelId((current) => {
            if (data.some((item) => item.id === current)) return current;
            return data.find((item) => item.defaultModel)?.id ?? data[0]?.id ?? null;
          });
          setActiveModelType(null);
        })
        .catch((err) => {
          if (!ignore) setModelsError(err instanceof Error ? err.message : "模型列表加载失败");
        })
        .finally(() => {
          if (!ignore) setModelsLoading(false);
        });
    }, 0);
    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, []);

  async function openNewProject(name?: string) {
    const projectName = name?.trim() || "未命名项目";
    try {
      const projectId = await canvasApi.createProject({ name: projectName });
      router.push(`/canvas?projectId=${encodeURIComponent(String(projectId))}`);
    } catch {
      const project = createProject({ name: projectName });
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
            <div className="flex flex-wrap items-center gap-1.5">
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
              {modelsLoading && models.length === 0 && (
                <span className="px-2 text-xs text-muted-gray">模型加载中...</span>
              )}
              {!modelsLoading && modelsError && models.length === 0 && (
                <span className="px-2 text-xs text-muted-gray">{modelsError}</span>
              )}
              {!modelsLoading && !modelsError && models.length === 0 && (
                <span className="px-2 text-xs text-muted-gray">暂无可用模型</span>
              )}
              {modelGroups.map(([type, items]) => (
                <select
                  key={type}
                  value={activeModelType === type && selectedModel?.type === type ? String(selectedModel.id) : ""}
                  onChange={(event) => {
                    setSelectedModelId(Number(event.target.value));
                    setActiveModelType(type);
                  }}
                  className="h-8 rounded-lg border border-border-warm bg-background px-2 text-xs text-muted-gray outline-none transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal focus:border-[rgba(28,28,28,0.55)]"
                >
                  <option value="" disabled>{MODEL_TYPE_LABELS[type] ?? `类型 ${type}`}</option>
                  {items.map((model) => (
                    <option key={model.id} value={model.id}>{model.name}</option>
                  ))}
                </select>
              ))}
            </div>
            <div className="flex items-center gap-2">
              {selectedModel && (
                <span className="max-w-40 truncate rounded-full bg-muted px-2.5 py-1 text-xs text-muted-gray" title={selectedModel.name}>
                  {selectedModel.name}
                </span>
              )}
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

        <div className="mt-4 columns-2 gap-4 sm:columns-3 lg:columns-4">


          {recentProjects.map((project) => (
            <Link
              key={project.id}
              href={`/canvas?projectId=${encodeURIComponent(project.id)}`}
              className="group relative mb-4 inline-block w-full break-inside-avoid overflow-hidden rounded-xl border border-border-warm bg-background align-top transition-colors hover:border-[rgba(28,28,28,0.4)]"
            >
              <ProjectCover project={project} />
              <div className="pointer-events-none absolute inset-x-0 top-0 p-6">
                <p className="truncate text-1xl font-bold text-white drop-shadow-[0_2px_4px_rgba(0,0,0,0.65)]">{project.name}</p>
              </div>
              <div className="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/55 to-transparent px-3 pt-10 pb-3 text-center">
                <p className="text-[11px] font-medium text-white/90 drop-shadow-[0_1px_2px_rgba(0,0,0,0.7)]">{projectRoleLabel(project)} · {formatDate(project.lastOpenedAt)}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
