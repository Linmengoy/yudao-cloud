"use client";

import { useCallback } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow } from "@xyflow/react";
import { Boxes, Ungroup } from "lucide-react";
import type { GroupNodeData, GroupUngroupEventDetail, NodeDataPatchEventDetail } from "./types";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";

type GroupNodeProps = NodeProps<Node<GroupNodeData, "canvasGroup">>;

export function GroupNodeComponent({ id, data, selected }: GroupNodeProps) {
  const { setNodes } = useReactFlow();

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
      {selected ? (
        <div
          className="nodrag nowheel pointer-events-auto absolute left-1/2 z-30 flex -translate-x-1/2 items-center gap-1 rounded-full border border-border-warm bg-background p-1 shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          style={{ top: -62 }}
          onDoubleClick={(event) => event.stopPropagation()}
          onMouseDown={(event) => event.stopPropagation()}
        >
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
