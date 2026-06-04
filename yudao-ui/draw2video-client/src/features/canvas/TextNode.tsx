"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import { ArrowUp, Loader2, Sparkles, Text, X } from "lucide-react";
import type { NodeDataPatchEventDetail, NodeEditingPresenceEventDetail, TextNodeData } from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { generationApi } from "@/features/generation/generation-api";
import { waitGenerationResult } from "@/features/generation/generation-poll";
import { canvasNodeRunApi, getCanvasNodeRunPatch, isServerCanvasProjectId, waitCanvasNodeRunResult } from "@/features/canvas/canvas-node-run-api";
import { cn } from "@/lib/utils";
import { EditableNodeTitle } from "./EditableNodeTitle";

type TextNodeProps = NodeProps<Node<TextNodeData, "text">>;

const MIN_WIDTH = 220;
const MIN_HEIGHT = 160;
const DEFAULT_MODEL = "Gemini 3.1 Flash Lite";
const COMPOSER_WIDTH = 620;

export function TextNodeComponent({ id, data, selected, dragging }: TextNodeProps) {
  const { setNodes } = useReactFlow();
  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) => s.nodes.filter((node) => node.selected).length);
  const [editing, setEditing] = useState(false);
  const [draftContent, setDraftContent] = useState(data.content);
  const [resizing, setResizing] = useState(false);
  const resizeStartRef = useRef<{
    x: number;
    y: number;
    width: number;
    height: number;
  } | null>(null);
  const activeRunPollRef = useRef<string | null>(null);

  const isGenerating = data.status === "pending";
  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = 1 / zoom;

  const sendEditingPresence = useCallback((nodeId: string | null) => {
    window.dispatchEvent(new CustomEvent<NodeEditingPresenceEventDetail>("copse:node-editing-presence", {
      detail: { nodeId },
    }));
  }, []);

  const updateData = useCallback(
    (patch: Partial<TextNodeData>, options?: { flush?: boolean }) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node
        )
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch, flush: options?.flush },
      }));
    },
    [id, setNodes]
  );

  const waitAndApplyServerRun = useCallback(async (projectId: string | number, taskId: number, startedAt: string) => {
    const pollKey = `${projectId}:${id}:${taskId}`;
    if (activeRunPollRef.current === pollKey) return;
    activeRunPollRef.current = pollKey;
    try {
      const result = await waitCanvasNodeRunResult(projectId, id, {
        taskId,
        baseVersion: 0,
        nodeType: "text",
      });
      const patch = getCanvasNodeRunPatch(result, id);
      if (patch) updateData(patch as Partial<TextNodeData>, { flush: true });
    } catch (error) {
      updateData({
        status: "failed",
        taskId: String(taskId),
        errorMessage: error instanceof Error ? error.message : "文本任务同步失败",
        generationCompletedAt: new Date().toISOString(),
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      }, { flush: true });
    } finally {
      if (activeRunPollRef.current === pollKey) {
        activeRunPollRef.current = null;
      }
    }
  }, [id, updateData]);

  useEffect(() => {
    if (data.status !== "pending" || !data.taskId) return;
    const projectId = new URLSearchParams(window.location.search).get("projectId");
    if (!isServerCanvasProjectId(projectId)) return;
    const taskId = Number(data.taskId);
    if (!Number.isFinite(taskId)) return;
    void waitAndApplyServerRun(projectId, taskId, data.generationStartedAt ?? data.createdAt);
  }, [data.createdAt, data.generationStartedAt, data.status, data.taskId, waitAndApplyServerRun]);

  const commitContent = useCallback(() => {
    setEditing(false);
    sendEditingPresence(null);
    updateData({ content: draftContent, updatedAt: new Date().toISOString() });
  }, [draftContent, sendEditingPresence, updateData]);

  const handleGenerate = useCallback(async () => {
    const prompt = data.prompt.trim();
    if (!prompt || isGenerating) return;

    if (!data.aigcModelId) {
      updateData({ status: "failed", errorMessage: "请选择 AIGC 文本模型后再生成。" });
      return;
    }

    const startedAt = new Date().toISOString();
    updateData({ status: "pending", taskId: null, errorMessage: null, generationStartedAt: startedAt, generationCompletedAt: null, elapsedMs: null });

    const params = new URLSearchParams(window.location.search);
    const projectId = params.get("projectId");
    const clientId = `node_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    if (isServerCanvasProjectId(projectId)) {
      try {
        const run = await canvasNodeRunApi.runNode(projectId, id, {
          clientId,
          baseVersion: 0,
          runId: clientId,
          nodeType: "text",
          generateType: "TEXT",
          generateMode: "TEXT_GENERATE",
          modelId: data.aigcModelId,
          prompt,
          inputParams: JSON.stringify({ previousContent: data.content }),
          sync: false,
        });
        updateData({ taskId: String(run.taskId), taskStatus: run.status }, { flush: true });
        await waitAndApplyServerRun(projectId, run.taskId, startedAt);
        return;
      } catch (error) {
        updateData({
          status: "failed",
          taskId: null,
          errorMessage: error instanceof Error ? error.message : "文本任务提交失败",
          generationCompletedAt: new Date().toISOString(),
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
      }
    }

    try {
      const submit = await generationApi.generateText({
        modelId: data.aigcModelId,
        prompt,
        inputParams: JSON.stringify({ previousContent: data.content }),
        sync: false,
      });
      const result = await waitGenerationResult(submit.taskId);
      const completedAt = result.finishTime ?? new Date().toISOString();

      if (result.status === "SUCCESS") {
        updateData({
          status: "idle",
          taskId: String(submit.taskId),
          content: result.outputText ?? String(result.outputDataValue ?? ""),
          errorMessage: null,
          updatedAt: completedAt,
          generationCompletedAt: completedAt,
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
      }

      updateData({
        status: "failed",
        taskId: String(submit.taskId),
        errorMessage: result.failMessage ?? "文本生成失败，请稍后重试。",
        generationCompletedAt: completedAt,
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    } catch (error) {
      updateData({
        status: "failed",
        taskId: null,
        errorMessage: error instanceof Error ? error.message : "文本生成失败，请稍后重试。",
        generationCompletedAt: new Date().toISOString(),
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    }
  }, [data.aigcModelId, data.content, data.prompt, id, isGenerating, updateData, waitAndApplyServerRun]);

  useEffect(() => {
    if (!resizing) return;

    function handlePointerMove(event: PointerEvent) {
      const start = resizeStartRef.current;
      if (!start) return;
      updateData({
        width: Math.max(MIN_WIDTH, Math.round(start.width + (event.clientX - start.x) / zoom)),
        height: Math.max(MIN_HEIGHT, Math.round(start.height + (event.clientY - start.y) / zoom)),
      });
    }

    function handlePointerUp() {
      setResizing(false);
      resizeStartRef.current = null;
    }

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
    };
  }, [resizing, updateData, zoom]);

  return (
    <div className="relative" style={{ width: data.width }}>
      <div
        className="flex items-center gap-1.5 bg-transparent px-1 text-sm font-medium text-muted-gray"
        style={{
          marginBottom: 8,
        }}
      >
        <Text className="size-4" />
        <EditableNodeTitle
          value={data.fileName}
          fallback="Text"
          onCommit={(fileName) => updateData({ fileName, updatedAt: new Date().toISOString() }, { flush: true })}
        />
      </div>

      <motion.div
        data-node-preview-card
        data-node-id={id}
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.18, ease: "easeOut" }}
        className={cn(
          "canvas-node-drag-handle group relative rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
          selected ? "border-charcoal/60 ring-2 ring-charcoal/35" : "border-border-warm"
        )}
        style={{ width: data.width, height: data.height, pointerEvents: "auto" }}
        onDoubleClick={(event) => {
          event.stopPropagation();
          setDraftContent(data.content);
          setEditing(true);
          sendEditingPresence(id);
        }}
      >
        {editing ? (
          <textarea
            value={draftContent}
            autoFocus
            onChange={(event) => setDraftContent(event.target.value)}
            onBlur={commitContent}
            onKeyDown={(event) => {
              if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
                commitContent();
              }
              if (event.key === "Escape") {
                setEditing(false);
                sendEditingPresence(null);
                setDraftContent(data.content);
              }
            }}
            className="nodrag nowheel size-full resize-none rounded-xl bg-transparent p-4 text-sm leading-6 text-charcoal outline-none"
          />
        ) : data.content ? (
          <div className="h-full overflow-hidden whitespace-pre-wrap p-4 text-sm leading-6 text-charcoal">
            {data.content}
          </div>
        ) : (
          <div className="p-4">
            {isGenerating ? (
              <div className="space-y-3">
                <div className="h-2.5 w-[92%] animate-pulse rounded-full bg-muted-gray/45" />
                <div className="h-2.5 w-[70%] animate-pulse rounded-full bg-muted-gray/45" />
                <div className="h-2.5 w-[32%] animate-pulse rounded-full bg-muted-gray/45" />
              </div>
            ) : (
              <p className="text-sm text-muted-gray">Double-click to start editing...</p>
            )}
          </div>
        )}

        <AnimatePresence>
          {isGenerating && data.content && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.16 }}
              className="absolute inset-0 flex items-center justify-center rounded-xl bg-background/65"
            >
              <Loader2 className="size-6 animate-spin text-muted-gray" />
            </motion.div>
          )}
        </AnimatePresence>

        <button
          type="button"
          aria-label="调整文本卡片大小"
          className="nodrag nowheel absolute bottom-1 right-1 size-5 cursor-nwse-resize rounded bg-charcoal/10 hover:bg-charcoal/20"
          onPointerDown={(event) => {
            event.preventDefault();
            event.stopPropagation();
            resizeStartRef.current = {
              x: event.clientX,
              y: event.clientY,
              width: data.width,
              height: data.height,
            };
            setResizing(true);
          }}
        />

        <NodeCreateHandle nodeId={id} direction="incoming" selected={selected} showButton={showNodeActions} />
        <NodeCreateHandle nodeId={id} direction="outgoing" selected={selected} showButton={showNodeActions} />
      </motion.div>

      <AnimatePresence>
        {isOnlySelectedNode && !dragging && (
          <motion.div
            initial={{ opacity: 0, y: -8 * fixedUiScale, scale: 0.99 * fixedUiScale }}
            animate={{ opacity: 1, y: 0, scale: fixedUiScale }}
            exit={{ opacity: 0, y: -8 * fixedUiScale, scale: 0.99 * fixedUiScale }}
            transition={{ duration: 0.18, ease: "easeOut" }}
            className="nodrag nowheel rounded-xl border border-border-warm bg-background p-4 shadow-[0_8px_24px_rgba(28,28,28,0.08)]"
            style={{
              width: COMPOSER_WIDTH,
              marginLeft: (data.width - COMPOSER_WIDTH) / 2,
              marginTop: 12 * fixedUiScale,
              transformOrigin: "top center",
              pointerEvents: "auto",
            }}
          >
          <textarea
            value={data.prompt}
            onChange={(event) => updateData({ prompt: event.target.value })}
            disabled={isGenerating}
            placeholder="Describe the text you want to generate or rewrite"
            className="min-h-[110px] w-full resize-none bg-transparent text-base leading-7 text-charcoal placeholder:text-muted-gray focus:outline-none disabled:cursor-not-allowed disabled:text-muted-gray"
          />
          <div className="mt-4 flex items-center justify-between gap-3 border-t border-border-warm pt-3">
            <div className="flex items-center gap-2 text-sm font-medium text-charcoal">
              <Sparkles className="size-4 text-muted-gray" />
              <span>{data.modelId || DEFAULT_MODEL}</span>
            </div>
            <div className="flex items-center gap-2">
              {isGenerating && <span className="text-xs text-muted-gray">生成中</span>}
              <button
                type="button"
                onClick={handleGenerate}
                disabled={!data.prompt.trim() || isGenerating}
                className="flex size-10 items-center justify-center rounded-full bg-charcoal text-off-white shadow-sm transition-opacity active:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                aria-label={isGenerating ? "生成中" : "生成文本"}
              >
                {isGenerating ? <Loader2 className="size-5 animate-spin" /> : <ArrowUp className="size-5" />}
              </button>
            </div>
          </div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {data.status === "failed" && (
          <motion.div
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.14 }}
            className="mt-2 flex items-center gap-1 text-xs text-destructive"
          >
            <X className="size-3" />
            {data.errorMessage ?? "生成失败"}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
