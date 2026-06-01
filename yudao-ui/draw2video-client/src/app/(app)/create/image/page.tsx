"use client";

import "@xyflow/react/dist/style.css";
import "tldraw/tldraw.css";

import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AnimatePresence, motion } from "motion/react";
import {
  ReactFlow,
  ReactFlowProvider,
  Background,
  BackgroundVariant,
  MiniMap,
  useNodesState,
  useEdgesState,
  useReactFlow,
  addEdge,
  applyNodeChanges,
  applyEdgeChanges,
  PanOnScrollMode,
  SelectionMode,
  type Connection,
  type FinalConnectionState,
  type NodeTypes,
  type EdgeTypes,
  type NodeChange,
  type EdgeChange,
  type ConnectionLineComponentProps,
} from "@xyflow/react";
import type { AppNode, AppEdge, CanvasMember, CanvasPresence, CanvasProjectRole, ImageNodeData, NodeCreateMenuEventDetail, NodeDataPatchEventDetail, NodeEditingPresenceEventDetail, ReferencePickerEventDetail, SketchNodeData, TextNodeData, VideoNodeData } from "@/features/canvas/types";
import { DEFAULT_PROMPT_DATA } from "@/features/canvas/types";
import { canvasApi, snapshotRecordToCanvasState } from "@/features/canvas/canvas-api";
import { CanvasShareDialog } from "@/features/canvas/CanvasShareDialog";
import { PromptNodeComponent } from "@/features/canvas/PromptNode";
import { ResultNodeComponent } from "@/features/canvas/ResultNode";
import { ImageNodeComponent } from "@/features/canvas/ImageNode";
import { SketchNodeComponent } from "@/features/canvas/SketchNode";
import { TextNodeComponent } from "@/features/canvas/TextNode";
import { VideoNodeComponent } from "@/features/canvas/VideoNode";
import { CanvasSignalEdge } from "@/features/canvas/CanvasSignalEdge";
import { loadCanvas, saveCanvas, clearCanvas } from "@/features/canvas/use-canvas-storage";
import { filterSyncableNodeDataPatch, sanitizeNodeForCanvasOperation } from "@/features/canvas/canvas-syncable-data";
import { useCanvasServerStorage } from "@/features/canvas/use-canvas-server-storage";
import { useCanvasRealtime } from "@/features/canvas/use-canvas-realtime";
import { useCanvasOperations } from "@/features/canvas/use-canvas-operations";
import { saveImage, loadImage, clearImages, saveVideo, loadVideo, clearVideos } from "@/features/canvas/image-store";
import { ToolbarIconButton } from "@/features/canvas/ToolbarIconButton";
import { fileToImageNodeData, fileToVideoNodeData, getFilesFromDrop, isAcceptedImageType, isAcceptedVideoFile } from "@/features/canvas/image-upload";
import { attachImageAsset, attachVideoAsset } from "@/features/canvas/canvas-asset-upload";
import {
  isEditableElement,
  getImageFilesFromPasteEvent,
  getImageFilesFromClipboardAPI,
} from "@/features/canvas/clipboard";
import { CanvasContextMenu, type ContextMenuState } from "@/features/canvas/CanvasContextMenu";
import { findOpenNodePosition } from "@/features/canvas/positioning";
import { createProject, updateProject as updateLocalProject, type ProjectKind } from "@/features/projects/project-store";
import { cn } from "@/lib/utils";
import { Plus, Trash2, ImagePlus, Share2, Type, Video, Map as MapIcon, Grid3X3, Scan, PenLine } from "lucide-react";

// Static outside component to avoid React Flow "new nodeTypes object" warning
const CANVAS_NODE_TYPES = {
  prompt: PromptNodeComponent,
  result: ResultNodeComponent,
  image: ImageNodeComponent,
  sketch: SketchNodeComponent,
  text: TextNodeComponent,
  video: VideoNodeComponent,
} satisfies NodeTypes;

const CANVAS_EDGE_TYPES = {
  signal: CanvasSignalEdge,
} satisfies EdgeTypes;

type CreateNodeKind = "text" | "image" | "sketch" | "video";
type LinkedCreateDirection = "incoming" | "outgoing";
type PendingConnectionPreview = {
  from: { x: number; y: number };
  to: { x: number; y: number };
  direction: LinkedCreateDirection;
};

const CREATE_NODE_KINDS: CreateNodeKind[] = ["text", "image", "sketch", "video"];

function getPresenceColor(clientId: string) {
  const colors = ["#7c3aed", "#0891b2", "#ea580c", "#16a34a", "#dc2626", "#2563eb"];
  let hash = 0;
  for (let i = 0; i < clientId.length; i++) hash = (hash + clientId.charCodeAt(i)) % colors.length;
  return colors[hash];
}

function isValidNodeKindConnection(sourceType: AppNode["type"] | CreateNodeKind, targetType: AppNode["type"] | CreateNodeKind) {
  return (
    (sourceType === "image" && targetType === "image") ||
    (sourceType === "image" && targetType === "video") ||
    (sourceType === "image" && targetType === "text") ||
    (sourceType === "sketch" && targetType === "image") ||
    (sourceType === "sketch" && targetType === "video") ||
    (sourceType === "text" && targetType === "image") ||
    (sourceType === "text" && targetType === "text")
  );
}

function getCreateKindsForOrigin(originType: AppNode["type"] | undefined, direction: LinkedCreateDirection | null) {
  if (!originType || !direction) return CREATE_NODE_KINDS;
  return CREATE_NODE_KINDS.filter((kind) =>
    direction === "incoming"
      ? isValidNodeKindConnection(kind, originType)
      : isValidNodeKindConnection(originType, kind)
  );
}

function getClientPoint(event: MouseEvent | TouchEvent) {
  if ("changedTouches" in event) {
    const touch = event.changedTouches[0];
    return touch ? { x: touch.clientX, y: touch.clientY } : null;
  }
  return { x: event.clientX, y: event.clientY };
}

function getConnectionPreviewPath(preview: PendingConnectionPreview) {
  const { from, to, direction } = preview;
  const distance = Math.max(72, Math.abs(to.x - from.x) * 0.45);
  const controlOffset = direction === "outgoing" ? distance : -distance;

  return `M ${from.x} ${from.y} C ${from.x + controlOffset} ${from.y}, ${to.x - controlOffset} ${to.y}, ${to.x} ${to.y}`;
}

function getNodeCardPreviewAnchor(nodeId: string, direction: LinkedCreateDirection) {
  const escapedId = CSS.escape(nodeId);
  const cardElement =
    document.querySelector(`[data-node-preview-card][data-node-id="${escapedId}"]`) ??
    document.querySelector(`.react-flow__node[data-id="${escapedId}"] [data-node-preview-card]`);

  if (!cardElement) {
    const nodeElement = document.querySelector(`.react-flow__node[data-id="${escapedId}"]`);
    if (!nodeElement) return null;

    const rect = nodeElement.getBoundingClientRect();
    return {
      x: direction === "outgoing" ? rect.right : rect.left,
      y: rect.top + rect.height / 2,
    };
  }

  const rect = cardElement.getBoundingClientRect();
  return {
    x: direction === "outgoing" ? rect.right : rect.left,
    y: rect.top + rect.height / 2,
  };
}

function CanvasConnectionLine({
  fromNode,
  fromHandle,
  fromX,
  fromY,
  toX,
  toY,
}: ConnectionLineComponentProps<AppNode>) {
  const { screenToFlowPosition } = useReactFlow();
  const direction = fromHandle.type === "target" ? "incoming" : "outgoing";
  const cardAnchor = getNodeCardPreviewAnchor(fromNode.id, direction);
  const from = cardAnchor ? screenToFlowPosition(cardAnchor) : { x: fromX, y: fromY };

  return (
    <path
      d={getConnectionPreviewPath({
        from,
        to: { x: toX, y: toY },
        direction,
      })}
      fill="none"
      stroke="#eceae4"
      strokeLinecap="round"
      strokeWidth={2}
    />
  );
}

type CanvasViewToolbarProps = {
  showMiniMap: boolean;
  snapToGrid: boolean;
  zoom: number;
  onToggleMiniMap: () => void;
  onToggleSnapToGrid: () => void;
  onResetView: () => void;
  onZoomChange: (zoom: number) => void;
};

function CanvasViewToolbar({
  showMiniMap,
  snapToGrid,
  zoom,
  onToggleMiniMap,
  onToggleSnapToGrid,
  onResetView,
  onZoomChange,
}: CanvasViewToolbarProps) {
  const buttonClass =
    "flex size-7 items-center justify-center rounded-full text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]";

  return (
    <div className="canvas-view-toolbar nowheel absolute bottom-4 left-4 z-[80] flex items-center gap-1.5 rounded-full border border-border-warm bg-background/95 px-2 py-1.5 shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] backdrop-blur-sm dark:shadow-[rgba(255,255,255,0.12)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.45)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.24)_0px_1px_2px_0px]">
      <button
        type="button"
        aria-pressed={showMiniMap}
        title="Open minimap"
        onClick={onToggleMiniMap}
        className={cn(buttonClass, showMiniMap && "bg-charcoal text-off-white hover:bg-charcoal hover:text-off-white")}
      >
        <MapIcon className="size-5" strokeWidth={2} />
      </button>
      <button
        type="button"
        aria-pressed={snapToGrid}
        title="Snap to Grid"
        onClick={onToggleSnapToGrid}
        className={cn(buttonClass, snapToGrid && "bg-charcoal text-off-white hover:bg-charcoal hover:text-off-white")}
      >
        <Grid3X3 className="size-5" strokeWidth={2} />
      </button>
      <button
        type="button"
        title="Reset"
        onClick={onResetView}
        className={buttonClass}
      >
        <Scan className="size-5" strokeWidth={2} />
      </button>
      <div className="flex items-center pl-0.5" title="Zoom in/out canvas (Ctrl/⌘ + mouse wheel)">
        <input
          aria-label="Zoom in/out canvas"
          className="canvas-zoom-slider w-[77px]"
          type="range"
          min={0.2}
          max={2}
          step={0.01}
          value={zoom}
          onChange={(event) => onZoomChange(Number(event.currentTarget.value))}
        />
      </div>
    </div>
  );
}

type CanvasSnapshot = {
  nodes: AppNode[];
  edges: AppEdge[];
};

type RemoteCanvasPresence = CanvasPresence & { updatedAt?: number };

function cloneSnapshot(nodes: AppNode[], edges: AppEdge[]): CanvasSnapshot {
  return {
    nodes: structuredClone(nodes),
    edges: structuredClone(edges),
  };
}

function snapshotSignature(snapshot: CanvasSnapshot) {
  return JSON.stringify({ nodes: snapshot.nodes, edges: snapshot.edges });
}

function defaultNodes(): AppNode[] {
  const id = "draft_default";
  return [
    {
      id,
      type: "image",
      position: { x: 250, y: 200 },
      data: {
        imageId: id,
        fileName: "Image",
        dataUrl: "",
        mimeType: "image/png",
        createdAt: new Date().toISOString(),
        kind: "draft",
        prompt: "",
        modelId: DEFAULT_PROMPT_DATA.modelId,
        params: { ...DEFAULT_PROMPT_DATA.params },
        status: "idle",
        taskId: null,
        errorMessage: null,
        elapsedMs: null,
      },
    },
  ];
}

function migrateNode(n: AppNode): AppNode {
  if (n.type === "prompt") {
    const d = n.data as Record<string, unknown>;
    return {
      id: `draft_${n.id}`,
      type: "image",
      position: n.position,
      selected: n.selected,
      data: {
        imageId: `draft_${n.id}`,
        fileName: "Image",
        dataUrl: "",
        mimeType: "image/png",
        createdAt: new Date().toISOString(),
        kind: "draft",
        prompt: typeof d.prompt === "string" ? d.prompt : "",
        modelId: typeof d.modelId === "string" ? d.modelId : DEFAULT_PROMPT_DATA.modelId,
        params: typeof d.params === "object" && d.params ? d.params : { ...DEFAULT_PROMPT_DATA.params },
        status: d.isGenerating ? "pending" : "idle",
        taskId: null,
        errorMessage: null,
        elapsedMs: null,
      },
    } as AppNode;
  }
  if (n.type === "result") {
    const d = n.data as Record<string, unknown>;
    const imageUrls = Array.isArray(d.imageUrls) ? d.imageUrls : d.imageUrl ? [d.imageUrl] : [];
    return {
      id: `generated_${n.id}`,
      type: "image",
      position: n.position,
      selected: n.selected,
      data: {
        imageId: `generated_${n.id}`,
        fileName: "generated-image.png",
        dataUrl: typeof imageUrls[0] === "string" ? imageUrls[0] : "",
        mimeType: "image/png",
        createdAt: typeof d.createdAt === "string" ? d.createdAt : new Date().toISOString(),
        kind: imageUrls[0] ? "generated" : "draft",
        prompt: typeof d.prompt === "string" ? d.prompt : "",
        modelId: typeof d.modelId === "string" ? d.modelId : DEFAULT_PROMPT_DATA.modelId,
        params: typeof d.params === "object" && d.params ? d.params : { ...DEFAULT_PROMPT_DATA.params },
        status: d.status === "failed" ? "failed" : "idle",
        taskId: typeof d.taskId === "string" ? d.taskId : null,
        errorMessage: typeof d.errorMessage === "string" ? d.errorMessage : null,
        elapsedMs: typeof d.elapsedMs === "number" ? d.elapsedMs : null,
      },
    } as AppNode;
  }
  if (n.type === "text") {
    const d = n.data as Record<string, unknown>;
    return {
      ...n,
      data: {
        content: typeof d.content === "string" ? d.content : "",
        prompt: typeof d.prompt === "string" ? d.prompt : "",
        modelId: typeof d.modelId === "string" ? d.modelId : "Gemini 3.1 Flash Lite",
        status: d.status === "pending" || d.status === "failed" ? d.status : "idle",
        errorMessage: typeof d.errorMessage === "string" ? d.errorMessage : null,
        width: typeof d.width === "number" ? d.width : 320,
        height: typeof d.height === "number" ? d.height : 260,
        createdAt: typeof d.createdAt === "string" ? d.createdAt : new Date().toISOString(),
        updatedAt: typeof d.updatedAt === "string" ? d.updatedAt : undefined,
      },
    } as AppNode;
  }
  if (n.type === "sketch") {
    const d = n.data as Record<string, unknown>;
    return {
      ...n,
      data: {
        sketchId: typeof d.sketchId === "string" ? d.sketchId : n.id,
        projectId: typeof d.projectId === "string" || typeof d.projectId === "number" ? d.projectId : null,
        fileName: typeof d.fileName === "string" ? d.fileName : "Sketch",
        sceneJson: d.sceneJson,
        previewUrl: typeof d.previewUrl === "string" ? d.previewUrl : null,
        dataUrl: typeof d.dataUrl === "string" ? d.dataUrl : "",
        mimeType: typeof d.mimeType === "string" ? d.mimeType : "image/png",
        width: typeof d.width === "number" ? d.width : undefined,
        height: typeof d.height === "number" ? d.height : undefined,
        background: d.background === "transparent" ? "transparent" : "white",
        createdAt: typeof d.createdAt === "string" ? d.createdAt : new Date().toISOString(),
        updatedAt: typeof d.updatedAt === "string" ? d.updatedAt : undefined,
      },
    } as AppNode;
  }
  if (n.type === "video") {
    const d = n.data as Record<string, unknown>;
    const status = d.status === "pending" || d.status === "complete" || d.status === "failed" ? d.status : "idle";
    const ratio = d.ratio === "4:3" || d.ratio === "1:1" || d.ratio === "3:4" || d.ratio === "9:16" || d.ratio === "21:9" ? d.ratio : "16:9";
    const resolution = d.resolution === "480p" || d.resolution === "720p" ? d.resolution : "1080p";
    const duration = d.duration === 10 ? 10 : 5;
    return {
      ...n,
      data: {
        videoId: typeof d.videoId === "string" ? d.videoId : undefined,
        fileName: typeof d.fileName === "string" ? d.fileName : "Video",
        mimeType: typeof d.mimeType === "string" ? d.mimeType : "video/mp4",
        width: typeof d.width === "number" ? d.width : undefined,
        height: typeof d.height === "number" ? d.height : undefined,
        durationSec: typeof d.durationSec === "number" ? d.durationSec : undefined,
        sizeBytes: typeof d.sizeBytes === "number" ? d.sizeBytes : undefined,
        prompt: typeof d.prompt === "string" ? d.prompt : "",
        provider: d.provider === "wan" ? "wan" : "seedance",
        modelId: typeof d.modelId === "string" ? d.modelId : "doubao-seedance-2-0-260128",
        modelName: typeof d.modelName === "string" ? d.modelName : "Seedance 2.0",
        kind: d.kind === "uploaded" || d.kind === "generated" ? d.kind : "draft",
        status,
        taskId: typeof d.taskId === "string" ? d.taskId : null,
        videoUrl: typeof d.videoUrl === "string" ? d.videoUrl : null,
        errorMessage: typeof d.errorMessage === "string" ? d.errorMessage : null,
        ratio,
        resolution,
        duration,
        size: d.size === "704*1280" ? "704*1280" : d.size === "1280*704" ? "1280*704" : undefined,
        generateAudio: typeof d.generateAudio === "boolean" ? d.generateAudio : true,
        watermark: typeof d.watermark === "boolean" ? d.watermark : false,
        createdAt: typeof d.createdAt === "string" ? d.createdAt : new Date().toISOString(),
        generationStartedAt: typeof d.generationStartedAt === "string" ? d.generationStartedAt : null,
        generationRunStartedAt: typeof d.generationRunStartedAt === "string" ? d.generationRunStartedAt : null,
        elapsedMs: typeof d.elapsedMs === "number" ? d.elapsedMs : null,
        upstreamStatus: typeof d.upstreamStatus === "string" ? d.upstreamStatus : null,
      },
    } as AppNode;
  }
  return n;
}

function migrateEdge(e: AppEdge): AppEdge {
  if (!e.type || e.type === "default" || e.type === "smoothstep" || e.type === "bezier") {
    return { ...e, type: "signal" };
  }
  return e;
}

function isValidCanvasConnection(connection: { source: string; target: string }, nodes: AppNode[]) {
  const source = nodes.find((n) => n.id === connection.source);
  const target = nodes.find((n) => n.id === connection.target);
  return (
    (source?.type === "image" && target?.type === "image" && connection.source !== connection.target) ||
    (source?.type === "image" && target?.type === "video") ||
    (source?.type === "image" && target?.type === "text") ||
    (source?.type === "sketch" && target?.type === "image") ||
    (source?.type === "sketch" && target?.type === "video") ||
    (source?.type === "text" && target?.type === "image") ||
    (source?.type === "text" && target?.type === "text" && connection.source !== connection.target) ||
    (source?.type === "image" && target?.type === "prompt") ||
    (source?.type === "prompt" && target?.type === "result")
  );
}

function isSameCanvasEdge(left: AppEdge, right: AppEdge) {
  return left.source === right.source &&
    left.target === right.target &&
    (left.sourceHandle ?? null) === (right.sourceHandle ?? null) &&
    (left.targetHandle ?? null) === (right.targetHandle ?? null);
}

async function blobFromDataUrl(dataUrl: string): Promise<Blob> {
  const response = await fetch(dataUrl);
  return response.blob();
}

function summarizeCanvas(nodes: AppNode[]): { kind: ProjectKind; nodeCount: number; assetCount: number } {
  const hasImage = nodes.some((node) => node.type === "image");
  const hasVideo = nodes.some((node) => node.type === "video");
  const kind: ProjectKind = hasImage && hasVideo ? "mixed" : hasVideo ? "video" : "image";
  return {
    kind,
    nodeCount: nodes.length,
    assetCount: nodes.filter((node) => {
      if (node.type === "image") return Boolean((node.data as ImageNodeData).dataUrl);
      if (node.type === "sketch") return Boolean((node.data as SketchNodeData).dataUrl || (node.data as SketchNodeData).sceneJson);
      if (node.type === "video") return Boolean((node.data as VideoNodeData).videoUrl);
      return false;
    }).length,
  };
}

function isServerProjectId(projectId: string | null | undefined): projectId is string {
  return typeof projectId === "string" && /^\d+$/.test(projectId);
}

function CanvasFlow() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const routeProjectId = searchParams.get("projectId");
  const [activeProjectId, setActiveProjectId] = useState<string | null>(null);
  const serverProjectId = isServerProjectId(activeProjectId) ? activeProjectId : null;
  const [nodes, setNodes] = useNodesState<AppNode>(defaultNodes());
  const [edges, setEdges] = useEdgesState<AppEdge>([]);
  const [saveError, setSaveError] = useState("");
  const [pasteToast, setPasteToast] = useState("");
  const [isHydrated, setIsHydrated] = useState(false);
  const [isReadOnly, setIsReadOnly] = useState(false);
  const [projectRole, setProjectRole] = useState<CanvasProjectRole | null>(null);
  const [projectMembers, setProjectMembers] = useState<CanvasMember[]>([]);
  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [lastAppliedVersion, setLastAppliedVersion] = useState(0);
  const [latestKnownVersion, setLatestKnownVersion] = useState(0);
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const [showMiniMap, setShowMiniMap] = useState(false);
  const [snapToGrid, setSnapToGrid] = useState(false);
  const [canvasZoom, setCanvasZoom] = useState(1);
  const [createMenu, setCreateMenu] = useState<{
    x: number;
    y: number;
    flowX: number;
    flowY: number;
    visible: boolean;
    originNodeId: string | null;
    direction: LinkedCreateDirection | null;
  }>({ x: 0, y: 0, flowX: 0, flowY: 0, visible: false, originNodeId: null, direction: null });
  const [pendingConnectionPreview, setPendingConnectionPreview] = useState<PendingConnectionPreview | null>(null);

  const {
    getNodes,
    getViewport,
    screenToFlowPosition,
    flowToScreenPosition,
    setViewport,
    zoomIn,
    zoomOut,
    fitView,
  } = useReactFlow();

  // Track last mouse position for paste placement
  const lastMouseRef = useRef<{ x: number; y: number }>({
    x: window.innerWidth / 2,
    y: window.innerHeight / 2,
  });
  const historyPastRef = useRef<CanvasSnapshot[]>([]);
  const historyFutureRef = useRef<CanvasSnapshot[]>([]);
  const lastHistorySignatureRef = useRef<string | null>(null);
  const restoringHistoryRef = useRef(false);
  const projectCreationRef = useRef(false);
  const ignoreNextPaneClickRef = useRef(false);
  const [clientId] = useState(() => `canvas_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`);
  const { createProject: createServerProject, loadProject, saveSnapshot } = useCanvasServerStorage();
  const canvasRealtime = useCanvasRealtime(serverProjectId, clientId, lastAppliedVersion);
  const canvasOperations = useCanvasOperations(serverProjectId, clientId, latestKnownVersion, setLatestKnownVersion, canvasRealtime.sendOperation, canvasRealtime.isConnected);
  const processedRealtimeMessageCountRef = useRef(0);
  const [remotePresences, setRemotePresences] = useState<Record<string, RemoteCanvasPresence>>({});
  const lastPresenceSentAtRef = useRef(0);
  const editingNodeIdRef = useRef<string | null>(null);
  const nodeDataPatchTimersRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  const lastAppliedVersionRef = useRef(0);

  const markAppliedVersion = useCallback((version: number) => {
    lastAppliedVersionRef.current = Math.max(lastAppliedVersionRef.current, version);
    setLastAppliedVersion((prev) => Math.max(prev, version));
    setLatestKnownVersion((prev) => Math.max(prev, version));
  }, []);

  const applyRemoteOperation = useCallback((operationType: string, payload: Record<string, unknown>) => {
    if (operationType === "NODE_MOVE" && typeof payload.nodeId === "string") {
      setNodes((nds) => nds.map((node) => node.id === payload.nodeId ? { ...node, position: payload.position as AppNode["position"] } : node));
      return;
    }
    if (operationType === "NODE_RESIZE" && typeof payload.nodeId === "string") {
      setNodes((nds) => nds.map((node) => node.id === payload.nodeId ? { ...node, measured: payload.dimensions as AppNode["measured"] } : node));
      return;
    }
    if (operationType === "NODE_DELETE" && typeof payload.nodeId === "string") {
      setNodes((nds) => nds.filter((node) => node.id !== payload.nodeId));
      setEdges((eds) => eds.filter((edge) => edge.source !== payload.nodeId && edge.target !== payload.nodeId));
      return;
    }
    if (operationType === "NODE_CREATE" && payload.node) {
      const node = payload.node as AppNode;
      setNodes((nds) => nds.some((item) => item.id === node.id) ? nds : [...nds.map((item) => ({ ...item, selected: false })), node]);
      return;
    }
    if ((operationType === "NODE_UPDATE_DATA" || operationType === "TASK_STATUS_PATCH") && typeof payload.nodeId === "string" && payload.patch) {
      setNodes((nds) => nds.map((node) => node.id === payload.nodeId ? migrateNode({ ...node, data: { ...node.data, ...(payload.patch as Record<string, unknown>) } } as AppNode) : node));
      return;
    }
    if (operationType === "ASSET_ATTACH" && typeof payload.nodeId === "string") {
      setNodes((nds) => nds.map((node) => node.id === payload.nodeId ? {
        ...node,
        data: {
          ...node.data,
          assetId: payload.assetId,
          assetVersionId: payload.assetVersionId ?? node.data.assetVersionId,
          previewUrl: payload.previewUrl ?? node.data.previewUrl,
        },
      } as AppNode : node));
      return;
    }
    if (operationType === "EDGE_CREATE" && payload.edge) {
      const edge = payload.edge as AppEdge;
      setEdges((eds) => {
        const nodes = getNodes() as AppNode[];
        if (!isValidCanvasConnection(edge, nodes)) return eds;
        if (eds.some((item) => item.id === edge.id || isSameCanvasEdge(item, edge))) return eds;
        return addEdge(edge, eds);
      });
      return;
    }
    if (operationType === "EDGE_DELETE" && typeof payload.edgeId === "string") {
      setEdges((eds) => eds.filter((edge) => edge.id !== payload.edgeId));
      return;
    }
    if (operationType === "CANVAS_CLEAR") {
      setNodes(defaultNodes());
      setEdges([]);
    }
  }, [getNodes, setEdges, setNodes]);

  const hydrateRemoteSnapshot = useCallback((snapshot: Parameters<typeof snapshotRecordToCanvasState>[0]) => {
    const state = snapshotRecordToCanvasState(snapshot);
    if (!state) return;
    setNodes(state.nodes.map(migrateNode));
    setEdges(state.edges.map(migrateEdge));
    if (state.viewport) setViewport(state.viewport);
    if (typeof snapshot?.version === "number") markAppliedVersion(snapshot.version);
  }, [markAppliedVersion, setEdges, setNodes, setViewport]);

  const applyOperationRecord = useCallback((operationRecord: { clientId: string; nextVersion: number; operationType: string; operationJson: string }) => {
    setLatestKnownVersion((prev) => Math.max(prev, operationRecord.nextVersion));
    if (operationRecord.clientId !== clientId) {
      try {
        const operation = JSON.parse(operationRecord.operationJson) as { type?: string; payload?: Record<string, unknown> };
        applyRemoteOperation(operation.type ?? operationRecord.operationType, operation.payload ?? {});
      } catch {
      }
    }
    markAppliedVersion(operationRecord.nextVersion);
  }, [applyRemoteOperation, clientId, markAppliedVersion]);

  const syncFromVersion = useCallback((afterVersion: number) => {
    if (!serverProjectId) return;
    canvasApi.syncOperations(serverProjectId, afterVersion)
      .then((syncResult) => {
        if (syncResult.mode === "snapshot") {
          hydrateRemoteSnapshot(syncResult.snapshot);
          return;
        }
        for (const operationRecord of syncResult.operations ?? []) {
          applyOperationRecord(operationRecord);
        }
        if (typeof syncResult.toVersion === "number") {
          setLatestKnownVersion((prev) => Math.max(prev, syncResult.toVersion ?? prev));
        }
      })
      .catch(() => undefined);
  }, [serverProjectId, applyOperationRecord, hydrateRemoteSnapshot]);

  useEffect(() => {
    const newMessages = canvasRealtime.messages.slice(processedRealtimeMessageCountRef.current);
    processedRealtimeMessageCountRef.current = canvasRealtime.messages.length;
    for (const message of newMessages) {
      if (message.type === "canvas-presence") {
        if (typeof message.clientId === "string" && message.clientId !== clientId) {
          const remoteClientId = message.clientId;
          const presence = {
            ...(message as CanvasPresence),
            projectId: String(message.projectId ?? serverProjectId ?? activeProjectId ?? ""),
            clientId: remoteClientId,
            updatedAt: Date.now(),
          } satisfies RemoteCanvasPresence;
          setRemotePresences((prev) => ({ ...prev, [remoteClientId]: presence }));
        }
        continue;
      }
      if (message.type === "canvas-member-updated") {
        if (message.event === "leave" && typeof message.clientId === "string") {
          setRemotePresences((prev) => {
            const next = { ...prev };
            delete next[message.clientId as string];
            return next;
          });
        }
        if (serverProjectId) {
          canvasApi.getProjectMembers(serverProjectId).then(setProjectMembers).catch(() => undefined);
          canvasApi.getProject(serverProjectId).then((project) => {
            setProjectRole((project.role ?? null) as CanvasProjectRole | null);
            setIsReadOnly(Boolean(project.readonly) || project.role === "viewer" || project.canEdit === false);
          }).catch(() => {
            setIsReadOnly(true);
            setSaveError("你已无权访问该画布");
          });
        }
        continue;
      }
      if (message.type === "canvas-op-rejected") {
        if (message.clientId === clientId && typeof message.opId === "string") {
          canvasOperations.markOperationRejected(message.opId);
        }
        syncFromVersion(lastAppliedVersionRef.current);
        continue;
      }
      if (message.type !== "canvas-op-applied") continue;
      const version = Number(message.version);
      if (!Number.isFinite(version)) continue;
      setLatestKnownVersion((prev) => Math.max(prev, version));
      if (message.clientId === clientId && typeof message.opId === "string") {
        canvasOperations.markOperationAcked(message.opId, version);
      }
      if (version > lastAppliedVersionRef.current + 1) {
        syncFromVersion(lastAppliedVersionRef.current);
        continue;
      }
      if (typeof message.clientId !== "string" || typeof message.operationType !== "string" || typeof message.operationJson !== "string") {
        continue;
      }
      applyOperationRecord({
        clientId: String(message.clientId),
        nextVersion: version,
        operationType: String(message.operationType),
        operationJson: String(message.operationJson),
      });
    }
  }, [activeProjectId, applyOperationRecord, canvasOperations, canvasRealtime.messages, clientId, serverProjectId, syncFromVersion]);

  useEffect(() => {
    if (!serverProjectId || !isHydrated || !canvasRealtime.isConnected) return;
    syncFromVersion(lastAppliedVersionRef.current);
  }, [serverProjectId, canvasRealtime.isConnected, isHydrated, syncFromVersion]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      const now = Date.now();
      setRemotePresences((prev) => {
        let changed = false;
        const next = { ...prev };
        for (const [presenceClientId, presence] of Object.entries(prev)) {
          const updatedAt = typeof (presence as CanvasPresence & { updatedAt?: number }).updatedAt === "number"
            ? (presence as CanvasPresence & { updatedAt: number }).updatedAt
            : now;
          if (now - updatedAt > 10_000) {
            delete next[presenceClientId];
            changed = true;
          }
        }
        return changed ? next : prev;
      });
    }, 2_000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    function handleNodeDataPatch(event: Event) {
      if (isReadOnly) return;
      const detail = (event as CustomEvent<NodeDataPatchEventDetail>).detail;
      if (!detail?.nodeId || !detail.patch) return;
      const key = `${detail.nodeId}:${Object.keys(detail.patch).sort().join(",")}`;
      clearTimeout(nodeDataPatchTimersRef.current[key]);
      nodeDataPatchTimersRef.current[key] = setTimeout(() => {
        const patch = filterSyncableNodeDataPatch(detail.patch);
        if (Object.keys(patch).length === 0) {
          delete nodeDataPatchTimersRef.current[key];
          return;
        }
        canvasOperations.submitOperation("NODE_UPDATE_DATA", {
          nodeId: detail.nodeId,
          patch,
        });
        delete nodeDataPatchTimersRef.current[key];
      }, 160);
    }
    window.addEventListener("copse:node-data-patch", handleNodeDataPatch);
    return () => window.removeEventListener("copse:node-data-patch", handleNodeDataPatch);
  }, [canvasOperations, isReadOnly]);

  useEffect(() => {
    function handleNodeEditingPresence(event: Event) {
      const detail = (event as CustomEvent<NodeEditingPresenceEventDetail>).detail;
      editingNodeIdRef.current = detail?.nodeId ?? null;
      canvasRealtime.sendPresence({
        editingNodeId: editingNodeIdRef.current,
        selectedNodeIds: nodes.filter((node) => node.selected).map((node) => node.id),
        viewport: getViewport(),
      });
    }
    window.addEventListener("copse:node-editing-presence", handleNodeEditingPresence);
    return () => window.removeEventListener("copse:node-editing-presence", handleNodeEditingPresence);
  }, [canvasRealtime, getViewport, nodes]);

  const handleCanvasMouseMove = useCallback((event: React.MouseEvent<HTMLDivElement>) => {
    if (!serverProjectId) return;
    const now = Date.now();
    if (now - lastPresenceSentAtRef.current < 120) return;
    lastPresenceSentAtRef.current = now;
    const rect = event.currentTarget.getBoundingClientRect();
    const screenCursor = { x: event.clientX - rect.left, y: event.clientY - rect.top };
    const cursor = screenToFlowPosition({ x: event.clientX, y: event.clientY });
    canvasRealtime.sendPresence({
      cursor,
      screenCursor,
      selectedNodeIds: nodes.filter((node) => node.selected).map((node) => node.id),
      editingNodeId: editingNodeIdRef.current,
      viewport: getViewport(),
    });
  }, [serverProjectId, canvasRealtime, getViewport, nodes, screenToFlowPosition]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (routeProjectId) {
        setActiveProjectId(routeProjectId);
        return;
      }

      if (projectCreationRef.current) return;
      projectCreationRef.current = true;
      createServerProject("未命名项目", "image")
        .then((projectId) => {
          const id = String(projectId);
          router.replace(`/create/image?projectId=${encodeURIComponent(id)}`);
          setActiveProjectId(id);
        })
        .catch(() => {
          const project = createProject({ name: "未命名项目", kind: "image" });
          router.replace(`/create/image?projectId=${encodeURIComponent(project.id)}`);
          setActiveProjectId(project.id);
        });
    }, 0);
    return () => window.clearTimeout(timer);
  }, [createServerProject, routeProjectId, router]);

  useEffect(() => {
    if (!isHydrated) return;
    const snapshot = cloneSnapshot(nodes, edges);
    const signature = snapshotSignature(snapshot);

    if (restoringHistoryRef.current) {
      lastHistorySignatureRef.current = signature;
      restoringHistoryRef.current = false;
      return;
    }

    const previousSignature = lastHistorySignatureRef.current;
    if (!previousSignature) {
      lastHistorySignatureRef.current = signature;
      return;
    }

    if (previousSignature === signature) return;
    try {
      historyPastRef.current.push(JSON.parse(previousSignature) as CanvasSnapshot);
      if (historyPastRef.current.length > 80) {
        historyPastRef.current.shift();
      }
      historyFutureRef.current = [];
      lastHistorySignatureRef.current = signature;
    } catch {
      lastHistorySignatureRef.current = signature;
    }
  }, [edges, isHydrated, nodes]);

  const restoreSnapshot = useCallback(
    (snapshot: CanvasSnapshot) => {
      restoringHistoryRef.current = true;
      setNodes(snapshot.nodes);
      setEdges(snapshot.edges);
      setCreateMenu((prev) => ({ ...prev, visible: false }));
    },
    [setEdges, setNodes]
  );

  const undo = useCallback(() => {
    if (isReadOnly) return;
    const previous = historyPastRef.current.pop();
    if (!previous) return;
    historyFutureRef.current.push(cloneSnapshot(nodes, edges));
    restoreSnapshot(previous);
  }, [edges, isReadOnly, nodes, restoreSnapshot]);

  const redo = useCallback(() => {
    if (isReadOnly) return;
    const next = historyFutureRef.current.pop();
    if (!next) return;
    historyPastRef.current.push(cloneSnapshot(nodes, edges));
    restoreSnapshot(next);
  }, [edges, isReadOnly, nodes, restoreSnapshot]);

  const onNodesChange = useCallback(
    (changes: NodeChange<AppNode>[]) => {
      if (isReadOnly) {
        const selectionChanges = changes.filter((change) => change.type === "select");
        if (selectionChanges.length > 0) setNodes((nds) => applyNodeChanges(selectionChanges, nds));
        return;
      }
      setNodes((nds) => applyNodeChanges(changes, nds));
      for (const change of changes) {
        if (change.type === "position" && change.dragging === false && change.position) {
          canvasOperations.submitOperation("NODE_MOVE", {
            nodeId: change.id,
            position: change.position,
          });
        }
        if (change.type === "dimensions" && change.dimensions) {
          canvasOperations.submitOperation("NODE_RESIZE", {
            nodeId: change.id,
            dimensions: change.dimensions,
          });
        }
        if (change.type === "remove") {
          canvasOperations.submitOperation("NODE_DELETE", {
            nodeId: change.id,
          });
        }
      }
    },
    [canvasOperations, isReadOnly, setNodes]
  );

  const onEdgesChange = useCallback(
    (changes: EdgeChange<AppEdge>[]) => {
      if (isReadOnly) {
        const selectionChanges = changes.filter((change) => change.type === "select");
        if (selectionChanges.length > 0) setEdges((eds) => applyEdgeChanges(selectionChanges, eds));
        return;
      }
      setEdges((eds) => applyEdgeChanges(changes, eds));
      for (const change of changes) {
        if (change.type === "remove") {
          canvasOperations.submitOperation("EDGE_DELETE", {
            edgeId: change.id,
          });
        }
      }
    },
    [canvasOperations, isReadOnly, setEdges]
  );

  useEffect(() => {
    function handleUndoRedo(event: KeyboardEvent) {
      if (isEditableElement(event.target)) return;
      const isUndo = (event.metaKey || event.ctrlKey) && !event.shiftKey && event.key.toLowerCase() === "z";
      const isRedo =
        (event.metaKey || event.ctrlKey) &&
        ((event.shiftKey && event.key.toLowerCase() === "z") || event.key.toLowerCase() === "y");

      if (isUndo) {
        event.preventDefault();
        undo();
      } else if (isRedo) {
        event.preventDefault();
        redo();
      }
    }

    window.addEventListener("keydown", handleUndoRedo);
    return () => window.removeEventListener("keydown", handleUndoRedo);
  }, [redo, undo]);

  // --- Connection handling ---
  const onConnect = useCallback((connection: Connection) => {
    if (isReadOnly) return;
    const edge: AppEdge = { ...connection, id: `e-${connection.source}-${connection.target}-${Date.now()}`, type: "signal" };
    let shouldSubmit = false;
    setEdges((eds) => {
      if (eds.some((item) => isSameCanvasEdge(item, edge))) return eds;
      shouldSubmit = true;
      return addEdge(edge, eds);
    });
    if (shouldSubmit) canvasOperations.submitOperation("EDGE_CREATE", { edge });
  }, [canvasOperations, isReadOnly, setEdges]);

  // Hydrate from localStorage + IndexedDB when the project changes
  useEffect(() => {
    if (!activeProjectId) return;
    const projectId = activeProjectId;
    let cancelled = false;

    async function hydrate() {
      setIsHydrated(false);
      historyPastRef.current = [];
      historyFutureRef.current = [];
      lastHistorySignatureRef.current = null;

      let saved = loadCanvas(projectId);
      if (isServerProjectId(projectId)) {
        try {
          const serverResult = await loadProject(projectId);
          saved = serverResult.state ?? saved;
          setIsReadOnly(serverResult.project.readonly === true || serverResult.project.canEdit === false);
          setProjectRole(serverResult.project.role ?? null);
          canvasApi.getProjectMembers(projectId).then(setProjectMembers).catch(() => setProjectMembers([]));
          markAppliedVersion(serverResult.project.currentVersion ?? 0);
        } catch {
        }
      } else {
        setIsReadOnly(false);
        setProjectRole(null);
        setProjectMembers([]);
        markAppliedVersion(0);
      }
      if (!saved) {
        if (!cancelled) {
          setNodes(defaultNodes());
          setEdges([]);
          setIsHydrated(true);
        }
        return;
      }

      const idMap = new Map<string, string>();
      const migratedNodes = saved.nodes.map((node) => {
        const migrated = migrateNode(node);
        idMap.set(node.id, migrated.id);
        return migrated;
      });
      const migratedEdges = saved.edges
        .map(migrateEdge)
        .map((edge) => ({
          ...edge,
          source: idMap.get(edge.source) ?? edge.source,
          target: idMap.get(edge.target) ?? edge.target,
        }))
        .filter((edge) => isValidCanvasConnection(edge, migratedNodes));

      const restoredNodes: AppNode[] = [];

      for (const node of migratedNodes) {
        if (cancelled) return;
        if (node.type !== "image" && node.type !== "video") {
          restoredNodes.push(node);
          continue;
        }
        const d = node.data as Record<string, unknown>;
        if (node.type === "image" && typeof d.dataUrl === "string" && d.dataUrl) {
          await saveImage(d as ImageNodeData);
          const full = await loadImage(d.imageId as string);
          restoredNodes.push(full?.dataUrl ? { ...node, data: full } : { ...node, data: { ...(d as ImageNodeData), dataUrl: "" } });
          continue;
        }
        if (node.type === "image") {
          const full = await loadImage(d.imageId as string);
          restoredNodes.push(full?.dataUrl ? { ...node, data: full } : { ...node, data: { ...(d as ImageNodeData), dataUrl: "" } });
          continue;
        }
        if (node.type === "video" && typeof d.videoId === "string" && typeof d.videoUrl === "string" && d.videoUrl.startsWith("data:")) {
          const blob = await blobFromDataUrl(d.videoUrl);
          await saveVideo(d as VideoNodeData, blob);
          restoredNodes.push({ ...node, data: { ...(d as VideoNodeData), videoUrl: URL.createObjectURL(blob) } });
          continue;
        }
        if (node.type === "video" && typeof d.videoId === "string") {
          const full = await loadVideo(d.videoId);
          restoredNodes.push(full?.videoUrl ? { ...node, data: full } : node);
          continue;
        }
        restoredNodes.push(node);
      }

      if (cancelled) return;
      setNodes(restoredNodes);
      setEdges(migratedEdges);
      if (saved.viewport) setViewport(saved.viewport);
      setIsHydrated(true);
    }

    hydrate();
    return () => { cancelled = true; };
  }, [activeProjectId, loadProject, markAppliedVersion, setNodes, setEdges, setViewport]);

  // Debounced save — only after hydration completes
  const saveTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => {
    if (!isHydrated || !activeProjectId) return;
    clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => {
      try {
        saveCanvas({ nodes, edges, viewport: getViewport() }, activeProjectId);
        const summary = summarizeCanvas(nodes);
        if (!isReadOnly && serverProjectId && canvasOperations.pendingOperationCount === 0) {
          saveSnapshot(serverProjectId, {
            nodes,
            edges,
            viewport: getViewport(),
            baseVersion: lastAppliedVersionRef.current,
            clientId,
            ...summary,
          }).then((snapshot) => {
            markAppliedVersion(snapshot.version);
          }).catch(() => syncFromVersion(lastAppliedVersionRef.current));
          canvasApi.updateProject(serverProjectId, summary).catch(() => {
            updateLocalProject(activeProjectId, summary);
          });
        } else if (!serverProjectId) {
          updateLocalProject(activeProjectId, summary);
        }
        setSaveError("");
      } catch {
        setSaveError("图片太大，当前浏览器无法保存到本地草稿");
      }
    }, 500);
    return () => clearTimeout(saveTimer.current);
  }, [activeProjectId, canvasOperations.pendingOperationCount, clientId, edges, getViewport, isHydrated, isReadOnly, markAppliedVersion, nodes, saveSnapshot, serverProjectId, syncFromVersion]);

  // --- Add nodes ---
  const addImageDraftNode = useCallback((position?: { x: number; y: number }) => {
    if (isReadOnly) return null;
    const center = position ?? screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `draft_${Date.now()}`;
    const newNode: AppNode = {
      id,
      type: "image",
      position: findOpenNodePosition(
        { x: center.x - 190, y: center.y - 150 },
        { width: 360, height: 340 },
        getNodes() as AppNode[]
      ),
      data: {
        imageId: id,
        fileName: "Image",
        dataUrl: "",
        mimeType: "image/png",
        createdAt: new Date().toISOString(),
        kind: "draft",
        prompt: "",
        modelId: DEFAULT_PROMPT_DATA.modelId,
        params: { ...DEFAULT_PROMPT_DATA.params },
        status: "idle",
        taskId: null,
        errorMessage: null,
        elapsedMs: null,
      },
      selected: true,
    };
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  const addSketchNode = useCallback((position?: { x: number; y: number }) => {
    if (isReadOnly) return null;
    const center = position ?? screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `sketch_${Date.now()}`;
    const now = new Date().toISOString();
    const sketchData: SketchNodeData = {
      sketchId: id,
      projectId: serverProjectId,
      fileName: "Sketch",
      sceneJson: undefined,
      previewUrl: null,
      dataUrl: "",
      mimeType: "image/png",
      background: "white",
      createdAt: now,
      updatedAt: now,
    };
    const newNode: AppNode = {
      id,
      type: "sketch",
      position: findOpenNodePosition(
        { x: center.x - 150, y: center.y - 110 },
        { width: 300, height: 240 },
        getNodes() as AppNode[]
      ),
      data: sketchData,
      selected: true,
    };
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, serverProjectId, setNodes]);

  const addTextNode = useCallback((position?: { x: number; y: number }) => {
    if (isReadOnly) return null;
    const center = position ?? screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `text_${Date.now()}`;
    const textData: TextNodeData = {
      content: "",
      prompt: "",
      modelId: "Gemini 3.1 Flash Lite",
      status: "idle",
      errorMessage: null,
      width: 320,
      height: 260,
      createdAt: new Date().toISOString(),
    };
    const newNode: AppNode = {
      id,
      type: "text",
      position: findOpenNodePosition(
        { x: center.x - 160, y: center.y - 130 },
        { width: 320, height: 260 },
        getNodes() as AppNode[]
      ),
      data: textData,
      selected: true,
    };
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  const addVideoNode = useCallback((position?: { x: number; y: number }) => {
    if (isReadOnly) return null;
    const center = position ?? screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `video_${Date.now()}`;
    const videoData: VideoNodeData = {
      prompt: "",
      provider: "seedance",
      modelId: "doubao-seedance-2-0-260128",
      modelName: "Seedance 2.0",
      kind: "draft",
      status: "idle",
      taskId: null,
      videoUrl: null,
      errorMessage: null,
      ratio: "16:9",
      resolution: "1080p",
      duration: 5,
      size: "1280*704",
      generateAudio: true,
      watermark: false,
      createdAt: new Date().toISOString(),
      generationStartedAt: null,
      generationRunStartedAt: null,
      elapsedMs: null,
      upstreamStatus: null,
    };
    const newNode: AppNode = {
      id,
      type: "video",
      position: findOpenNodePosition(
        { x: center.x - 210, y: center.y - 130 },
        { width: 420, height: 448 },
        getNodes() as AppNode[]
      ),
      data: videoData,
      selected: true,
    };
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  const closeReferencePicker = useCallback(() => {
    window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
      detail: { promptId: null },
    }));
  }, []);

  useEffect(() => {
    function handleReferencePicker(e: Event) {
      const detail = (e as CustomEvent<ReferencePickerEventDetail>).detail;
      setReferencePickerPromptId(detail?.promptId ?? null);
    }
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") closeReferencePicker();
    }

    window.addEventListener("copse:reference-picker", handleReferencePicker);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("copse:reference-picker", handleReferencePicker);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [closeReferencePicker]);

  // --- Upload media ---
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFiles = useCallback(
    async (files: File[], position?: { x: number; y: number }) => {
      if (isReadOnly) return;
      const center = position ?? screenToFlowPosition({
        x: window.innerWidth / 2,
        y: window.innerHeight / 2,
      });

      const newNodes: AppNode[] = [];
      const existingNodes = getNodes() as AppNode[];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        try {
          const preferred = {
            x: center.x + i * 100,
            y: center.y + i * 80 - ((files.length - 1) * 40),
          };
          if (isAcceptedImageType(file.type)) {
            let imageData: ImageNodeData = await fileToImageNodeData(file);
            try {
              imageData = await attachImageAsset(file, imageData);
            } catch {
            }
            await saveImage(imageData);
            newNodes.push({
              id: imageData.imageId,
              type: "image",
              position: findOpenNodePosition(
                preferred,
                { width: 220, height: 260 },
                [...existingNodes, ...newNodes],
                { padding: 28, stepX: 150, stepY: 130 }
              ),
              data: imageData,
            });
            continue;
          }
          if (isAcceptedVideoFile(file)) {
            const { data, blob } = await fileToVideoNodeData(file);
            let videoData = data;
            try {
              videoData = await attachVideoAsset(file, videoData);
            } catch {
            }
            await saveVideo(videoData, blob);
            newNodes.push({
              id: videoData.videoId ?? `uploaded_video_${Date.now()}`,
              type: "video",
              position: findOpenNodePosition(
                preferred,
                { width: 420, height: 260 },
                [...existingNodes, ...newNodes],
                { padding: 28, stepX: 170, stepY: 140 }
              ),
              data: videoData,
            });
          }
        } catch (error) {
          setPasteToast(error instanceof Error ? error.message : "素材上传失败");
          setTimeout(() => setPasteToast(""), 2500);
        }
      }
      if (newNodes.length > 0) {
        if (!activeProjectId) return;
        setNodes((nds) => [...nds, ...newNodes]);
        for (const node of newNodes) {
          canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(node) });
          if (serverProjectId && (node.data as ImageNodeData | VideoNodeData).assetId) {
            const mediaData = node.data as ImageNodeData | VideoNodeData;
            canvasApi.bindNodeAsset(serverProjectId, node.id, {
              assetId: mediaData.assetId!,
              assetVersionId: mediaData.assetVersionId ?? null,
              previewUrl: mediaData.previewUrl ?? null,
              usageType: "source",
            }).catch(() => undefined);
          }
        }
      }
    },
    [activeProjectId, canvasOperations, getNodes, isReadOnly, screenToFlowPosition, serverProjectId, setNodes]
  );

  const handleFileInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files;
      if (!files || files.length === 0) return;
      handleFiles(Array.from(files));
      e.target.value = "";
    },
    [handleFiles]
  );

  // --- Drag & Drop ---
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    if (isReadOnly) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "copy";
    setIsDragOver(true);
  }, [isReadOnly]);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(false);
      const mediaFiles = getFilesFromDrop(e);
      if (mediaFiles.length === 0) return;

      const flowPos = screenToFlowPosition({ x: e.clientX, y: e.clientY });
      handleFiles(mediaFiles, flowPos);
    },
    [screenToFlowPosition, handleFiles]
  );

  // --- Paste from clipboard (keyboard) ---
  const handlePaste = useCallback(
    (e: ClipboardEvent) => {
      if (isReadOnly) return;
      if (isEditableElement(e.target)) return;
      const files = getImageFilesFromPasteEvent(e);
      if (files.length === 0) return;
      e.preventDefault();
      const pos = screenToFlowPosition(lastMouseRef.current);
      handleFiles(files, pos);
    },
    [handleFiles, isReadOnly, screenToFlowPosition]
  );

  useEffect(() => {
    document.addEventListener("paste", handlePaste);
    return () => document.removeEventListener("paste", handlePaste);
  }, [handlePaste]);

  // --- Track mouse position ---
  useEffect(() => {
    function onMouseMove(e: MouseEvent) {
      lastMouseRef.current = { x: e.clientX, y: e.clientY };
    }
    document.addEventListener("mousemove", onMouseMove);
    return () => document.removeEventListener("mousemove", onMouseMove);
  }, []);

  // --- Context menu ---
  const [contextMenu, setContextMenu] = useState<ContextMenuState>({
    x: 0,
    y: 0,
    visible: false,
  });

  const handleContextMenu = useCallback((e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest(".react-flow__node")) return;
    e.preventDefault();
    setContextMenu({ x: e.clientX, y: e.clientY, visible: true });
  }, []);

  const closeContextMenu = useCallback(() => {
    setContextMenu((prev) => ({ ...prev, visible: false }));
  }, []);

  const handleMenuPaste = useCallback(async () => {
    if (isReadOnly) return;
    const files = await getImageFilesFromClipboardAPI();
    if (files && files.length > 0) {
      const pos = screenToFlowPosition(contextMenu);
      handleFiles(files, pos);
      setPasteToast("");
    } else {
      setPasteToast("请使用 ⌘V 粘贴图片");
      setTimeout(() => setPasteToast(""), 2500);
    }
  }, [contextMenu, screenToFlowPosition, handleFiles, isReadOnly, setPasteToast]);

  const handleMenuZoomIn = useCallback(() => { zoomIn({ duration: 200 }); }, [zoomIn]);
  const handleMenuZoomOut = useCallback(() => { zoomOut({ duration: 200 }); }, [zoomOut]);
  const handleMenuFitView = useCallback(() => { fitView({ padding: 0.2, duration: 200 }); }, [fitView]);
  const handleMenuZoom100 = useCallback(() => {
    const vp = getViewport();
    setViewport({ x: vp.x, y: vp.y, zoom: 1 }, { duration: 200 });
  }, [getViewport, setViewport]);
  const handleToolbarResetView = useCallback(() => {
    fitView({ padding: 0.2, duration: 220 });
  }, [fitView]);
  const handleToolbarZoomChange = useCallback((zoom: number) => {
    const vp = getViewport();
    const flowElement = document.querySelector(".react-flow");
    const rect = flowElement?.getBoundingClientRect();
    if (rect) {
      const centerScreen = {
        x: rect.left + rect.width / 2,
        y: rect.top + rect.height / 2,
      };
      const centerFlow = screenToFlowPosition(centerScreen);
      setCanvasZoom(zoom);
      setViewport({
        x: centerScreen.x - centerFlow.x * zoom,
        y: centerScreen.y - centerFlow.y * zoom,
        zoom,
      }, { duration: 80 });
      return;
    }
    setCanvasZoom(zoom);
    setViewport({ x: vp.x, y: vp.y, zoom }, { duration: 80 });
  }, [getViewport, screenToFlowPosition, setViewport]);

  const closeCreateMenu = useCallback(() => {
    setCreateMenu((prev) => ({ ...prev, visible: false }));
    setPendingConnectionPreview(null);
  }, []);

  const openCreateMenuAt = useCallback(
    (
      point: { x: number; y: number },
      origin?: { nodeId: string; direction: LinkedCreateDirection },
      preview?: PendingConnectionPreview | null
    ) => {
      if (isReadOnly) return;
      const originNode = origin ? nodes.find((node) => node.id === origin.nodeId) : undefined;
      if (origin && getCreateKindsForOrigin(originNode?.type, origin.direction).length === 0) return;

      closeContextMenu();
      closeReferencePicker();
      const flowPosition = screenToFlowPosition(point);
      setCreateMenu({
        x: point.x,
        y: point.y,
        flowX: flowPosition.x,
        flowY: flowPosition.y,
        visible: true,
        originNodeId: origin?.nodeId ?? null,
        direction: origin?.direction ?? null,
      });
      setPendingConnectionPreview(preview ?? null);
    },
    [closeContextMenu, closeReferencePicker, isReadOnly, nodes, screenToFlowPosition]
  );

  const createNodeAtMenu = useCallback(
    (kind: CreateNodeKind) => {
      const position = { x: createMenu.flowX, y: createMenu.flowY };
      const newNode =
        kind === "text"
          ? addTextNode(position)
          : kind === "image"
            ? addImageDraftNode(position)
            : kind === "sketch"
              ? addSketchNode(position)
              : addVideoNode(position);

      if (!newNode) return;

      if (createMenu.originNodeId && createMenu.direction) {
        const connection =
          createMenu.direction === "incoming"
            ? { source: newNode.id, target: createMenu.originNodeId }
            : { source: createMenu.originNodeId, target: newNode.id };
        const connectionNodes = [
          ...(getNodes() as AppNode[]).filter((node) => node.id !== newNode.id),
          newNode,
        ];

        if (isValidCanvasConnection(connection, connectionNodes)) {
          const edge: AppEdge = {
            ...connection,
            id: `e-${connection.source}-${connection.target}-${Date.now()}`,
            type: "signal",
          };
          let shouldSubmit = false;
          setEdges((eds) => {
            const exists = eds.some((edge) => edge.source === connection.source && edge.target === connection.target);
            if (exists) return eds;
            shouldSubmit = true;
            return addEdge(edge, eds);
          });
          if (shouldSubmit) canvasOperations.submitOperation("EDGE_CREATE", { edge });
        }
      }

      closeCreateMenu();
    },
    [addImageDraftNode, addSketchNode, addTextNode, addVideoNode, canvasOperations, closeCreateMenu, createMenu, getNodes, setEdges]
  );

  const handleConnectEnd = useCallback(
    (event: MouseEvent | TouchEvent, connectionState: FinalConnectionState) => {
      if (isReadOnly) return;
      window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
      if (connectionState.toHandle || !connectionState.from || !connectionState.fromNode?.id || !connectionState.fromHandle?.type) return;

      const point = getClientPoint(event);
      if (!point) return;

      const direction = connectionState.fromHandle.type === "target" ? "incoming" : "outgoing";
      const from = getNodeCardPreviewAnchor(connectionState.fromNode.id, direction) ?? flowToScreenPosition(connectionState.from);
      ignoreNextPaneClickRef.current = true;
      openCreateMenuAt(point, {
        nodeId: connectionState.fromNode.id,
        direction,
      }, {
        from,
        to: point,
        direction,
      });
    },
    [flowToScreenPosition, isReadOnly, openCreateMenuAt]
  );

  useEffect(() => {
    function handleNodeCreateMenu(event: Event) {
      const detail = (event as CustomEvent<NodeCreateMenuEventDetail>).detail;
      if (!detail?.nodeId) return;
      openCreateMenuAt(
        { x: detail.clientX, y: detail.clientY },
        { nodeId: detail.nodeId, direction: detail.direction }
      );
    }

    window.addEventListener("copse:node-create-menu", handleNodeCreateMenu);
    return () => window.removeEventListener("copse:node-create-menu", handleNodeCreateMenu);
  }, [openCreateMenuAt]);

  // --- Clear ---
  const [confirmClear, setConfirmClear] = useState(false);

  const handleClear = useCallback(() => {
    if (isReadOnly) return;
    if (!confirmClear) {
      setConfirmClear(true);
      return;
    }
    closeReferencePicker();
    closeCreateMenu();
    canvasOperations.submitOperation("CANVAS_CLEAR", {});
    setNodes(defaultNodes());
    setEdges([]);
    clearCanvas(activeProjectId);
    clearImages();
    clearVideos();
    setConfirmClear(false);
  }, [activeProjectId, canvasOperations, closeCreateMenu, closeReferencePicker, confirmClear, isReadOnly, setNodes, setEdges]);

  useEffect(() => {
    if (!confirmClear) return;
    const t = setTimeout(() => setConfirmClear(false), 3000);
    return () => clearTimeout(t);
  }, [confirmClear]);

  useEffect(() => {
    if (!createMenu.visible) return;

    function handlePointerDown(event: PointerEvent) {
      const target = event.target as HTMLElement;
      if (target.closest("[data-create-menu]")) return;
      closeCreateMenu();
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") closeCreateMenu();
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [closeCreateMenu, createMenu.visible]);

  if (!activeProjectId) {
    return (
      <div className="flex h-full w-full items-center justify-center text-sm text-muted-gray">
        正在准备项目...
      </div>
    );
  }

  const createMenuOrigin = createMenu.originNodeId ? nodes.find((node) => node.id === createMenu.originNodeId) : undefined;
  const createMenuKinds = getCreateKindsForOrigin(createMenuOrigin?.type, createMenu.direction);

  return (
    <div
      className="relative h-full w-full"
      onMouseMove={handleCanvasMouseMove}
      onContextMenu={handleContextMenu}
      onDoubleClick={(event) => {
        const target = event.target as HTMLElement;
        if (
          target.closest(".react-flow__node") ||
          target.closest(".react-flow__controls") ||
          target.closest(".canvas-view-toolbar") ||
          isEditableElement(target)
        ) {
          return;
        }
        openCreateMenuAt({ x: event.clientX, y: event.clientY });
      }}
    >
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={CANVAS_NODE_TYPES}
        edgeTypes={CANVAS_EDGE_TYPES}
        minZoom={0.2}
        maxZoom={2}
        zoomOnScroll
        zoomActivationKeyCode={["Meta", "Control"]}
        zoomOnPinch
        panOnScroll
        panOnScrollMode={PanOnScrollMode.Free}
        snapToGrid={snapToGrid}
        snapGrid={[36, 36]}
        panOnDrag={false}
        selectionOnDrag
        selectionMode={SelectionMode.Partial}
        zoomOnDoubleClick={false}
        deleteKeyCode={isReadOnly ? null : "Backspace"}
        fitView
        defaultEdgeOptions={{
          type: "signal",
        }}
        connectionLineComponent={CanvasConnectionLine}
        proOptions={{ hideAttribution: true }}
        onMove={(_, viewport) => {
          setCanvasZoom(viewport.zoom);
        }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onConnect={onConnect}
        onMoveStart={() => { closeContextMenu(); closeCreateMenu(); window.dispatchEvent(new CustomEvent("copse:canvas-interaction")); }}
        onPaneClick={() => {
          if (ignoreNextPaneClickRef.current) {
            ignoreNextPaneClickRef.current = false;
            return;
          }
          closeContextMenu();
          closeCreateMenu();
          closeReferencePicker();
          window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
        }}
        nodesDraggable={!isReadOnly}
        nodesConnectable={!isReadOnly}
        elementsSelectable
        onNodeDragStart={() => { if (!isReadOnly) window.dispatchEvent(new CustomEvent("copse:canvas-interaction")); }}
        onConnectStart={() => { if (isReadOnly) return; setPendingConnectionPreview(null); window.dispatchEvent(new CustomEvent("copse:canvas-interaction")); }}
        onConnectEnd={handleConnectEnd}
        isValidConnection={(connection) => !isReadOnly && isValidCanvasConnection(connection, nodes)}
      >
        <Background variant={BackgroundVariant.Dots} gap={36} size={1.8} color="var(--canvas-dot)" />
        {showMiniMap && (
          <MiniMap
            pannable
            zoomable
            nodeColor="var(--canvas-edge-selected)"
            maskColor="var(--canvas-minimap-mask)"
            className="!bottom-[104px] !left-5 !right-auto !rounded-2xl !border !border-border-warm !bg-background/92 !shadow-[0_14px_40px_rgba(0,0,0,0.16)]"
          />
        )}
      </ReactFlow>

      <CanvasViewToolbar
        showMiniMap={showMiniMap}
        snapToGrid={snapToGrid}
        zoom={canvasZoom}
        onToggleMiniMap={() => setShowMiniMap((value) => !value)}
        onToggleSnapToGrid={() => setSnapToGrid((value) => !value)}
        onResetView={handleToolbarResetView}
        onZoomChange={handleToolbarZoomChange}
      />

      {Object.entries(remotePresences).map(([presenceClientId, presence]) => {
        const cursor = presence.screenCursor;
        if (!cursor) return null;
        const color = getPresenceColor(presenceClientId);
        return (
          <div
            key={presenceClientId}
            className="pointer-events-none absolute z-50"
            style={{ left: cursor.x, top: cursor.y, color }}
          >
            <div
              className="h-0 w-0 border-y-[6px] border-l-[10px] border-y-transparent"
              style={{ borderLeftColor: color }}
            />
            <div
              className="mt-1 rounded-full px-2 py-0.5 text-[10px] font-medium text-white shadow-sm"
              style={{ backgroundColor: color }}
            >
              协作者
            </div>
          </div>
        );
      })}

      {Object.keys(remotePresences).length > 0 && (
        <div className="pointer-events-none absolute right-4 top-4 z-50 rounded-full border border-border-warm bg-background px-3 py-1.5 text-xs text-charcoal/70 shadow-sm">
          {Object.keys(remotePresences).length} 人在线协作 · {projectMembers.length || Object.keys(remotePresences).length + 1} 位成员
        </div>
      )}

      {Object.entries(remotePresences).map(([presenceClientId, presence]) => {
        if (!presence.editingNodeId) return null;
        const color = getPresenceColor(presenceClientId);
        return (
          <div
            key={`${presenceClientId}:${presence.editingNodeId}`}
            className="pointer-events-none absolute left-4 top-16 z-50 rounded-full px-3 py-1.5 text-xs font-medium text-white shadow-sm"
            style={{ backgroundColor: color }}
          >
            协作者正在编辑 {presence.editingNodeId}
          </div>
        );
      })}

      {isReadOnly && (
        <div className="pointer-events-none absolute left-1/2 top-4 z-50 -translate-x-1/2 rounded-full border border-border-warm bg-background px-3 py-1.5 text-xs text-charcoal/70 shadow-sm">
          只读模式{projectRole ? ` · ${projectRole}` : ""}
        </div>
      )}

      {serverProjectId && (
        <div className="pointer-events-auto absolute right-4 top-4 z-50">
        <ToolbarIconButton
          label="共享"
          icon={<Share2 className="size-3.5" />}
          onClick={() => setShareDialogOpen(true)}
        />
        </div>
      )}

      {serverProjectId && (
        <CanvasShareDialog
          open={shareDialogOpen}
          projectId={serverProjectId}
          projectRole={projectRole}
          members={projectMembers}
          onOpenChange={setShareDialogOpen}
          onMembersChange={setProjectMembers}
        />
      )}

      {createMenu.visible && pendingConnectionPreview && (
        <svg className="pointer-events-none fixed inset-0 z-40 overflow-visible" aria-hidden="true">
          <path
            d={getConnectionPreviewPath(pendingConnectionPreview)}
            fill="none"
            stroke="#eceae4"
            strokeLinecap="round"
            strokeWidth={2}
          />
        </svg>
      )}

      {/* Reference picker banner */}
      <AnimatePresence>
        {referencePickerPromptId && (
          <motion.div
            initial={{ opacity: 0, y: -8, x: "-50%", scale: 0.98 }}
            animate={{ opacity: 1, y: 0, x: "-50%", scale: 1 }}
            exit={{ opacity: 0, y: -8, x: "-50%", scale: 0.98 }}
            transition={{ duration: 0.16, ease: "easeOut" }}
            className="pointer-events-auto absolute left-1/2 top-4 z-50 flex items-center gap-3 rounded-xl bg-charcoal px-4 py-2 text-sm font-medium text-off-white shadow-lg"
          >
            <span>从画布选择参考图</span>
            <button
              type="button"
              onClick={closeReferencePicker}
              className="rounded-lg bg-off-white/15 px-2 py-1 text-xs hover:bg-off-white/25"
            >
              退出
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Drag overlay */}
      <AnimatePresence>
        {isDragOver && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.14 }}
            className="pointer-events-none absolute inset-0 z-40 flex items-center justify-center bg-background/60"
          >
            <motion.div
              initial={{ scale: 0.98, y: 4 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.98, y: 4 }}
              transition={{ duration: 0.16, ease: "easeOut" }}
              className="rounded-2xl border-2 border-dashed border-charcoal/30 px-8 py-6 text-charcoal/60"
            >
              <ImagePlus className="mx-auto mb-2 size-8" />
              <p className="text-sm">松开以添加图片或视频</p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {createMenu.visible && (
          <motion.div
            data-create-menu
            initial={{ opacity: 0, scale: 0.98, y: -2 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.98, y: -2 }}
            transition={{ duration: 0.14, ease: "easeOut" }}
            className="fixed z-50 min-w-[180px] origin-top-left rounded-xl border border-border-warm bg-background p-1 shadow-[0_8px_24px_rgba(28,28,28,0.12)]"
            style={{ left: createMenu.x, top: createMenu.y }}
          >
            {createMenuKinds.map((kind) => (
              <button
                key={kind}
                type="button"
                onClick={() => createNodeAtMenu(kind)}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-charcoal hover:bg-muted"
              >
                {kind === "text" && <Type className="size-4 text-muted-gray" />}
                {kind === "image" && <ImagePlus className="size-4 text-muted-gray" />}
                {kind === "sketch" && <PenLine className="size-4 text-muted-gray" />}
                {kind === "video" && <Video className="size-4 text-muted-gray" />}
                {kind === "text" ? "Text" : kind === "image" ? "Image" : kind === "sketch" ? "Sketch" : "Video"}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Floating toolbar */}
      {!referencePickerPromptId && !isReadOnly && (
        <div className="pointer-events-auto absolute left-1/2 top-4 flex -translate-x-1/2 items-center gap-2 rounded-xl border border-border-warm bg-background px-3 py-2 shadow-[0_1px_3px_rgba(0,0,0,0.06)]">
          <ToolbarIconButton
            label="新建 Image"
            icon={<Plus className="size-3.5" />}
            onClick={() => addImageDraftNode()}
            variant="primary"
          />
          <ToolbarIconButton
            label="新建 Sketch"
            icon={<PenLine className="size-3.5" />}
            onClick={() => addSketchNode()}
          />
          <ToolbarIconButton
            label="上传素材"
            icon={<ImagePlus className="size-3.5" />}
            onClick={() => fileInputRef.current?.click()}
          />
          <ToolbarIconButton
            label={confirmClear ? "确认清空" : "清空画布"}
            icon={<Trash2 className="size-3.5" />}
            onClick={handleClear}
            variant={confirmClear ? "danger" : "default"}
          />
        </div>
      )}

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp,image/gif,video/mp4,video/quicktime,.mov"
        multiple
        className="hidden"
        onChange={handleFileInputChange}
      />

      {/* Context menu */}
      <CanvasContextMenu
        state={contextMenu}
        onClose={closeContextMenu}
        onPaste={handleMenuPaste}
        onZoomIn={handleMenuZoomIn}
        onZoomOut={handleMenuZoomOut}
        onFitView={handleMenuFitView}
        onZoom100={handleMenuZoom100}
      />

      {/* Paste toast */}
      {pasteToast && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-lg bg-charcoal px-4 py-2 text-xs text-off-white shadow-lg">
          {pasteToast}
        </div>
      )}

      {/* Save error toast */}
      {saveError && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-lg bg-charcoal px-4 py-2 text-xs text-off-white shadow-lg">
          {saveError}
        </div>
      )}
    </div>
  );
}

export default function CreateImagePage() {
  return (
    <div className="h-full w-full bg-cream">
      <Suspense fallback={<div className="flex h-full items-center justify-center text-sm text-muted-gray">正在加载画布...</div>}>
        <ReactFlowProvider>
          <CanvasFlow />
        </ReactFlowProvider>
      </Suspense>
    </div>
  );
}
