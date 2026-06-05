"use client";

import { useId } from "react";
import { BaseEdge, EdgeLabelRenderer, getBezierPath, useReactFlow, useStore, type EdgeProps } from "@xyflow/react";
import { Scissors } from "lucide-react";
import type { EdgeDeleteEventDetail } from "./types";

export function CanvasSignalEdge({
  id,
  source,
  sourceX,
  sourceY,
  target,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  selected,
}: EdgeProps) {
  const gradientId = useId().replace(/:/g, "");
  const { setEdges } = useReactFlow();
  const isConnectedToSelectedNode = useStore((store) =>
    store.nodes.some((node) => node.selected && (node.id === source || node.id === target))
  );
  const shouldAnimate = selected || isConnectedToSelectedNode;
  const gradientDx = targetX - sourceX;
  const gradientDy = targetY - sourceY;
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
    curvature: 0.42,
  });

  return (
    <>
      <defs>
        <linearGradient
          id={gradientId}
          gradientUnits="userSpaceOnUse"
          x1={sourceX}
          y1={sourceY}
          x2={targetX}
          y2={targetY}
        >
          <stop offset="0%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="36%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="50%" stopColor="var(--canvas-edge-signal)" stopOpacity="0.78" />
          <stop offset="64%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <stop offset="100%" stopColor="var(--canvas-edge-signal)" stopOpacity="0" />
          <animate
            attributeName="x1"
            values={`${sourceX - gradientDx};${targetX}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="y1"
            values={`${sourceY - gradientDy};${targetY}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="x2"
            values={`${sourceX};${targetX + gradientDx}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
          <animate
            attributeName="y2"
            values={`${sourceY};${targetY + gradientDy}`}
            dur="2.1s"
            repeatCount="indefinite"
          />
        </linearGradient>
      </defs>
      <BaseEdge
        path={edgePath}
        style={{
          stroke: selected ? "var(--canvas-edge-selected)" : "var(--canvas-edge-base)",
          strokeWidth: selected ? 2.4 : 1.8,
        }}
      />
      <path
        d={edgePath}
        fill="none"
        stroke="transparent"
        strokeWidth={18}
        className="cursor-pointer"
        onClick={(event) => {
          event.stopPropagation();
          setEdges((eds) => eds.map((edge) => ({ ...edge, selected: edge.id === id })));
        }}
      />
      {shouldAnimate && <path className="copse-signal-edge-gradient" d={edgePath} stroke={`url(#${gradientId})`} />}
      {selected ? (
        <EdgeLabelRenderer>
          <button
            type="button"
            className="nodrag nopan pointer-events-auto absolute flex size-10 items-center justify-center rounded-full border border-border-warm bg-background text-charcoal shadow-[rgba(0,0,0,0.16)_0px_4px_14px] transition-colors hover:bg-muted focus:outline-none focus:shadow-[rgba(0,0,0,0.18)_0px_4px_16px]"
            style={{
              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
            }}
            aria-label="解除关联"
            onClick={(event) => {
              event.stopPropagation();
              window.dispatchEvent(new CustomEvent<EdgeDeleteEventDetail>("copse:edge-delete", {
                detail: { edgeId: id },
              }));
            }}
          >
            <Scissors className="size-5" />
          </button>
        </EdgeLabelRenderer>
      ) : null}
    </>
  );
}
