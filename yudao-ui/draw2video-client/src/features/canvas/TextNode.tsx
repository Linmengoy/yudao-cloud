"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import { ArrowUp, Loader2, Sparkles, Text, X } from "lucide-react";
import type { TextNodeData } from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { generationApi } from "@/features/generation/generation-api";
import { waitGenerationResult } from "@/features/generation/generation-poll";
import { cn } from "@/lib/utils";

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

  const isGenerating = data.status === "pending";
  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;

  const updateData = useCallback(
    (patch: Partial<TextNodeData>) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node
        )
      );
    },
    [id, setNodes]
  );

  const commitContent = useCallback(() => {
    setEditing(false);
    updateData({ content: draftContent, updatedAt: new Date().toISOString() });
  }, [draftContent, updateData]);

  const handleGenerate = useCallback(async () => {
    const prompt = data.prompt.trim();
    if (!prompt || isGenerating) return;

    if (!data.aigcModelId) {
      updateData({ status: "failed", errorMessage: "请选择 AIGC 文本模型后再生成。" });
      return;
    }

    const startedAt = new Date().toISOString();
    updateData({ status: "pending", taskId: null, errorMessage: null, generationStartedAt: startedAt, generationCompletedAt: null, elapsedMs: null });

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
  }, [data.aigcModelId, data.content, data.prompt, isGenerating, updateData]);

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
      <div className="mb-2 flex items-center gap-1.5 bg-transparent px-1 text-sm font-medium text-muted-gray">
        <Text className="size-4" />
        <span>Text</span>
      </div>

      <motion.div
        data-node-preview-card
        data-node-id={id}
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.18, ease: "easeOut" }}
        className={cn(
          "group relative rounded-xl border bg-muted shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
          selected ? "border-charcoal/60 ring-2 ring-charcoal/35" : "border-border-warm"
        )}
        style={{ width: data.width, height: data.height }}
        onDoubleClick={(event) => {
          event.stopPropagation();
          setDraftContent(data.content);
          setEditing(true);
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
            initial={{ opacity: 0, y: -8, scale: 0.99 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.99 }}
            transition={{ duration: 0.18, ease: "easeOut" }}
            className="nodrag nowheel mt-3 rounded-xl border border-border-warm bg-background p-4 shadow-[0_8px_24px_rgba(28,28,28,0.08)]"
            style={{ width: COMPOSER_WIDTH, marginLeft: (data.width - COMPOSER_WIDTH) / 2 }}
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
