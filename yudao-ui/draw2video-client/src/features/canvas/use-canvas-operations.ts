"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasOperationRecord } from "@/features/canvas/types";
import { API_BASE_URL, API_TENANT_ID, API_TERMINAL, getAccessToken } from "@/lib/api-client";

type PendingCanvasOperation = {
  opId: string;
  operationType: string;
  payload: Record<string, unknown>;
  baseVersion: number;
  retryAt: number;
  attempts: number;
  sending: boolean;
  reliable: boolean;
  resolve: (result: CanvasOperationRecord) => void;
  reject: (error: Error) => void;
};

const MAX_OPERATION_ATTEMPTS = 6;

export type CanvasOperationSubmitResult = {
  opId: string;
  promise: Promise<CanvasOperationRecord>;
};

function createOperationId() {
  return `op_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function isNonRetryableOperationError(error: unknown) {
  if (!(error instanceof Error)) return false;
  return error.message.includes("请求参数不正确") || error.message.includes("画布操作内容不合法");
}

function createOperationError(message: string) {
  return new Error(message);
}

function buildOperationBody(projectId: string, clientId: string, operation: PendingCanvasOperation) {
  return {
    projectId,
    clientId,
    opId: operation.opId,
    baseVersion: operation.baseVersion,
    operationType: operation.operationType,
    operationJson: JSON.stringify({ type: operation.operationType, payload: operation.payload }),
  };
}

function submitKeepaliveOperation(projectId: string, clientId: string, operation: PendingCanvasOperation) {
  const token = getAccessToken();
  const headers: Record<string, string> = {
    Accept: "*/*",
    "Content-Type": "application/json;charset=UTF-8",
    "tenant-id": API_TENANT_ID,
    terminal: API_TERMINAL,
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const body = JSON.stringify(buildOperationBody(projectId, clientId, operation));
  const url = `${API_BASE_URL}/canvas/projects/${projectId}/operations`;
  if (!token && navigator.sendBeacon && body.length < 60_000) {
    const blob = new Blob([body], { type: "application/json;charset=UTF-8" });
    if (navigator.sendBeacon(url, blob)) return;
  }
  void fetch(url, {
    method: "POST",
    headers,
    body,
    keepalive: true,
  });
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
  const flushPendingOperationsRef = useRef<(options?: { keepalive?: boolean }) => void>(() => undefined);
  const [pendingOperationCount, setPendingOperationCount] = useState(0);

  const updatePendingOperationCount = useCallback(() => {
    setPendingOperationCount(pendingRef.current.size);
  }, []);

  const scheduleFlushPendingOperations = useCallback(() => {
    window.setTimeout(() => flushPendingOperationsRef.current(), 0);
  }, []);

  const settleOperationSuccess = useCallback((operation: PendingCanvasOperation, record: CanvasOperationRecord) => {
    pendingRef.current.delete(operation.opId);
    operation.resolve(record);
    onVersionApplied(record.nextVersion);
    updatePendingOperationCount();
    scheduleFlushPendingOperations();
  }, [onVersionApplied, scheduleFlushPendingOperations, updatePendingOperationCount]);

  const settleOperationFailure = useCallback((operation: PendingCanvasOperation, error: Error) => {
    pendingRef.current.delete(operation.opId);
    operation.reject(error);
    updatePendingOperationCount();
    scheduleFlushPendingOperations();
  }, [scheduleFlushPendingOperations, updatePendingOperationCount]);

  const flushPendingOperations = useCallback((options?: { keepalive?: boolean }) => {
    if (!projectId) return;
    const now = Date.now();
    if (!options?.keepalive && [...pendingRef.current.values()].some((operation) => operation.sending)) return;

    const operations = [...pendingRef.current.values()]
      .filter((item) => options?.keepalive || item.retryAt <= now)
      .filter((item) => options?.keepalive || !item.sending);
    const operation = operations[0];
    if (!operation) return;

    if (options?.keepalive) {
      for (const pendingOperation of operations) {
        submitKeepaliveOperation(projectId, clientId, pendingOperation);
      }
      return;
    }

    if (!operation.reliable && sendRealtimeOperation?.(operation.operationType, operation.payload, operation.opId, operation.baseVersion)) {
      if (operation.attempts >= MAX_OPERATION_ATTEMPTS) {
        settleOperationFailure(operation, createOperationError("画布操作确认超时，请检查网络后重试"));
        return;
      }
      operation.sending = true;
      operation.attempts += 1;
      operation.retryAt = now + Math.min(20_000, 2_000 * 2 ** Math.min(operation.attempts, 4));
      return;
    }
    if (operation.attempts >= MAX_OPERATION_ATTEMPTS) {
      settleOperationFailure(operation, createOperationError("画布操作提交失败，请检查网络后重试"));
      return;
    }
    operation.sending = true;
    operation.attempts += 1;
    operation.retryAt = now + Math.min(20_000, 2_000 * 2 ** Math.min(operation.attempts, 4));
    canvasApi.submitOperation(projectId, buildOperationBody(projectId, clientId, operation)).then((record) => {
      settleOperationSuccess(operation, record);
    }).catch((error) => {
      if (isNonRetryableOperationError(error)) {
        settleOperationFailure(operation, error instanceof Error ? error : createOperationError("画布操作提交失败"));
        return;
      }
      operation.sending = false;
    });
  }, [clientId, projectId, sendRealtimeOperation, settleOperationFailure, settleOperationSuccess]);

  useEffect(() => {
    flushPendingOperationsRef.current = flushPendingOperations;
  }, [flushPendingOperations]);

  const submitOperation = useCallback((operationType: string, payload: Record<string, unknown>, options?: { reliable?: boolean }): CanvasOperationSubmitResult | undefined => {
    if (!projectId) return undefined;
    const pendingKey = `${operationType}:${JSON.stringify(payload)}`;
    for (const operation of pendingRef.current.values()) {
      if (`${operation.operationType}:${JSON.stringify(operation.payload)}` === pendingKey) {
        const promise = new Promise<CanvasOperationRecord>((resolve, reject) => {
          const previousResolve = operation.resolve;
          const previousReject = operation.reject;
          operation.resolve = (result) => {
            previousResolve(result);
            resolve(result);
          };
          operation.reject = (error) => {
            previousReject(error);
            reject(error);
          };
        });
        promise.catch(() => undefined);
        return {
          opId: operation.opId,
          promise,
        };
      }
    }
    const opId = createOperationId();
    let resolveOperation: (result: CanvasOperationRecord) => void = () => undefined;
    let rejectOperation: (error: Error) => void = () => undefined;
    const promise = new Promise<CanvasOperationRecord>((resolve, reject) => {
      resolveOperation = resolve;
      rejectOperation = reject;
    });
    promise.catch(() => undefined);
    pendingRef.current.set(opId, {
      opId,
      operationType,
      payload,
      baseVersion,
      retryAt: 0,
      attempts: 0,
      sending: false,
      reliable: options?.reliable === true,
      resolve: resolveOperation,
      reject: rejectOperation,
    });
    updatePendingOperationCount();
    flushPendingOperations();
    return { opId, promise };
  }, [baseVersion, flushPendingOperations, projectId, updatePendingOperationCount]);

  const markOperationAcked = useCallback((opId: string, version: number) => {
    const operation = pendingRef.current.get(opId);
    if (!operation) return;
    settleOperationSuccess(operation, {
      id: 0,
      projectId: Number(projectId),
      clientId,
      opId,
      actorUserId: 0,
      baseVersion: operation.baseVersion,
      nextVersion: version,
      operationType: operation.operationType,
      operationJson: JSON.stringify({ type: operation.operationType, payload: operation.payload }),
      inverseOperationJson: null,
    });
  }, [clientId, projectId, settleOperationSuccess]);

  const markOperationRejected = useCallback((opId: string) => {
    const operation = pendingRef.current.get(opId);
    if (!operation) return;
    settleOperationFailure(operation, createOperationError("画布操作被服务端拒绝"));
  }, [settleOperationFailure]);

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
