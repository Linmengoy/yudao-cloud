/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import { ImageIcon, Loader2, PenLine, Save, Upload, X } from "lucide-react";
import {
  ArrowToolbarItem,
  AssetRecordType,
  DefaultToolbar,
  DiamondToolbarItem,
  DrawToolbarItem,
  EllipseToolbarItem,
  EraserToolbarItem,
  HandToolbarItem,
  HighlightToolbarItem,
  LaserToolbarItem,
  LineToolbarItem,
  RectangleToolbarItem,
  SelectToolbarItem,
  TextToolbarItem,
  Tldraw,
  TriangleToolbarItem,
  createShapesForAssets,
  getAssetInfo,
  getHashForBuffer,
  getSnapshot,
  loadSnapshot,
  createShapeId,
  type Editor,
  type TLAsset,
  type TLComponents,
  type TLEditorSnapshot,
  type TLShapeId,
  type TLUiOverrides,
} from "tldraw";
import { NodeCreateHandle } from "./NodeCreateHandle";
import type { NodeDataPatchEventDetail, NodeEditingPresenceEventDetail, SketchNodeData } from "./types";
import { isAcceptedImageType } from "./image-upload";
import { canvasApi } from "@/features/canvas/canvas-api";
import { SelectedMediaToolbar } from "@/features/media-preview/SelectedMediaToolbar";
import { compactInfo, downloadMedia } from "@/features/media-preview/media-preview-utils";
import { cn } from "@/lib/utils";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";

type SketchNodeProps = NodeProps<Node<SketchNodeData, "sketch">>;

const CARD_WIDTH = 300;
const CARD_HEIGHT = 220;
const CARD_MAX_WIDTH = 320;
const CARD_MAX_HEIGHT = 320;
const SKETCH_BOARD_ID = createShapeId("copse-sketch-board");
const BLANK_PREVIEW_WIDTH = 1024;
const BLANK_PREVIEW_HEIGHT = 768;
const UPLOADED_IMAGE_MAX_WIDTH = 960;
const UPLOADED_IMAGE_MAX_HEIGHT = 720;

function scaleSketchPreview(width?: number, height?: number) {
  if (!width || !height) return { width: CARD_WIDTH, height: CARD_HEIGHT };
  const scale = Math.min(1, CARD_MAX_WIDTH / width, CARD_MAX_HEIGHT / height);
  return {
    width: Math.max(140, Math.round(width * scale)),
    height: Math.max(140, Math.round(height * scale)),
  };
}

function SketchToolbar() {
  return (
    <DefaultToolbar>
      <SelectToolbarItem />
      <HandToolbarItem />
      <DrawToolbarItem />
      <EraserToolbarItem />
      <ArrowToolbarItem />
      <TextToolbarItem />
      <RectangleToolbarItem />
      <EllipseToolbarItem />
      <TriangleToolbarItem />
      <DiamondToolbarItem />
      <LineToolbarItem />
      <HighlightToolbarItem />
      <LaserToolbarItem />
    </DefaultToolbar>
  );
}

const sketchTldrawComponents: TLComponents = {
  PageMenu: null,
  Toolbar: SketchToolbar,
};

const sketchTldrawOverrides: TLUiOverrides = {
  tools(_editor, tools) {
    const nextTools = { ...tools };
    delete nextTools.note;
    delete nextTools.asset;
    delete nextTools.frame;
    return nextTools;
  },
};

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
  canvas.width = BLANK_PREVIEW_WIDTH;
  canvas.height = BLANK_PREVIEW_HEIGHT;
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

function removeLegacyFixedSketchBoard(editor: Editor) {
  const existingBoard = editor.getShape(SKETCH_BOARD_ID);
  if (!existingBoard) return;
  editor.updateShape({
    id: existingBoard.id,
    type: existingBoard.type,
    isLocked: false,
  });
  editor.deleteShape(existingBoard.id);
}

function getViewportCenter(editor: Editor) {
  const bounds = editor.getViewportPageBounds();
  return { x: bounds.x + bounds.w / 2, y: bounds.y + bounds.h / 2 };
}

function fitUploadedImageNearViewport(editor: Editor, shapeId: TLShapeId) {
  const shape = editor.getShape(shapeId);
  if (!shape || shape.type !== "image" || !("w" in shape.props) || !("h" in shape.props)) return;
  const width = Number(shape.props.w);
  const height = Number(shape.props.h);
  if (!width || !height) return;
  const scale = Math.min(1, UPLOADED_IMAGE_MAX_WIDTH / width, UPLOADED_IMAGE_MAX_HEIGHT / height);
  const nextWidth = Math.round(width * scale);
  const nextHeight = Math.round(height * scale);
  const center = getViewportCenter(editor);
  editor.updateShape({
    id: shape.id,
    type: "image",
    x: Math.round(center.x - nextWidth / 2),
    y: Math.round(center.y - nextHeight / 2),
    props: {
      ...shape.props,
      w: nextWidth,
      h: nextHeight,
    },
  });
}

function getExportShapeIds(editor: Editor): TLShapeId[] {
  return Array.from(editor.getCurrentPageShapeIds()).filter((shapeId) => shapeId !== SKETCH_BOARD_ID);
}

function enforceSinglePage(editor: Editor) {
  const pages = editor.getPages();
  if (pages.length === 0) return;
  const [firstPage, ...extraPages] = pages;
  editor.setCurrentPage(firstPage);
  for (const page of extraPages) {
    editor.deletePage(page);
  }
}

export function SketchNodeComponent({ id, data, selected }: SketchNodeProps) {
  const { setNodes } = useReactFlow();
  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) => s.nodes.filter((node) => node.selected).length);
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = 1 / zoom;
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editor, setEditor] = useState<Editor | null>(null);
  const [saving, setSaving] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [loadingRemote, setLoadingRemote] = useState(false);
  const remoteLoadedRef = useRef(false);

  const previewSrc = data.previewUrl || data.dataUrl || "";
  const previewSize = previewSrc ? scaleSketchPreview(data.width, data.height) : { width: CARD_WIDTH, height: CARD_HEIGHT };
  const sceneSnapshot = useMemo(() => parseSceneJson(data.sceneJson), [data.sceneJson]);
  const previewItem = useMemo(() => {
    if (!previewSrc) return null;
    return {
      kind: "image" as const,
      url: previewSrc,
      title: data.fileName || "Sketch",
      fileName: data.fileName || "sketch.png",
      createdAt: data.updatedAt ?? data.createdAt,
      information: compactInfo([
        { label: "Type", value: "Sketch" },
        { label: "Dimensions", value: data.width && data.height ? `${data.width} x ${data.height}` : null },
        { label: "Format", value: data.mimeType?.replace("image/", "").toUpperCase() },
        { label: "Background", value: data.background },
      ]),
    };
  }, [data.background, data.createdAt, data.fileName, data.height, data.mimeType, data.updatedAt, data.width, previewSrc]);

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

  const sendEditingPresence = useCallback((nodeId: string | null) => {
    window.dispatchEvent(new CustomEvent<NodeEditingPresenceEventDetail>("copse:node-editing-presence", {
      detail: { nodeId },
    }));
  }, []);

  useEffect(() => {
    if (!editorOpen) return;
    sendEditingPresence(id);
    return () => sendEditingPresence(null);
  }, [editorOpen, id, sendEditingPresence]);

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
    enforceSinglePage(mountedEditor);
    removeLegacyFixedSketchBoard(mountedEditor);
    mountedEditor.selectNone();
    window.setTimeout(() => {
      const shapeIds = getExportShapeIds(mountedEditor);
      if (shapeIds.length > 0) {
        mountedEditor.zoomToFit({ animation: { duration: 0 } });
      }
    }, 0);
  }, [data.sceneJson]);

  const saveSketch = useCallback(async () => {
    if (!editor) return;
    setSaving(true);
    try {
      removeLegacyFixedSketchBoard(editor);
      const snapshot = getSnapshot(editor.store);
      const shapeIds = getExportShapeIds(editor);
      const exportResult = shapeIds.length > 0
        ? await editor.toImageDataUrl(shapeIds, {
          format: "png",
          background: true,
          darkMode: false,
          padding: "auto",
          scale: 1,
        })
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
    if (!previewItem) return;
    downloadMedia(previewItem);
  }, [previewItem]);

  const addImageToSketch = useCallback(async (file: File) => {
    if (!editor) return;
    if (!isAcceptedImageType(file.type)) {
      setUploadError("仅支持 PNG、JPG、WEBP 或 GIF 图片");
      return;
    }
    setUploadingImage(true);
    setUploadError(null);
    try {
      const assetId = AssetRecordType.createId(getHashForBuffer(await file.arrayBuffer()));
      const assetInfo = await getAssetInfo(editor, file, assetId);
      if (!assetInfo || assetInfo.type !== "image") {
        setUploadError("无法读取这张图片");
        return;
      }
      editor.createTemporaryAssetPreview(assetInfo.id, file);
      const uploadedAsset = await editor.getAssetForExternalContent({
        type: "file",
        file,
        assetId,
      });
      const asset = ({ ...(uploadedAsset ?? assetInfo), id: assetId } as TLAsset);
      const [shapeId] = await createShapesForAssets(editor, [asset], getViewportCenter(editor));
      if (shapeId) {
        fitUploadedImageNearViewport(editor, shapeId);
        editor.select(shapeId);
      }
    } catch {
      setUploadError("图片上传失败，请换一张再试");
    } finally {
      setUploadingImage(false);
    }
  }, [editor]);

  const handleImageInputChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = "";
    if (file) {
      void addImageToSketch(file);
    }
  }, [addImageToSketch]);

  return (
    <>
      <div className="relative" style={{ width: previewSize.width, height: previewSize.height }}>
        <AnimatePresence>
          {selected && showNodeActions && (
            <SelectedMediaToolbar
              canDownload={Boolean(previewItem?.url)}
              onDownload={downloadPreview}
              onOpenPreview={() => setEditorOpen(true)}
              uiScale={fixedUiScale}
              style={{
                left: previewSize.width / 2,
                top: -58 * fixedUiScale,
                pointerEvents: "auto",
              }}
            />
          )}
        </AnimatePresence>

        <CanvasNodeTitle fixedUiScale={fixedUiScale} maxWidth={previewSize.width}>
          <PenLine className="size-4" />
          <EditableNodeTitle
            value={data.fileName}
            fallback="Sketch"
            onCommit={(fileName) => updateData({ fileName })}
            className="min-w-0 truncate"
          />
          {loadingRemote && <Loader2 className="size-3.5 animate-spin" />}
        </CanvasNodeTitle>

        <motion.div
          data-node-preview-card
          data-node-id={id}
          whileHover={{ y: -1 }}
          onDoubleClick={() => setEditorOpen(true)}
          className={cn(
            "canvas-node-drag-handle group relative overflow-visible rounded-2xl transition-colors",
            previewSrc ? "bg-transparent shadow-none" : "border border-border-warm bg-background shadow-[0_8px_24px_rgba(28,28,28,0.08)]",
            selected && (previewSrc ? "ring-2 ring-off-white/80" : "border-charcoal ring-2 ring-charcoal/10"),
            !selected && !previewSrc && "hover:border-charcoal/35"
          )}
          style={{ width: previewSize.width, height: previewSize.height, pointerEvents: "auto" }}
        >
          <div className="size-full overflow-hidden rounded-[inherit]">
            {previewSrc ? (
              <img
                src={previewSrc}
                alt={data.fileName || "Sketch"}
                className="size-full object-contain"
                draggable={false}
                onLoad={(event) => {
                  const image = event.currentTarget;
                  if (
                    image.naturalWidth > 0 &&
                    image.naturalHeight > 0 &&
                    (data.width !== image.naturalWidth || data.height !== image.naturalHeight)
                  ) {
                    updateData({ width: image.naturalWidth, height: image.naturalHeight });
                  }
                }}
              />
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
          </div>

          <NodeCreateHandle nodeId={id} direction="incoming" selected={selected} showButton={showNodeActions} />
          <NodeCreateHandle nodeId={id} direction="outgoing" selected={selected} showButton={showNodeActions} />
        </motion.div>
      </div>

      {editorOpen && createPortal(
        <div className="fixed inset-0 z-[360] flex flex-col bg-background">
          <div className="flex h-14 shrink-0 items-center justify-between border-b border-border-warm bg-background px-4">
            <div className="flex items-center gap-2 text-sm font-medium text-charcoal">
              <PenLine className="size-4 text-muted-gray" />
              <span>{data.fileName || "Sketch"}</span>
              {uploadError && <span className="text-xs font-normal text-red-500">{uploadError}</span>}
            </div>
            <div className="flex items-center gap-2">
              <input
                ref={fileInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                className="hidden"
                onChange={handleImageInputChange}
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={!editor || uploadingImage}
                className="flex items-center gap-2 rounded-lg border border-border-warm bg-background px-3 py-2 text-sm font-medium text-charcoal transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
              >
                {uploadingImage ? <Loader2 className="size-4 animate-spin" /> : <Upload className="size-4" />}
                上传图片
              </button>
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
            <Tldraw
              key={id}
              components={sketchTldrawComponents}
              overrides={sketchTldrawOverrides}
              onMount={handleMount}
            />
          </div>
        </div>,
        document.body
      )}
    </>
  );
}
