"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  createCanvasOperationMessage,
  createCanvasWebSocket,
  sendCanvasFrame,
  type CanvasRealtimeMessage,
} from "@/features/canvas/canvas-realtime";
import type { CanvasPresence } from "@/features/canvas/types";

export function useCanvasRealtime(projectId: string | null, clientId: string, lastAppliedVersion: number) {
  const socketRef = useRef<WebSocket | null>(null);
  const lastAppliedVersionRef = useRef(lastAppliedVersion);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptRef = useRef(0);
  const closedByCleanupRef = useRef(false);
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<CanvasRealtimeMessage[]>([]);

  useEffect(() => {
    lastAppliedVersionRef.current = lastAppliedVersion;
  }, [lastAppliedVersion]);

  useEffect(() => {
    if (!projectId || typeof window === "undefined") return;
    closedByCleanupRef.current = false;

    const connect = () => {
      const socket = createCanvasWebSocket((message) => {
        setMessages((prev) => [...prev.slice(-100), message]);
      });
      socketRef.current = socket;

      socket.addEventListener("open", () => {
        reconnectAttemptRef.current = 0;
        setIsConnected(true);
        sendCanvasFrame(socket, "canvas-join", { projectId, clientId, lastAppliedVersion: lastAppliedVersionRef.current });
      });
      socket.addEventListener("close", () => {
        if (socketRef.current === socket) socketRef.current = null;
        setIsConnected(false);
        if (closedByCleanupRef.current) return;
        const attempt = Math.min(reconnectAttemptRef.current + 1, 6);
        reconnectAttemptRef.current = attempt;
        reconnectTimerRef.current = window.setTimeout(connect, Math.min(30_000, 1_000 * 2 ** (attempt - 1)));
      });
      socket.addEventListener("error", () => setIsConnected(false));
    };

    connect();

    return () => {
      closedByCleanupRef.current = true;
      if (reconnectTimerRef.current) window.clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
      const socket = socketRef.current;
      sendCanvasFrame(socket, "canvas-leave", { projectId, clientId });
      socket?.close();
      socketRef.current = null;
      setIsConnected(false);
    };
  }, [clientId, projectId]);

  const sendPresence = useCallback((presence: Omit<CanvasPresence, "projectId" | "clientId">) => {
    if (!projectId) return false;
    return sendCanvasFrame(socketRef.current, "canvas-presence", { projectId, clientId, ...presence });
  }, [clientId, projectId]);

  const sendOperation = useCallback((operationType: string, payload: Record<string, unknown>, opId?: string, baseVersion?: number) => {
    if (!projectId) return false;
    return sendCanvasFrame(
      socketRef.current,
      "canvas-op",
      createCanvasOperationMessage(projectId, clientId, baseVersion ?? lastAppliedVersionRef.current, operationType, payload, opId)
    );
  }, [clientId, projectId]);

  return { isConnected, messages, sendPresence, sendOperation };
}
