"use client";

export interface ProjectMeta {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
  lastOpenedAt: string;
  nodeCount: number;
  assetCount: number;
  thumbnailUrl?: string;
}

const PROJECTS_KEY = "copse_projects_v1";

function now() {
  return new Date().toISOString();
}

function readProjects(): ProjectMeta[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(PROJECTS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((project): project is ProjectMeta => {
      return Boolean(project && typeof project.id === "string" && typeof project.name === "string");
    });
  } catch {
    return [];
  }
}

function writeProjects(projects: ProjectMeta[]) {
  localStorage.setItem(PROJECTS_KEY, JSON.stringify(projects));
  window.dispatchEvent(new CustomEvent("copse:projects-changed"));
}

export function listProjects(): ProjectMeta[] {
  return readProjects().sort((a, b) => {
    return new Date(b.lastOpenedAt).getTime() - new Date(a.lastOpenedAt).getTime();
  });
}

export function getProject(projectId: string): ProjectMeta | null {
  return readProjects().find((project) => project.id === projectId) ?? null;
}

export function createProject(input: Partial<Pick<ProjectMeta, "name">> = {}): ProjectMeta {
  const timestamp = now();
  const projects = readProjects();
  const project: ProjectMeta = {
    id: `project_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    name: input.name?.trim() || "未命名项目",
    createdAt: timestamp,
    updatedAt: timestamp,
    lastOpenedAt: timestamp,
    nodeCount: 0,
    assetCount: 0,
  };
  writeProjects([project, ...projects]);
  return project;
}

export function touchProject(projectId: string): void {
  const projects = readProjects();
  const timestamp = now();
  writeProjects(projects.map((project) => (
    project.id === projectId ? { ...project, lastOpenedAt: timestamp, updatedAt: timestamp } : project
  )));
}

export function updateProject(projectId: string, patch: Partial<Omit<ProjectMeta, "id" | "createdAt">>): void {
  const projects = readProjects();
  const timestamp = now();
  writeProjects(projects.map((project) => (
    project.id === projectId ? { ...project, ...patch, updatedAt: patch.updatedAt ?? timestamp } : project
  )));
}

export function renameProject(projectId: string, name: string): void {
  const trimmed = name.trim();
  if (!trimmed) return;
  updateProject(projectId, { name: trimmed });
}

export function deleteProject(projectId: string): void {
  writeProjects(readProjects().filter((project) => project.id !== projectId));
}

