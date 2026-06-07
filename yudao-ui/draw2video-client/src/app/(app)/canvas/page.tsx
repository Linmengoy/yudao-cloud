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
import type { AppNode, AppEdge, CanvasMember, CanvasPresence, CanvasProjectRole, EdgeDeleteEventDetail, GroupArrangeEventDetail, GroupNodeData, GroupUngroupEventDetail, ImageNodeData, NodeCreateMenuEventDetail, NodeDataPatchEventDetail, NodeEditingPresenceEventDetail, ReferencePickerEventDetail, SketchNodeData, TextNodeData, VideoNodeData } from "@/features/canvas/types";
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
import { filterSyncableNodeDataPatch, sanitizeNodeForCanvasOperation } from "@/features/canvas/canvas-syncable-data";
import { useCanvasServerStorage } from "@/features/canvas/use-canvas-server-storage";
import { useCanvasRealtime } from "@/features/canvas/use-canvas-realtime";
import { useCanvasOperations } from "@/features/canvas/use-canvas-operations";
import { saveImage, saveVideo } from "@/features/canvas/image-store";
import { fileToImageNodeData, fileToVideoNodeData, getFilesFromDrop, isAcceptedImageType, isAcceptedVideoFile } from "@/features/canvas/image-upload";
import { attachImageAsset, attachVideoAsset } from "@/features/canvas/canvas-asset-upload";
import { getMyAsset } from "@/features/assets/asset-api";
import { getAssetPreviewUrl } from "@/features/assets/asset-dictionaries";
import { useAuth } from "@/features/auth/auth-store";
import { ThemeToggle } from "@/features/theme/ThemeToggle";
import { NotificationBell } from "@/features/notifications/components/notification-bell";
import { formatCompactPoints } from "@/features/wallet/wallet-api";
import {
  isEditableElement,
  getImageFilesFromPasteEvent,
  getImageFilesFromClipboardAPI,
} from "@/features/canvas/clipboard";
import { CanvasContextMenu, type ContextMenuState } from "@/features/canvas/CanvasContextMenu";
import { findOpenNodePosition } from "@/features/canvas/positioning";
import { cn } from "@/lib/utils";
import Link from "next/link";
import { ArrowLeft, BookOpen, Boxes, ChevronRight, Folder, Globe, HelpCircle, ImagePlus, LogOut, MessageCircle, Palette, PenLine, Plus, Settings, Share2, Sparkles, Type, Video, Wallet, Map as MapIcon, Grid3X3, Scan } from "lucide-react";

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

const CANVAS_NODE_DRAG_HANDLE = ".canvas-node-drag-handle";
const TRANSPARENT_NODE_WRAPPER_STYLE = { pointerEvents: "none" as const };
const GROUP_LAYOUT_PADDING = 32;
const GROUP_LAYOUT_GAP = 48;

type CreateNodeKind = "text" | "image" | "sketch" | "video";
type LinkedCreateDirection = "incoming" | "outgoing";
type PendingConnectionPreview = {
  from: { x: number; y: number };
  to: { x: number; y: number };
  direction: LinkedCreateDirection;
};

type SelectionRectSnapshot = {
  x: number;
  y: number;
  width: number;
  height: number;
};

type MultiSelectionAction = "group" | "merge";

type PointerSnapshot = {
  x: number;
  y: number;
};

const CREATE_NODE_KINDS: CreateNodeKind[] = ["text", "image", "sketch", "video"];
const DEFAULT_CANVAS_VIEWPORT = { x: 110, y: 90, zoom: 0.78 };
const NODE_DATA_PATCH_DEBOUNCE_MS = 200;
const CANVAS_SAVE_DEBOUNCE_MS = 1500;
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

type CanvasSyncState = "saved" | "saving" | "offline" | "error";

type CanvasProjectHeaderProps = {
  projectName: string;
  syncState: CanvasSyncState;
  readOnly: boolean;
  onBack: () => void;
  onRename: (name: string) => void;
};

function CanvasProjectHeader({
  projectName,
  syncState,
  readOnly,
  onBack,
  onRename,
}: CanvasProjectHeaderProps) {
  const [draftName, setDraftName] = useState(projectName);
  const [editing, setEditing] = useState(false);

  const syncLabel =
    syncState === "error"
      ? "保存失败"
      : syncState === "offline"
        ? "离线"
        : syncState === "saving"
          ? "保存中"
          : "已保存";

  const syncClass =
    syncState === "error"
      ? "bg-destructive"
      : syncState === "offline"
        ? "bg-muted-gray"
        : syncState === "saving"
          ? "bg-amber-500"
          : "bg-green-500";

  function commitName() {
    const nextName = draftName.trim() || "未命名项目";
    setEditing(false);
    if (nextName !== projectName) onRename(nextName);
  }

  return (
    <div className="pointer-events-auto absolute left-4 top-4 z-[90] flex items-center gap-3">
      <button
        type="button"
        onClick={onBack}
        aria-label="返回项目库"
        title="返回项目库"
        className="flex size-10 items-center justify-center rounded-full border border-border-warm bg-background/95 text-charcoal shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.08)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] backdrop-blur-sm transition-colors hover:bg-muted focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
      >
        <ArrowLeft className="size-5" />
      </button>

      <div className="flex min-w-0 items-center gap-2 px-1 py-1">
        {editing ? (
          <input
            value={draftName}
            autoFocus
            onChange={(event) => setDraftName(event.currentTarget.value)}
            onBlur={commitName}
            onKeyDown={(event) => {
              if (event.key === "Enter") commitName();
              if (event.key === "Escape") {
                setDraftName(projectName);
                setEditing(false);
              }
            }}
            className="h-6 w-[180px] rounded-md border border-border-warm bg-background px-2 text-sm font-medium text-charcoal outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          />
        ) : (
          <button
            type="button"
            onClick={() => {
              if (!readOnly) {
                setDraftName(projectName);
                setEditing(true);
              }
            }}
            className="max-w-[220px] truncate text-sm font-medium text-charcoal"
            title={readOnly ? projectName : "点击修改项目名"}
          >
            {projectName}
          </button>
        )}

        <span className="flex items-center gap-1.5 text-xs text-muted-gray">
          <span className={cn("size-2 rounded-full", syncClass)} />
          {syncLabel}
        </span>
      </div>
    </div>
  );
}

type CanvasUtilityBarProps = {
  canShare: boolean;
  onShare: () => void;
};

function CanvasUtilityBar({ canShare, onShare }: CanvasUtilityBarProps) {
  const { wallet, user, logout } = useAuth();
  const credits = wallet ? formatCompactPoints(wallet.balance) : "0";
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!profileOpen) return;
    function handlePointerDown(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setProfileOpen(false);
    }
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [profileOpen]);

  return (
    <div className="pointer-events-auto absolute right-4 top-4 z-[90] flex items-center gap-2 rounded-full border border-border-warm bg-background/95 px-2 py-1.5 shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.08)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] backdrop-blur-sm">
      {canShare && (
        <button
          type="button"
          onClick={onShare}
          className="inline-flex size-9 items-center justify-center rounded-full text-charcoal transition-colors hover:bg-muted"
          aria-label="分享"
          title="分享"
        >
          <Share2 className="size-5" />
        </button>
      )}

      <Link
        href="/pricing"
        className="inline-flex size-9 items-center justify-center rounded-full text-charcoal transition-colors hover:bg-muted"
        aria-label="开会员"
        title="开会员"
      >
        <Sparkles className="size-5 text-violet-500" />
      </Link>

      <Link
        href="/wallet"
        className="inline-flex h-9 min-w-9 items-center justify-center gap-1.5 rounded-full px-2.5 text-xs font-medium text-charcoal transition-colors hover:bg-muted"
        title="余额 / 用量"
      >
        <Wallet className="size-5" />
        {credits}
      </Link>

      <NotificationBell className="size-9 rounded-full [&_svg]:size-5" />
      <ThemeToggle className="size-9 rounded-full p-0 [&_svg]:size-5" />

      <div className="relative" ref={profileRef}>
        <button
          type="button"
          onClick={() => setProfileOpen((value) => !value)}
          title={user?.nickname ?? "用户"}
          aria-label="用户菜单"
          aria-expanded={profileOpen}
          className="flex size-9 items-center justify-center rounded-full bg-charcoal text-xs font-medium text-off-white transition-opacity hover:opacity-80"
        >
          {user?.nickname?.[0] ?? "U"}
        </button>

        {profileOpen && (
          <div className="absolute right-0 top-full z-[220] mt-2 w-[340px] rounded-xl border border-border-warm bg-background shadow-lg">
            <div className="flex items-center gap-3 border-b border-border-warm px-4 py-4">
              <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-charcoal text-sm font-medium text-off-white">
                {user?.nickname?.[0] ?? "U"}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-charcoal">
                  {user?.nickname ?? "用户"}
                </p>
                {user?.mobile && (
                  <p className="truncate text-xs text-muted-gray">
                    {user.mobile.replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2")}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center justify-between border-b border-border-warm px-4 py-3">
              <div>
                <p className="text-xs text-muted-gray">Free 计划</p>
                <p className="text-sm font-medium text-charcoal">{credits}</p>
              </div>
              <Link
                href="/pricing"
                onClick={() => setProfileOpen(false)}
                className="inline-flex items-center gap-1 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-1.5 text-xs font-medium text-charcoal active:opacity-80"
              >
                升级
                <ChevronRight className="size-3" />
              </Link>
            </div>

            <div className="py-1">
              <CanvasProfileMenuLink href="/profile" icon={<Settings className="size-4" />} label="账户管理" onClick={() => setProfileOpen(false)} />
              <CanvasProfileMenuLink href="/wallet" icon={<Wallet className="size-4" />} label="钱包 / 用量" onClick={() => setProfileOpen(false)} />
              <CanvasProfileMenuLink href="#" icon={<BookOpen className="size-4" />} label="使用指南" onClick={() => setProfileOpen(false)} />
              <CanvasProfileMenuLink href="#" icon={<MessageCircle className="size-4" />} label="联系我们" onClick={() => setProfileOpen(false)} />
              <CanvasProfileMenuLink href="#" icon={<Globe className="size-4" />} label="简体中文" onClick={() => setProfileOpen(false)} />
            </div>

            <div className="border-t border-border-warm py-1">
              <button
                type="button"
                onClick={async () => {
                  if (!window.confirm("确定要退出登录吗？")) return;
                  setProfileOpen(false);
                  await logout();
                }}
                className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
              >
                <LogOut className="size-4" />
                退出登录
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function CanvasProfileMenuLink({
  href,
  icon,
  label,
  onClick,
}: {
  href: string;
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="flex items-center gap-3 px-4 py-2.5 text-sm text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
    >
      {icon}
      {label}
    </Link>
  );
}

type CanvasToolDockProps = {
  readOnly: boolean;
  onAddNode: (kind: CreateNodeKind) => void;
};

function CanvasToolDock({ readOnly, onAddNode }: CanvasToolDockProps) {
  const [menuOpen, setMenuOpen] = useState(false);

  if (readOnly) return null;

  function createNode(kind: CreateNodeKind) {
    onAddNode(kind);
    setMenuOpen(false);
  }

  return (
    <div
      className="pointer-events-auto absolute left-4 top-1/2 z-[90] -translate-y-1/2"
      onMouseLeave={() => setMenuOpen(false)}
    >
      <nav className="flex w-[58px] flex-col items-center gap-1 rounded-full border border-border-warm bg-background/95 px-[7px] py-2 shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.08)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] backdrop-blur-sm">
        <button
          type="button"
          onMouseEnter={() => setMenuOpen(true)}
          onClick={() => setMenuOpen((value) => !value)}
          className="flex size-11 items-center justify-center rounded-full bg-charcoal text-off-white transition-opacity hover:opacity-90"
          aria-expanded={menuOpen}
          aria-label={menuOpen ? "关闭添加节点菜单" : "添加节点"}
          title={menuOpen ? "关闭添加节点菜单" : "添加节点"}
        >
          <Plus
            className={cn(
              "size-5 transition-transform duration-200 ease-out",
              menuOpen && "rotate-45"
            )}
          />
        </button>
        <ToolDockButton label="画板" icon={<Palette className="size-5" />} />
        <ToolDockButton label="素材库" icon={<Folder className="size-5" />} />
        <ToolDockButton label="帮助" icon={<HelpCircle className="size-5" />} />
      </nav>

      <AnimatePresence>
        {menuOpen && (
          <>
            <div className="absolute left-[58px] top-0 h-full w-6" aria-hidden="true" />
            <motion.div
              initial={{ opacity: 0, x: -6, scale: 0.98 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              exit={{ opacity: 0, x: -6, scale: 0.98 }}
              transition={{ duration: 0.14, ease: "easeOut" }}
              className="absolute left-[76px] top-0 w-[300px] rounded-[28px] border border-border-warm bg-background/98 p-4 shadow-[0_18px_50px_rgba(28,28,28,0.16)] backdrop-blur-md"
            >
              <p className="px-2 pb-2 text-xs text-muted-gray">添加节点</p>
              <div className="space-y-1">
                <CreateNodeMenuButton icon={<Type className="size-5" />} title="文字" description="文本提示词、设定和旁白内容" onClick={() => createNode("text")} />
                <CreateNodeMenuButton icon={<PenLine className="size-5" />} title="画板" description="草图画板，用来规划镜头和构图" onClick={() => createNode("sketch")} />
                <CreateNodeMenuButton icon={<ImagePlus className="size-5" />} title="图片" description="图片生成和图像参考节点" onClick={() => createNode("image")} />
                <CreateNodeMenuButton icon={<Video className="size-5" />} title="视频" description="视频生成节点" onClick={() => createNode("video")} />
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}

function ToolDockButton({ label, icon }: { label: string; icon: React.ReactNode }) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className="flex size-11 items-center justify-center rounded-full text-charcoal transition-colors hover:bg-muted"
    >
      {icon}
    </button>
  );
}

function CreateNodeMenuButton({
  icon,
  title,
  description,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-left transition-colors hover:bg-muted"
    >
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-muted text-charcoal">
        {icon}
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-medium text-charcoal">{title}</span>
        <span className="block truncate text-xs text-muted-gray">{description}</span>
      </span>
    </button>
  );
}

type MultiSelectionToolbarProps = {
  bounds: SelectionRectSnapshot;
  actionLabel: string;
  onGroup: () => void;
};

function MultiSelectionToolbar({ bounds, actionLabel, onGroup }: MultiSelectionToolbarProps) {
  return (
    <div
      className="pointer-events-auto fixed z-[85] -translate-x-1/2 rounded-full border border-border-warm bg-background/95 p-1 shadow-[rgba(0,0,0,0.1)_0px_4px_12px] backdrop-blur-sm"
      style={{
        left: bounds.x + bounds.width / 2,
        top: Math.max(12, bounds.y - 46),
      }}
    >
      <button
        type="button"
        onClick={(event) => {
          event.stopPropagation();
          onGroup();
        }}
        className="flex h-8 items-center gap-1.5 rounded-full px-3 text-xs font-medium text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
      >
        <Boxes className="size-3.5" />
        {actionLabel}
      </button>
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

function getNodeAssetId(node: AppNode) {
  if (node.type !== "image" && node.type !== "video") return null;
  const data = node.data as ImageNodeData | VideoNodeData;
  return data.assetId ?? data.outputAssetId ?? null;
}

function withFreshAssetUrl(node: AppNode, url: string): AppNode {
  if (node.type === "video") {
    return {
      ...node,
      data: {
        ...node.data,
        assetId: getNodeAssetId(node),
        previewUrl: url,
        videoUrl: url,
      },
    } as AppNode;
  }
  if (node.type === "image") {
    return {
      ...node,
      data: {
        ...node.data,
        assetId: getNodeAssetId(node),
        previewUrl: url,
        outputPreviewUrl: url,
      },
    } as AppNode;
  }
  return node;
}

function defaultNodes(): AppNode[] {
  const id = "draft_default";
  return [
    withCardNodeInteraction({
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
    }),
  ];
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
    return withCardNodeInteraction({
      ...n,
      data: {
        videoId: typeof d.videoId === "string" ? d.videoId : undefined,
        fileName: typeof d.fileName === "string" ? d.fileName : "Video",
        mimeType: typeof d.mimeType === "string" ? d.mimeType : "video/mp4",
        assetId: typeof d.assetId === "number" ? d.assetId : typeof d.outputAssetId === "number" ? d.outputAssetId : null,
        assetVersionId: typeof d.assetVersionId === "number" ? d.assetVersionId : null,
        previewUrl: typeof d.previewUrl === "string" ? d.previewUrl : typeof d.outputPreviewUrl === "string" ? d.outputPreviewUrl : null,
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
        videoUrl: typeof d.videoUrl === "string" ? d.videoUrl : typeof d.previewUrl === "string" ? d.previewUrl : null,
        errorMessage: typeof d.errorMessage === "string" ? d.errorMessage : null,
        taskStatus: typeof d.taskStatus === "string" ? d.taskStatus : null,
        progress: typeof d.progress === "number" ? d.progress : null,
        outputAssetId: typeof d.outputAssetId === "number" ? d.outputAssetId : null,
        outputPreviewUrl: typeof d.outputPreviewUrl === "string" ? d.outputPreviewUrl : null,
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

function isValidCanvasConnection(connection: { source: string; target: string }, nodes: AppNode[]) {
  const source = nodes.find((n) => n.id === connection.source);
  const target = nodes.find((n) => n.id === connection.target);
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

function isSameCanvasEdge(left: AppEdge, right: AppEdge) {
  return left.source === right.source &&
    left.target === right.target &&
    (left.sourceHandle ?? null) === (right.sourceHandle ?? null) &&
    (left.targetHandle ?? null) === (right.targetHandle ?? null);
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
  return {
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
  const [showMiniMap, setShowMiniMap] = useState(false);
  const [snapToGrid, setSnapToGrid] = useState(false);
  const [canvasZoom, setCanvasZoom] = useState(DEFAULT_CANVAS_VIEWPORT.zoom);
  const [nodeDragCommitVersion, setNodeDragCommitVersion] = useState(0);
  const [keyboardEditingNodeId, setKeyboardEditingNodeId] = useState<string | null>(null);
  const [multiSelectionBounds, setMultiSelectionBounds] = useState<SelectionRectSnapshot | null>(null);
  const [multiSelectionAction, setMultiSelectionAction] = useState<MultiSelectionAction | null>(null);
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
  const storeApi = useStoreApi();

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

  const refreshMultiSelectionBounds = useCallback((sourceNodes?: AppNode[]) => {
    const currentNodes = sourceNodes ?? getNodes() as AppNode[];
    const selectedNodes = currentNodes.filter((node) => node.selected);
    const action: MultiSelectionAction | null = canGroupSelectedNodes(selectedNodes, currentNodes)
      ? "group"
      : getMergeTargetForSelectedNodes(selectedNodes, currentNodes)
        ? "merge"
        : null;
    window.requestAnimationFrame(() => {
      const bounds = getNodesSelectionViewportRect();
      setMultiSelectionAction(action);
      setMultiSelectionBounds(bounds && selectedNodes.length > 1 ? bounds : null);
    });
  }, [getNodes]);

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
        refreshMultiSelectionBounds(nextNodes);
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
        setMultiSelectionBounds(null);
        setMultiSelectionAction(null);
        return;
      }

      if (selectedNodes.length !== 1) {
        if (!selectionRect) refreshMultiSelectionBounds(getNodes() as AppNode[]);
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
      setMultiSelectionBounds(null);
      setMultiSelectionAction(null);
    });
  }, [getNodes, refreshMultiSelectionBounds, setNodes, storeApi]);

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
  const { createProject: createServerProject, loadProject, saveSnapshot } = useCanvasServerStorage();
  const canvasRealtime = useCanvasRealtime(serverProjectId, clientId, lastAppliedVersion);
  const canvasOperations = useCanvasOperations(serverProjectId, clientId, latestKnownVersion, setLatestKnownVersion, canvasRealtime.sendOperation, canvasRealtime.isConnected);
  const processedRealtimeMessageCountRef = useRef(0);
  const canvasZoomRef = useRef(DEFAULT_CANVAS_VIEWPORT.zoom);
  const [remotePresences, setRemotePresences] = useState<Record<string, RemoteCanvasPresence>>({});
  const lastPresenceSentAtRef = useRef(0);
  const editingNodeIdRef = useRef<string | null>(null);
  const nodeDataPatchTimersRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  const lastAppliedVersionRef = useRef(0);

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
    const mediaNodes = nodesToRefresh
      .map((node) => ({ nodeId: node.id, assetId: getNodeAssetId(node) }))
      .filter((item): item is { nodeId: string; assetId: number } => typeof item.assetId === "number");
    if (mediaNodes.length === 0) return;

    const entries = await Promise.all(mediaNodes.map(async ({ nodeId, assetId }) => {
      try {
        const asset = await getMyAsset(assetId);
        const url = getAssetPreviewUrl(asset);
        return url ? { nodeId, url } : null;
      } catch {
        return null;
      }
    }));
    const urlByNodeId = new Map(entries.filter((entry): entry is { nodeId: string; url: string } => Boolean(entry)).map((entry) => [entry.nodeId, entry.url]));
    if (urlByNodeId.size === 0) return;

    setNodes((nds) => nds.map((node) => {
      const url = urlByNodeId.get(node.id);
      return url ? withFreshAssetUrl(node, url) : node;
    }));
  }, [setNodes]);

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
      const node = migrateNode(payload.node as AppNode);
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
      if (typeof payload.assetId === "number") {
        getMyAsset(payload.assetId)
          .then((asset) => {
            const url = getAssetPreviewUrl(asset);
            if (!url) return;
            setNodes((nds) => nds.map((node) => node.id === payload.nodeId ? withFreshAssetUrl(node, url) : node));
          })
          .catch(() => undefined);
      }
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
    const migratedNodes = state.nodes.map(migrateNode);
    setNodes(migratedNodes);
    setEdges(state.edges.map(migrateEdge));
    if (state.viewport) setViewport(state.viewport);
    if (typeof snapshot?.version === "number") markAppliedVersion(snapshot.version);
    refreshAssetUrls(migratedNodes);
  }, [markAppliedVersion, refreshAssetUrls, setEdges, setNodes, setViewport]);

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
    function handleEdgeDelete(event: Event) {
      if (isReadOnly) return;
      const detail = (event as CustomEvent<EdgeDeleteEventDetail>).detail;
      if (!detail?.edgeId) return;
      setEdges((eds) => eds.filter((edge) => edge.id !== detail.edgeId));
      canvasOperations.submitOperation("EDGE_DELETE", {
        edgeId: detail.edgeId,
      });
    }

    window.addEventListener("copse:edge-delete", handleEdgeDelete);
    return () => window.removeEventListener("copse:edge-delete", handleEdgeDelete);
  }, [canvasOperations, isReadOnly, setEdges]);

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
      setMultiSelectionBounds(null);
      setMultiSelectionAction(null);
      canvasOperations.submitOperation("NODE_DELETE", {
        nodeId: detail.groupId,
      });
    }

    window.addEventListener("copse:group-ungroup", handleGroupUngroup);
    return () => window.removeEventListener("copse:group-ungroup", handleGroupUngroup);
  }, [canvasOperations, getNodes, isReadOnly, setEdges, setNodes]);

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

    const maxContentWidth = mode === "grid"
      ? Math.max(220, groupData.width - GROUP_LAYOUT_PADDING * 2)
      : Number.POSITIVE_INFINITY;
    let cursorX = 0;
    let cursorY = 0;
    let rowHeight = 0;
    let contentWidth = 0;
    const layout = new Map<string, { x: number; y: number; width: number; height: number }>();

    for (const { node, rect } of childRects) {
      if (mode === "grid" && cursorX > 0 && cursorX + rect.width > maxContentWidth) {
        cursorX = 0;
        cursorY += rowHeight + GROUP_LAYOUT_GAP;
        rowHeight = 0;
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
      if (node.id === groupNode.id) {
        return {
          ...node,
          type: "canvasGroup",
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
    setMultiSelectionBounds(null);
    setMultiSelectionAction(null);
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
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

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
          router.replace(`/canvas?projectId=${encodeURIComponent(id)}`);
          setActiveProjectId(id);
        })
        .catch(() => {
          setSaveError("服务端项目创建失败，请稍后重试");
          projectCreationRef.current = false;
        });
    }, 0);
    return () => window.clearTimeout(timer);
  }, [createServerProject, routeProjectId, router]);

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
            queueMicrotask(() => refreshMultiSelectionBounds(selectedNodes as AppNode[]));
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
        queueMicrotask(() => refreshMultiSelectionBounds(nextNodes));
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
    [canvasOperations, getNodes, isReadOnly, refreshMultiSelectionBounds, setNodes]
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

        if (!saved) {
          if (!cancelled) {
            setNodes(defaultNodes());
            setEdges([]);
            setViewport(DEFAULT_CANVAS_VIEWPORT);
            setIsHydrated(true);
          }
          if (currentVersion > snapshotVersion) syncFromVersion(snapshotVersion);
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

        if (!cancelled) {
          setNodes(migratedNodes);
          setEdges(migratedEdges);
          setViewport(saved.viewport ?? DEFAULT_CANVAS_VIEWPORT);
          setIsHydrated(true);
          refreshAssetUrls(migratedNodes);
        }

        if (currentVersion > snapshotVersion) syncFromVersion(snapshotVersion);
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
  }, [activeProjectId, loadProject, markAppliedVersion, refreshAssetUrls, resetAppliedVersion, router, setNodes, setEdges, setViewport, syncFromVersion]);

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
          saveSnapshot(serverProjectId, {
            nodes,
            edges,
            viewport: getViewport(),
            baseVersion: lastAppliedVersionRef.current,
            clientId,
            ...summary,
          }).then((snapshot) => {
            markAppliedVersion(snapshot.version);
            setSaveError("");
          }).catch(() => {
            setSaveError("服务端画布保存失败，请稍后重试");
            syncFromVersion(lastAppliedVersionRef.current);
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
  }, [activeProjectId, canvasOperations.pendingOperationCount, clientId, edges, getViewport, isHydrated, isReadOnly, markAppliedVersion, nodeDragCommitVersion, nodes, saveSnapshot, serverProjectId, syncFromVersion]);

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
    setMultiSelectionBounds(null);
    setMultiSelectionAction(null);
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
  }, [canvasOperations, getNodes, isReadOnly, screenToFlowPosition, setNodes]);

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
    setMultiSelectionBounds(null);
    setMultiSelectionAction(null);
    canvasOperations.submitOperation("NODE_CREATE", { node: sanitizeNodeForCanvasOperation(groupNode) });
  }, [canvasOperations, getNodes, isReadOnly, mergeSelectedNodesIntoGroup, screenToFlowPosition, setNodes]);

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
            let videoData = data;
            try {
              videoData = await attachVideoAsset(file, videoData);
            } catch {
            }
            await saveVideo(videoData, blob);
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
        setMultiSelectionBounds(null);
        setMultiSelectionAction(null);
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
      />

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
        onNodeDragStart={() => {
          if (isReadOnly) return;
          nodeDragActiveRef.current = true;
          groupDragStartRef.current = Object.fromEntries(
            (getNodes() as AppNode[]).map((node) => [node.id, { ...node.position }])
          );
          setMultiSelectionBounds(null);
          setMultiSelectionAction(null);
          window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
        }}
        onNodeDragStop={() => {
          nodeDragActiveRef.current = false;
          groupDragStartRef.current = null;
          refreshMultiSelectionBounds();
          setNodeDragCommitVersion((version) => version + 1);
        }}
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
