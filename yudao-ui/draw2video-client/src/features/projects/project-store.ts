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

function scopedProjectsKey(ownerKey?: string | number | null) {
  return ownerKey == null ? PROJECTS_KEY : `${PROJECTS_KEY}:${ownerKey}`;
}

function readProjects(ownerKey?: string | number | null): ProjectMeta[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(scopedProjectsKey(ownerKey));
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

function writeProjects(projects: ProjectMeta[], ownerKey?: string | number | null) {
  localStorage.setItem(scopedProjectsKey(ownerKey), JSON.stringify(projects));
  window.dispatchEvent(new CustomEvent("copse:projects-changed"));
}

export function listProjects(ownerKey?: string | number | null): ProjectMeta[] {
  return readProjects(ownerKey).sort((a, b) => {
    return new Date(b.lastOpenedAt).getTime() - new Date(a.lastOpenedAt).getTime();
  });
}

export function getProject(projectId: string, ownerKey?: string | number | null): ProjectMeta | null {
  return readProjects(ownerKey).find((project) => project.id === projectId) ?? null;
}

export function createProject(input: Partial<Pick<ProjectMeta, "name">> = {}, ownerKey?: string | number | null): ProjectMeta {
  const timestamp = now();
  const projects = readProjects(ownerKey);
  const project: ProjectMeta = {
    id: `project_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    name: input.name?.trim() || "未命名项目",
    createdAt: timestamp,
    updatedAt: timestamp,
    lastOpenedAt: timestamp,
    nodeCount: 0,
    assetCount: 0,
  };
  writeProjects([project, ...projects], ownerKey);
  return project;
}

export function touchProject(projectId: string, ownerKey?: string | number | null): void {
  const projects = readProjects(ownerKey);
  const timestamp = now();
  writeProjects(projects.map((project) => (
    project.id === projectId ? { ...project, lastOpenedAt: timestamp, updatedAt: timestamp } : project
  )), ownerKey);
}

export function updateProject(
  projectId: string,
  patch: Partial<Omit<ProjectMeta, "id" | "createdAt">>,
  ownerKey?: string | number | null
): void {
  const projects = readProjects(ownerKey);
  const timestamp = now();
  writeProjects(projects.map((project) => (
    project.id === projectId ? { ...project, ...patch, updatedAt: patch.updatedAt ?? timestamp } : project
  )), ownerKey);
}

export function renameProject(projectId: string, name: string, ownerKey?: string | number | null): void {
  const trimmed = name.trim();
  if (!trimmed) return;
  updateProject(projectId, { name: trimmed }, ownerKey);
}

export function deleteProject(projectId: string, ownerKey?: string | number | null): void {
  writeProjects(readProjects(ownerKey).filter((project) => project.id !== projectId), ownerKey);
}
