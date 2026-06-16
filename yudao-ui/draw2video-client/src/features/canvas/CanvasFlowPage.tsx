"use client";

import "@xyflow/react/dist/style.css";
import "tldraw/tldraw.css";

import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
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
  useStore,
  useStoreApi,
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
import type { AppNode, AppEdge, CanvasMember, CanvasPresence, CanvasProjectRole, CanvasState, EdgeDeleteEventDetail, GroupArrangeEventDetail, GroupNodeData, GroupUngroupEventDetail, ImageNodeData, NodeCreateMenuEventDetail, NodeDataPatchEventDetail, NodeEditingPresenceEventDetail, NodePositionPatchEventDetail, ReferencePickerEventDetail, SketchNodeData, TextNodeData, VideoFrameCaptureEventDetail, VideoGenerationMode, VideoNodeData } from "@/features/canvas/types";
import { DEFAULT_PROMPT_DATA } from "@/features/canvas/types";
import { canvasApi, snapshotRecordToCanvasState } from "@/features/canvas/canvas-api";
import { CanvasShareDialog } from "@/features/canvas/CanvasShareDialog";
import { PromptNodeComponent } from "@/features/canvas/PromptNode";
import { ResultNodeComponent } from "@/features/canvas/ResultNode";
import { ImageNodeComponent } from "@/features/canvas/ImageNode";
import { SketchNodeComponent } from "@/features/canvas/SketchNode";
import { TextNodeComponent } from "@/features/canvas/TextNode";
import { VideoNodeComponent } from "@/features/canvas/VideoNode";
import { GroupNodeComponent } from "@/features/canvas/GroupNode";
import { CanvasSignalEdge } from "@/features/canvas/CanvasSignalEdge";
import { collectNodeAssetIds, getNodeAssetAccessRequest, withFreshAssetUrl } from "@/features/canvas/canvas-asset-runtime";
import { filterSyncableNodeDataPatch, isCanvasNodeSyncable, sanitizeCanvasStateForPersistence, sanitizeNodeForCanvasOperation, stripRuntimeAssetUrlsFromPatch } from "@/features/canvas/canvas-syncable-data";
import { useCanvasServerStorage } from "@/features/canvas/use-canvas-server-storage";
import { useCanvasRealtime } from "@/features/canvas/use-canvas-realtime";
import { useCanvasOperations } from "@/features/canvas/use-canvas-operations";
import { loadCanvas, saveCanvas } from "@/features/canvas/use-canvas-storage";
import { loadImage, loadVideo, saveImage, saveVideo } from "@/features/canvas/image-store";
import { fileToImageNodeData, fileToVideoNodeData, getFilesFromDrop, isAcceptedImageType, isAcceptedVideoFile } from "@/features/canvas/image-upload";
import { attachImageAsset, attachVideoAsset } from "@/features/canvas/canvas-asset-upload";
import { getAssetAccessUrls, getMyAsset } from "@/features/assets/asset-api";
import { getAssetOriginalExpireTime, getAssetOriginalUrl } from "@/features/assets/asset-dictionaries";
import { useAuth } from "@/features/auth/auth-store";
import {
  isEditableElement,
  getImageFilesFromPasteEvent,
  getImageFilesFromClipboardAPI,
} from "@/features/canvas/clipboard";
import { CanvasContextMenu, type ContextMenuState } from "@/features/canvas/CanvasContextMenu";
import { CanvasProjectHeader, CanvasToolDock, CanvasUtilityBar, CanvasViewToolbar, MultiSelectionToolbar, type CanvasSyncState, type CreateNodeKind, type SelectionRectSnapshot } from "@/features/canvas/CanvasChrome";
import { useCanvasMultiSelection, type MultiSelectionAction } from "@/features/canvas/use-canvas-multi-selection";
import { getPromptTemplate, markPromptTemplateUsed } from "@/features/templates/template-api";
import { CanvasTemplateLibraryDialog } from "@/features/templates/CanvasTemplateLibraryDialog";
import type { PromptTemplate } from "@/features/templates/template-types";
import { findOpenNodePosition } from "@/features/canvas/positioning";
import { cn } from "@/lib/utils";
import { ImagePlus, MousePointerClick, PenLine, Sparkles, Type, Video } from "lucide-react";

// Static outside component to avoid React Flow "new nodeTypes object" warning
const CANVAS_NODE_TYPES = {
  prompt: PromptNodeComponent,
  result: ResultNodeComponent,
  image: ImageNodeComponent,
  sketch: SketchNodeComponent,
  text: TextNodeComponent,
  video: VideoNodeComponent,
  canvasGroup: GroupNodeComponent,
} satisfies NodeTypes;

const CANVAS_EDGE_TYPES = {
  signal: CanvasSignalEdge,
} satisfies EdgeTypes;

type AssetUrlEntry = { assetId: number; url: string; expireTime: string | null };
type CanvasViewport = { x: number; y: number; zoom: number };

function assetUrlCacheKey(projectId: string | null, assetId: number) {
  return `${projectId ?? "global"}:${assetId}`;
}

function isUsableAssetUrlEntry(entry: AssetUrlEntry | undefined): entry is AssetUrlEntry {
  if (!entry?.url) return false;
  if (!entry.expireTime) return true;
  const expireAt = new Date(entry.expireTime).getTime();
  return Number.isFinite(expireAt) && expireAt - Date.now() > 120_000;
}

const CANVAS_NODE_DRAG_HANDLE = ".canvas-node-drag-handle";
const TRANSPARENT_NODE_WRAPPER_STYLE = { pointerEvents: "none" as const };
const GROUP_LAYOUT_PADDING = 32;
const GROUP_LAYOUT_GAP = 96;
const CANVAS_ARRANGE_COLUMN_GAP = 420;
const CANVAS_ARRANGE_ROW_GAP = 240;
const CANVAS_ARRANGE_COMPONENT_GAP = 360;
const CANVAS_ASSET_PREFETCH_SCREEN_PADDING = 720;

type LinkedCreateDirection = "incoming" | "outgoing";
type PendingConnectionPreview = {
  from: { x: number; y: number };
  to: { x: number; y: number };
  direction: LinkedCreateDirection;
};

type PointerSnapshot = {
  x: number;
  y: number;
};

const CREATE_NODE_KINDS: CreateNodeKind[] = ["text", "image", "sketch", "video", "prompt"];
const DEFAULT_CANVAS_VIEWPORT = { x: 110, y: 90, zoom: 0.78 };
const NODE_DATA_PATCH_DEBOUNCE_MS = 200;
const CANVAS_SAVE_DEBOUNCE_MS = 1500;
const CANVAS_DRAFT_MAX_AGE_MS = 5 * 60_000;
const SNAPSHOT_ONLY_NODE_DATA_KEYS = new Set(["prompt", "content"]);

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
    (sourceType === "image" && targetType === "sketch") ||
    (sourceType === "sketch" && targetType === "image") ||
    (sourceType === "sketch" && targetType === "video") ||
    (sourceType === "text" && targetType === "image") ||
    (sourceType === "text" && targetType === "sketch") ||
    (sourceType === "text" && targetType === "text") ||
    (sourceType === "prompt" && targetType === "image") ||
    (sourceType === "prompt" && targetType === "video") ||
    (sourceType === "prompt" && targetType === "text") ||
    (sourceType === "text" && targetType === "prompt") ||
    (sourceType === "image" && targetType === "prompt")
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

function rectsIntersect(a: SelectionRectSnapshot, b: SelectionRectSnapshot) {
  return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
}

function unionSelectionRects(rects: SelectionRectSnapshot[]): SelectionRectSnapshot | null {
  if (rects.length === 0) return null;
  const left = Math.min(...rects.map((rect) => rect.x));
  const top = Math.min(...rects.map((rect) => rect.y));
  const right = Math.max(...rects.map((rect) => rect.x + rect.width));
  const bottom = Math.max(...rects.map((rect) => rect.y + rect.height));
  return { x: left, y: top, width: right - left, height: bottom - top };
}

function getNodeScreenRect(node: AppNode | undefined, transform: readonly [number, number, number]): SelectionRectSnapshot | null {
  if (!node) return null;
  const data = node.data as Record<string, unknown> | undefined;
  const width = node.measured?.width ?? (typeof data?.width === "number" ? (data.width as number) : null);
  const height = node.measured?.height ?? (typeof data?.height === "number" ? (data.height as number) : null);
  if (width == null || height == null) return null;
  const [x, y, zoom] = transform;
  return {
    x: node.position.x * zoom + x,
    y: node.position.y * zoom + y,
    width: width * zoom,
    height: height * zoom,
  };
}

function getPreviewCardViewportRect(nodeId: string): SelectionRectSnapshot | null {
  const escapedId = CSS.escape(nodeId);
  const card =
    document.querySelector<HTMLElement>(`[data-node-preview-card][data-node-id="${escapedId}"]`) ??
    document.querySelector<HTMLElement>(`.react-flow__node[data-id="${escapedId}"] [data-node-preview-card]`);
  if (!card) return null;
  const rect = card.getBoundingClientRect();
  return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };
}

function getPreviewCardFlowRect(node: AppNode, screenToFlowPosition: (position: { x: number; y: number }) => { x: number; y: number }) {
  const rect = getPreviewCardViewportRect(node.id);
  if (!rect) {
    const data = node.data as Record<string, unknown>;
    const fallbackWidth = node.measured?.width ?? (typeof data.width === "number" ? data.width : 320);
    const fallbackHeight = node.measured?.height ?? (typeof data.height === "number" ? data.height : 240);
    return {
      width: Math.max(1, fallbackWidth),
      height: Math.max(1, fallbackHeight),
    };
  }
  const topLeft = screenToFlowPosition({ x: rect.x, y: rect.y });
  const bottomRight = screenToFlowPosition({ x: rect.x + rect.width, y: rect.y + rect.height });
  return {
    width: Math.max(1, bottomRight.x - topLeft.x),
    height: Math.max(1, bottomRight.y - topLeft.y),
  };
}

function getApproxNodeSize(node: AppNode) {
  const data = node.data as Record<string, unknown>;
  const width = node.measured?.width ?? (typeof data.width === "number" ? data.width : undefined);
  const height = node.measured?.height ?? (typeof data.height === "number" ? data.height : undefined);
  if (width && height) return { width, height };

  if (node.type === "image") return { width: 420, height: 420 };
  if (node.type === "video") return { width: 420, height: 448 };
  if (node.type === "sketch") return { width: 300, height: 240 };
  if (node.type === "text") return { width: 320, height: 260 };
  if (node.type === "canvasGroup") return { width: Number(data.width) || 420, height: Number(data.height) || 320 };
  return { width: 320, height: 280 };
}

function getNodeAbsolutePosition(node: AppNode) {
  const candidate = (
    node as AppNode & {
      positionAbsolute?: { x: number; y: number };
      internals?: { positionAbsolute?: { x: number; y: number } };
    }
  ).positionAbsolute ?? (
    node as AppNode & {
      internals?: { positionAbsolute?: { x: number; y: number } };
    }
  ).internals?.positionAbsolute;
  if (typeof candidate?.x === "number" && typeof candidate.y === "number") {
    return candidate;
  }
  return node.position;
}

function getNodeFlowRect(node: AppNode): SelectionRectSnapshot {
  const size = getApproxNodeSize(node);
  const position = getNodeAbsolutePosition(node);
  return { x: position.x, y: position.y, width: size.width, height: size.height };
}

function getExpandedCanvasViewportFlowRect(
  screenToFlowPosition: (position: { x: number; y: number }) => { x: number; y: number }
): SelectionRectSnapshot | null {
  const flowElement = document.querySelector<HTMLElement>(".react-flow");
  if (!flowElement) return null;
  const rect = flowElement.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return null;
  const topLeft = screenToFlowPosition({
    x: rect.left - CANVAS_ASSET_PREFETCH_SCREEN_PADDING,
    y: rect.top - CANVAS_ASSET_PREFETCH_SCREEN_PADDING,
  });
  const bottomRight = screenToFlowPosition({
    x: rect.right + CANVAS_ASSET_PREFETCH_SCREEN_PADDING,
    y: rect.bottom + CANVAS_ASSET_PREFETCH_SCREEN_PADDING,
  });
  const left = Math.min(topLeft.x, bottomRight.x);
  const top = Math.min(topLeft.y, bottomRight.y);
  const right = Math.max(topLeft.x, bottomRight.x);
  const bottom = Math.max(topLeft.y, bottomRight.y);
  return { x: left, y: top, width: right - left, height: bottom - top };
}

function filterNodesInExpandedCanvasViewport(
  nodes: AppNode[],
  screenToFlowPosition: (position: { x: number; y: number }) => { x: number; y: number }
) {
  const viewportRect = getExpandedCanvasViewportFlowRect(screenToFlowPosition);
  if (!viewportRect) return [];
  return nodes.filter((node) => rectsIntersect(viewportRect, getNodeFlowRect(node)));
}

async function dataUrlToFile(dataUrl: string, fileName: string, fallbackMimeType: string) {
  const response = await fetch(dataUrl);
  const blob = await response.blob();
  return new File([blob], fileName, { type: blob.type || fallbackMimeType });
}

type ArrangeUnit = {
  id: string;
  node: AppNode;
  size: { width: number; height: number };
};

function getConnectedArrangeComponents(units: ArrangeUnit[], edges: AppEdge[], childToGroup: Map<string, string>) {
  const unitIds = new Set(units.map((unit) => unit.id));
  const adjacency = new Map(units.map((unit) => [unit.id, new Set<string>()]));

  for (const edge of edges) {
    const sourceId = childToGroup.get(edge.source) ?? edge.source;
    const targetId = childToGroup.get(edge.target) ?? edge.target;
    if (sourceId === targetId || !unitIds.has(sourceId) || !unitIds.has(targetId)) continue;
    adjacency.get(sourceId)?.add(targetId);
    adjacency.get(targetId)?.add(sourceId);
  }

  const visited = new Set<string>();
  const components: ArrangeUnit[][] = [];
  const byId = new Map(units.map((unit) => [unit.id, unit]));

  for (const unit of units) {
    if (visited.has(unit.id)) continue;
    const queue = [unit.id];
    visited.add(unit.id);
    const component: ArrangeUnit[] = [];
    while (queue.length > 0) {
      const id = queue.shift()!;
      const item = byId.get(id);
      if (item) component.push(item);
      for (const nextId of adjacency.get(id) ?? []) {
        if (visited.has(nextId)) continue;
        visited.add(nextId);
        queue.push(nextId);
      }
    }
    components.push(component);
  }

  return components.sort((a, b) => {
    const aTop = Math.min(...a.map((unit) => unit.node.position.y));
    const bTop = Math.min(...b.map((unit) => unit.node.position.y));
    const aLeft = Math.min(...a.map((unit) => unit.node.position.x));
    const bLeft = Math.min(...b.map((unit) => unit.node.position.x));
    return aTop - bTop || aLeft - bLeft;
  });
}

function layoutArrangeComponent(component: ArrangeUnit[], edges: AppEdge[], childToGroup: Map<string, string>, origin: { x: number; y: number }) {
  const componentIds = new Set(component.map((unit) => unit.id));
  const incomingCount = new Map(component.map((unit) => [unit.id, 0]));
  const outgoing = new Map(component.map((unit) => [unit.id, [] as string[]]));
  const byId = new Map(component.map((unit) => [unit.id, unit]));

  for (const edge of edges) {
    const sourceId = childToGroup.get(edge.source) ?? edge.source;
    const targetId = childToGroup.get(edge.target) ?? edge.target;
    if (sourceId === targetId || !componentIds.has(sourceId) || !componentIds.has(targetId)) continue;
    outgoing.get(sourceId)?.push(targetId);
    incomingCount.set(targetId, (incomingCount.get(targetId) ?? 0) + 1);
  }

  const remainingIncoming = new Map(incomingCount);
  const queue = component
    .filter((unit) => (remainingIncoming.get(unit.id) ?? 0) === 0)
    .sort((a, b) => a.node.position.y - b.node.position.y || a.node.position.x - b.node.position.x);
  const depth = new Map<string, number>();

  for (const unit of queue) depth.set(unit.id, 0);
  while (queue.length > 0) {
    const unit = queue.shift();
    if (!unit) continue;
    const unitDepth = depth.get(unit.id) ?? 0;
    for (const targetId of outgoing.get(unit.id) ?? []) {
      depth.set(targetId, Math.max(depth.get(targetId) ?? 0, unitDepth + 1));
      remainingIncoming.set(targetId, (remainingIncoming.get(targetId) ?? 1) - 1);
      if ((remainingIncoming.get(targetId) ?? 0) <= 0) {
        const target = byId.get(targetId);
        if (target) queue.push(target);
      }
    }
  }

  const unresolved = component.filter((unit) => !depth.has(unit.id));
  if (unresolved.length > 0) {
    const fallbackDepth = Math.max(0, ...Array.from(depth.values())) + 1;
    unresolved
      .sort((a, b) => a.node.position.y - b.node.position.y || a.node.position.x - b.node.position.x)
      .forEach((unit, index) => depth.set(unit.id, fallbackDepth + Math.floor(index / 4)));
  }

  const columns = new Map<number, ArrangeUnit[]>();
  for (const unit of component) {
    const column = depth.get(unit.id) ?? 0;
    columns.set(column, [...(columns.get(column) ?? []), unit]);
  }

  const positions = new Map<string, { x: number; y: number }>();
  let cursorX = origin.x;
  let componentBottom = origin.y;
  for (const column of Array.from(columns.keys()).sort((a, b) => a - b)) {
    const columnUnits = (columns.get(column) ?? []).sort((a, b) => {
      const aEdgeCount = (incomingCount.get(a.id) ?? 0) + (outgoing.get(a.id)?.length ?? 0);
      const bEdgeCount = (incomingCount.get(b.id) ?? 0) + (outgoing.get(b.id)?.length ?? 0);
      return bEdgeCount - aEdgeCount || a.node.position.y - b.node.position.y || a.node.position.x - b.node.position.x;
    });
    let cursorY = origin.y;
    let maxWidth = 0;
    for (const unit of columnUnits) {
      positions.set(unit.id, { x: Math.round(cursorX), y: Math.round(cursorY) });
      cursorY += unit.size.height + CANVAS_ARRANGE_ROW_GAP;
      componentBottom = Math.max(componentBottom, cursorY - CANVAS_ARRANGE_ROW_GAP);
      maxWidth = Math.max(maxWidth, unit.size.width);
    }
    cursorX += maxWidth + CANVAS_ARRANGE_COLUMN_GAP;
  }

  return {
    positions,
    width: Math.max(1, cursorX - origin.x - CANVAS_ARRANGE_COLUMN_GAP),
    height: Math.max(1, componentBottom - origin.y),
  };
}

function arrangeCanvasNodes(nodes: AppNode[], edges: AppEdge[]) {
  const childToGroup = new Map<string, string>();
  for (const node of nodes) {
    if (node.type !== "canvasGroup") continue;
    const childNodeIds = (node.data as GroupNodeData).childNodeIds ?? [];
    for (const childId of childNodeIds) childToGroup.set(childId, node.id);
  }

  const layoutNodes = nodes.filter((node) => node.type === "canvasGroup" || !childToGroup.has(node.id));
  if (layoutNodes.length <= 1) return null;

  const minX = Math.min(...layoutNodes.map((node) => node.position.x));
  const minY = Math.min(...layoutNodes.map((node) => node.position.y));
  const units = layoutNodes.map((node) => ({ id: node.id, node, size: getApproxNodeSize(node) }));
  const components = getConnectedArrangeComponents(units, edges, childToGroup);
  const nextPositions = new Map<string, { x: number; y: number }>();
  let cursorY = minY;
  for (const component of components) {
    const result = layoutArrangeComponent(component, edges, childToGroup, { x: minX, y: cursorY });
    for (const [id, position] of result.positions) nextPositions.set(id, position);
    cursorY += result.height + CANVAS_ARRANGE_COMPONENT_GAP;
  }

  const groupDeltas = new Map<string, { x: number; y: number }>();
  for (const node of layoutNodes) {
    if (node.type !== "canvasGroup") continue;
    const nextPosition = nextPositions.get(node.id);
    if (!nextPosition) continue;
    groupDeltas.set(node.id, {
      x: nextPosition.x - node.position.x,
      y: nextPosition.y - node.position.y,
    });
  }

  return nodes.map((node) => {
    const position = nextPositions.get(node.id);
    if (position) return { ...node, position, selected: false };

    const groupId = childToGroup.get(node.id);
    const delta = groupId ? groupDeltas.get(groupId) : null;
    if (!delta || (delta.x === 0 && delta.y === 0)) return { ...node, selected: false };
    return {
      ...node,
      position: {
        x: node.position.x + delta.x,
        y: node.position.y + delta.y,
      },
      selected: false,
    };
  });
}

function getPreviewCardRects(nodes: AppNode[]) {
  const result: { node: AppNode; rect: SelectionRectSnapshot }[] = [];
  for (const node of nodes) {
    if (node.type === "canvasGroup") continue;
    const rect = getPreviewCardViewportRect(node.id);
    if (rect) result.push({ node, rect });
  }
  return result;
}

function getSelectedPreviewCardBounds(nodes: AppNode[]): SelectionRectSnapshot | null {
  return unionSelectionRects(
    getPreviewCardRects(nodes)
      .filter(({ node }) => node.selected)
      .map(({ rect }) => rect)
  );
}

function getPreviewCardBoundsForNodeIds(nodes: AppNode[], nodeIds: Set<string>): SelectionRectSnapshot | null {
  return unionSelectionRects(
    nodes
      .filter((node) => nodeIds.has(node.id) && node.type !== "canvasGroup")
      .map((node) => getPreviewCardViewportRect(node.id))
      .filter((rect): rect is SelectionRectSnapshot => Boolean(rect))
  );
}

function getNodesSelectionViewportRect(): SelectionRectSnapshot | null {
  const rectElement = document.querySelector<HTMLElement>(".react-flow__nodesselection-rect");
  if (!rectElement) return null;
  const rect = rectElement.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return null;
  return { x: rect.left, y: rect.top, width: rect.width, height: rect.height };
}

function applyPreviewCardSelection(nodes: AppNode[], selectionRect: SelectionRectSnapshot): AppNode[] {
  return nodes.map((node) => {
    const cardRect = getPreviewCardViewportRect(node.id);
    if (!cardRect) return node;
    return {
      ...node,
      selected: rectsIntersect(selectionRect, cardRect),
    };
  });
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

function usesCardNodeInteraction(node: AppNode) {
  return node.type === "image" || node.type === "video" || node.type === "sketch" || node.type === "text" || node.type === "canvasGroup";
}

function canGroupSelectedNodes(selectedNodes: AppNode[], allNodes: AppNode[]) {
  const selectedIds = new Set(selectedNodes.map((node) => node.id));
  if (selectedIds.size < 2) return false;
  if (selectedNodes.some((node) => node.type === "canvasGroup")) return false;
  return !allNodes.some((node) => {
    if (node.type !== "canvasGroup") return false;
    const childIds = (node.data as GroupNodeData).childNodeIds;
    return childIds.some((childId) => selectedIds.has(childId));
  });
}

function getMergeTargetForSelectedNodes(selectedNodes: AppNode[], allNodes: AppNode[]) {
  const selectedGroups = selectedNodes.filter((node) => node.type === "canvasGroup");
  if (selectedGroups.length !== 1) return null;
  const targetGroup = selectedGroups[0];
  const targetChildIds = new Set((targetGroup.data as GroupNodeData).childNodeIds);
  const selectedItems = selectedNodes.filter((node) => node.type !== "canvasGroup");
  const mergeItemIds = new Set(selectedItems.filter((node) => !targetChildIds.has(node.id)).map((node) => node.id));
  if (mergeItemIds.size === 0) return null;

  const belongsToOtherGroup = allNodes.some((node) => {
    if (node.type !== "canvasGroup" || node.id === targetGroup.id) return false;
    return (node.data as GroupNodeData).childNodeIds.some((childId) => mergeItemIds.has(childId));
  });
  return belongsToOtherGroup ? null : targetGroup;
}

function getSingleSelectedGroup(selectedNodes: AppNode[]) {
  const selectedGroups = selectedNodes.filter((node) => node.type === "canvasGroup");
  if (selectedGroups.length !== 1) return null;
  const groupNode = selectedGroups[0];
  const childIds = new Set((groupNode.data as GroupNodeData).childNodeIds);
  const onlyGroupAndItsChildren = selectedNodes.every((node) => node.id === groupNode.id || childIds.has(node.id));
  return onlyGroupAndItsChildren ? groupNode : null;
}

function withCardNodeInteraction(node: AppNode): AppNode {
  if (!usesCardNodeInteraction(node)) return node;
  return {
    ...node,
    dragHandle: CANVAS_NODE_DRAG_HANDLE,
    style: { ...node.style, ...TRANSPARENT_NODE_WRAPPER_STYLE },
  };
}

function getImageOutputIdentity(output: Record<string, unknown>) {
  if (typeof output.id === "string" && output.id) return output.id;
  if (typeof output.assetId === "number") return `asset-${output.assetId}`;
  if (typeof output.previewUrl === "string" && output.previewUrl) return output.previewUrl;
  return null;
}

function getVideoOutputIdentity(output: Record<string, unknown>) {
  if (typeof output.id === "string" && output.id) return output.id;
  if (typeof output.assetId === "number") return `asset-${output.assetId}`;
  if (typeof output.videoUrl === "string" && output.videoUrl) return output.videoUrl;
  if (typeof output.previewUrl === "string" && output.previewUrl) return output.previewUrl;
  return null;
}

function mergeImageOutputPatch(node: AppNode, patch: Record<string, unknown>) {
  if (node.type !== "image" || !Array.isArray(patch.outputs)) return patch;

  const existingOutputs = Array.isArray((node.data as ImageNodeData).outputs)
    ? (node.data as ImageNodeData).outputs ?? []
    : [];
  const existingByKey = new Map(
    existingOutputs
      .map((output) => [getImageOutputIdentity(output as unknown as Record<string, unknown>), output] as const)
      .filter((entry): entry is [string, typeof existingOutputs[number]] => Boolean(entry[0]))
  );
  const seen = new Set<string>();
  const mergedOutputs = [...patch.outputs, ...existingOutputs].flatMap((rawOutput) => {
    if (!rawOutput || typeof rawOutput !== "object") return [];
    const output = rawOutput as Record<string, unknown>;
    const key = getImageOutputIdentity(output);
    if (!key || seen.has(key)) return [];
    seen.add(key);
    const existingOutput = existingByKey.get(key);
    return [{ ...existingOutput, ...output, previewUrl: typeof output.previewUrl === "string" && output.previewUrl ? output.previewUrl : existingOutput?.previewUrl }];
  });

  return { ...patch, outputs: mergedOutputs };
}

function mergeVideoOutputPatch(node: AppNode, patch: Record<string, unknown>) {
  if (node.type !== "video" || !Array.isArray(patch.outputs)) return patch;

  const existingOutputs = Array.isArray(node.data.outputs)
    ? node.data.outputs ?? []
    : [];
  const existingByKey = new Map(
    existingOutputs
      .map((output) => [getVideoOutputIdentity(output as unknown as Record<string, unknown>), output] as const)
      .filter((entry): entry is [string, typeof existingOutputs[number]] => Boolean(entry[0]))
  );
  const seen = new Set<string>();
  const mergedOutputs = [...patch.outputs, ...existingOutputs].flatMap((rawOutput) => {
    if (!rawOutput || typeof rawOutput !== "object") return [];
    const output = rawOutput as Record<string, unknown>;
    const key = getVideoOutputIdentity(output);
    if (!key || seen.has(key)) return [];
    seen.add(key);
    const existingOutput = existingByKey.get(key);
    const videoUrl =
      typeof output.videoUrl === "string" && output.videoUrl
        ? output.videoUrl
        : existingOutput?.videoUrl;
    const previewUrl =
      typeof output.previewUrl === "string" && output.previewUrl
        ? output.previewUrl
        : existingOutput?.previewUrl ?? videoUrl;
    return [{ ...existingOutput, ...output, videoUrl, previewUrl }];
  });

  return { ...patch, outputs: mergedOutputs };
}

function defaultNodes(): AppNode[] {
  return [];
}

function migrateNode(n: AppNode): AppNode {
  const rawType = (n as { type: string }).type;
  const nodeType = rawType === "group" ? "canvasGroup" : n.type;
  if (n.type === "prompt") {
    const d = n.data as Record<string, unknown>;
    return withCardNodeInteraction({
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
    } as AppNode);
  }
  if (n.type === "result") {
    const d = n.data as Record<string, unknown>;
    const imageUrls = Array.isArray(d.imageUrls) ? d.imageUrls : d.imageUrl ? [d.imageUrl] : [];
    return withCardNodeInteraction({
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
    } as AppNode);
  }
  if (n.type === "image") {
    const d = n.data as Record<string, unknown>;
    const assetId = typeof d.assetId === "number" ? d.assetId : typeof d.outputAssetId === "number" ? d.outputAssetId : null;
    return withCardNodeInteraction({
      ...n,
      data: {
        ...n.data,
        imageId: typeof d.imageId === "string" ? d.imageId : n.id,
        assetId,
        previewUrl: assetId ? null : typeof d.previewUrl === "string" ? d.previewUrl : null,
        outputPreviewUrl: assetId ? null : typeof d.outputPreviewUrl === "string" ? d.outputPreviewUrl : null,
        assetUrlExpireTime: null,
      },
    } as AppNode);
  }
  if (n.type === "text") {
    const d = n.data as Record<string, unknown>;
    return withCardNodeInteraction({
      ...n,
      data: {
        content: typeof d.content === "string" ? d.content : "",
        fileName: typeof d.fileName === "string" ? d.fileName : "Text",
        prompt: typeof d.prompt === "string" ? d.prompt : "",
        modelId: typeof d.modelId === "string" ? d.modelId : "Gemini 3.1 Flash Lite",
        status: d.status === "pending" || d.status === "failed" ? d.status : "idle",
        errorMessage: typeof d.errorMessage === "string" ? d.errorMessage : null,
        width: typeof d.width === "number" ? d.width : 320,
        height: typeof d.height === "number" ? d.height : 260,
        createdAt: typeof d.createdAt === "string" ? d.createdAt : new Date().toISOString(),
        updatedAt: typeof d.updatedAt === "string" ? d.updatedAt : undefined,
      },
    } as AppNode);
  }
  if (n.type === "sketch") {
    const d = n.data as Record<string, unknown>;
    return withCardNodeInteraction({
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
    } as AppNode);
  }
  if (n.type === "video") {
    const d = n.data as Record<string, unknown>;
    const status = d.status === "pending" || d.status === "complete" || d.status === "failed" ? d.status : "idle";
    const ratio = d.ratio === "4:3" || d.ratio === "1:1" || d.ratio === "3:4" || d.ratio === "9:16" || d.ratio === "21:9" ? d.ratio : "16:9";
    const resolution = d.resolution === "480p" || d.resolution === "720p" ? d.resolution : "1080p";
    const duration = d.duration === 10 ? 10 : 5;
    const assetId = typeof d.assetId === "number" ? d.assetId : typeof d.outputAssetId === "number" ? d.outputAssetId : null;
    return withCardNodeInteraction({
      ...n,
      data: {
        ...n.data,
        videoId: typeof d.videoId === "string" ? d.videoId : undefined,
        fileName: typeof d.fileName === "string" ? d.fileName : "Video",
        mimeType: typeof d.mimeType === "string" ? d.mimeType : "video/mp4",
        assetId,
        assetVersionId: typeof d.assetVersionId === "number" ? d.assetVersionId : null,
        previewUrl: assetId ? null : typeof d.previewUrl === "string" ? d.previewUrl : null,
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
        videoUrl: assetId ? null : typeof d.videoUrl === "string" ? d.videoUrl : typeof d.previewUrl === "string" ? d.previewUrl : null,
        errorMessage: typeof d.errorMessage === "string" ? d.errorMessage : null,
        taskStatus: typeof d.taskStatus === "string" ? d.taskStatus : null,
        progress: typeof d.progress === "number" ? d.progress : null,
        outputAssetId: typeof d.outputAssetId === "number" ? d.outputAssetId : null,
        outputPreviewUrl: assetId ? null : typeof d.outputPreviewUrl === "string" ? d.outputPreviewUrl : null,
        assetUrlExpireTime: null,
        sourceTaskId: typeof d.sourceTaskId === "number" ? d.sourceTaskId : null,
        safetyStatus: typeof d.safetyStatus === "string" ? d.safetyStatus : null,
        safetyReason: typeof d.safetyReason === "string" ? d.safetyReason : null,
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
    } as AppNode);
  }
  if (nodeType === "canvasGroup") {
    const d = n.data as Record<string, unknown>;
    return withCardNodeInteraction({
      ...n,
      type: "canvasGroup",
      data: {
        fileName: typeof d.fileName === "string" ? d.fileName : "Group",
        childNodeIds: Array.isArray(d.childNodeIds) ? d.childNodeIds.filter((item): item is string => typeof item === "string") : [],
        width: typeof d.width === "number" ? d.width : 320,
        height: typeof d.height === "number" ? d.height : 240,
        createdAt: typeof d.createdAt === "string" ? d.createdAt : new Date().toISOString(),
        updatedAt: typeof d.updatedAt === "string" ? d.updatedAt : undefined,
      },
    } as AppNode);
  }
  return withCardNodeInteraction(n);
}

function migrateEdge(e: AppEdge): AppEdge {
  if (!e.type || e.type === "default" || e.type === "smoothstep" || e.type === "bezier") {
    return { ...e, type: "signal" };
  }
  return e;
}

const VIDEO_MODE_MAX_REFS: Record<VideoGenerationMode, number> = {
  TEXT_TO_VIDEO: 0,
  IMAGE_TO_VIDEO: 1,
  FIRST_LAST_FRAME_VIDEO: 2,
  MULTI_REF_VIDEO: 9,
};

function getVideoNodeAllowedRefCount(targetNode: AppNode | undefined, edges: AppEdge[]): number {
  if (targetNode?.type !== "video") return 9;
  const data = targetNode.data as VideoNodeData;
  const mode = data.explicitMode;
  const currentCount = edges.filter((e) => e.target === targetNode.id).length;
  if (mode && mode in VIDEO_MODE_MAX_REFS) return Math.max(0, VIDEO_MODE_MAX_REFS[mode] - currentCount);
  if (currentCount >= 9) return 0;
  return 9 - currentCount;
}

function isValidCanvasConnection(connection: { source: string; target: string }, nodes: AppNode[], edges?: AppEdge[]) {
  const source = nodes.find((n) => n.id === connection.source);
  const target = nodes.find((n) => n.id === connection.target);
  const isImageToVideo = (source?.type === "image" || source?.type === "sketch") && target?.type === "video";
  if (isImageToVideo && edges) {
    const remaining = getVideoNodeAllowedRefCount(target, edges);
    if (remaining <= 0) return false;
  }
  return (
    (source?.type === "image" && target?.type === "image" && connection.source !== connection.target) ||
    (source?.type === "image" && target?.type === "video") ||
    (source?.type === "image" && target?.type === "text") ||
    (source?.type === "image" && target?.type === "sketch") ||
    (source?.type === "sketch" && target?.type === "image") ||
    (source?.type === "sketch" && target?.type === "video") ||
    (source?.type === "text" && target?.type === "image") ||
    (source?.type === "text" && target?.type === "sketch") ||
    (source?.type === "text" && target?.type === "text" && connection.source !== connection.target) ||
    (source?.type === "image" && target?.type === "prompt") ||
    (source?.type === "prompt" && target?.type === "result")
  );
}

function migrateCanvasStateForHydration(state: { nodes: AppNode[]; edges: AppEdge[]; viewport?: CanvasViewport }): CanvasState {
  const idMap = new Map<string, string>();
  const migratedNodes = state.nodes.map((node) => {
    const migrated = migrateNode(node);
    idMap.set(node.id, migrated.id);
    return migrated;
  });
  const migratedEdges = state.edges
    .map(migrateEdge)
    .map((edge) => ({
      ...edge,
      source: idMap.get(edge.source) ?? edge.source,
      target: idMap.get(edge.target) ?? edge.target,
    }))
    .filter((edge) => isValidCanvasConnection(edge, migratedNodes));
  return {
    nodes: migratedNodes,
    edges: migratedEdges,
    viewport: state.viewport,
  };
}

function getCanvasDraftSavedAt(state: CanvasState | null) {
  const savedAt = (state as (CanvasState & { savedAt?: unknown }) | null)?.savedAt;
  return typeof savedAt === "number" && Number.isFinite(savedAt) ? savedAt : null;
}

function hasActiveGenerationNode(nodes: AppNode[]) {
  return nodes.some((node) => {
    if (node.type !== "image" && node.type !== "video" && node.type !== "text") return false;
    const data = node.data as Record<string, unknown>;
    return data.status === "pending" || (typeof data.taskId === "string" && data.taskId.length > 0);
  });
}

function hasDraftOnlyEdge(draftEdges: AppEdge[], serverEdges: AppEdge[]) {
  return draftEdges.some((draftEdge) => !serverEdges.some((serverEdge) =>
    serverEdge.id === draftEdge.id ||
    (serverEdge.source === draftEdge.source && serverEdge.target === draftEdge.target)
  ));
}

function shouldHydrateFromLocalDraft(draftState: CanvasState | null, serverState: CanvasState | null) {
  if (!draftState) return false;
  const savedAt = getCanvasDraftSavedAt(draftState);
  if (!savedAt || Date.now() - savedAt > CANVAS_DRAFT_MAX_AGE_MS) return false;
  if (!serverState) return draftState.nodes.length > 0 || draftState.edges.length > 0;
  if (draftState.nodes.length > serverState.nodes.length) return true;
  if (hasDraftOnlyEdge(draftState.edges, serverState.edges)) return true;
  return hasActiveGenerationNode(draftState.nodes);
}

function isSameCanvasEdge(left: AppEdge, right: AppEdge) {
  return left.source === right.source &&
    left.target === right.target &&
    (left.sourceHandle ?? null) === (right.sourceHandle ?? null) &&
    (left.targetHandle ?? null) === (right.targetHandle ?? null);
}

function getVideoReferenceCleanupPatch(data: VideoNodeData, removedEdgeIds: Set<string>, remainingReferenceNodeIds: Set<string>) {
  const patch: Partial<VideoNodeData> = {};
  if (data.firstFrameEdgeId && removedEdgeIds.has(data.firstFrameEdgeId)) patch.firstFrameEdgeId = null;
  if (data.lastFrameEdgeId && removedEdgeIds.has(data.lastFrameEdgeId)) patch.lastFrameEdgeId = null;
  if (data.referenceImageOrder?.length) {
    const nextOrder = data.referenceImageOrder.filter((nodeId) => remainingReferenceNodeIds.has(nodeId));
    if (nextOrder.length !== data.referenceImageOrder.length) patch.referenceImageOrder = nextOrder;
  }
  return patch;
}

function getNodeBounds(node: AppNode) {
  const measured = node.measured;
  const width = node.width ?? measured?.width ?? 0;
  const height = node.height ?? measured?.height ?? 0;
  if (width <= 0 || height <= 0) return null;
  return {
    x: node.position.x,
    y: node.position.y,
    width,
    height,
  };
}

function findNodeAtFlowPoint(nodes: AppNode[], point: { x: number; y: number }, excludedNodeId?: string) {
  for (let index = nodes.length - 1; index >= 0; index -= 1) {
    const node = nodes[index];
    if (node.id === excludedNodeId || node.type === "canvasGroup") continue;
    const bounds = getNodeBounds(node);
    if (!bounds) continue;
    if (
      point.x >= bounds.x &&
      point.x <= bounds.x + bounds.width &&
      point.y >= bounds.y &&
      point.y <= bounds.y + bounds.height
    ) {
      return node;
    }
  }
  return null;
}

function summarizeCanvas(nodes: AppNode[]): { nodeCount: number; assetCount: number } {
  const assetIds = new Set<number>();
  const addAssetId = (value: unknown) => {
    if (typeof value === "number" && value > 0) {
      assetIds.add(value);
      return;
    }
    if (typeof value === "string" && /^\d+$/.test(value)) {
      assetIds.add(Number(value));
      return;
    }
    if (Array.isArray(value)) {
      value.forEach(addAssetId);
    }
  };
  nodes.forEach((node) => {
    const data = node.data as Record<string, unknown>;
    addAssetId(data.assetId);
    addAssetId(data.outputAssetId);
    addAssetId(data.previewAssetId);
    addAssetId(data.assetIdList);
    addAssetId(data.assetIds);
  });
  return {
    nodeCount: nodes.length,
    assetCount: assetIds.size,
  };
}

async function bindUploadedNodeAsset(projectId: string | null, node: AppNode, usageType = "source") {
  if (!projectId) return;
  const mediaData = node.data as ImageNodeData | VideoNodeData;
  if (!mediaData.assetId) return;
  await canvasApi.bindNodeAsset(projectId, node.id, {
    assetId: mediaData.assetId,
    assetVersionId: mediaData.assetVersionId ?? null,
    usageType,
  });
}

function EmptyCanvasButton({
  icon,
  label,
  description,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  description: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex min-w-[104px] flex-col items-center gap-1 rounded-[14px] border border-border-warm bg-background/92 px-4 py-3 text-center text-sm text-charcoal shadow-sm backdrop-blur transition-colors hover:border-[rgba(28,28,28,0.4)] hover:bg-background active:opacity-80"
    >
      <span className="text-muted-gray [&>svg]:size-4">{icon}</span>
      <span className="font-medium">{label}</span>
      <span className="text-[10px] leading-3 text-muted-gray">{description}</span>
    </button>
  );
}

function isServerProjectId(projectId: string | null | undefined): projectId is string {
  return typeof projectId === "string" && /^\d+$/.test(projectId);
}

function CanvasFlow() {
  const canvasT = useTranslations("canvas");
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const routeProjectId = searchParams.get("projectId");
  const routeTemplateId = searchParams.get("templateId");
  const [activeProjectId, setActiveProjectId] = useState<string | null>(null);
  const serverProjectId = isServerProjectId(activeProjectId) ? activeProjectId : null;
  const [nodes, setNodes] = useNodesState<AppNode>([]);
  const [edges, setEdges] = useEdgesState<AppEdge>([]);
  const [saveError, setSaveError] = useState("");
  const [pasteToast, setPasteToast] = useState("");
  const [isHydrated, setIsHydrated] = useState(false);
  const [isReadOnly, setIsReadOnly] = useState(false);
  const [projectName, setProjectName] = useState("未命名项目");
  const [projectRole, setProjectRole] = useState<CanvasProjectRole | null>(null);
  const [projectMembers, setProjectMembers] = useState<CanvasMember[]>([]);
  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [isSavingSnapshot, setIsSavingSnapshot] = useState(false);
  const [lastAppliedVersion, setLastAppliedVersion] = useState(0);
  const [latestKnownVersion, setLatestKnownVersion] = useState(0);
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const [templateLibraryOpen, setTemplateLibraryOpen] = useState(false);
  const [showMiniMap, setShowMiniMap] = useState(false);
  const [snapToGrid, setSnapToGrid] = useState(false);
  const [canvasZoom, setCanvasZoom] = useState(DEFAULT_CANVAS_VIEWPORT.zoom);
  const [nodeDragCommitVersion, setNodeDragCommitVersion] = useState(0);
  const [keyboardEditingNodeId, setKeyboardEditingNodeId] = useState<string | null>(null);
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
  const selectionStartRef = useRef<PointerSnapshot | null>(null);
  const selectionRectRef = useRef<SelectionRectSnapshot | null>(null);
  const selectionRafRef = useRef<number | null>(null);
  const groupDragStartRef = useRef<Record<string, { x: number; y: number }> | null>(null);
  // Keep asset URLs scoped to this canvas instance so project sessions do not leak cached URLs across tabs.
  const assetUrlCacheRef = useRef(new Map<string, AssetUrlEntry>());
  const storeApi = useStoreApi();

  const {
    getNodes,
    getEdges,
    getViewport,
    screenToFlowPosition,
    flowToScreenPosition,
    setViewport,
    zoomIn,
    zoomOut,
    fitView,
  } = useReactFlow();

  const {
    bounds: multiSelectionBounds,
    action: multiSelectionAction,
    clear: clearMultiSelection,
    refresh: refreshMultiSelection,
  } = useCanvasMultiSelection({
    getNodes: () => getNodes() as AppNode[],
    readBounds: getNodesSelectionViewportRect,
    resolveAction: (selectedNodes, currentNodes) => canGroupSelectedNodes(selectedNodes, currentNodes)
      ? "group"
      : getMergeTargetForSelectedNodes(selectedNodes, currentNodes)
        ? "merge"
        : null,
  });

  const handleSelectionEnd = useCallback(() => {
    const selectionRect = selectionRectRef.current;
    selectionRectRef.current = null;

    window.requestAnimationFrame(() => {
      let selectedNodes = getNodes().filter((node) => node.selected);

      if (selectionRect) {
        const selectedIds = new Set(
          selectedNodes
            .filter((node) => {
              const cardRect = getPreviewCardViewportRect(node.id);
              return cardRect ? rectsIntersect(selectionRect, cardRect) : true;
            })
            .map((node) => node.id)
        );

        const nextNodes: AppNode[] = (getNodes() as AppNode[]).map((node) => ({
            ...node,
            selected: selectedIds.has(node.id),
        }));
        setNodes(nextNodes);
        refreshMultiSelection(nextNodes);
        selectedNodes = nextNodes.filter((node) => selectedIds.has(node.id));
      }

      const singleSelectedGroup = getSingleSelectedGroup(selectedNodes as AppNode[]);
      if (singleSelectedGroup) {
        const selectedId = singleSelectedGroup.id;
        storeApi.setState({ nodesSelectionActive: false });
        setNodes((nds) =>
          nds.map((node) => ({
            ...node,
            selected: node.id === selectedId,
          }))
        );
        clearMultiSelection();
        return;
      }

      if (selectedNodes.length !== 1) {
        if (!selectionRect) refreshMultiSelection(getNodes() as AppNode[]);
        return;
      }

      const selectedId = selectedNodes[0].id;
      storeApi.setState({ nodesSelectionActive: false });
      setNodes((nds) =>
        nds.map((node) => ({
          ...node,
          selected: node.id === selectedId,
        }))
      );
      clearMultiSelection();
    });
  }, [clearMultiSelection, getNodes, refreshMultiSelection, setNodes, storeApi]);

  // Track last mouse position for paste placement
  const lastMouseRef = useRef<{ x: number; y: number }>({
    x: window.innerWidth / 2,
    y: window.innerHeight / 2,
  });
  const historyPastRef = useRef<CanvasSnapshot[]>([]);
  const historyFutureRef = useRef<CanvasSnapshot[]>([]);
  const lastHistorySignatureRef = useRef<string | null>(null);
  const restoringHistoryRef = useRef(false);
  const nodeDragActiveRef = useRef(false);
  const projectCreationRef = useRef(false);
  const ignoreNextPaneClickRef = useRef(false);
  const [clientId] = useState(() => `canvas_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`);

  // 初始化服务器存储
  const { createProject: createServerProject, loadProject, saveSnapshot } = useCanvasServerStorage();
  // 初始化实时操作
  const canvasRealtime = useCanvasRealtime(serverProjectId, clientId, lastAppliedVersion);
  // 初始化操作操作
  const canvasOperations = useCanvasOperations(serverProjectId, clientId, latestKnownVersion, setLatestKnownVersion, canvasRealtime.sendOperation, canvasRealtime.isConnected);
  const cleanupVideoReferenceState = useCallback((removedEdges: AppEdge[], remainingEdges: AppEdge[], options?: { submit?: boolean }) => {
    if (removedEdges.length === 0) return;
    const removedEdgeIds = new Set(removedEdges.map((edge) => edge.id));
    const affectedVideoIds = new Set(removedEdges.map((edge) => edge.target));
    if (affectedVideoIds.size === 0) return;

    const remainingByVideoId = new Map<string, Set<string>>();
    for (const edge of remainingEdges) {
      if (!affectedVideoIds.has(edge.target)) continue;
      const set = remainingByVideoId.get(edge.target) ?? new Set<string>();
      set.add(edge.source);
      remainingByVideoId.set(edge.target, set);
    }

    const patches = (getNodes() as AppNode[]).flatMap((node) => {
      if (node.type !== "video" || !affectedVideoIds.has(node.id)) return [];
      const patch = getVideoReferenceCleanupPatch(node.data as VideoNodeData, removedEdgeIds, remainingByVideoId.get(node.id) ?? new Set());
      return Object.keys(patch).length > 0 ? [{ nodeId: node.id, patch }] : [];
    });

    if (patches.length === 0) return;
    setNodes((nds) => nds.map((node) => {
      const item = patches.find((patch) => patch.nodeId === node.id);
      return item ? { ...node, data: { ...node.data, ...item.patch } } as AppNode : node;
    }));

    if (options?.submit === false) return;
    for (const { nodeId, patch } of patches) {
      canvasOperations.submitOperation("NODE_UPDATE_DATA", {
        nodeId,
        patch,
      });
    }
  }, [canvasOperations, getNodes, setNodes]);
  const mediaStoreScope = useMemo(() => ({
    ownerKey: user?.id ?? null,
    projectId: activeProjectId,
  }), [activeProjectId, user?.id]);
  // 初始化实时消息计数
  const processedRealtimeMessageCountRef = useRef(0);
  // 初始化画布缩放
  const canvasZoomRef = useRef(DEFAULT_CANVAS_VIEWPORT.zoom);
  // 初始化远程存在
  const [remotePresences, setRemotePresences] = useState<Record<string, RemoteCanvasPresence>>({});
  // 初始化最后发送存在时间
  const lastPresenceSentAtRef = useRef(0);

  const canvasTransform = useStore((state) => state.transform);

  const membersByUserId = useMemo(() => {
    const map = new Map<number, CanvasMember>();
    for (const member of projectMembers) map.set(member.userId, member);
    return map;
  }, [projectMembers]);

  const getPresenceDisplay = useCallback((presence: RemoteCanvasPresence) => {
    const member = typeof presence.userId === "number" ? membersByUserId.get(presence.userId) : undefined;
    return {
      name: member?.nickname?.trim() || "协作者",
      avatar: member?.avatar || null,
    };
  }, [membersByUserId]);

  const remoteSelectionHighlights = useMemo(() => {
    const highlights: Array<{ key: string; nodeId: string; color: string; name: string; editing: boolean; rect: SelectionRectSnapshot }> = [];
    for (const [presenceClientId, presence] of Object.entries(remotePresences)) {
      if (!presence.selectedNodeIds?.length) continue;
      const color = getPresenceColor(presenceClientId);
      const { name } = getPresenceDisplay(presence);
      for (const nodeId of presence.selectedNodeIds) {
        const node = nodes.find((candidate) => candidate.id === nodeId);
        const rect = getNodeScreenRect(node, canvasTransform);
        if (!rect) continue;
        highlights.push({ key: `${presenceClientId}:${nodeId}`, nodeId, color, name, editing: presence.editingNodeId === nodeId, rect });
      }
    }
    return highlights;
  }, [remotePresences, getPresenceDisplay, nodes, canvasTransform]);
  // 初始化编辑编辑节点ID
   const editingNodeIdRef = useRef<string | null>(null);
  // 初始化节点数据补丁定时器
  const nodeDataPatchTimersRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  // 初始化最后应用版本
  const lastAppliedVersionRef = useRef(0);
  const syncInFlightVersionsRef = useRef<Set<number>>(new Set());
  const pendingOperationCountRef = useRef(0);
  const deferredSyncUntilPendingClearRef = useRef(false);
  const viewportAssetRefreshTimerRef = useRef<number | null>(null);

  // 初始化同步状态
  const syncState: CanvasSyncState = saveError
    ? "error"
    : !canvasRealtime.isConnected && serverProjectId
      ? "offline"
      : isSavingSnapshot || canvasOperations.pendingOperationCount > 0
        ? "saving"
        : "saved";

  const markAppliedVersion = useCallback((version: number) => {
    lastAppliedVersionRef.current = Math.max(lastAppliedVersionRef.current, version);
    setLastAppliedVersion((prev) => Math.max(prev, version));
    setLatestKnownVersion((prev) => Math.max(prev, version));
  }, []);

  const resetAppliedVersion = useCallback((version = 0) => {
    lastAppliedVersionRef.current = version;
    setLastAppliedVersion(version);
    setLatestKnownVersion(version);
  }, []);

  const refreshAssetUrls = useCallback(async (nodesToRefresh: AppNode[]) => {
    const assetUrlCache = assetUrlCacheRef.current;
    const mediaAssetRefs = nodesToRefresh.flatMap((node) => (
      collectNodeAssetIds(node).map((assetId) => ({ nodeId: node.id, node, assetId }))
    ));
    if (mediaAssetRefs.length === 0) return;
    const assetIdsByNodeId = new Map<string, number[]>();
    mediaAssetRefs.forEach(({ nodeId, assetId }) => {
      assetIdsByNodeId.set(nodeId, [...(assetIdsByNodeId.get(nodeId) ?? []), assetId]);
    });

    const requestsByAssetId = new Map<number, ReturnType<typeof getNodeAssetAccessRequest>>();
    mediaAssetRefs.forEach(({ node, assetId }) => {
      if (!requestsByAssetId.has(assetId)) {
        requestsByAssetId.set(assetId, getNodeAssetAccessRequest(node, assetId));
      }
    });

    const projectIdForAccess = serverProjectId;
    const entriesByAssetId = new Map<number, AssetUrlEntry>();
    requestsByAssetId.forEach((_, assetId) => {
      const cached = assetUrlCache.get(assetUrlCacheKey(projectIdForAccess, assetId))
        ?? assetUrlCache.get(assetUrlCacheKey(null, assetId));
      if (isUsableAssetUrlEntry(cached)) {
        entriesByAssetId.set(assetId, cached);
      }
    });

    if (entriesByAssetId.size > 0) {
      setNodes((nds) => nds.map((node) => {
        const assetIds = assetIdsByNodeId.get(node.id) ?? collectNodeAssetIds(node);
        return assetIds.reduce((nextNode, assetId) => {
          const entry = entriesByAssetId.get(assetId);
          return entry ? withFreshAssetUrl(nextNode, entry.url, entry.expireTime, assetId) : nextNode;
        }, node);
      }));
    }

    const pendingRequests = [...requestsByAssetId.values()].filter((request) => !entriesByAssetId.has(Number(request.assetId)));
    if (pendingRequests.length === 0) return;

    type AssetUrlResponse = { assetId: number | string; url?: string | null; expireTime?: string | null };
    const fetchPersonalAssetUrlEntries = async (requests: typeof pendingRequests): Promise<AssetUrlResponse[]> => {
      const fallbackEntries = await Promise.all(requests.map(async ({ assetId }) => {
          try {
            const asset = await getMyAsset(assetId);
            const url = getAssetOriginalUrl(asset);
            return url ? { assetId: Number(assetId), url, expireTime: getAssetOriginalExpireTime(asset) ?? null } : null;
          } catch {
            return null;
          }
        }));
      return fallbackEntries.filter((entry): entry is AssetUrlEntry => Boolean(entry));
    };

    const rememberAssetUrlEntries = (responses: AssetUrlResponse[]) => responses.forEach((entry) => {
      if (!entry?.url) return;
      const assetId = Number(entry.assetId);
      if (!Number.isFinite(assetId)) return;
      const normalizedEntry = { assetId, url: entry.url, expireTime: entry.expireTime ?? null };
      entriesByAssetId.set(normalizedEntry.assetId, normalizedEntry);
      assetUrlCache.set(assetUrlCacheKey(projectIdForAccess, normalizedEntry.assetId), normalizedEntry);
    });

    try {
      const responses = projectIdForAccess
        ? await canvasApi.getProjectAssetAccessUrls(projectIdForAccess, pendingRequests)
        : await getAssetAccessUrls(pendingRequests);
      rememberAssetUrlEntries(responses);
    } catch {
      rememberAssetUrlEntries(await fetchPersonalAssetUrlEntries(pendingRequests));
    }

    if (projectIdForAccess) {
      const missingRequests = pendingRequests.filter((request) => !entriesByAssetId.has(Number(request.assetId)));
      if (missingRequests.length > 0) {
        rememberAssetUrlEntries(await fetchPersonalAssetUrlEntries(missingRequests));
      }
    }

    if (entriesByAssetId.size === 0) return;

    setNodes((nds) => nds.map((node) => {
      const assetIds = assetIdsByNodeId.get(node.id) ?? collectNodeAssetIds(node);
      return assetIds.reduce((nextNode, assetId) => {
        const entry = entriesByAssetId.get(assetId);
        return entry ? withFreshAssetUrl(nextNode, entry.url, entry.expireTime, assetId) : nextNode;
      }, node);
    }));
  }, [serverProjectId, setNodes]);

  const refreshVisibleAssetUrls = useCallback((nodesToRefresh?: AppNode[]) => {
    const visibleNodes = filterNodesInExpandedCanvasViewport(nodesToRefresh ?? getNodes() as AppNode[], screenToFlowPosition);
    if (visibleNodes.length > 0) {
      refreshAssetUrls(visibleNodes);
    }
  }, [getNodes, refreshAssetUrls, screenToFlowPosition]);

  const scheduleVisibleAssetRefresh = useCallback(() => {
    if (viewportAssetRefreshTimerRef.current) {
      window.clearTimeout(viewportAssetRefreshTimerRef.current);
    }
    viewportAssetRefreshTimerRef.current = window.setTimeout(() => {
      viewportAssetRefreshTimerRef.current = null;
      refreshVisibleAssetUrls();
    }, 120);
  }, [refreshVisibleAssetUrls]);

  useEffect(() => () => {
    if (viewportAssetRefreshTimerRef.current) {
      window.clearTimeout(viewportAssetRefreshTimerRef.current);
    }
  }, []);

  useEffect(() => {
    if (!isHydrated) return;
    const refreshExpiringAssetUrls = () => {
      const now = Date.now();
      const visibleNodes = filterNodesInExpandedCanvasViewport(getNodes() as AppNode[], screenToFlowPosition);
      const expiringNodes = visibleNodes.filter((node) => {
        if (node.type !== "image" && node.type !== "video") return false;
        if (collectNodeAssetIds(node).length === 0) return false;
        const data = node.data as ImageNodeData | VideoNodeData;
        const expireTime = data.assetUrlExpireTime;
        const displayUrl = node.type === "video" ? (data as VideoNodeData).videoUrl || data.previewUrl : data.previewUrl;
        if (!expireTime) return !displayUrl;
        const expireAt = new Date(expireTime).getTime();
        return !Number.isFinite(expireAt) || expireAt - now < 120_000;
      });
      if (expiringNodes.length > 0) refreshAssetUrls(expiringNodes);
    };
    refreshExpiringAssetUrls();
    const timer = window.setInterval(refreshExpiringAssetUrls, 60_000);
    return () => window.clearInterval(timer);
  }, [getNodes, isHydrated, refreshAssetUrls, screenToFlowPosition]);

  const hydrateLocalMediaNodes = useCallback(async (nodesToHydrate: AppNode[]) => {
    const hydrated = await Promise.all(nodesToHydrate.map(async (node) => {
      try {
        if (node.type === "image") {
          const data = node.data as ImageNodeData;
          if (data.previewUrl || data.dataUrl || data.assetId || !data.imageId) return node;
          const stored = await loadImage(data.imageId, mediaStoreScope);
          return stored ? { ...node, data: { ...data, ...stored, projectId: data.projectId ?? stored.projectId } } as AppNode : node;
        }
        if (node.type === "video") {
          const data = node.data as VideoNodeData;
          if (data.previewUrl || data.videoUrl || data.assetId || !data.videoId) return node;
          const stored = await loadVideo(data.videoId, mediaStoreScope);
          return stored ? { ...node, data: { ...data, ...stored, projectId: data.projectId ?? stored.projectId } } as AppNode : node;
        }
      } catch {
        return node;
      }
      return node;
    }));
    setNodes((currentNodes) => currentNodes.map((node) => hydrated.find((item) => item.id === node.id) ?? node));
  }, [mediaStoreScope, setNodes]);

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
      setEdges((eds) => {
        const nextEdges = eds.filter((edge) => edge.source !== payload.nodeId && edge.target !== payload.nodeId);
        cleanupVideoReferenceState(eds.filter((edge) => !nextEdges.includes(edge)), nextEdges, { submit: false });
        return nextEdges;
      });
      return;
    }
    if (operationType === "NODE_CREATE" && payload.node) {
      const node = migrateNode(payload.node as AppNode);
      setNodes((nds) => nds.some((item) => item.id === node.id) ? nds : [...nds.map((item) => ({ ...item, selected: false })), node]);
      return;
    }
    if ((operationType === "NODE_UPDATE_DATA" || operationType === "TASK_STATUS_PATCH") && typeof payload.nodeId === "string" && payload.patch) {
      const patch = stripRuntimeAssetUrlsFromPatch(payload.patch as Record<string, unknown>);
      let nodeToRefresh: AppNode | null = null;
      setNodes((nds) => nds.map((node) => {
        if (node.id !== payload.nodeId) return node;
        const imageMergedPatch = mergeImageOutputPatch(node as AppNode, patch);
        const mergedPatch = mergeVideoOutputPatch(node as AppNode, imageMergedPatch);
        const nextNode = migrateNode({ ...node, data: { ...node.data, ...mergedPatch } } as AppNode);
        nodeToRefresh = nextNode;
        return nextNode;
      }));
      if (nodeToRefresh && collectNodeAssetIds(nodeToRefresh).length > 0) {
        window.requestAnimationFrame(() => refreshAssetUrls([nodeToRefresh as AppNode]));
      }
      return;
    }
    if (operationType === "ASSET_ATTACH" && typeof payload.nodeId === "string") {
      setNodes((nds) => nds.map((node) => node.id === payload.nodeId ? {
        ...node,
        data: {
          ...node.data,
          assetId: payload.assetId,
          assetVersionId: payload.assetVersionId ?? node.data.assetVersionId,
        },
      } as AppNode : node));
      if (typeof payload.assetId === "number") {
        const node = (getNodes() as AppNode[]).find((item) => item.id === payload.nodeId);
        if (node) {
          refreshAssetUrls([{ ...node, data: { ...node.data, assetId: payload.assetId } } as AppNode]);
        }
      }
      return;
    }
    if (operationType === "EDGE_CREATE" && payload.edge) {
      const edge = payload.edge as AppEdge;
      setEdges((eds) => {
        const nodes = getNodes() as AppNode[];
        if (!isValidCanvasConnection(edge, nodes, eds)) return eds;
        if (eds.some((item) => item.id === edge.id || isSameCanvasEdge(item, edge))) return eds;
        return addEdge(edge, eds);
      });
      return;
    }
    if (operationType === "EDGE_DELETE" && typeof payload.edgeId === "string") {
      setEdges((eds) => {
        const nextEdges = eds.filter((edge) => edge.id !== payload.edgeId);
        cleanupVideoReferenceState(eds.filter((edge) => edge.id === payload.edgeId), nextEdges, { submit: false });
        return nextEdges;
      });
      return;
    }
    if (operationType === "CANVAS_CLEAR") {
      setNodes(defaultNodes());
      setEdges([]);
    }
  }, [cleanupVideoReferenceState, getNodes, refreshAssetUrls, setEdges, setNodes]);

  const hydrateRemoteSnapshot = useCallback((snapshot: Parameters<typeof snapshotRecordToCanvasState>[0]) => {
    const state = snapshotRecordToCanvasState(snapshot);
    if (!state) return;
    const migratedNodes = state.nodes.map(migrateNode);
    setNodes(migratedNodes);
    setEdges(state.edges.map(migrateEdge));
    if (state.viewport) setViewport(state.viewport);
    if (typeof snapshot?.version === "number") markAppliedVersion(snapshot.version);
    window.requestAnimationFrame(() => refreshVisibleAssetUrls(migratedNodes));
  }, [markAppliedVersion, refreshVisibleAssetUrls, setEdges, setNodes, setViewport]);

  const applyOperationRecord = useCallback((operationRecord: { clientId: string; nextVersion: number; operationType: string; operationJson: string }) => {
    if (operationRecord.nextVersion <= lastAppliedVersionRef.current) return;
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
    if (syncInFlightVersionsRef.current.has(afterVersion)) return;
    syncInFlightVersionsRef.current.add(afterVersion);
    canvasApi.syncOperations(serverProjectId, afterVersion)
      .then((syncResult) => {
        if (syncResult.mode === "snapshot") {
          const snapshotVersion = Number(syncResult.snapshot?.version ?? syncResult.toVersion ?? 0);
          if (Number.isFinite(snapshotVersion) && snapshotVersion <= lastAppliedVersionRef.current) return;
          if (pendingOperationCountRef.current > 0) {
            deferredSyncUntilPendingClearRef.current = true;
            return;
          }
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
      .catch(() => undefined)
      .finally(() => {
        syncInFlightVersionsRef.current.delete(afterVersion);
      });
  }, [serverProjectId, applyOperationRecord, hydrateRemoteSnapshot]);

  const refreshVisibleAssetUrlsRef = useRef(refreshVisibleAssetUrls);
  useEffect(() => {
    refreshVisibleAssetUrlsRef.current = refreshVisibleAssetUrls;
  }, [refreshVisibleAssetUrls]);

  const syncFromVersionRef = useRef(syncFromVersion);
  useEffect(() => {
    syncFromVersionRef.current = syncFromVersion;
  }, [syncFromVersion]);

  useEffect(() => {
    pendingOperationCountRef.current = canvasOperations.pendingOperationCount;
    if (canvasOperations.pendingOperationCount !== 0 || !deferredSyncUntilPendingClearRef.current) return;
    deferredSyncUntilPendingClearRef.current = false;
    syncFromVersion(lastAppliedVersionRef.current);
  }, [canvasOperations.pendingOperationCount, syncFromVersion]);

  useEffect(() => {
    const newMessages = canvasRealtime.messages.slice(processedRealtimeMessageCountRef.current);
    processedRealtimeMessageCountRef.current = canvasRealtime.messages.length;
    const isCurrentProjectMessage = (message: { type: string; projectId?: unknown }) => {
      if (!serverProjectId) return false;
      return String(message.projectId ?? "") === serverProjectId;
    };
    for (const message of newMessages) {
      if (!isCurrentProjectMessage(message)) continue;
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
    if (!serverProjectId || !isHydrated) return;
    const pollSync = () => {
      if (document.visibilityState !== "visible") return;
      if (pendingOperationCountRef.current > 0) return;
      syncFromVersionRef.current(lastAppliedVersionRef.current);
    };
    const timer = window.setInterval(pollSync, 30_000);
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") pollSync();
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [serverProjectId, isHydrated]);

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
      const submitPatch = () => {
        const patch = Object.fromEntries(
          Object.entries(filterSyncableNodeDataPatch(detail.patch)).filter(([patchKey]) => !SNAPSHOT_ONLY_NODE_DATA_KEYS.has(patchKey))
        );
        if (Object.keys(patch).length > 0) {
          canvasOperations.submitOperation("NODE_UPDATE_DATA", {
            nodeId: detail.nodeId,
            patch,
          });
        }
        delete nodeDataPatchTimersRef.current[key];
      };
      if (detail.flush) {
        submitPatch();
      } else {
        nodeDataPatchTimersRef.current[key] = setTimeout(submitPatch, NODE_DATA_PATCH_DEBOUNCE_MS);
      }
    }
    window.addEventListener("copse:node-data-patch", handleNodeDataPatch);
    return () => window.removeEventListener("copse:node-data-patch", handleNodeDataPatch);
  }, [canvasOperations, isReadOnly]);

  useEffect(() => {
    function handleNodePositionPatch(event: Event) {
      if (isReadOnly) return;
      const detail = (event as CustomEvent<NodePositionPatchEventDetail>).detail;
      if (!detail?.nodeId || !detail.position) return;
      canvasOperations.submitOperation("NODE_MOVE", {
        nodeId: detail.nodeId,
        position: detail.position,
      });
    }

    window.addEventListener("copse:node-position-patch", handleNodePositionPatch);
    return () => window.removeEventListener("copse:node-position-patch", handleNodePositionPatch);
  }, [canvasOperations, isReadOnly]);

  useEffect(() => {
    function handleEdgeDelete(event: Event) {
      if (isReadOnly) return;
      const detail = (event as CustomEvent<EdgeDeleteEventDetail>).detail;
      if (!detail?.edgeId) return;
      setEdges((eds) => {
        const nextEdges = eds.filter((edge) => edge.id !== detail.edgeId);
        cleanupVideoReferenceState(eds.filter((edge) => edge.id === detail.edgeId), nextEdges, { submit: true });
        return nextEdges;
      });
      canvasOperations.submitOperation("EDGE_DELETE", {
        edgeId: detail.edgeId,
      });
    }

    window.addEventListener("copse:edge-delete", handleEdgeDelete);
    return () => window.removeEventListener("copse:edge-delete", handleEdgeDelete);
  }, [canvasOperations, cleanupVideoReferenceState, isReadOnly, setEdges]);

  useEffect(() => {
    function handleGroupUngroup(event: Event) {
      if (isReadOnly) return;
      const detail = (event as CustomEvent<GroupUngroupEventDetail>).detail;
      if (!detail?.groupId) return;

      const groupNode = (getNodes() as AppNode[]).find((node) => node.id === detail.groupId && node.type === "canvasGroup");
      if (!groupNode || groupNode.type !== "canvasGroup") return;

      const childNodeIds = new Set((groupNode.data as GroupNodeData).childNodeIds);
      setNodes((nds) => nds
        .filter((node) => node.id !== detail.groupId)
        .map((node) => ({ ...node, selected: childNodeIds.has(node.id) }))
      );
      setEdges((eds) => eds.filter((edge) => edge.source !== detail.groupId && edge.target !== detail.groupId));
      clearMultiSelection();
      canvasOperations.submitOperation("NODE_DELETE", {
        nodeId: detail.groupId,
      });
    }

    window.addEventListener("copse:group-ungroup", handleGroupUngroup);
    return () => window.removeEventListener("copse:group-ungroup", handleGroupUngroup);
  }, [canvasOperations, clearMultiSelection, getNodes, isReadOnly, setEdges, setNodes]);

  const arrangeGroupNodes = useCallback((groupId: string, mode: GroupArrangeEventDetail["mode"]) => {
    if (isReadOnly) return;
    const currentNodes = getNodes() as AppNode[];
    const groupNode = currentNodes.find((node) => node.id === groupId && node.type === "canvasGroup");
    if (!groupNode || groupNode.type !== "canvasGroup") return;

    const groupData = groupNode.data as GroupNodeData;
    const childNodes = groupData.childNodeIds
      .map((childId) => currentNodes.find((node) => node.id === childId && node.type !== "canvasGroup"))
      .filter((node): node is AppNode => Boolean(node));
    if (childNodes.length === 0) return;

    const childRects = childNodes
      .map((node) => ({ node, rect: getPreviewCardFlowRect(node, screenToFlowPosition) }))
      .filter((item): item is { node: AppNode; rect: SelectionRectSnapshot } => Boolean(item.rect));
    if (childRects.length === 0) return;

    const gridColumnCount = mode === "grid" ? Math.max(1, Math.ceil(Math.sqrt(childRects.length))) : Number.POSITIVE_INFINITY;
    let cursorX = 0;
    let cursorY = 0;
    let rowHeight = 0;
    let rowItemCount = 0;
    let contentWidth = 0;
    const layout = new Map<string, { x: number; y: number; width: number; height: number }>();

    for (const { node, rect } of childRects) {
      if (mode === "grid" && rowItemCount >= gridColumnCount) {
        cursorX = 0;
        cursorY += rowHeight + GROUP_LAYOUT_GAP;
        rowHeight = 0;
        rowItemCount = 0;
      }
      layout.set(node.id, {
        x: cursorX,
        y: cursorY,
        width: rect.width,
        height: rect.height,
      });
      contentWidth = Math.max(contentWidth, cursorX + rect.width);
      rowHeight = Math.max(rowHeight, rect.height);
      cursorX += rect.width + GROUP_LAYOUT_GAP;
      rowItemCount += 1;
    }

    const contentHeight = cursorY + rowHeight;
    const nextGroupPosition = groupNode.position;
    const nextGroupData: GroupNodeData = {
      ...groupData,
      width: Math.max(120, contentWidth + GROUP_LAYOUT_PADDING * 2),
      height: Math.max(96, contentHeight + GROUP_LAYOUT_PADDING * 2),
      updatedAt: new Date().toISOString(),
    };
    const nextPositions = new Map<string, { x: number; y: number }>();
    for (const [nodeId, item] of layout.entries()) {
      nextPositions.set(nodeId, {
        x: nextGroupPosition.x + GROUP_LAYOUT_PADDING + item.x,
        y: nextGroupPosition.y + GROUP_LAYOUT_PADDING + item.y,
      });
    }

    setNodes((nds): AppNode[] => nds.map((node): AppNode => {
      if (node.id === groupNode.id && node.type === "canvasGroup") {
        return {
          ...node,
          data: nextGroupData,
          selected: true,
        };
      }
      const nextPosition = nextPositions.get(node.id);
      if (!nextPosition) return { ...node, selected: false };
      return {
        ...node,
        position: nextPosition,
        selected: false,
      };
    }));
    clearMultiSelection();
    for (const [nodeId, position] of nextPositions.entries()) {
      canvasOperations.submitOperation("NODE_MOVE", {
        nodeId,
        position,
      });
    }
    canvasOperations.submitOperation("NODE_UPDATE_DATA", {
      nodeId: groupNode.id,
      patch: {
        width: nextGroupData.width,
        height: nextGroupData.height,
        updatedAt: nextGroupData.updatedAt,
      },
    });
  }, [canvasOperations, clearMultiSelection, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  useEffect(() => {
    function handleGroupArrange(event: Event) {
      const detail = (event as CustomEvent<GroupArrangeEventDetail>).detail;
      if (!detail?.groupId || !detail.mode) return;
      arrangeGroupNodes(detail.groupId, detail.mode);
    }

    window.addEventListener("copse:group-arrange", handleGroupArrange);
    return () => window.removeEventListener("copse:group-arrange", handleGroupArrange);
  }, [arrangeGroupNodes]);

  useEffect(() => {
    function handleNodeEditingPresence(event: Event) {
      const detail = (event as CustomEvent<NodeEditingPresenceEventDetail>).detail;
      editingNodeIdRef.current = detail?.nodeId ?? null;
      setKeyboardEditingNodeId(editingNodeIdRef.current);
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

  // 初始化项目
  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (routeProjectId) {
        setActiveProjectId(routeProjectId);
        return;
      }

      if (projectCreationRef.current) return;
      projectCreationRef.current = true;
      createServerProject("未命名项目")
        .then((projectId) => {
          const id = String(projectId);
          const templateQuery = routeTemplateId ? `&templateId=${encodeURIComponent(routeTemplateId)}` : "";
          router.replace(`/canvas?projectId=${encodeURIComponent(id)}${templateQuery}`);
          setActiveProjectId(id);
        })
        .catch(() => {
          setSaveError("服务端项目创建失败，请稍后重试");
          projectCreationRef.current = false;
        });
    }, 0);
    return () => window.clearTimeout(timer);
  }, [createServerProject, routeProjectId, routeTemplateId, router]);

  useEffect(() => {
    if (!isHydrated) return;
    if (nodeDragActiveRef.current) return;
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
  }, [edges, isHydrated, nodeDragCommitVersion, nodes]);

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
        if (selectionChanges.length > 0) {
          setNodes((nds) => {
            const selectionRect = selectionRectRef.current;
            const nextNodes = applyNodeChanges(selectionChanges, nds);
            const selectedNodes = selectionRect ? applyPreviewCardSelection(nextNodes, selectionRect) : nextNodes;
            queueMicrotask(() => refreshMultiSelection(selectedNodes as AppNode[]));
            return selectedNodes;
          });
        }
        return;
      }
      setNodes((nds) => {
        const selectionRect = selectionRectRef.current;
        let nextNodes = applyNodeChanges(changes, nds) as AppNode[];

        for (const change of changes) {
          if (change.type !== "position" || !change.position) continue;
          const groupNode = nds.find((node) => node.id === change.id && node.type === "canvasGroup");
          if (groupNode?.type !== "canvasGroup") continue;
          const previousPositions = groupDragStartRef.current;
          if (!previousPositions) continue;
          const previousGroupPosition = previousPositions[groupNode.id] ?? groupNode.position;
          const delta = {
            x: change.position.x - previousGroupPosition.x,
            y: change.position.y - previousGroupPosition.y,
          };
          if (delta.x === 0 && delta.y === 0) continue;
          const childIds = new Set((groupNode.data as GroupNodeData).childNodeIds);
          nextNodes = nextNodes.map((node) => {
            if (!childIds.has(node.id)) return node;
            const previousChildPosition = previousPositions[node.id] ?? node.position;
            return {
              ...node,
              position: {
                x: previousChildPosition.x + delta.x,
                y: previousChildPosition.y + delta.y,
              },
            };
          });
        }

        if (selectionRect && changes.some((change) => change.type === "select")) {
          nextNodes = applyPreviewCardSelection(nextNodes, selectionRect) as AppNode[];
        }
        queueMicrotask(() => refreshMultiSelection(nextNodes));
        return nextNodes;
      });
      for (const change of changes) {
        if (change.type === "position" && change.dragging === false && change.position) {
          const movedNode = getNodes().find((node) => node.id === change.id);
          canvasOperations.submitOperation("NODE_MOVE", {
            nodeId: change.id,
            position: change.position,
          });
          if (movedNode?.type === "canvasGroup") {
            const childIds = new Set((movedNode.data as GroupNodeData).childNodeIds);
            const previousPositions = groupDragStartRef.current;
            const previousGroupPosition = previousPositions?.[movedNode.id] ?? movedNode.position;
            const delta = {
              x: change.position.x - previousGroupPosition.x,
              y: change.position.y - previousGroupPosition.y,
            };
            for (const childNode of getNodes().filter((node) => childIds.has(node.id))) {
              const previousChildPosition = previousPositions?.[childNode.id] ?? childNode.position;
              canvasOperations.submitOperation("NODE_MOVE", {
                nodeId: childNode.id,
                position: {
                  x: previousChildPosition.x + delta.x,
                  y: previousChildPosition.y + delta.y,
                },
              });
            }
          }
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
    [canvasOperations, getNodes, isReadOnly, refreshMultiSelection, setNodes]
  );

  const onEdgesChange = useCallback(
    (changes: EdgeChange<AppEdge>[]) => {
      if (isReadOnly) {
        const selectionChanges = changes.filter((change) => change.type === "select");
        if (selectionChanges.length > 0) setEdges((eds) => applyEdgeChanges(selectionChanges, eds));
        return;
      }
      setEdges((eds) => {
        const nextEdges = applyEdgeChanges(changes, eds);
        cleanupVideoReferenceState(eds.filter((edge) => !nextEdges.some((nextEdge) => nextEdge.id === edge.id)), nextEdges, { submit: true });
        return nextEdges;
      });
      for (const change of changes) {
        if (change.type === "remove") {
          canvasOperations.submitOperation("EDGE_DELETE", {
            edgeId: change.id,
          });
        }
      }
    },
    [canvasOperations, cleanupVideoReferenceState, isReadOnly, setEdges]
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
    const currentEdges = getEdges() as AppEdge[];
    if (currentEdges.some((item) => isSameCanvasEdge(item, edge))) return;

    let nextVideoPatch: Partial<VideoNodeData> | null = null;
    const targetNode = getNodes().find((n) => n.id === connection.target);
    if (targetNode?.type === "video") {
      const data = targetNode.data as VideoNodeData;
      const nextIncomingEdges = [...currentEdges.filter((item) => item.target === connection.target), edge];
      const patch: Partial<VideoNodeData> = {};
      if (data.explicitMode === "FIRST_LAST_FRAME_VIDEO") {
        if (nextIncomingEdges.length === 1 && !data.firstFrameEdgeId) {
          patch.firstFrameEdgeId = edge.id;
        } else if (nextIncomingEdges.length === 2 && !data.lastFrameEdgeId) {
          patch.lastFrameEdgeId = edge.id;
        }
      }
      if (data.explicitMode === "MULTI_REF_VIDEO") {
        const connectedNodeIds = new Set(nextIncomingEdges.map((incomingEdge) => incomingEdge.source));
        const order = (data.referenceImageOrder ?? []).filter((nodeId) => connectedNodeIds.has(nodeId));
        if (connection.source && !order.includes(connection.source)) {
          patch.referenceImageOrder = [...order, connection.source];
        }
      }
      if (Object.keys(patch).length > 0) nextVideoPatch = patch;
    }

    setEdges((eds) => {
      if (eds.some((item) => isSameCanvasEdge(item, edge))) return eds;
      return addEdge(edge, eds);
    });
    canvasOperations.submitOperation("EDGE_CREATE", { edge });
    if (nextVideoPatch && connection.target) {
      setNodes((nds) => nds.map((n) => n.id === connection.target ? { ...n, data: { ...(n.data as Record<string, unknown>), ...nextVideoPatch } } as AppNode : n));
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: connection.target, patch: nextVideoPatch, flush: true },
      }));
    }
  }, [canvasOperations, getEdges, getNodes, isReadOnly, setEdges, setNodes]);

  // Hydrate from server state when the project changes
  useEffect(() => {
    if (!activeProjectId) return;
    const projectId = activeProjectId;
    let cancelled = false;

    async function hydrate() {
      setIsHydrated(false);
      resetAppliedVersion(0);
      historyPastRef.current = [];
      historyFutureRef.current = [];
      lastHistorySignatureRef.current = null;

      if (!isServerProjectId(projectId)) {
        router.replace("/canvas");
        setSaveError("本地草稿已弃用，请使用服务端项目");
        return;
      }

      try {
        const serverResult = await loadProject(projectId);
        const saved = serverResult.state;
        setProjectName(serverResult.project.name || "未命名项目");
        setIsReadOnly(serverResult.project.readonly === true || serverResult.project.canEdit === false);
        setProjectRole(serverResult.project.role ?? null);
        canvasApi.getProjectMembers(projectId).then(setProjectMembers).catch(() => setProjectMembers([]));

        const snapshotVersion = serverResult.snapshot?.version ?? 0;
        const currentVersion = serverResult.project.currentVersion ?? snapshotVersion;
        markAppliedVersion(snapshotVersion);
        const localDraft = loadCanvas(projectId, user?.id ?? null);

        if (!saved) {
          if (!cancelled) {
            const draftState = localDraft && shouldHydrateFromLocalDraft(localDraft, null)
              ? migrateCanvasStateForHydration(localDraft)
              : null;
            const nextNodes = draftState?.nodes ?? defaultNodes();
            setNodes(nextNodes);
            setEdges(draftState?.edges ?? []);
            setViewport(draftState?.viewport ?? DEFAULT_CANVAS_VIEWPORT);
            setIsHydrated(true);
            window.requestAnimationFrame(() => {
              hydrateLocalMediaNodes(nextNodes);
              refreshVisibleAssetUrlsRef.current(nextNodes);
            });
          }
          if (currentVersion > snapshotVersion) syncFromVersionRef.current(snapshotVersion);
          return;
        }

        const serverState = migrateCanvasStateForHydration(saved);
        const draftState = localDraft && shouldHydrateFromLocalDraft(localDraft, serverState)
          ? migrateCanvasStateForHydration(localDraft)
          : null;
        const hydratedState = draftState ?? serverState;

        if (!cancelled) {
          setNodes(hydratedState.nodes);
          setEdges(hydratedState.edges);
          setViewport(hydratedState.viewport ?? DEFAULT_CANVAS_VIEWPORT);
          setIsHydrated(true);
          window.requestAnimationFrame(() => {
            hydrateLocalMediaNodes(hydratedState.nodes);
            refreshVisibleAssetUrlsRef.current(hydratedState.nodes);
          });
        }

        if (currentVersion > snapshotVersion) syncFromVersionRef.current(snapshotVersion);
      } catch {
        if (!cancelled) {
          setIsHydrated(true);
          setIsReadOnly(true);
          setSaveError("服务端画布加载失败，请刷新重试");
        }
      }
    }

    hydrate();
    return () => { cancelled = true; };
  }, [activeProjectId, hydrateLocalMediaNodes, loadProject, markAppliedVersion, resetAppliedVersion, router, setNodes, setEdges, setViewport, user?.id]);

  const localDraftSaveTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => {
    if (!isHydrated || !activeProjectId || isReadOnly) return;
    clearTimeout(localDraftSaveTimer.current);
    localDraftSaveTimer.current = setTimeout(() => {
      try {
        saveCanvas({ nodes, edges, viewport: getViewport() }, activeProjectId, user?.id ?? null);
      } catch {
      }
    }, 200);
    return () => clearTimeout(localDraftSaveTimer.current);
  }, [activeProjectId, edges, getViewport, isHydrated, isReadOnly, nodes, user?.id]);

  // Debounced save — only after hydration completes
  const saveTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => {
    if (!isHydrated || !activeProjectId) return;
    if (nodeDragActiveRef.current) return;
    clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => {
      try {
        const summary = summarizeCanvas(nodes);
        if (!isReadOnly && serverProjectId && canvasOperations.pendingOperationCount === 0) {
          setIsSavingSnapshot(true);
          const snapshotState = sanitizeCanvasStateForPersistence({
            nodes,
            edges,
            viewport: getViewport(),
          });
          saveSnapshot(serverProjectId, {
            nodes: snapshotState.nodes,
            edges: snapshotState.edges,
            viewport: snapshotState.viewport,
            baseVersion: lastAppliedVersionRef.current,
            clientId,
            ...summary,
          }).then((snapshot) => {
            markAppliedVersion(snapshot.version);
            setSaveError("");
          }).catch(() => {
            setSaveError("服务端画布保存失败，请稍后重试");
          }).finally(() => {
            setIsSavingSnapshot(false);
          });
          canvasApi.updateProject(serverProjectId, summary).catch(() => undefined);
        }
      } catch {
        setIsSavingSnapshot(false);
        setSaveError("服务端画布保存失败，请稍后重试");
      }
    }, CANVAS_SAVE_DEBOUNCE_MS);
    return () => clearTimeout(saveTimer.current);
  }, [activeProjectId, canvasOperations.pendingOperationCount, clientId, edges, getViewport, isHydrated, isReadOnly, markAppliedVersion, nodeDragCommitVersion, nodes, saveSnapshot, serverProjectId]);

  // --- Add nodes ---
  const addImageDraftNode = useCallback((position?: { x: number; y: number }) => {
    if (isReadOnly) return null;
    const center = position ?? screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `draft_${Date.now()}`;
    const newNode: AppNode = withCardNodeInteraction({
      id,
      type: "image",
      position: findOpenNodePosition(
        { x: center.x - 190, y: center.y - 150 },
        { width: 360, height: 340 },
        getNodes() as AppNode[]
      ),
      data: {
        imageId: id,
        projectId: serverProjectId,
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
    });
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, serverProjectId, setNodes]);

  const addPromptTemplateNode = useCallback((template: Pick<PromptTemplate, "id" | "title" | "prompt" | "width" | "height" | "mimeType" | "modelCode" | "modelName" | "aigcModelId" | "providerModel">) => {
    if (isReadOnly) return null;
    const center = screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `template_${template.id}_${Date.now()}`;
    const newNode: AppNode = withCardNodeInteraction({
      id,
      type: "image",
      position: findOpenNodePosition(
        { x: center.x - 190, y: center.y - 150 },
        { width: 360, height: 340 },
        getNodes() as AppNode[]
      ),
      data: {
        imageId: id,
        projectId: serverProjectId,
        fileName: template.title || "Template",
        dataUrl: "",
        mimeType: template.mimeType || "image/png",
        width: template.width,
        height: template.height,
        createdAt: new Date().toISOString(),
        kind: "draft",
        prompt: template.prompt,
        sourceTemplateId: template.id,
        modelId: template.aigcModelId ? String(template.aigcModelId) : template.modelCode || DEFAULT_PROMPT_DATA.modelId,
        modelCode: template.modelCode,
        providerModel: template.providerModel,
        modelName: template.modelName,
        aigcModelId: template.aigcModelId,
        params: { ...DEFAULT_PROMPT_DATA.params },
        status: "idle",
        taskId: null,
        errorMessage: null,
        elapsedMs: null,
      },
      selected: true,
    });
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, serverProjectId, setNodes]);

  const handleSelectTemplateFromLibrary = useCallback((template: PromptTemplate) => {
    const node = addPromptTemplateNode(template);
    if (node) {
      markPromptTemplateUsed(template.id).catch(() => undefined);
    }
  }, [addPromptTemplateNode]);

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
    const newNode: AppNode = withCardNodeInteraction({
      id,
      type: "sketch",
      position: findOpenNodePosition(
        { x: center.x - 150, y: center.y - 110 },
        { width: 300, height: 240 },
        getNodes() as AppNode[]
      ),
      data: sketchData,
      selected: true,
    });
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
      fileName: "Text",
      content: "",
      prompt: "",
      modelId: "Gemini 3.1 Flash Lite",
      status: "idle",
      errorMessage: null,
      width: 320,
      height: 260,
      createdAt: new Date().toISOString(),
    };
    const newNode: AppNode = withCardNodeInteraction({
      id,
      type: "text",
      position: findOpenNodePosition(
        { x: center.x - 160, y: center.y - 130 },
        { width: 320, height: 260 },
        getNodes() as AppNode[]
      ),
      data: textData,
      selected: true,
    });
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  const addPromptNode = useCallback((position?: { x: number; y: number }) => {
    if (isReadOnly) return null;
    const center = position ?? screenToFlowPosition({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    });
    const id = `prompt_${Date.now()}`;
    const currentNodes = getNodes() as AppNode[];
    const newNode: AppNode = withCardNodeInteraction({
      id,
      type: "prompt",
      position: findOpenNodePosition(
        center,
        { width: 280, height: 240 },
        currentNodes,
        { padding: 28, stepX: 150, stepY: 130 }
      ),
      data: {
        ...DEFAULT_PROMPT_DATA,
        params: { ...DEFAULT_PROMPT_DATA.params },
      },
      selected: true,
    });
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
      explicitMode: null,
      firstFrameEdgeId: null,
      lastFrameEdgeId: null,
      referenceImageOrder: [],
    };
    const newNode: AppNode = withCardNodeInteraction({
      id,
      type: "video",
      position: findOpenNodePosition(
        { x: center.x - 210, y: center.y - 130 },
        { width: 420, height: 448 },
        getNodes() as AppNode[]
      ),
      data: videoData,
      selected: true,
    });
    setNodes((nds) => [...nds.map((node) => ({ ...node, selected: false })), newNode]);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    return newNode;
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  useEffect(() => {
    if (!routeTemplateId || !isHydrated || isReadOnly) return;
    const templateId = Number(routeTemplateId);
    if (!Number.isFinite(templateId) || templateId <= 0) return;
    const exists = (getNodes() as AppNode[]).some((node) => (
      node.type === "image" && Number((node.data as Record<string, unknown>).sourceTemplateId) === templateId
    ));
    const cleanUrl = routeProjectId
      ? `/canvas?projectId=${encodeURIComponent(routeProjectId)}`
      : "/canvas";
    if (exists) {
      router.replace(cleanUrl);
      return;
    }
    let cancelled = false;
    getPromptTemplate(templateId)
      .then(async (template) => {
        if (cancelled) return;
        addPromptTemplateNode({
          id: template.id,
          title: template.title,
          prompt: template.prompt,
          width: template.width,
          height: template.height,
          mimeType: template.mimeType,
          modelCode: template.modelCode,
          modelName: template.modelName,
          aigcModelId: template.aigcModelId,
          providerModel: template.providerModel,
        });
        await markPromptTemplateUsed(template.id).catch(() => undefined);
        router.replace(cleanUrl);
      })
      .catch(() => {
        if (!cancelled) setSaveError("模板加载失败，请返回模板库重试");
      });
    return () => {
      cancelled = true;
    };
  }, [addPromptTemplateNode, getNodes, isHydrated, isReadOnly, routeProjectId, routeTemplateId, router]);

  const mergeSelectedNodesIntoGroup = useCallback((groupNode: AppNode) => {
    if (isReadOnly || groupNode.type !== "canvasGroup") return false;
    const currentNodes = getNodes() as AppNode[];
    const selectedNodes = currentNodes.filter((node) => node.selected);
    const targetGroup = getMergeTargetForSelectedNodes(selectedNodes, currentNodes);
    if (!targetGroup || targetGroup.id !== groupNode.id || targetGroup.type !== "canvasGroup") return false;

    const now = new Date().toISOString();
    const currentChildIds = (targetGroup.data as GroupNodeData).childNodeIds;
    const selectedItemIds = selectedNodes
      .filter((node) => node.type !== "canvasGroup")
      .map((node) => node.id);
    const mergedChildIds = Array.from(new Set([...currentChildIds, ...selectedItemIds]));
    const bounds = getPreviewCardBoundsForNodeIds(currentNodes, new Set(mergedChildIds));
    if (!bounds) return false;

    const padding = 18;
    const topLeft = screenToFlowPosition({ x: bounds.x - padding, y: bounds.y - padding });
    const bottomRight = screenToFlowPosition({ x: bounds.x + bounds.width + padding, y: bounds.y + bounds.height + padding });
    const nextData: GroupNodeData = {
      ...(targetGroup.data as GroupNodeData),
      childNodeIds: mergedChildIds,
      width: Math.max(120, bottomRight.x - topLeft.x),
      height: Math.max(96, bottomRight.y - topLeft.y),
      updatedAt: now,
    };

    setNodes((nds): AppNode[] => nds.map((node): AppNode => {
      if (node.id !== targetGroup.id) return { ...node, selected: false };
      return {
        ...node,
        type: "canvasGroup",
        position: topLeft,
        data: nextData,
        selected: true,
      };
    }));
    clearMultiSelection();
    canvasOperations.submitOperation("NODE_MOVE", {
      nodeId: targetGroup.id,
      position: topLeft,
    });
    canvasOperations.submitOperation("NODE_UPDATE_DATA", {
      nodeId: targetGroup.id,
      patch: {
        childNodeIds: nextData.childNodeIds,
        width: nextData.width,
        height: nextData.height,
        updatedAt: nextData.updatedAt,
      },
    });
    return true;
  }, [canvasOperations, clearMultiSelection, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

  const groupSelectedNodes = useCallback(() => {
    if (isReadOnly) return;
    const currentNodes = getNodes() as AppNode[];
    const allSelectedNodes = currentNodes.filter((node) => node.selected);
    const mergeTarget = getMergeTargetForSelectedNodes(allSelectedNodes, currentNodes);
    if (mergeTarget && mergeSelectedNodesIntoGroup(mergeTarget)) return;
    if (allSelectedNodes.some((node) => node.type === "canvasGroup")) return;

    const selectedNodes = allSelectedNodes.filter((node) => node.type !== "canvasGroup");
    if (!canGroupSelectedNodes(selectedNodes, currentNodes)) return;
    const selectedIds = new Set(selectedNodes.map((node) => node.id));
    const bounds = getSelectedPreviewCardBounds(selectedNodes);
    if (!bounds) return;

    const padding = 18;
    const topLeft = screenToFlowPosition({ x: bounds.x - padding, y: bounds.y - padding });
    const bottomRight = screenToFlowPosition({ x: bounds.x + bounds.width + padding, y: bounds.y + bounds.height + padding });
    const id = `group_${Date.now()}`;
    const now = new Date().toISOString();
    const groupData: GroupNodeData = {
      fileName: "Group",
      childNodeIds: selectedNodes.map((node) => node.id),
      width: Math.max(120, bottomRight.x - topLeft.x),
      height: Math.max(96, bottomRight.y - topLeft.y),
      createdAt: now,
      updatedAt: now,
    };
    const groupNode: AppNode = withCardNodeInteraction({
      id,
      type: "canvasGroup",
      position: topLeft,
      data: groupData,
      selected: true,
      zIndex: Math.min(-1, ...selectedNodes.map((node) => node.zIndex ?? 0)) - 1,
    });

    const firstSelectedIndex = currentNodes.findIndex((node) => selectedIds.has(node.id));
    const unselectedNodes: AppNode[] = currentNodes.map((node) => ({
      ...node,
      selected: false,
    }));
    const insertIndex = firstSelectedIndex >= 0 ? firstSelectedIndex : 0;
    setNodes([
      ...unselectedNodes.slice(0, insertIndex),
      groupNode,
      ...unselectedNodes.slice(insertIndex),
    ]);
    clearMultiSelection();
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(groupNode) });
  }, [canvasOperations, clearMultiSelection, getNodes, isReadOnly, mergeSelectedNodesIntoGroup, screenToFlowPosition, setNodes]);

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
      let keptLocalFallback = false;
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
              keptLocalFallback = true;
            }
            imageData = {
              ...imageData,
              projectId: activeProjectId,
            };
            await saveImage(imageData, mediaStoreScope);
            newNodes.push(withCardNodeInteraction({
              id: imageData.imageId,
              type: "image",
              position: findOpenNodePosition(
                preferred,
                { width: 220, height: 260 },
                [...existingNodes, ...newNodes],
                { padding: 28, stepX: 150, stepY: 130 }
              ),
              data: imageData,
            }));
            continue;
          }
          if (isAcceptedVideoFile(file)) {
            const { data, blob } = await fileToVideoNodeData(file);
            let videoData = await attachVideoAsset(file, data);
            videoData = {
              ...videoData,
              projectId: activeProjectId,
            };
            await saveVideo(videoData, blob, mediaStoreScope);
            newNodes.push(withCardNodeInteraction({
              id: videoData.videoId ?? `uploaded_video_${Date.now()}`,
              type: "video",
              position: findOpenNodePosition(
                preferred,
                { width: 420, height: 260 },
                [...existingNodes, ...newNodes],
                { padding: 28, stepX: 170, stepY: 140 }
              ),
              data: videoData,
            }));
          }
        } catch (error) {
          setPasteToast(error instanceof Error ? error.message : "素材上传失败");
          setTimeout(() => setPasteToast(""), 2500);
        }
      }
      if (newNodes.length > 0) {
        if (!activeProjectId) return;
        if (serverProjectId) {
          try {
            await Promise.all(newNodes.map((node) => bindUploadedNodeAsset(serverProjectId, node)));
          } catch (error) {
            setPasteToast(error instanceof Error ? error.message : "资产绑定失败");
            setTimeout(() => setPasteToast(""), 2500);
            return;
          }
        }
        setNodes((nds) => [...nds, ...newNodes]);
        for (const node of newNodes) {
          if (!isCanvasNodeSyncable(node)) continue;
          canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(node) });
        }
        if (keptLocalFallback) {
          setPasteToast("图片已添加到当前画布，但云端上传失败；刷新或协作端可能无法看到这张图。");
          setTimeout(() => setPasteToast(""), 3500);
        }
      }
    },
    [activeProjectId, canvasOperations, getNodes, isReadOnly, mediaStoreScope, screenToFlowPosition, serverProjectId, setNodes]
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

  useEffect(() => {
    async function handleVideoFrameCapture(event: Event) {
      if (isReadOnly) return;
      const detail = (event as CustomEvent<VideoFrameCaptureEventDetail>).detail;
      if (!detail?.sourceNodeId || (!detail.dataUrl && !detail.assetId && !detail.previewUrl)) return;

      const currentNodes = getNodes() as AppNode[];
      const sourceNode = currentNodes.find((node) => node.id === detail.sourceNodeId);
      if (!sourceNode) return;

      const sourceSize = getApproxNodeSize(sourceNode);
      const imageId = `frame_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
      const now = new Date().toISOString();
      let imageData: ImageNodeData = {
        imageId,
        projectId: activeProjectId,
        fileName: detail.fileName || "Video frame.png",
        dataUrl: detail.dataUrl ?? "",
        assetId: detail.assetId ?? null,
        assetVersionId: detail.assetVersionId ?? null,
        previewUrl: detail.previewUrl ?? null,
        assetUrlExpireTime: detail.assetUrlExpireTime ?? null,
        mimeType: detail.mimeType || "image/png",
        width: detail.width,
        height: detail.height,
        createdAt: now,
        kind: "uploaded",
        prompt: "",
        modelId: DEFAULT_PROMPT_DATA.modelId,
        params: { ...DEFAULT_PROMPT_DATA.params },
        status: "idle",
        taskId: null,
        errorMessage: null,
      };

      try {
        if (!detail.dataUrl) throw new Error("server asset frame");
        const file = await dataUrlToFile(detail.dataUrl, imageData.fileName, imageData.mimeType);
        imageData = await attachImageAsset(file, imageData);
      } catch {
        // Keep the local frame usable even if the asset upload path is unavailable.
      }

      await saveImage(imageData, mediaStoreScope);

      const newNode: AppNode = withCardNodeInteraction({
        id: imageId,
        type: "image",
        position: findOpenNodePosition(
          {
            x: sourceNode.position.x + sourceSize.width + 96,
            y: sourceNode.position.y,
          },
          { width: 360, height: 340 },
          currentNodes,
          { padding: 36, stepX: 180, stepY: 150 }
        ),
        data: imageData,
        selected: false,
      });

      if (serverProjectId && imageData.assetId) {
        await canvasApi.bindNodeAsset(serverProjectId, newNode.id, {
          assetId: imageData.assetId,
          assetVersionId: imageData.assetVersionId ?? null,
          usageType: "source",
        });
      }
      setNodes((nodes) => [...nodes, newNode]);
      canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(newNode) });
    }

    window.addEventListener("copse:video-frame-capture", handleVideoFrameCapture);
    return () => window.removeEventListener("copse:video-frame-capture", handleVideoFrameCapture);
  }, [activeProjectId, canvasOperations, getNodes, isReadOnly, mediaStoreScope, serverProjectId, setNodes]);

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
  const handleArrangeCanvas = useCallback(() => {
    if (isReadOnly) return;
    const currentNodes = getNodes() as AppNode[];
    const nextNodes = arrangeCanvasNodes(currentNodes, edges);
    if (!nextNodes) return;

    const movedNodes = nextNodes.filter((node) => {
      const previous = currentNodes.find((item) => item.id === node.id);
      return previous && (previous.position.x !== node.position.x || previous.position.y !== node.position.y);
    });
    if (movedNodes.length === 0) return;

    setNodes(nextNodes);
    clearMultiSelection();
    for (const node of movedNodes) {
      canvasOperations.submitOperation("NODE_MOVE", {
        nodeId: node.id,
        position: node.position,
      });
    }
    window.requestAnimationFrame(() => fitView({ padding: 0.24, duration: 260 }));
  }, [canvasOperations, clearMultiSelection, edges, fitView, getNodes, isReadOnly, setNodes]);
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
              : kind === "video"
                ? addVideoNode(position)
                : addPromptNode(position);

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
    [addImageDraftNode, addPromptNode, addSketchNode, addTextNode, addVideoNode, canvasOperations, closeCreateMenu, createMenu, getNodes, setEdges]
  );

  const handleConnectEnd = useCallback(
    (event: MouseEvent | TouchEvent, connectionState: FinalConnectionState) => {
      if (isReadOnly) return;
      window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
      if (connectionState.toHandle || !connectionState.from || !connectionState.fromNode?.id || !connectionState.fromHandle?.type) return;

      const point = getClientPoint(event);
      if (!point) return;

      const direction = connectionState.fromHandle.type === "target" ? "incoming" : "outgoing";
      const targetNode = findNodeAtFlowPoint(getNodes() as AppNode[], screenToFlowPosition(point), connectionState.fromNode.id);
      if (targetNode) {
        const connection =
          direction === "incoming"
            ? { source: targetNode.id, target: connectionState.fromNode.id }
            : { source: connectionState.fromNode.id, target: targetNode.id };
        if (isValidCanvasConnection(connection, getNodes() as AppNode[])) {
          const edge: AppEdge = {
            ...connection,
            id: `e-${connection.source}-${connection.target}-${Date.now()}`,
            type: "signal",
          };
          let shouldSubmit = false;
          setEdges((eds) => {
            if (eds.some((item) => isSameCanvasEdge(item, edge))) return eds;
            shouldSubmit = true;
            return addEdge(edge, eds);
          });
          if (shouldSubmit) canvasOperations.submitOperation("EDGE_CREATE", { edge });
        }
        closeCreateMenu();
        return;
      }

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
    [canvasOperations, closeCreateMenu, flowToScreenPosition, getNodes, isReadOnly, openCreateMenuAt, screenToFlowPosition, setEdges]
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

  if (!isHydrated) {
    return (
      <div className="flex h-full w-full items-center justify-center text-sm text-muted-gray">
        正在加载画布...
      </div>
    );
  }

  const createMenuOrigin = createMenu.originNodeId ? nodes.find((node) => node.id === createMenu.originNodeId) : undefined;
  const createMenuKinds = getCreateKindsForOrigin(createMenuOrigin?.type, createMenu.direction);
  const isCanvasEmpty = nodes.filter((node) => node.type !== "canvasGroup").length === 0;

  const handleRenameProject = (name: string) => {
    if (!serverProjectId || isReadOnly) return;
    const previousName = projectName;
    setProjectName(name);
    canvasApi.updateProject(serverProjectId, { name }).catch(() => {
      setProjectName(previousName);
      setSaveError("项目名称保存失败，请稍后重试");
    });
  };

  const addNodeFromDock = (kind: CreateNodeKind) => {
    const position = screenToFlowPosition({
      x: Math.min(window.innerWidth - 240, 360),
      y: window.innerHeight / 2,
    });
    if (kind === "text") addTextNode(position);
    if (kind === "image") addImageDraftNode(position);
    if (kind === "sketch") addSketchNode(position);
    if (kind === "video") addVideoNode(position);
    if (kind === "prompt") addPromptNode(position);
  };

  return (
    <div
      className="relative h-full w-full"
      onMouseMove={handleCanvasMouseMove}
      onContextMenu={handleContextMenu}
      onPointerDownCapture={(event) => {
        if (event.button !== 0) return;
        const target = event.target as HTMLElement;
        if (!target.classList.contains("react-flow__pane")) {
          selectionStartRef.current = null;
          selectionRectRef.current = null;
          return;
        }
        selectionStartRef.current = { x: event.clientX, y: event.clientY };
        selectionRectRef.current = { x: event.clientX, y: event.clientY, width: 0, height: 0 };
        clearMultiSelection();
      }}
      onPointerMoveCapture={(event) => {
        const start = selectionStartRef.current;
        if (!start) return;
        const selectionRect = {
          x: Math.min(start.x, event.clientX),
          y: Math.min(start.y, event.clientY),
          width: Math.abs(event.clientX - start.x),
          height: Math.abs(event.clientY - start.y),
        };
        selectionRectRef.current = selectionRect;
        if (selectionRafRef.current !== null) return;
        selectionRafRef.current = window.requestAnimationFrame(() => {
          selectionRafRef.current = null;
          const latestSelectionRect = selectionRectRef.current;
          if (!latestSelectionRect) return;
          setNodes((current) => applyPreviewCardSelection(current, latestSelectionRect));
        });
      }}
      onPointerUpCapture={() => {
        selectionStartRef.current = null;
        if (selectionRafRef.current !== null) {
          window.cancelAnimationFrame(selectionRafRef.current);
          selectionRafRef.current = null;
        }
      }}
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
      <CanvasProjectHeader
        projectName={projectName}
        syncState={syncState}
        readOnly={isReadOnly}
        onBack={() => router.push("/projects")}
        onRename={handleRenameProject}
      />

      <CanvasUtilityBar
        canShare={Boolean(serverProjectId)}
        onShare={() => setShareDialogOpen(true)}
      />

      <CanvasToolDock
        readOnly={isReadOnly || Boolean(referencePickerPromptId)}
        onAddNode={addNodeFromDock}
        onOpenTemplateLibrary={() => setTemplateLibraryOpen(true)}
      />

      <CanvasTemplateLibraryDialog
        open={templateLibraryOpen}
        onClose={() => setTemplateLibraryOpen(false)}
        onSelect={handleSelectTemplateFromLibrary}
      />

      <AnimatePresence>
        {isCanvasEmpty && !isReadOnly && !referencePickerPromptId && (
          <motion.div
            key="canvas-empty-hint"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 8 }}
            transition={{ duration: 0.18, ease: "easeOut" }}
            className="pointer-events-none absolute inset-0 z-[50] flex flex-col items-center justify-center gap-4 px-6"
          >
            <div className="pointer-events-auto flex flex-wrap items-center justify-center gap-2">
              <EmptyCanvasButton icon={<Video />} label={canvasT("nodes.video")} description={canvasT("empty.video")} onClick={() => addNodeFromDock("video")} />
              <EmptyCanvasButton icon={<ImagePlus />} label={canvasT("nodes.image")} description={canvasT("empty.image")} onClick={() => addNodeFromDock("image")} />
              <EmptyCanvasButton icon={<Type />} label={canvasT("nodes.text")} description={canvasT("empty.text")} onClick={() => addNodeFromDock("text")} />
              <EmptyCanvasButton icon={<PenLine />} label={canvasT("nodes.sketch")} description={canvasT("empty.sketch")} onClick={() => addNodeFromDock("sketch")} />
              <EmptyCanvasButton icon={<Sparkles />} label={canvasT("nodes.prompt")} description={canvasT("empty.prompt")} onClick={() => addNodeFromDock("prompt")} />
            </div>
            <p className="pointer-events-none flex items-center gap-1.5 text-xs text-muted-gray">
              <MousePointerClick className="size-3.5" />
              {canvasT("empty.hint")}
            </p>
          </motion.div>
        )}
      </AnimatePresence>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={CANVAS_NODE_TYPES}
        edgeTypes={CANVAS_EDGE_TYPES}
        minZoom={0.2}
        maxZoom={2}
        defaultViewport={DEFAULT_CANVAS_VIEWPORT}
        zoomOnScroll
        zoomActivationKeyCode={keyboardEditingNodeId ? null : ["Meta", "Control"]}
        zoomOnPinch
        panOnScroll
        panOnScrollMode={PanOnScrollMode.Free}
        snapToGrid={snapToGrid}
        snapGrid={[36, 36]}
        panOnDrag={false}
        selectionOnDrag
        selectionMode={SelectionMode.Partial}
        onSelectionEnd={handleSelectionEnd}
        zoomOnDoubleClick={false}
        deleteKeyCode={isReadOnly || keyboardEditingNodeId ? null : "Backspace"}
        selectionKeyCode={keyboardEditingNodeId ? null : "Shift"}
        multiSelectionKeyCode={keyboardEditingNodeId ? null : undefined}
        panActivationKeyCode={keyboardEditingNodeId ? null : "Space"}
        disableKeyboardA11y={Boolean(keyboardEditingNodeId)}
        fitView
        onlyRenderVisibleElements
        defaultEdgeOptions={{
          type: "signal",
        }}
        connectionLineComponent={CanvasConnectionLine}
        proOptions={{ hideAttribution: true }}
        onMove={(_, viewport) => {
          if (Math.abs(viewport.zoom - canvasZoomRef.current) < 0.005) return;
          canvasZoomRef.current = viewport.zoom;
          setCanvasZoom(viewport.zoom);
        }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onConnect={onConnect}
        onMoveEnd={scheduleVisibleAssetRefresh}
        onMoveStart={() => { closeContextMenu(); closeCreateMenu(); window.dispatchEvent(new CustomEvent("copse:canvas-interaction")); }}
        onPaneClick={() => {
          if (ignoreNextPaneClickRef.current) {
            ignoreNextPaneClickRef.current = false;
            return;
          }
          if (isCanvasEmpty && !isReadOnly) {
            addPromptNode();
          }
          closeContextMenu();
          closeCreateMenu();
          closeReferencePicker();
          window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
        }}
        nodesDraggable={!isReadOnly}
        nodesConnectable={!isReadOnly}
        elementsSelectable
        onNodeDragStart={() => {
          if (isReadOnly) return;
          nodeDragActiveRef.current = true;
          groupDragStartRef.current = Object.fromEntries(
            (getNodes() as AppNode[]).map((node) => [node.id, { ...node.position }])
          );
          clearMultiSelection();
          window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
        }}
        onNodeDragStop={() => {
          nodeDragActiveRef.current = false;
          groupDragStartRef.current = null;
          refreshMultiSelection();
          setNodeDragCommitVersion((version) => version + 1);
        }}
        onConnectStart={() => { if (isReadOnly) return; setPendingConnectionPreview(null); window.dispatchEvent(new CustomEvent("copse:canvas-interaction")); }}
        onConnectEnd={handleConnectEnd}
        isValidConnection={(connection) => !isReadOnly && isValidCanvasConnection(connection, nodes, edges)}
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

      {multiSelectionBounds && multiSelectionAction && (
        <MultiSelectionToolbar
          bounds={multiSelectionBounds}
          actionLabel={multiSelectionAction === "merge" ? "合并" : "分组"}
          onGroup={groupSelectedNodes}
        />
      )}

      <CanvasViewToolbar
        showMiniMap={showMiniMap}
        snapToGrid={snapToGrid}
        zoom={canvasZoom}
        onToggleMiniMap={() => setShowMiniMap((value) => !value)}
        onToggleSnapToGrid={() => setSnapToGrid((value) => !value)}
        onArrangeCanvas={handleArrangeCanvas}
        onResetView={handleToolbarResetView}
        onZoomChange={handleToolbarZoomChange}
      />

      {remoteSelectionHighlights.map((highlight) => (
        <div
          key={highlight.key}
          className="pointer-events-none absolute z-40 rounded-lg transition-[left,top,width,height] duration-100 ease-linear"
          style={{
            left: highlight.rect.x,
            top: highlight.rect.y,
            width: highlight.rect.width,
            height: highlight.rect.height,
            border: `2px ${highlight.editing ? "solid" : "dashed"} ${highlight.color}`,
            boxShadow: highlight.editing ? `0 0 0 3px ${highlight.color}33` : undefined,
          }}
        >
          <div
            className="absolute -top-6 left-0 flex items-center gap-1 whitespace-nowrap rounded-full px-2 py-0.5 text-[10px] font-medium text-white shadow-sm"
            style={{ backgroundColor: highlight.color }}
          >
            {highlight.editing && <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-white" />}
            {highlight.name}
            {highlight.editing ? " 正在编辑" : ""}
          </div>
        </div>
      ))}

      {Object.entries(remotePresences).map(([presenceClientId, presence]) => {
        const cursor = presence.screenCursor;
        if (!cursor) return null;
        const color = getPresenceColor(presenceClientId);
        const { name, avatar } = getPresenceDisplay(presence);
        return (
          <div
            key={presenceClientId}
            className="pointer-events-none absolute z-50 transition-[left,top] duration-100 ease-linear"
            style={{ left: cursor.x, top: cursor.y, color }}
          >
            <div
              className="h-0 w-0 border-y-[6px] border-l-[10px] border-y-transparent"
              style={{ borderLeftColor: color }}
            />
            <div
              className="mt-1 flex items-center gap-1.5 rounded-full pl-0.5 pr-2 py-0.5 text-[10px] font-medium text-white shadow-sm"
              style={{ backgroundColor: color }}
            >
              {avatar ? (
                <img src={avatar} alt={name} className="h-4 w-4 rounded-full object-cover ring-1 ring-white/60" />
              ) : (
                <span className="flex h-4 w-4 items-center justify-center rounded-full bg-white/25 text-[9px]">
                  {name.slice(0, 1)}
                </span>
              )}
              {name}
            </div>
          </div>
        );
      })}

      {Object.keys(remotePresences).length > 0 && (
        <div className="pointer-events-none absolute right-4 top-4 z-50 flex items-center gap-2 rounded-full border border-border-warm bg-background px-2 py-1 shadow-sm">
          <div className="flex -space-x-2">
            {Object.entries(remotePresences).slice(0, 5).map(([presenceClientId, presence]) => {
              const color = getPresenceColor(presenceClientId);
              const { name, avatar } = getPresenceDisplay(presence);
              return avatar ? (
                <img
                  key={presenceClientId}
                  src={avatar}
                  alt={name}
                  title={name}
                  className="h-6 w-6 rounded-full object-cover ring-2 ring-background"
                  style={{ boxShadow: `0 0 0 1px ${color}` }}
                />
              ) : (
                <div
                  key={presenceClientId}
                  title={name}
                  className="flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-medium text-white ring-2 ring-background"
                  style={{ backgroundColor: color }}
                >
                  {name.slice(0, 1)}
                </div>
              );
            })}
            {Object.keys(remotePresences).length > 5 && (
              <div className="flex h-6 w-6 items-center justify-center rounded-full bg-charcoal/10 text-[10px] font-medium text-charcoal/70 ring-2 ring-background">
                +{Object.keys(remotePresences).length - 5}
              </div>
            )}
          </div>
          <span className="text-xs text-charcoal/70">
            {Object.keys(remotePresences).length} 人在线协作
          </span>
        </div>
      )}

      {isReadOnly && (
        <div className="pointer-events-none absolute left-1/2 top-4 z-50 -translate-x-1/2 rounded-full border border-border-warm bg-background px-3 py-1.5 text-xs text-charcoal/70 shadow-sm">
          只读模式{projectRole ? ` · ${projectRole}` : ""}
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

export function CanvasFlowPage() {
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
