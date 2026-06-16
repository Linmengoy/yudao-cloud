"use client";

import { useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import Link from "next/link";
import { AnimatePresence, motion } from "motion/react";
import { ArrowLeft, BookOpen, Boxes, ChevronRight, Folder, Globe, Grid2X2, Grid3X3, HelpCircle, ImagePlus, LogOut, Map as MapIcon, MessageCircle, Palette, PenLine, Plus, Scan, Settings, Share2, Sparkles, Type, Video, Wallet } from "lucide-react";
import { useAuth } from "@/features/auth/auth-store";
import { NotificationBell } from "@/features/notifications/components/notification-bell";
import { ThemeToggle } from "@/features/theme/ThemeToggle";
import { formatCompactPoints } from "@/features/wallet/wallet-api";
import { cn } from "@/lib/utils";

export type CreateNodeKind = "text" | "image" | "sketch" | "video";

export type SelectionRectSnapshot = {
  x: number;
  y: number;
  width: number;
  height: number;
};

type CanvasViewToolbarProps = {
  showMiniMap: boolean;
  snapToGrid: boolean;
  zoom: number;
  onToggleMiniMap: () => void;
  onToggleSnapToGrid: () => void;
  onArrangeCanvas: () => void;
  onResetView: () => void;
  onZoomChange: (zoom: number) => void;
};

export function CanvasViewToolbar({
  showMiniMap,
  snapToGrid,
  zoom,
  onToggleMiniMap,
  onToggleSnapToGrid,
  onArrangeCanvas,
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
        title="一键整理画布"
        onClick={onArrangeCanvas}
        className={buttonClass}
      >
        <Grid2X2 className="size-5" strokeWidth={2} />
      </button>
      <button
        type="button"
        aria-pressed={snapToGrid}
        title={snapToGrid ? "关闭吸附网格" : "开启吸附网格"}
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

export type CanvasSyncState = "saved" | "saving" | "offline" | "error";

type CanvasProjectHeaderProps = {
  projectName: string;
  syncState: CanvasSyncState;
  readOnly: boolean;
  onBack: () => void;
  onRename: (name: string) => void;
};

export function CanvasProjectHeader({
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

export function CanvasUtilityBar({ canShare, onShare }: CanvasUtilityBarProps) {
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
              <CanvasProfileMenuLink href="/guide/" icon={<BookOpen className="size-4" />} label="使用指南" onClick={() => setProfileOpen(false)} />
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
  icon: ReactNode;
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

export function CanvasToolDock({ readOnly, onAddNode }: CanvasToolDockProps) {
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

function ToolDockButton({ label, icon }: { label: string; icon: ReactNode }) {
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
  icon: ReactNode;
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

export function MultiSelectionToolbar({ bounds, actionLabel, onGroup }: MultiSelectionToolbarProps) {
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

