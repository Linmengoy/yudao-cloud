import { getAccessToken } from "@/lib/api-client";
import type { CanvasPresence } from "@/features/canvas/types";

export type CanvasRealtimeMessage =
  | { type: "canvas-join"; projectId: string; clientId: string; lastAppliedVersion: number }
  | { type: "canvas-leave"; projectId: string; clientId: string }
  | ({ type: "canvas-presence" } & CanvasPresence)
  | {
      type: "canvas-op-applied";
      projectId: number;
      clientId: string;
      opId: string;
      actorUserId?: number;
      baseVersion: number;
      version: number;
      operationType: string;
      operationJson: string;
      inverseOperationJson?: string | null;
    }
  | {
      type: "canvas-op-rejected";
      projectId: number;
      clientId: string;
      opId: string;
      reason?: string;
      message?: string;
      serverVersion?: number;
    }
  | ({ type: "canvas-member-updated"; projectId: number; clientId?: string; userId?: number; event?: "join" | "leave" } & Record<string, unknown>)
  | { type: string; [key: string]: unknown };

function getWebSocketBaseUrl() {
  const configured = process.env.NEXT_PUBLIC_WS_BASE_URL;
  if (configured) return configured.replace(/\/$/, "");
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";
  if (apiBase.startsWith("https://")) return apiBase.replace(/^https:\/\//, "wss://").replace(/\/$/, "");
  if (apiBase.startsWith("http://")) return apiBase.replace(/^http:\/\//, "ws://").replace(/\/$/, "");
  if (typeof window !== "undefined") {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${protocol}//${window.location.host}`;
  }
  return "";
}

// 创建画布WebSocket
export function createCanvasWebSocket(onMessage: (message: CanvasRealtimeMessage) => void) {
  const token = getAccessToken();
  const baseUrl = getWebSocketBaseUrl();
  const tokenQuery = token ? `?token=${encodeURIComponent(token)}` : "";
  const socket = new WebSocket(`${baseUrl}/aigc/workflow/ws${tokenQuery}`);

  socket.addEventListener("message", (event) => {
    try {
      const frame = JSON.parse(event.data);
      const content = typeof frame.content === "string" ? JSON.parse(frame.content) : frame.content;
      onMessage({ type: frame.type, ...content });
    } catch {
    }
  });

  return socket;
}

export function sendCanvasFrame(socket: WebSocket | null, type: string, content: unknown) {
  if (!socket || socket.readyState !== WebSocket.OPEN) return false;
  socket.send(JSON.stringify({ type, content: JSON.stringify(content) }));
  return true;
}

export function createCanvasOperationMessage(
  projectId: string,
  clientId: string,
  baseVersion: number,
  operationType: string,
  payload: Record<string, unknown>,
  opId?: string
) {
  return {
    projectId: Number(projectId),
    clientId,
    opId: opId ?? `op_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    baseVersion,
    operationType,
    operationJson: JSON.stringify({ type: operationType, payload }),
  };
}
