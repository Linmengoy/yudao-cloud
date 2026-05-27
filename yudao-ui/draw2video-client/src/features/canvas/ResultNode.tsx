"use client";

/* eslint-disable @next/next/no-img-element */

import { useCallback, useEffect, useMemo, useState } from "react";
import type { NodeProps, Node } from "@xyflow/react";
import { Handle, Position, useReactFlow } from "@xyflow/react";
import { X, AlertCircle, RefreshCw, ImageIcon } from "lucide-react";
import type { PromptNodeData, ResultNodeData } from "./types";
import { createGenerationTask } from "./use-generation";

type ResultNodeProps = NodeProps<Node<ResultNodeData, "result">>;

function sizeToDimensions(size: string): { width: number; height: number } {
  const match = size.match(/^(\d+)[xX×](\d+)$/);
  if (match) {
    const w = Number(match[1]);
    const h = Number(match[2]);
    const maxDim = 220;
    const scale = maxDim / Math.max(w, h);
    return { width: Math.round(w * scale), height: Math.round(h * scale) };
  }
  return { width: 220, height: 220 };
}

function formatElapsed(ms: number | null | undefined) {
  if (!ms || ms < 0) return "0s";
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  if (totalSeconds < 60) return `${totalSeconds}s`;
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}m ${seconds}s`;
}

export function ResultNodeComponent({ id, data }: ResultNodeProps) {
  const { setNodes, setEdges } = useReactFlow();
  const dims = sizeToDimensions(data.params.size);
  const createdAtMs = useMemo(() => new Date(data.createdAt).getTime(), [data.createdAt]);
  const [now, setNow] = useState(createdAtMs);

  useEffect(() => {
    if (data.status !== "pending") return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [createdAtMs, data.status]);

  const elapsedMs = data.elapsedMs ?? (
    data.status === "pending"
      ? now - createdAtMs
      : data.completedAt
        ? new Date(data.completedAt).getTime() - createdAtMs
        : null
  );

  const handleDelete = useCallback(() => {
    setNodes((nds) => nds.filter((n) => n.id !== id));
    setEdges((eds) => eds.filter((e) => e.target !== id));
  }, [id, setNodes, setEdges]);

  const handleRetry = useCallback(async () => {
    if (data.status === "pending") return;

    const createdAt = new Date().toISOString();
    const retryDraft: ResultNodeData = {
      ...data,
      taskId: null,
      status: "pending",
      imageUrls: [],
      errorMessage: null,
      createdAt,
      completedAt: null,
      elapsedMs: null,
    };
    const promptData: PromptNodeData = {
      prompt: data.prompt,
      modelId: data.modelId,
      params: data.params,
      isGenerating: true,
    };

    setNodes((nds) =>
      nds.map((node) => {
        if (node.id === id) return { ...node, data: retryDraft };
        if (node.id === data.promptNodeId) {
          return { ...node, data: { ...node.data, isGenerating: true } };
        }
        return node;
      })
    );

    try {
      const updates = await createGenerationTask(promptData, retryDraft);
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === id) return { ...node, data: { ...node.data, ...updates } };
          if (node.id === data.promptNodeId) {
            return { ...node, data: { ...node.data, isGenerating: false } };
          }
          return node;
        })
      );
    } catch (error) {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === id) {
            return {
              ...node,
              data: {
                ...node.data,
                status: "failed",
                imageUrls: [],
                errorMessage: error instanceof Error ? error.message : "生成失败",
                elapsedMs: Date.now() - new Date(createdAt).getTime(),
                completedAt: new Date().toISOString(),
              },
            };
          }
          if (node.id === data.promptNodeId) {
            return { ...node, data: { ...node.data, isGenerating: false } };
          }
          return node;
        })
      );
    }
  }, [data, id, setNodes]);

  return (
    <div
      className="rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]"
      style={{
        width: dims.width + 24,
        borderColor: data.status === "failed" ? "var(--destructive)" : undefined,
      }}
    >
      <Handle
        type="target"
        position={Position.Left}
        className="!size-3 !border-2 !border-border-warm !bg-background"
      />

      {/* Header */}
      <div className="flex items-center justify-between px-2 pt-2">
        <div className="flex items-center gap-1">
          <span className="text-[10px] text-muted-gray">{data.modelName}</span>
          {data.mode === "edit" && (
            <span className="rounded border border-border-warm px-1 text-[9px] text-muted-gray">
              参考图
            </span>
          )}
          <span className="rounded border border-border-warm px-1 text-[9px] text-muted-gray">
            {data.status === "pending" ? "处理中" : data.status === "complete" ? "完成" : "失败"} {formatElapsed(elapsedMs)}
          </span>
        </div>
        <button
          onClick={handleDelete}
          className="nodrag nowheel flex size-5 items-center justify-center rounded text-muted-gray hover:bg-muted hover:text-charcoal"
        >
          <X className="size-3" />
        </button>
      </div>

      {/* Content */}
      <div className="flex items-center justify-center px-3 pb-1 pt-1">
        {data.status === "pending" && (
          <div
            className="flex flex-col items-center justify-center rounded-lg bg-muted"
            style={{ width: dims.width, height: dims.height }}
          >
            <div className="mb-2 size-6 animate-spin rounded-full border-2 border-border-warm border-t-charcoal" />
            <span className="text-xs text-muted-gray">生成中...</span>
          </div>
        )}

        {data.status === "complete" && (
          <div
            className="flex items-center justify-center overflow-hidden rounded-lg bg-muted"
            style={{ width: dims.width, height: dims.height }}
          >
            {data.imageUrls.length > 0 ? (
              data.imageUrls.length === 1 ? (
                <img
                  src={data.imageUrls[0]}
                  alt={data.prompt || "生成图片"}
                  className="size-full object-cover"
                  draggable={false}
                />
              ) : (
                <div className="grid size-full grid-cols-2 gap-1 p-1">
                  {data.imageUrls.slice(0, 4).map((url, index) => (
                    <div key={`${url}-${index}`} className="relative overflow-hidden rounded bg-background">
                      <img
                        src={url}
                        alt={`${data.prompt || "生成图片"} ${index + 1}`}
                        className="size-full object-cover"
                        draggable={false}
                      />
                      {index === 3 && data.imageUrls.length > 4 && (
                        <div className="absolute inset-0 flex items-center justify-center bg-charcoal/60 text-xs text-off-white">
                          +{data.imageUrls.length - 4}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )
            ) : (
              <ImageIcon className="size-8 text-muted-gray/40" />
            )}
          </div>
        )}

        {data.status === "failed" && (
          <div
            className="flex flex-col items-center justify-center rounded-lg"
            style={{
              width: dims.width,
              height: dims.height,
              backgroundColor: "rgba(229,62,62,0.05)",
            }}
          >
            <AlertCircle className="size-6 text-destructive" />
            <span
              className="mt-1 max-w-[90%] text-center text-xs text-destructive"
              title={data.errorMessage ?? "生成失败"}
            >
              {data.errorMessage ?? "生成失败"}
            </span>
            <button
              onClick={handleRetry}
              className="nodrag nowheel mt-2 flex items-center gap-1 text-[10px] text-muted-gray hover:text-charcoal"
            >
              <RefreshCw className="size-3" />
              重试
            </button>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="px-3 pb-2 pt-1">
        <p className="line-clamp-1 text-[10px] text-muted-gray">{data.prompt}</p>
        {data.mode === "edit" && data.inputImages.length > 0 && (
          <p className="mt-0.5 text-[10px] text-muted-gray">
            参考图 {data.inputImages.length} 张
          </p>
        )}
        {data.status !== "pending" && (
          <p className="mt-0.5 text-[10px] text-muted-gray">耗时 {formatElapsed(elapsedMs)}</p>
        )}
      </div>
    </div>
  );
}
