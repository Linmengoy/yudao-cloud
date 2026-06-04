"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore } from "@xyflow/react";
import { Boxes, Rows3, LayoutGrid, Ungroup } from "lucide-react";
import type { GroupArrangeEventDetail, GroupNodeData, GroupUngroupEventDetail, NodeDataPatchEventDetail } from "./types";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";

type GroupNodeProps = NodeProps<Node<GroupNodeData, "canvasGroup">>;

export function GroupNodeComponent({ id, data, selected }: GroupNodeProps) {
  const { setNodes } = useReactFlow();
  const selectedNodeCount = useStore((s) => s.nodes.reduce((count, node) => count + (node.selected ? 1 : 0), 0));
  const zoom = useStore((s) => s.transform[2] || 1);
  const uiScale = 1 / zoom;
  const showGroupActions = selected && selectedNodeCount === 1;
  const arrangeMenuRef = useRef<HTMLDivElement>(null);
  const [arrangeMenuOpen, setArrangeMenuOpen] = useState(false);

  const updateData = useCallback(
    (patch: Partial<GroupNodeData>, options?: { flush?: boolean }) => {
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

  const ungroup = useCallback(() => {
    window.dispatchEvent(new CustomEvent<GroupUngroupEventDetail>("copse:group-ungroup", {
      detail: { groupId: id },
    }));
  }, [id]);

  const arrange = useCallback((mode: GroupArrangeEventDetail["mode"]) => {
    window.dispatchEvent(new CustomEvent<GroupArrangeEventDetail>("copse:group-arrange", {
      detail: { groupId: id, mode },
    }));
    setArrangeMenuOpen(false);
  }, [id]);

  useEffect(() => {
    if (!arrangeMenuOpen) return;

    function handlePointerDown(event: PointerEvent) {
      if (arrangeMenuRef.current?.contains(event.target as HTMLElement)) return;
      setArrangeMenuOpen(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setArrangeMenuOpen(false);
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [arrangeMenuOpen]);

  return (
    <div
      className="canvas-node-drag-handle pointer-events-auto relative rounded-2xl border border-dashed border-charcoal/25 bg-charcoal/[0.025] transition-[background-color,border-color,box-shadow] data-[selected=true]:border-blue-500/70 data-[selected=true]:bg-blue-500/[0.035] data-[selected=true]:shadow-[0_0_0_1px_rgba(59,130,246,0.18),0_12px_32px_rgba(59,130,246,0.08)] dark:border-white/25 dark:bg-white/[0.025] dark:data-[selected=true]:border-blue-300/75 dark:data-[selected=true]:bg-blue-300/[0.055]"
      data-node-preview-card
      data-node-id={id}
      data-selected={selected ? "true" : "false"}
      onContextMenuCapture={(event) => {
        event.preventDefault();
        event.stopPropagation();
      }}
      onContextMenu={(event) => {
        event.preventDefault();
        event.stopPropagation();
      }}
      style={{
        width: data.width,
        height: data.height,
      }}
    >
      {showGroupActions ? (
        <div
          ref={arrangeMenuRef}
          className="nodrag nowheel pointer-events-auto absolute left-1/2 z-30 flex items-center gap-1 rounded-full border border-border-warm bg-background p-1 shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          style={{
            top: -54,
            transform: `translateX(-50%) scale(${uiScale})`,
            transformOrigin: "bottom center",
          }}
          onDoubleClick={(event) => event.stopPropagation()}
          onMouseDown={(event) => event.stopPropagation()}
        >
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              setArrangeMenuOpen((open) => !open);
            }}
            className="flex h-8 items-center gap-1.5 rounded-full px-3 text-xs font-medium text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          >
            <LayoutGrid className="size-3.5" />
            整理
          </button>
          {arrangeMenuOpen ? (
            <div
              className="absolute left-1/2 top-full mt-2 min-w-[132px] -translate-x-1/2 rounded-xl border border-border-warm bg-background py-1 shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
              onMouseDown={(event) => event.stopPropagation()}
              onDoubleClick={(event) => event.stopPropagation()}
            >
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  arrange("horizontal");
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-xs font-medium text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
              >
                <Rows3 className="size-3.5" />
                横向整理
              </button>
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  arrange("grid");
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-xs font-medium text-muted-gray transition-colors hover:bg-muted hover:text-charcoal"
              >
                <LayoutGrid className="size-3.5" />
                网格整理
              </button>
            </div>
          ) : null}
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              ungroup();
            }}
            className="flex h-8 items-center gap-1.5 rounded-full px-3 text-xs font-medium text-muted-gray transition-colors hover:bg-muted hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          >
            <Ungroup className="size-3.5" />
            Ungroup
          </button>
        </div>
      ) : null}
      <CanvasNodeTitle left={8} maxWidth={data.width}>
        <Boxes className="size-4" />
        <EditableNodeTitle
          value={data.fileName}
          fallback="Group"
          onCommit={(fileName) => updateData({ fileName, updatedAt: new Date().toISOString() }, { flush: true })}
        />
      </CanvasNodeTitle>
    </div>
  );
}
