"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { canvasApi } from "@/features/canvas/canvas-api";

type PendingCanvasOperation = {
  opId: string;
  operationType: string;
  payload: Record<string, unknown>;
  baseVersion: number;
  retryAt: number;
  attempts: number;
  sending: boolean;
  reliable: boolean;
};

function createOperationId() {
  return `op_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function isNonRetryableOperationError(error: unknown) {
  if (!(error instanceof Error)) return false;
  return error.message.includes("请求参数不正确") || error.message.includes("画布操作内容不合法");
}

export function useCanvasOperations(
  projectId: string | null,
  clientId: string,
  baseVersion: number,
  onVersionApplied: (version: number) => void,
  sendRealtimeOperation?: (operationType: string, payload: Record<string, unknown>, opId?: string, baseVersion?: number) => boolean,
  isRealtimeConnected = false
) {
  const pendingRef = useRef<Map<string, PendingCanvasOperation>>(new Map());
  const flushPendingOperationsRef = useRef<() => void>(() => undefined);
  const [pendingOperationCount, setPendingOperationCount] = useState(0);

  const updatePendingOperationCount = useCallback(() => {
    setPendingOperationCount(pendingRef.current.size);
  }, []);

  const scheduleFlushPendingOperations = useCallback(() => {
    window.setTimeout(() => flushPendingOperationsRef.current(), 0);
  }, []);

  const flushPendingOperations = useCallback(() => {
    if (!projectId) return;
    const now = Date.now();
    if ([...pendingRef.current.values()].some((operation) => operation.sending)) return;

    const operation = [...pendingRef.current.values()].find((item) => item.retryAt <= now);
    if (!operation) return;

    if (!operation.reliable && sendRealtimeOperation?.(operation.operationType, operation.payload, operation.opId, operation.baseVersion)) {
      operation.sending = true;
      operation.attempts += 1;
      operation.retryAt = now + Math.min(20_000, 2_000 * 2 ** Math.min(operation.attempts, 4));
      return;
    }
    operation.sending = true;
    operation.attempts += 1;
    operation.retryAt = now + Math.min(20_000, 2_000 * 2 ** Math.min(operation.attempts, 4));
    canvasApi.submitOperation(projectId, {
      clientId,
      opId: operation.opId,
      baseVersion: operation.baseVersion,
      operationType: operation.operationType,
      operationJson: JSON.stringify({ type: operation.operationType, payload: operation.payload }),
    }).then((record) => {
      pendingRef.current.delete(operation.opId);
      onVersionApplied(record.nextVersion);
      updatePendingOperationCount();
      scheduleFlushPendingOperations();
    }).catch((error) => {
      if (isNonRetryableOperationError(error)) {
        pendingRef.current.delete(operation.opId);
        updatePendingOperationCount();
        scheduleFlushPendingOperations();
        return;
      }
      operation.sending = false;
    });
  }, [clientId, onVersionApplied, projectId, scheduleFlushPendingOperations, sendRealtimeOperation, updatePendingOperationCount]);

  useEffect(() => {
    flushPendingOperationsRef.current = flushPendingOperations;
  }, [flushPendingOperations]);

  const submitOperation = useCallback((operationType: string, payload: Record<string, unknown>, options?: { reliable?: boolean }) => {
    if (!projectId) return;
    const pendingKey = `${operationType}:${JSON.stringify(payload)}`;
    for (const operation of pendingRef.current.values()) {
      if (`${operation.operationType}:${JSON.stringify(operation.payload)}` === pendingKey) return;
    }
    const opId = createOperationId();
    pendingRef.current.set(opId, {
      opId,
      operationType,
      payload,
      baseVersion,
      retryAt: 0,
      attempts: 0,
      sending: false,
      reliable: options?.reliable === true,
    });
    updatePendingOperationCount();
    flushPendingOperations();
  }, [baseVersion, flushPendingOperations, projectId, updatePendingOperationCount]);

  const markOperationAcked = useCallback((opId: string, version: number) => {
    if (!pendingRef.current.delete(opId)) return;
    onVersionApplied(version);
    updatePendingOperationCount();
    scheduleFlushPendingOperations();
  }, [onVersionApplied, scheduleFlushPendingOperations, updatePendingOperationCount]);

  const markOperationRejected = useCallback((opId: string) => {
    const operation = pendingRef.current.get(opId);
    if (!operation) return;
    operation.sending = false;
    operation.retryAt = 0;
    operation.baseVersion = baseVersion;
    flushPendingOperations();
  }, [baseVersion, flushPendingOperations]);

  useEffect(() => {
    flushPendingOperations();
  }, [flushPendingOperations, isRealtimeConnected]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      for (const operation of pendingRef.current.values()) {
        if (operation.sending && operation.retryAt <= Date.now()) operation.sending = false;
      }
      flushPendingOperations();
    }, 2_000);
    return () => window.clearInterval(timer);
  }, [flushPendingOperations]);

  return {
    submitOperation,
    markOperationAcked,
    markOperationRejected,
    flushPendingOperations,
    pendingOperationCount,
  };
}
