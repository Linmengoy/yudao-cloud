/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore } from "@xyflow/react";
import { motion } from "motion/react";
import { Download, ImageIcon, Loader2, Maximize2, PenLine, Save, X } from "lucide-react";
import { Tldraw, getSnapshot, loadSnapshot, type Editor, type TLEditorSnapshot } from "tldraw";
import { NodeCreateHandle } from "./NodeCreateHandle";
import type { NodeDataPatchEventDetail, SketchNodeData } from "./types";
import { canvasApi } from "@/features/canvas/canvas-api";
import { cn } from "@/lib/utils";

type SketchNodeProps = NodeProps<Node<SketchNodeData, "sketch">>;

const CARD_WIDTH = 300;
const CARD_HEIGHT = 220;

function parseSceneJson(value: unknown): Partial<TLEditorSnapshot> | null {
  if (!value) return null;
  if (typeof value === "object") return value as Partial<TLEditorSnapshot>;
  if (typeof value !== "string") return null;
  try {
    return JSON.parse(value) as Partial<TLEditorSnapshot>;
  } catch {
    return null;
  }
}

function createBlankPreview(): { url: string; width: number; height: number } {
  const canvas = document.createElement("canvas");
  canvas.width = 960;
  canvas.height = 640;
  const ctx = canvas.getContext("2d");
  if (ctx) {
    ctx.fillStyle = "#f7f4ed";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#d8d2c8";
    for (let x = 24; x < canvas.width; x += 36) {
      for (let y = 24; y < canvas.height; y += 36) {
        ctx.beginPath();
        ctx.arc(x, y, 2, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }
  return { url: canvas.toDataURL("image/png"), width: canvas.width, height: canvas.height };
}

export function SketchNodeComponent({ id, data, selected }: SketchNodeProps) {
  const { setNodes } = useReactFlow();
  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) => s.nodes.filter((node) => node.selected).length);
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = 1 / zoom;
  const [editorOpen, setEditorOpen] = useState(false);
  const [editor, setEditor] = useState<Editor | null>(null);
  const [saving, setSaving] = useState(false);
  const [loadingRemote, setLoadingRemote] = useState(false);
  const remoteLoadedRef = useRef(false);

  const previewSrc = data.previewUrl || data.dataUrl || "";
  const sceneSnapshot = useMemo(() => parseSceneJson(data.sceneJson), [data.sceneJson]);

  const updateData = useCallback(
    (patch: Partial<SketchNodeData>) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node
        )
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch },
      }));
    },
    [id, setNodes]
  );

  useEffect(() => {
    if (remoteLoadedRef.current || !data.projectId) return;
    remoteLoadedRef.current = true;
    setLoadingRemote(true);
    canvasApi.getSketch(data.projectId, id)
      .then((record) => {
        if (!record) return;
        updateData({
          sceneJson: parseSceneJson(record.sceneJson) ?? record.sceneJson,
          previewUrl: record.previewUrl ?? null,
          dataUrl: record.previewDataUrl ?? data.dataUrl,
          mimeType: record.mimeType || data.mimeType,
          width: record.width ?? data.width,
          height: record.height ?? data.height,
          background: record.background === "transparent" ? "transparent" : "white",
          updatedAt: record.updateTime ?? data.updatedAt,
        });
      })
      .catch(() => undefined)
      .finally(() => setLoadingRemote(false));
  }, [data.dataUrl, data.height, data.mimeType, data.projectId, data.updatedAt, data.width, id, updateData]);

  const handleMount = useCallback((mountedEditor: Editor) => {
    setEditor(mountedEditor);
    const snapshot = parseSceneJson(data.sceneJson);
    if (snapshot) {
      try {
        loadSnapshot(mountedEditor.store, snapshot);
      } catch {
      }
    }
  }, [data.sceneJson]);

  const saveSketch = useCallback(async () => {
    if (!editor) return;
    setSaving(true);
    try {
      const snapshot = getSnapshot(editor.store);
      const shapeIds = Array.from(editor.getCurrentPageShapeIds());
      const exportResult = shapeIds.length > 0
        ? await editor.toImageDataUrl(shapeIds, { format: "png", background: true, padding: 24, scale: 1 })
        : createBlankPreview();
      const updatedAt = new Date().toISOString();
      updateData({
        sceneJson: snapshot,
        dataUrl: exportResult.url,
        previewUrl: null,
        mimeType: "image/png",
        width: exportResult.width,
        height: exportResult.height,
        background: "white",
        updatedAt,
      });
      if (data.projectId) {
        canvasApi.saveSketch(data.projectId, id, {
          sceneJson: JSON.stringify(snapshot),
          previewDataUrl: exportResult.url,
          previewUrl: null,
          mimeType: "image/png",
          width: exportResult.width,
          height: exportResult.height,
          background: "white",
        }).catch(() => undefined);
      }
      setEditorOpen(false);
    } finally {
      setSaving(false);
    }
  }, [data.projectId, editor, id, updateData]);

  const downloadPreview = useCallback(() => {
    if (!previewSrc) return;
    const link = document.createElement("a");
    link.href = previewSrc;
    link.download = data.fileName || "sketch.png";
    document.body.appendChild(link);
    link.click();
    link.remove();
  }, [data.fileName, previewSrc]);

  return (
    <>
      <div className="relative" style={{ width: CARD_WIDTH }}>
        <div
          className="mb-2 flex items-center gap-1.5 bg-transparent px-1 text-sm font-medium text-muted-gray"
          style={{
            transform: `scale(${fixedUiScale})`,
            transformOrigin: "bottom left",
            width: CARD_WIDTH / fixedUiScale,
          }}
        >
          <PenLine className="size-4" />
          <span className="truncate">{data.fileName || "Sketch"}</span>
          {loadingRemote && <Loader2 className="size-3.5 animate-spin" />}
        </div>

        <motion.div
          data-node-preview-card
          data-node-id={id}
          whileHover={{ y: -1 }}
          onDoubleClick={() => setEditorOpen(true)}
          className={cn(
            "relative overflow-hidden rounded-2xl border bg-background shadow-[0_8px_24px_rgba(28,28,28,0.08)] transition-colors",
            selected ? "border-charcoal ring-2 ring-charcoal/10" : "border-border-warm hover:border-charcoal/35"
          )}
          style={{ width: CARD_WIDTH, height: CARD_HEIGHT }}
        >
          {previewSrc ? (
            <img src={previewSrc} alt={data.fileName || "Sketch"} className="size-full object-cover" draggable={false} />
          ) : sceneSnapshot ? (
            <div className="flex size-full items-center justify-center bg-muted text-muted-gray">
              <ImageIcon className="size-12 opacity-50" />
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setEditorOpen(true)}
              className="flex size-full flex-col items-center justify-center gap-3 bg-muted text-muted-gray transition-colors hover:bg-border-warm/40 hover:text-charcoal"
            >
              <PenLine className="size-10" strokeWidth={1.8} />
              <span className="text-sm font-medium">打开白板</span>
            </button>
          )}

          <button
            type="button"
            onClick={() => setEditorOpen(true)}
            className="absolute right-3 top-3 flex size-8 items-center justify-center rounded-full border border-border-warm bg-background/90 text-muted-gray shadow-sm backdrop-blur transition-colors hover:text-charcoal"
            aria-label="放大编辑草图"
          >
            <Maximize2 className="size-4" />
          </button>

          {previewSrc && (
            <button
              type="button"
              onClick={downloadPreview}
              className="absolute bottom-3 right-3 flex size-8 items-center justify-center rounded-full border border-border-warm bg-background/90 text-muted-gray shadow-sm backdrop-blur transition-colors hover:text-charcoal"
              aria-label="下载草图预览"
            >
              <Download className="size-4" />
            </button>
          )}

          <NodeCreateHandle nodeId={id} direction="outgoing" selected={selected} showButton={showNodeActions} />
        </motion.div>
      </div>

      {editorOpen && createPortal(
        <div className="fixed inset-0 z-[360] flex flex-col bg-background">
          <div className="flex h-14 shrink-0 items-center justify-between border-b border-border-warm bg-background px-4">
            <div className="flex items-center gap-2 text-sm font-medium text-charcoal">
              <PenLine className="size-4 text-muted-gray" />
              <span>{data.fileName || "Sketch"}</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={saveSketch}
                disabled={!editor || saving}
                className="flex items-center gap-2 rounded-lg bg-charcoal px-3 py-2 text-sm font-medium text-off-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
                保存
              </button>
              <button
                type="button"
                onClick={() => setEditorOpen(false)}
                className="flex size-9 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
                aria-label="关闭白板"
              >
                <X className="size-5" />
              </button>
            </div>
          </div>
          <div className="min-h-0 flex-1">
            <Tldraw key={id} onMount={handleMount} />
          </div>
        </div>,
        document.body
      )}
    </>
  );
}
