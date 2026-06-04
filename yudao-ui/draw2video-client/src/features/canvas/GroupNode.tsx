"use client";

import { useCallback } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow } from "@xyflow/react";
import { Boxes } from "lucide-react";
import type { GroupNodeData, NodeDataPatchEventDetail } from "./types";
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

  return (
    <div
      className="canvas-node-drag-handle pointer-events-auto relative rounded-2xl border border-dashed border-charcoal/25 bg-charcoal/[0.025] transition-colors data-[selected=true]:border-charcoal/55 data-[selected=true]:bg-charcoal/[0.035] dark:border-white/25 dark:bg-white/[0.025] dark:data-[selected=true]:border-white/55"
      data-node-preview-card
      data-node-id={id}
      data-selected={selected ? "true" : "false"}
      style={{
        width: data.width,
        height: data.height,
      }}
    >
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
